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

package com.io7m.gtyrell.core;

import com.io7m.jnull.NullCheck;
import org.valid4j.Assertive;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A repository name. For example: {@code gtyrell}.
 */

public final class GTRepositoryName
{
  private static final Pattern PATTERN;
  private static final String  PATTERN_TEXT;

  static {
    PATTERN_TEXT = "[\\p{Alnum}_\\-]+[\\p{Alnum}_\\-\\.]*";
    PATTERN = NullCheck.notNull(
      Pattern.compile(
        GTRepositoryName.PATTERN_TEXT, Pattern.UNICODE_CHARACTER_CLASS));
  }

  private final String value;

  /**
   * Construct a repository name.
   *
   * @param in_value The repository name
   */

  public GTRepositoryName(
    final String in_value)
  {
    this.value = NullCheck.notNull(in_value);

    Assertive.require(
      in_value.length() <= 128,
      "Length of repository string '%s' (%d) must be <= %d",
      in_value, Integer.valueOf(in_value.length()), Integer.valueOf(128));

    final Matcher matcher = GTRepositoryName.PATTERN.matcher(this.value);
    Assertive.require(
      matcher.matches(),
      "Repository name '%s' does not match the pattern '%s'",
      in_value,
      GTRepositoryName.PATTERN_TEXT);
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final GTRepositoryName that = (GTRepositoryName) o;
    return this.value.equals(that.value);
  }

  @Override public String toString()
  {
    return this.value;
  }

  @Override public int hashCode()
  {
    return this.value.hashCode();
  }
}
