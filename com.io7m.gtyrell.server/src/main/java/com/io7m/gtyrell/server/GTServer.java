/*
 * Copyright © 2017 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.gtyrell.server;

import com.io7m.gtyrell.core.GTRepositoryGroupName;
import com.io7m.gtyrell.core.GTRepositoryGroupType;
import com.io7m.gtyrell.core.GTRepositoryName;
import com.io7m.gtyrell.core.GTRepositoryType;
import io.vavr.collection.Map;
import io.vavr.collection.SortedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * A server that mirrors a set of repository groups into a directory.
 */

public final class GTServer implements GTServerType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GTServer.class);
  }

  private final AtomicBoolean done;
  private final AtomicBoolean started;
  private final GTServerConfiguration config;
  private final GTServerMetricsBean metrics;
  private final ExecutorService executor;
  private volatile Instant timeSyncStart;
  private volatile Instant timeSyncNext;

  private GTServer(
    final GTServerConfiguration in_config)
  {
    this.config = Objects.requireNonNull(in_config, "Config");
    this.done = new AtomicBoolean(false);
    this.started = new AtomicBoolean(false);
    this.metrics = new GTServerMetricsBean();
    this.executor = Executors.newSingleThreadExecutor(r -> {
      final var thread = new Thread(r);
      thread.setName(String.format(
        "com.io7m.gtyrell.server[%d]",
        Long.valueOf(thread.getId()))
      );
      return thread;
    });

    this.timeSyncStart = Instant.now();
    this.timeSyncNext = this.timeSyncStart.plus(this.config.pauseDuration());
    this.updateSyncTime(this.timeSyncStart);
  }

  /**
   * Create a new server.
   *
   * @param config The server configuration
   *
   * @return A new server
   */

  public static GTServerType newServer(
    final GTServerConfiguration config)
  {
    Objects.requireNonNull(config, "config");

    return new GTServer(config);
  }

  private static File makeRepositoryName(
    final File directory,
    final GTRepositoryGroupName group,
    final GTRepositoryName name)
  {
    return new File(new File(directory, group.text()), name.text() + ".git");
  }

  @Override
  public void stop()
  {
    if (this.done.compareAndSet(false, true)) {
      LOG.debug("scheduling server shutdown");
      this.executor.shutdown();
    }
  }

  @Override
  public void run()
  {
    if (this.started.compareAndSet(false, true)) {
      if (this.done.get()) {
        throw new IllegalStateException(
          "server has been stopped, create a new server!");
      }

      LOG.debug("starting server");
      LOG.info("{} start", this.version());

      this.setupMetrics();

      this.executor.execute(() -> {
        while (!this.done.get()) {
          try {
            this.runOnce();
          } catch (final Throwable e) {
            LOG.error("crashed: ", e);
          }
        }
      });
    } else {
      throw new IllegalStateException("Server is already running!");
    }
  }

  private void setupMetrics()
  {
    try {
      final var server =
        ManagementFactory.getPlatformMBeanServer();
      final var objectName =
        new ObjectName("com.io7m.gtyrell:name=Metrics");

      server.registerMBean(this.metrics, objectName);
    } catch (final MalformedObjectNameException
      | InstanceAlreadyExistsException
      | MBeanRegistrationException
      | NotCompliantMBeanException e) {
      LOG.error("unable to register metrics bean: ", e);
    }
  }

  private String version()
  {
    final var p = this.getClass().getPackage();
    final var v = p.getImplementationVersion();
    if (v != null) {
      return "gtyrell " + v;
    }
    return "gtyrell";
  }

  private void runOnce()
  {
    LOG.debug("running sync");

    this.timeSyncStart = Instant.now();
    this.timeSyncNext = this.timeSyncStart.plus(this.config.pauseDuration());

    final var producers = this.config.producers();
    for (var index = 0; index < producers.size(); ++index) {
      if (this.done.get()) {
        LOG.debug("stopping server");
        return;
      }

      final var p =
        Objects.requireNonNull(producers.get(index), "producers.get(index)");

      LOG.debug("retrieving repository groups");
      final SortedMap<GTRepositoryGroupName, GTRepositoryGroupType> groups;

      try {
        groups = p.get(this.config.git());
        for (final var group : groups) {
          try {
            this.metrics.repositoryCountAdd(group._2.repositories().size());
            this.syncGroup(group._2);
          } catch (final Exception e) {
            LOG.error("error syncing group: {}: ", group._1.text(), e);
          }
        }
      } catch (final Exception e) {
        LOG.error("error retrieving repository groups: ", e);
        this.metrics.repositoryGroupSyncFailed();
      }
    }

    this.metrics.update();

    final var timeNow = Instant.now();
    LOG.debug("sync took {}", elapsedTime(this.timeSyncStart, timeNow));
    this.metrics.setRepositorySyncTimeSecondsLatest(this.timeSyncStart.until(timeNow, SECONDS));
    LOG.debug("sync completed, pausing");
    this.pauseUntilNextSync();
  }

  private void pauseUntilNextSync()
  {
    this.checkPauseTimeIsSane();

    while (true) {
      final var timeNow = Instant.now();
      if (timeNow.isAfter(this.timeSyncNext)) {
        break;
      }

      this.updateSyncTime(timeNow);

      try {
        Thread.sleep(1_000L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void checkPauseTimeIsSane()
  {
    final var timeNow = 
      Instant.now();
    final var pauseTime = 
      Duration.between(timeNow, this.timeSyncNext);

    if (pauseTime.toSeconds() < 60L) {
      LOG.warn("very little time left for pausing; pause duration may be too short for this workload");
      this.metrics.repositoryShortPause();
    }
  }

  private void updateSyncTime(
    final Instant timeNow)
  {
    this.metrics.setRepositorySyncWaitSecondsRemaining(
      Duration.between(timeNow, this.timeSyncNext).toSeconds()
    );
    this.metrics.setRepositorySyncTimeNext(
      this.timeSyncNext.atOffset(ZoneOffset.UTC)
        .format(ISO_OFFSET_DATE_TIME)
    );
  }

  private static String elapsedTime(
    final Instant time_then,
    final Instant time_now)
  {
    final var elapsed = time_then.until(time_now, SECONDS);
    final var seconds = Math.toIntExact(elapsed % 60L);
    final var new_elapsed = Math.toIntExact(elapsed / 60L);
    final var minutes = new_elapsed % 60;
    final var hours = new_elapsed / 60;

    return String.format(
      "%sh %sm %ss",
      Integer.valueOf(hours),
      Integer.valueOf(minutes),
      Integer.valueOf(seconds));
  }

  private void syncGroup(final GTRepositoryGroupType g)
  {
    final var group = g.groupName();
    LOG.debug("syncing repository group: {}", group.text());

    final Map<GTRepositoryName, GTRepositoryType> repositories = g.repositories();
    for (final var name : repositories.keySet()) {
      this.metrics.repositorySyncAttempted();

      if (this.done.get()) {
        LOG.debug("stopping server");
        return;
      }

      final var repos =
        Objects.requireNonNull(
          repositories.get(name).get(),
          "repositories.get(name).get()");

      LOG.debug("syncing {}", repos);
      try {
        final var output =
          makeRepositoryName(this.config.directory(), group, name);
        if (!this.config.dryRun()) {
          repos.update(output);
        } else {
          LOG.debug("not syncing due to dry run");
        }
        this.metrics.repositorySyncSucceeded();
      } catch (final IOException e) {
        LOG.error("error syncing {}: ", repos, e);
        this.metrics.repositorySyncFailed();
      }
    }
  }
}
