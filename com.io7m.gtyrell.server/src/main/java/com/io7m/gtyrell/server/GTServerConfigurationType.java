/*
 * Copyright © 2017 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.gtyrell.core.GTGitExecutableType;
import com.io7m.gtyrell.core.GTImmutableStyleType;
import com.io7m.gtyrell.core.GTRepositorySourceType;
import io.vavr.collection.List;
import org.immutables.value.Value;

import java.io.File;
import java.time.Duration;

/**
 * Configuration values for the server.
 */

@Value.Immutable
@GTImmutableStyleType
public interface GTServerConfigurationType
{
  /**
   * @return The root directory inside which new directories will be created
   */

  @Value.Parameter
  File directory();

  /**
   * @return The list of repository group producers
   */

  @Value.Parameter
  List<GTRepositorySourceType> producers();

  /**
   * @return The git executable
   */

  @Value.Parameter
  GTGitExecutableType git();

  /**
   * @return The duration of time that the server will pause between
   * synchronization attempts
   */

  @Value.Parameter
  Duration pauseDuration();

  /**
   * @return {@code true} iff repositories should not actually be cloned
   */

  @Value.Parameter
  @Value.Default
  default boolean dryRun()
  {
    return false;
  }
}
