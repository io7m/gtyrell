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

package com.io7m.gtyrell.tests;

import com.io7m.junreachable.UnreachableCodeException;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

final class GitHubTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GitHubTest.class);
  }

  private GitHubTest()
  {
    throw new UnreachableCodeException();
  }

  public static void main(final String[] args)
    throws IOException
  {
    final GitHub gh = GitHub.connect();
    final GHMyself me = gh.getMyself();

    final PagedIterable<GHRepository> rs =
      me.listRepositories(10, GHMyself.RepositoryListFilter.OWNER);
    final PagedIterator<GHRepository> rsi = rs.iterator();
    while (rsi.hasNext()) {
      final GHRepository r = rsi.next();
      LOG.debug(
        "repository: {} {}", r.getName(), r.getGitTransportUrl());
    }
  }
}
