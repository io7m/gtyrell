/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final Timer timer;
  private final AtomicBoolean started;
  private final GTServerConfiguration config;

  private GTServer(
    final GTServerConfiguration in_config)
  {
    this.config = Objects.requireNonNull(in_config, "Config");
    this.done = new AtomicBoolean(false);
    this.started = new AtomicBoolean(false);
    this.timer = new Timer();
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
      this.timer.cancel();
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

      this.timer.scheduleAtFixedRate(
        new TimerTask()
        {
          @Override
          public void run()
          {
            GTServer.this.runOnce();
          }
        }, 0L, this.config.pauseDuration().toMillis());
    } else {
      throw new IllegalStateException("Server is already running!");
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

    final var time_then = Instant.now();

    final var producers =
      this.config.producers();
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
            this.syncGroup(group._2);
          } catch (final Exception e) {
            LOG.error("error syncing group: {}: ", group._1.text(), e);
          }
        }
      } catch (final Exception e) {
        LOG.error("error retrieving repository groups: ", e);
      }
    }

    final var time_now = Instant.now();
    LOG.debug("sync took {}", elapsedTime(time_then, time_now));
    LOG.debug("sync completed, pausing");
  }

  private static String elapsedTime(
    final Instant time_then,
    final Instant time_now)
  {
    final var elapsed = time_then.until(time_now, ChronoUnit.SECONDS);

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
      } catch (final IOException e) {
        LOG.error("error syncing {}: ", repos, e);
      }
    }
  }
}
