/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.gtyrell.filter;

import com.io7m.jlexing.core.LexicalPosition;
import io.vavr.collection.List;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * The default filter compiler implementation.
 */

public final class GTFilterCompilers implements GTFilterCompilersType
{
  private final Clock clock;

  private GTFilterCompilers(final Clock in_clock)
  {
    this.clock = Objects.requireNonNull(in_clock, "clock");
  }

  /**
   * Construct compilers.
   *
   * @param in_clock The system clock
   *
   * @return A compiler provider
   */

  public static GTFilterCompilersType create(
    final Clock in_clock)
  {
    return new GTFilterCompilers(in_clock);
  }

  @Override
  public GTFilterCompilerType createFor(
    final URI uri,
    final InputStream stream)
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");
    return new FilterCompiler(this.clock, uri, stream);
  }

  private static final class FilterCompiler implements GTFilterCompilerType
  {
    private final URI uri;
    private final InputStream stream;
    private final Clock clock;

    FilterCompiler(
      final Clock in_clock,
      final URI in_uri,
      final InputStream in_stream)
    {
      this.clock = Objects.requireNonNull(in_clock, "clock");
      this.uri = Objects.requireNonNull(in_uri, "uri");
      this.stream = Objects.requireNonNull(in_stream, "stream");
    }

    @Override
    public GTFilterProgram compile()
      throws GTFilterCompilerException
    {
      final var lines =
        new BufferedReader(new InputStreamReader(this.stream, StandardCharsets.UTF_8))
          .lines()
          .collect(Collectors.toList());

      var filters = List.<GTFilterRule>of();
      var errors = List.<GTFilterCompilationError>of();
      for (var line_number = 0; line_number < lines.size(); ++line_number) {
        final var line = lines.get(line_number).trim();
        if (line.startsWith("#")) {
          continue;
        }

        final String pattern;
        final GTFilterRuleType.Kind kind;
        if (line.startsWith("include-and-halt")) {
          kind = GTFilterRuleType.Kind.INCLUDE_AND_HALT;
          pattern = removePrefix(line, "include-and-halt");
        } else if (line.startsWith("include")) {
          kind = GTFilterRuleType.Kind.INCLUDE;
          pattern = removePrefix(line, "include");
        } else if (line.startsWith("exclude-and-halt")) {
          kind = GTFilterRuleType.Kind.EXCLUDE_AND_HALT;
          pattern = removePrefix(line, "exclude-and-halt");
        } else if (line.startsWith("exclude")) {
          kind = GTFilterRuleType.Kind.EXCLUDE;
          pattern = removePrefix(line, "exclude");
        } else {
          errors = errors.append(
            GTFilterCompilationError.builder()
              .setPosition(
                LexicalPosition.<URI>builder()
                  .setLine(line_number + 1)
                  .setColumn(0)
                  .setFile(this.uri)
                  .build())
              .setMessage("Expected an 'include' or 'exclude' rule, but received: " + line)
              .build());
          continue;
        }

        final var trimmed = pattern.trim();
        try {
          final var compiled = Pattern.compile(trimmed);
          filters = filters.append(
            GTFilterRule.builder()
              .setKind(kind)
              .setPattern(compiled)
              .build());
        } catch (final PatternSyntaxException e) {
          errors = errors.append(
            GTFilterCompilationError.builder()
              .setPosition(
                LexicalPosition.<URI>builder()
                  .setLine(line_number + 1)
                  .setColumn(0)
                  .setFile(this.uri)
                  .build())
              .setMessage("Invalid pattern: " + trimmed)
              .setException(e)
              .build());
        }
      }

      if (errors.isEmpty()) {
        return GTFilterProgram.builder()
          .setCompiled(LocalDateTime.now(this.clock))
          .setRules(filters)
          .build();
      }

      throw new GTFilterCompilerException("One or more compilation errors occurred", errors);
    }

    private static String removePrefix(
      final String line,
      final String prefix)
    {
      return line.substring(prefix.length());
    }
  }
}
