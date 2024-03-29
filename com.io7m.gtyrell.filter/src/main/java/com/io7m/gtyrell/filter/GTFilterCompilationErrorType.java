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
import com.io7m.jlexing.core.LexicalPosition;
import org.immutables.value.Value;

import java.net.URI;
import java.util.Optional;

/**
 * A filter program compilation error.
 */

@GTImmutableStyleType
@Value.Immutable
public interface GTFilterCompilationErrorType
{
  /**
   * @return The lexical position of the error
   */

  @Value.Parameter
  LexicalPosition<URI> position();

  /**
   * @return The error message
   */

  @Value.Parameter
  String message();

  /**
   * @return The exception raised, if any
   */

  @Value.Parameter
  Optional<Exception> exception();
}
