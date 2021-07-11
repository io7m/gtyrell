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

/**
 * The metrics bean implementation.
 */

public final class GTServerMetricsBean implements GTServerMetricsMXBean
{
  private volatile long repositoryCountLatest;
  private volatile long repositoryCount;
  private volatile long repositoryGroupFailures;
  private volatile long repositorySyncTimeSeconds;
  private volatile long repositorySyncsLatest;
  private volatile long repositorySyncsOKLatest;
  private volatile long repositorySyncsFailLatest;
  private volatile long repositorySyncsTotal;
  private volatile long repositorySyncsOKTotal;
  private volatile long repositorySyncsFailTotal;
  private volatile long repositorySyncWaitSecondsRemaining;
  private volatile String repositorySyncTimeNext;
  private volatile long repositoryShortPauses;

  /**
   * The metrics bean implementation.
   */

  public GTServerMetricsBean()
  {
    this.repositorySyncTimeNext = "";
  }

  private void repositorySyncsFinish()
  {
    this.repositorySyncsTotal += this.repositorySyncsLatest;
    this.repositorySyncsOKTotal += this.repositorySyncsOKLatest;
    this.repositorySyncsFailTotal += this.repositorySyncsFailLatest;

    this.repositorySyncsLatest = 0L;
    this.repositorySyncsOKLatest = 0L;
    this.repositorySyncsFailLatest = 0L;
  }

  /**
   * Update the bean after a sync period.
   */

  public void update()
  {
    this.setRepositoryCount(this.repositoryCountLatest);
    this.repositorySyncsFinish();
  }

  void repositoryCountAdd(
    final int size)
  {
    this.repositoryCountLatest += (long) size;
  }

  void repositoryGroupSyncFailed()
  {
    this.repositoryGroupFailures += 1L;
  }

  @Override
  public long getRepositoryCount()
  {
    return this.repositoryCount;
  }

  void setRepositoryCount(
    final long count)
  {
    this.repositoryCountLatest = 0L;
    this.repositoryCount = count;
  }

  @Override
  public long getRepositoryGroupFailures()
  {
    return this.repositoryGroupFailures;
  }

  @Override
  public long getRepositorySyncTimeSecondsLatest()
  {
    return this.repositorySyncTimeSeconds;
  }

  void setRepositorySyncTimeSecondsLatest(
    final long time)
  {
    this.repositorySyncTimeSeconds = time;
  }

  @Override
  public long getRepositorySyncAttemptsTotal()
  {
    return this.repositorySyncsTotal;
  }

  @Override
  public long getRepositorySyncsSucceededTotal()
  {
    return this.repositorySyncsOKTotal;
  }

  @Override
  public long getRepositorySyncsFailedTotal()
  {
    return this.repositorySyncsFailTotal;
  }

  @Override
  public long getRepositorySyncAttemptsLatest()
  {
    return this.repositorySyncsLatest;
  }

  @Override
  public long getRepositorySyncsSucceededLatest()
  {
    return this.repositorySyncsOKLatest;
  }

  @Override
  public long getRepositorySyncsFailedLatest()
  {
    return this.repositorySyncsFailLatest;
  }

  void repositorySyncAttempted()
  {
    this.repositorySyncsLatest += 1L;
  }

  void repositorySyncSucceeded()
  {
    this.repositorySyncsOKLatest += 1L;
  }

  void repositorySyncFailed()
  {
    this.repositorySyncsFailLatest += 1L;
  }

  void repositoryShortPause()
  {
    this.repositoryShortPauses += 1L;
  }

  @Override
  public long getRepositorySyncWaitSecondsRemaining()
  {
    return this.repositorySyncWaitSecondsRemaining;
  }

  void setRepositorySyncWaitSecondsRemaining(
    final long seconds)
  {
    this.repositorySyncWaitSecondsRemaining = seconds;
  }

  @Override
  public String getRepositorySyncTimeNext()
  {
    return this.repositorySyncTimeNext;
  }

  @Override
  public long getRepositorySyncShortPauses()
  {
    return this.repositoryShortPauses;
  }

  void setRepositorySyncTimeNext(
    final String timestamp)
  {
    this.repositorySyncTimeNext = timestamp;
  }
}
