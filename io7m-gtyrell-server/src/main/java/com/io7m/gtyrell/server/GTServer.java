/*
 * Copyright © 2015 <code@io7m.com> http://io7m.com
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

import com.io7m.gtyrell.core.GTGitExecutableType;
import com.io7m.gtyrell.core.GTRepositoryGroupName;
import com.io7m.gtyrell.core.GTRepositoryGroupType;
import com.io7m.gtyrell.core.GTRepositoryName;
import com.io7m.gtyrell.core.GTRepositorySourceType;
import com.io7m.jnull.NullCheck;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
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

  private final File directory;
  private final List<GTRepositorySourceType> producers;
  private final GTGitExecutableType git;
  private final AtomicBoolean done;
  private final Duration pause_duration;
  private final Timer timer;
  private final AtomicBoolean started;

  private GTServer(
    final GTGitExecutableType in_git,
    final File in_directory,
    final List<GTRepositorySourceType> in_producers,
    final Duration in_pause_duration)
  {
    this.git = NullCheck.notNull(in_git);
    this.directory = NullCheck.notNull(in_directory);
    this.producers = NullCheck.notNull(in_producers);
    this.pause_duration = NullCheck.notNull(in_pause_duration);
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
    NullCheck.notNull(config);

    return new GTServer(
      config.getGit(),
      config.getDirectory(),
      config.getProducers(),
      config.getPauseDuration());
  }

  private static File makeRepositoryName(
    final File directory,
    final GTRepositoryGroupName group,
    final GTRepositoryName name)
  {
    return new File(
      new File(directory, group.toString()), name.toString() + ".git");
  }

  @Override public void stop()
  {
    if (this.done.compareAndSet(false, true)) {
      GTServer.LOG.debug("scheduling server shutdown");
      this.timer.cancel();
    }
  }

  @Override public void run()
  {
    if (this.started.compareAndSet(false, true)) {
      if (this.done.get()) {
        throw new IllegalStateException(
          "server has been stopped, create a new server!");
      }

      GTServer.LOG.debug("starting server");

      this.timer.scheduleAtFixedRate(
        new TimerTask()
        {
          @Override public void run()
          {
            GTServer.this.runOnce();
          }
        }, 0L, this.pause_duration.getMillis());
    } else {
      throw new IllegalStateException("Server is already running!");
    }
  }

  void runOnce()
  {
    GTServer.LOG.debug("running sync");

    for (int index = 0; index < this.producers.size(); ++index) {
      if (this.done.get()) {
        GTServer.LOG.debug("stopping server");
        return;
      }

      final GTRepositorySourceType p =
        NullCheck.notNull(this.producers.get(index));

      GTServer.LOG.debug("retrieving repository group");

      final GTRepositoryGroupType g;
      try {
        g = p.getRepositoryGroup();
      } catch (final IOException e) {
        GTServer.LOG.error("error syncing group: ", e);
        continue;
      }

      this.syncGroup(g);
    }

    GTServer.LOG.debug("sync completed, pausing");
  }

  private void syncGroup(final GTRepositoryGroupType g)
  {
    final GTRepositoryGroupName group = g.getGroupName();
    GTServer.LOG.debug("syncing repository group: {}", group);

    final Map<GTRepositoryName, URI> repositories = g.getRepositories();
    for (final GTRepositoryName name : repositories.keySet()) {
      if (this.done.get()) {
        GTServer.LOG.debug("stopping server");
        return;
      }

      final URI url = NullCheck.notNull(repositories.get(name));
      GTServer.LOG.debug("syncing {}/{}: {}", group, name, url);

      try {
        final File repos =
          GTServer.makeRepositoryName(this.directory, group, name);
        if (repos.isDirectory()) {
          this.git.fetch(repos);
        } else {
          final File parent = repos.getParentFile();
          if (parent.mkdirs() == false) {
            if (parent.isDirectory() == false) {
              throw new IOException(
                String.format(
                  "Not a directory: %s", parent));
            }
          }

          this.git.clone(url, repos);
        }
      } catch (final IOException e) {
        GTServer.LOG.error(
          "error updating repository {}/{} @ {}: ",
          group,
          name,
          url,
          e);
      }
    }
  }
}
