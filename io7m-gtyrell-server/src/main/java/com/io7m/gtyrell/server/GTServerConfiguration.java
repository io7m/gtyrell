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

import com.io7m.gtyrell.core.GTGitExecutable;
import com.io7m.gtyrell.core.GTGitExecutableType;
import com.io7m.gtyrell.core.GTRepositorySourceType;
import com.io7m.gytrell.github.GTGithubRepositories;
import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyException;
import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.jproperties.JPropertyNonexistent;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.MutablePeriod;
import org.joda.time.format.PeriodFormatterBuilder;
import org.joda.time.format.PeriodParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Server configuration values.
 */

public final class GTServerConfiguration
{
  private final File                         directory;
  private final List<GTRepositorySourceType> producers;
  private final GTGitExecutableType          git;
  private final Duration                     pause_duration;

  private GTServerConfiguration(
    final File in_directory,
    final List<GTRepositorySourceType> in_producers,
    final GTGitExecutableType in_git,
    final Duration in_pause_duration)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.producers = NullCheck.notNull(in_producers);
    this.git = NullCheck.notNull(in_git);
    this.pause_duration = NullCheck.notNull(in_pause_duration);
  }

  /**
   * Load a server configuration from the given properties.
   *
   * @param p The properties
   *
   * @return A new server configuration
   *
   * @throws JPropertyException On missing or malformed properties
   */

  public static GTServerConfiguration fromProperties(final Properties p)
    throws JPropertyException
  {
    NullCheck.notNull(p);

    final File in_directory = GTServerConfiguration.parseDirectory(p);
    final GTGitExecutableType in_git = GTServerConfiguration.parseGit(p);
    final Duration in_pause = GTServerConfiguration.parseDuration(p);

    final List<GTRepositorySourceType> in_sources = new ArrayList<>(8);
    final String source_names_text =
      JProperties.getString(p, "com.io7m.gtyrell.server.repository_sources");
    final String[] source_names = source_names_text.split("\\s+");
    for (final String source_name : source_names) {
      final GTRepositorySourceType source =
        GTServerConfiguration.parseSource(p, source_name);
      in_sources.add(source);
    }

    return new GTServerConfiguration(
      in_directory, in_sources, in_git, in_pause);
  }

  private static GTRepositorySourceType parseSource(
    final Properties p,
    final String source_name)
    throws JPropertyException
  {
    NullCheck.notNull(p);
    NullCheck.notNull(source_name);

    final String type_key = String.format(
      "com.io7m.gtyrell.server.repository_source.%s.type", source_name);
    final String type = JProperties.getString(
      p, type_key);

    if ("github".equals(type)) {
      final String user_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.user", source_name);
      final String password_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.password", source_name);
      final String user = JProperties.getString(p, user_key);
      final String pass = JProperties.getString(p, password_key);
      return GTGithubRepositories.newSource(user, pass);
    }

    throw new JPropertyException(
      String.format(
        "%s: unsupported repository source type '%s'", type_key, type));
  }

  private static File parseDirectory(final Properties p)
    throws JPropertyNonexistent
  {
    return new File(
      JProperties.getString(
        p, "com.io7m.gtyrell.server.directory"));
  }

  private static GTGitExecutableType parseGit(final Properties p)
    throws JPropertyNonexistent
  {
    return GTGitExecutable.newExecutable(
      new File(
        JProperties.getString(
          p, "com.io7m.gtyrell.server.git_executable")));
  }

  private static Duration parseDuration(final Properties p)
    throws JPropertyNonexistent, JPropertyIncorrectType
  {
    final PeriodFormatterBuilder b = new PeriodFormatterBuilder();
    b.appendHours().appendSuffix("h ");
    b.appendMinutes().appendSuffix("m ");
    b.appendSeconds().appendSuffix("s ");

    final PeriodParser parser = b.toParser();
    final String duration_text = JProperties.getString(
      p, "com.io7m.gtyrell.server.pause_duration");

    final MutablePeriod period = new MutablePeriod();
    parser.parseInto(period, duration_text, 0, Locale.getDefault());
    final Duration in_pause = period.toDurationFrom(new DateTime(0L));

    if (in_pause.getMillis() < 1L) {
      throw new JPropertyIncorrectType(
        "com.io7m.gtyrell.server.pause_duration: Duration is too small");
    }
    return in_pause;
  }

  /**
   * @return The root directory inside which new directories will be created
   */

  public File getDirectory()
  {
    return this.directory;
  }

  /**
   * @return The git executable
   */

  public GTGitExecutableType getGit()
  {
    return this.git;
  }

  /**
   * @return The duration of time that the server will pause between
   * synchronization attempts
   */

  public Duration getPauseDuration()
  {
    return this.pause_duration;
  }

  /**
   * @return The list of repository group producers
   */

  public List<GTRepositorySourceType> getProducers()
  {
    return this.producers;
  }
}
