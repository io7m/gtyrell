/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import javax.management.MXBean;

/**
 * Server metrics.
 */

// CHECKSTYLE:OFF
@MXBean
public interface GTServerMetricsMXBean
{
  // CHECKSTYLE:ON

  /**
   * @return The number of repositories visible
   */

  long getRepositoryCount();

  /**
   * @return The number of times retrieving a repository group has failed
   */

  long getRepositoryGroupFailures();

  /**
   * @return The number of seconds it took to sync everything in the last sync period
   */

  long getRepositorySyncTimeSecondsLatest();

  /**
   * @return The number of repository sync attempts that have been made in total
   */

  long getRepositorySyncAttemptsTotal();

  /**
   * @return The number of repository sync attempts that succeeded in total
   */

  long getRepositorySyncsSucceededTotal();

  /**
   * @return The number of repository sync attempts that failed in total
   */

  long getRepositorySyncsFailedTotal();

  /**
   * @return The number of repository sync attempts made in the last sync period
   */

  long getRepositorySyncAttemptsLatest();

  /**
   * @return The number of repository sync attempts that succeeded in the last sync period
   */

  long getRepositorySyncsSucceededLatest();

  /**
   * @return The number of repository sync attempts that failed in the last sync period
   */

  long getRepositorySyncsFailedLatest();

  /**
   * @return The number of seconds remaining until the next sync attempt
   */

  long getRepositorySyncWaitSecondsRemaining();

  /**
   * @return The timestamp of the next sync event
   */

  String getRepositorySyncTimeNext();

  /**
   * @return The number of times a pause completed very quickly
   */

  long getRepositorySyncShortPauses();
}
