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


package com.io7m.gtyrell.filter;

import com.io7m.gtyrell.core.GTImmutableStyleType;
import io.vavr.collection.List;
import org.immutables.value.Value;
import org.slf4j.Logger;

import java.time.LocalDateTime;

/**
 * A compiled filter program.
 */

@GTImmutableStyleType
@Value.Immutable
public interface GTFilterProgramType
{
  /**
   * @return The time that this program was compiled
   */

  LocalDateTime compiled();

  /**
   * @return The filter rules
   */

  List<GTFilterRule> rules();

  /**
   * Determine if a repository name is included by the given rules. By default, no repositories are
   * included, so a program with no rules automatically excludes all repositories.
   *
   * @param logger The logger used for debug messages
   * @param name   The repository name
   *
   * @return {@code true} if the given repository is permitted by the filter rules
   */

  default boolean includes(
    final Logger logger,
    final String name)
  {
    var included = false;

    EVALUATION: for (final var rule : this.rules()) {
      final var matches = rule.pattern().matcher(name).matches();
      if (logger.isDebugEnabled()) {
        logger.debug(
          "filter: (rule {} {}) {} {}",
          rule.kind(),
          rule.pattern().pattern(),
          name,
          matches ? "matches" : "does not match");
      }

      switch (rule.kind()) {
        case INCLUDE: {
          if (matches) {
            included = true;
          }
          break;
        }
        case EXCLUDE: {
          if (matches) {
            included = false;
          }
          break;
        }
        case INCLUDE_AND_HALT: {
          if (matches) {
            included = true;
            break EVALUATION;
          }
          break;
        }
        case EXCLUDE_AND_HALT: {
          if (matches) {
            included = false;
            break EVALUATION;
          }
          break;
        }
      }
    }

    logger.debug("filter: result {} -> {}", name, Boolean.valueOf(included));
    return included;
  }
}
