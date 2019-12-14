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

package com.io7m.gtyrell.core;

import java.util.Objects;
import org.immutables.value.Value;

import java.util.regex.Matcher;

/**
 * The type of repository group names. For example: {@code io7m}.
 */

@GTImmutableStyleType
@Value.Immutable
public interface GTRepositoryGroupNameType
  extends Comparable<GTRepositoryGroupNameType>
{
  /**
   * @return The name
   */

  @Value.Parameter
  String text();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final Matcher matcher = GTRepositoryGroupNames.PATTERN.matcher(this.text());
    if (!matcher.matches()) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Invalid repository group name.");
      sb.append(System.lineSeparator());
      sb.append("  Expected: ");
      sb.append(GTRepositoryNames.PATTERN.pattern());
      sb.append(System.lineSeparator());
      sb.append("  Received: ");
      sb.append(this.text());
      sb.append(System.lineSeparator());
      throw new IllegalArgumentException(sb.toString());
    }
  }

  @Override
  default int compareTo(
    final GTRepositoryGroupNameType o)
  {
    return this.text().compareTo(Objects.requireNonNull(o, "Other").text());
  }
}
