/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.gtyrell.tests;

import com.io7m.gtyrell.filter.GTFilterProgram;
import com.io7m.gtyrell.filter.GTFilterRule;
import com.io7m.gtyrell.filter.GTFilterRuleType;
import io.vavr.collection.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public final class GTFilterProgramTest
{
  private static final Logger LOG = LoggerFactory.getLogger(GTFilterProgramTest.class);

  @Test
  public void testEmptyExcludes()
  {
    final var program =
      GTFilterProgram.builder()
        .setCompiled(LocalDateTime.now())
        .setRules(List.empty())
        .build();

    Assertions.assertFalse(program.includes(LOG, "example/x"));
  }

  @Test
  public void testIncludesAll()
  {
    final var program =
      GTFilterProgram.builder()
        .setCompiled(LocalDateTime.now())
        .setRules(List.of(
          GTFilterRule.builder()
            .setKind(GTFilterRuleType.Kind.INCLUDE)
            .setPattern(Pattern.compile(".*"))
            .build()))
        .build();

    Assertions.assertTrue(program.includes(LOG, "example/x"));
  }

  @Test
  public void testExcludesButThenIncludes()
  {
    final var program =
      GTFilterProgram.builder()
        .setCompiled(LocalDateTime.now())
        .setRules(
          List.of(
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.EXCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build(),
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.INCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build()
          )
        )
        .build();

    Assertions.assertTrue(program.includes(LOG, "example/x"));
  }

  @Test
  public void testIncludesButThenExcludes()
  {
    final var program =
      GTFilterProgram.builder()
        .setCompiled(LocalDateTime.now())
        .setRules(
          List.of(
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.INCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build(),
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.EXCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build()
          )
        )
        .build();

    Assertions.assertFalse(program.includes(LOG, "example/x"));
  }

  @Test
  public void testExcludesButThenIncludesHalt()
  {
    final var program =
      GTFilterProgram.builder()
        .setCompiled(LocalDateTime.now())
        .setRules(
          List.of(
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.EXCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build(),
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.INCLUDE_AND_HALT)
              .setPattern(Pattern.compile(".*"))
              .build(),
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.EXCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build()
          )
        )
        .build();

    Assertions.assertTrue(program.includes(LOG, "example/x"));
  }

  @Test
  public void testIncludesButThenExcludesHalt()
  {
    final var program =
      GTFilterProgram.builder()
        .setCompiled(LocalDateTime.now())
        .setRules(
          List.of(
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.INCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build(),
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.EXCLUDE_AND_HALT)
              .setPattern(Pattern.compile(".*"))
              .build(),
            GTFilterRule.builder()
              .setKind(GTFilterRuleType.Kind.INCLUDE)
              .setPattern(Pattern.compile(".*"))
              .build()
          )
        )
        .build();

    Assertions.assertFalse(program.includes(LOG, "example/x"));
  }
}
