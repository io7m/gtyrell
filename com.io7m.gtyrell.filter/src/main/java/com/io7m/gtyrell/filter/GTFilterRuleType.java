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

import com.io7m.gtyrell.core.GTImmutableStyleType;
import org.immutables.value.Value;

import java.util.regex.Pattern;

/**
 * A single filter rule.
 */

@GTImmutableStyleType
@Value.Immutable
public interface GTFilterRuleType
{
  /**
   * The kind of filter rule.
   */

  enum Kind
  {
    /**
     * The filter rule specifies an include.
     */

    INCLUDE,

    /**
     * The filter rule specifies an exclude.
     */

    EXCLUDE,

    /**
     * The filter rule specifies an include that immediately halts evaluation.
     */

    INCLUDE_AND_HALT,

    /**
     * The filter rule specifies an exclude that immediately halts evaluation.
     */

    EXCLUDE_AND_HALT
  }

  /**
   * @return The filter rule kind
   */

  @Value.Parameter
  Kind kind();

  /**
   * @return The filter pattern
   */

  @Value.Parameter
  Pattern pattern();
}
