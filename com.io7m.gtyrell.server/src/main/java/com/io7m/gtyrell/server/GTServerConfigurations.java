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
import com.io7m.gtyrell.filter.GTFilterCompilerException;
import com.io7m.gtyrell.filter.GTFilterCompilersType;
import com.io7m.gtyrell.filter.GTFilterProgram;
import com.io7m.gtyrell.github.GTGithubRepositories;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyException;
import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.junreachable.UnreachableCodeException;
import io.vavr.collection.List;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Server configuration values.
 */

public final class GTServerConfigurations
{
  private GTServerConfigurations()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Load a server configuration from the given properties.
   *
   * @param p         The properties
   * @param compilers A provider of compilers for filter rules
   *
   * @return A new server configuration
   *
   * @throws JPropertyException        On missing or malformed properties
   * @throws IOException               On I/O errors
   * @throws GTFilterCompilerException On filter program compilation errors
   */

  public static GTServerConfiguration fromProperties(
    final GTFilterCompilersType compilers,
    final Properties p)
    throws JPropertyException, IOException, GTFilterCompilerException
  {
    Objects.requireNonNull(compilers, "compilers");
    Objects.requireNonNull(p, "p");

    final var root = parseDirectory(p);
    final var git = parseGit(p);
    final var pause = parseDuration(p);

    List<GTRepositorySourceType> sources = List.empty();
    final var source_names_text =
      JProperties.getString(p, "com.io7m.gtyrell.server.repository_sources");
    final var source_names = source_names_text.split("\\s+");
    for (final var source_name : source_names) {
      final var source = parseSource(compilers, p, source_name);
      sources = sources.append(source);
    }

    final var dry_run =
      JProperties.getBooleanWithDefault(
        p, "com.io7m.gtyrell.server.dry_run", false);

    return GTServerConfiguration.of(root, sources, git, pause, dry_run);
  }

  private static GTRepositorySourceType parseSource(
    final GTFilterCompilersType compilers,
    final Properties p,
    final String source_name)
    throws JPropertyException, IOException, GTFilterCompilerException
  {
    Objects.requireNonNull(p, "p");
    Objects.requireNonNull(source_name, "source_name");

    final var type_key = String.format(
      "com.io7m.gtyrell.server.repository_source.%s.type", source_name);
    final var type = JProperties.getString(
      p, type_key);

    if (Objects.equals("github", type)) {
      final var user_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.user", source_name);
      final var password_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.password", source_name);
      final var filter_file_key = String.format(
        "com.io7m.gtyrell.server.repository_source.%s.filter", source_name);

      final var user =
        JProperties.getString(p, user_key);
      final var pass =
        JProperties.getString(p, password_key);
      final var filter_file =
        JProperties.getString(p, filter_file_key);

      final GTFilterProgram filter;
      try (var stream = Files.newInputStream(Paths.get(filter_file))) {
        final var compiler = compilers.createFor(
          URI.create(filter_file),
          stream);
        filter = compiler.compile();
      }

      return GTGithubRepositories.newSource(user, pass, filter);
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
    final var duration_text = JProperties.getString(
      p, "com.io7m.gtyrell.server.pause_duration");

    final var pattern = Pattern.compile("([0-9]+)h ([0-9]+)m ([0-9]+)s");
    final var matcher = pattern.matcher(duration_text);
    if (matcher.matches()) {
      final var hours =
        Duration.ofHours(Long.parseUnsignedLong(matcher.group(1)));
      final var minutes =
        Duration.ofMinutes(Long.parseUnsignedLong(matcher.group(2)));
      final var seconds =
        Duration.ofSeconds(Long.parseUnsignedLong(matcher.group(3)));
      final var total =
        hours.plus(minutes).plus(seconds);

      if (total.getSeconds() < 1L) {
        throw new JPropertyIncorrectType(
          "com.io7m.gtyrell.server.pause_duration: Duration is too small (must be at least 1 second)",
          new IllegalArgumentException());
      }
      return total;
    }

    throw new JPropertyIncorrectType(
      "com.io7m.gtyrell.server.pause_duration: Expected a duration of the form: " + pattern.pattern(),
      new IllegalArgumentException());
  }
}
