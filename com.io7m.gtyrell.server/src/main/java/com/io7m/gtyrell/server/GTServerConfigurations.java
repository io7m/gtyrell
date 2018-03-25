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

import com.io7m.gtyrell.core.GTGitExecutable;
import com.io7m.gtyrell.core.GTGitExecutableType;
import com.io7m.gtyrell.core.GTRepositorySourceType;
import com.io7m.gtyrell.github.GTGithubRepositories;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyException;
import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.junreachable.UnreachableCodeException;
import io.vavr.collection.List;

import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server configuration values.
 */

public final class GTServerConfigurations
{
  /**
   * A pattern against which repositories will be matched. If the pattern
   * matches the repository name, the repository will be included in the set
   * of repositories to be updated/cloned. Inclusions occur <i>before</i> exclusions.
   *
   * @return The default inclusion pattern
   */

  private static Pattern inclusionPatternDefault()
  {
    return Pattern.compile(".*", Pattern.UNICODE_CHARACTER_CLASS);
  }

  /**
   * A pattern against which repositories will be matched. If the pattern
   * matches the repository name, the repository will be ignored and not
   * updated/cloned. Exclusions occur <i>after</i> inclusions.
   *
   * @return The default exclusion pattern
   */

  private static Pattern exclusionPatternDefault()
  {
    return Pattern.compile("^$", Pattern.UNICODE_CHARACTER_CLASS);
  }

  private GTServerConfigurations()
  {
    throw new UnreachableCodeException();
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

  public static GTServerConfiguration fromProperties(
    final Properties p)
    throws JPropertyException
  {
    Objects.requireNonNull(p, "p");

    final File root = parseDirectory(p);
    final GTGitExecutableType git = parseGit(p);
    final Duration pause = parseDuration(p);

    List<GTRepositorySourceType> sources = List.empty();
    final String source_names_text =
      JProperties.getString(p, "com.io7m.gtyrell.server.repository_sources");
    final String[] source_names = source_names_text.split("\\s+");
    for (final String source_name : source_names) {
      final GTRepositorySourceType source = parseSource(p, source_name);
      sources = sources.append(source);
    }

    final boolean dry_run =
      JProperties.getBooleanOptional(
        p, "com.io7m.gtyrell.server.dry_run", false);

    return GTServerConfiguration.of(root, sources, git, pause, dry_run);
  }

  private static GTRepositorySourceType parseSource(
    final Properties p,
    final String source_name)
    throws JPropertyException
  {
    Objects.requireNonNull(p, "p");
    Objects.requireNonNull(source_name, "source_name");

    final String type_key = String.format(
      "com.io7m.gtyrell.server.repository_source.%s.type", source_name);
    final String type = JProperties.getString(
      p, type_key);

    if (Objects.equals("github", type)) {
      final String user_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.user", source_name);
      final String password_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.password", source_name);
      final String inclusion_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.include", source_name);
      final String exclusion_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.exclude", source_name);

      final String user =
        JProperties.getString(p, user_key);
      final String pass =
        JProperties.getString(p, password_key);
      final String include =
        JProperties.getStringOptional(
          p, inclusion_key, inclusionPatternDefault().pattern());
      final String exclude =
        JProperties.getStringOptional(
          p, exclusion_key, exclusionPatternDefault().pattern());

      return GTGithubRepositories.newSource(
        user,
        pass,
        Pattern.compile(include),
        Pattern.compile(exclude));
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
    final String duration_text = JProperties.getString(
      p, "com.io7m.gtyrell.server.pause_duration");

    final Pattern pattern = Pattern.compile("([0-9]+)h ([0-9]+)m ([0-9]+)s");
    final Matcher matcher = pattern.matcher(duration_text);
    if (matcher.matches()) {
      final Duration hours =
        Duration.ofHours(Long.parseUnsignedLong(matcher.group(1)));
      final Duration minutes =
        Duration.ofMinutes(Long.parseUnsignedLong(matcher.group(2)));
      final Duration seconds =
        Duration.ofSeconds(Long.parseUnsignedLong(matcher.group(3)));
      final Duration total =
        hours.plus(minutes).plus(seconds);

      if (total.getSeconds() < 1L) {
        throw new JPropertyIncorrectType(
          "com.io7m.gtyrell.server.pause_duration: Duration is too small (must be at least 1 second)");
      }
      return total;
    }

    throw new JPropertyIncorrectType(
      "com.io7m.gtyrell.server.pause_duration: Expected a duration of the form: " + pattern.pattern());
  }
}
