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

package com.io7m.gytrell.github;

import com.io7m.gtyrell.core.GTRepositoryGroupName;
import com.io7m.gtyrell.core.GTRepositoryGroupType;
import com.io7m.gtyrell.core.GTRepositoryName;
import com.io7m.gtyrell.core.GTRepositorySourceType;
import com.io7m.jnull.NullCheck;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A repository group producer that fetches the owned repositories of a single
 * (authenticated) user on GitHub.
 */

public final class GTGithubRepositories implements GTRepositorySourceType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GTGithubRepositories.class);
  }

  private final Properties props;

  private GTGithubRepositories(
    final String username,
    final String password)
  {
    this.props = new Properties();
    this.props.setProperty("login", NullCheck.notNull(username));
    this.props.setProperty("password", NullCheck.notNull(password));
  }

  /**
   * Create a new repository source.
   *
   * @param in_username The GitHub user
   * @param in_password The user's password
   *
   * @return A new source
   */

  public static GTRepositorySourceType newSource(
    final String in_username,
    final String in_password)
  {
    return new GTGithubRepositories(in_username, in_password);
  }

  @Override public GTRepositoryGroupType getRepositoryGroup()
    throws IOException
  {
    try {
      final GitHubBuilder ghb = GitHubBuilder.fromProperties(this.props);
      final GitHub gh = ghb.build();
      final GHMyself me = gh.getMyself();
      final String user = NullCheck.notNull(me.getLogin());

      final Map<GTRepositoryName, URI> repositories = new HashMap<>(50);
      final PagedIterable<GHRepository> rs =
        me.listRepositories(100, GHMyself.RepositoryListFilter.OWNER);
      final PagedIterator<GHRepository> rsi = rs.iterator();
      while (rsi.hasNext()) {
        final GHRepository r = rsi.next();
        GTGithubRepositories.LOG.debug(
          "repository: {} {}", r.getName(), r.getGitTransportUrl());

        final GTRepositoryName name = new GTRepositoryName(r.getName());
        final URI url = new URI(r.gitHttpTransportUrl());
        repositories.put(name, url);
      }

      return new Group(
        new GTRepositoryGroupName(user),
        Collections.unmodifiableMap(repositories));
    } catch (final URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private static final class Group implements GTRepositoryGroupType
  {
    private final GTRepositoryGroupName      name;
    private final Map<GTRepositoryName, URI> repositories;

    Group(
      final GTRepositoryGroupName in_name,
      final Map<GTRepositoryName, URI> in_repositories)
    {
      this.name = NullCheck.notNull(in_name);
      this.repositories = NullCheck.notNull(in_repositories);
    }

    @Override public GTRepositoryGroupName getGroupName()
    {
      return this.name;
    }

    @Override public Map<GTRepositoryName, URI> getRepositories()
    {
      return this.repositories;
    }
  }
}
