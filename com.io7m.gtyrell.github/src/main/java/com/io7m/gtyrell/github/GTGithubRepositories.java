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

package com.io7m.gtyrell.github;

import com.io7m.gtyrell.core.GTGitExecutableType;
import com.io7m.gtyrell.core.GTRepositoryGroup;
import com.io7m.gtyrell.core.GTRepositoryGroupName;
import com.io7m.gtyrell.core.GTRepositoryGroupType;
import com.io7m.gtyrell.core.GTRepositoryName;
import com.io7m.gtyrell.core.GTRepositorySourceType;
import com.io7m.gtyrell.core.GTRepositoryType;
import com.io7m.gtyrell.filter.GTFilterProgram;
import io.vavr.Tuple;
import io.vavr.collection.SortedMap;
import io.vavr.collection.TreeMap;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Properties;

/**
 * A repository group producer that fetches the owned repositories of a single (authenticated) user
 * on GitHub.
 */

public final class GTGithubRepositories implements GTRepositorySourceType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GTGithubRepositories.class);
  }

  private final Properties props;
  private final String username;
  private final String password;
  private final SimpleDateFormat formatter;
  private final GTFilterProgram filter;

  private GTGithubRepositories(
    final String in_username,
    final String in_password,
    final GTFilterProgram in_filter)
  {
    this.username =
      Objects.requireNonNull(in_username, "in_username");
    this.password =
      Objects.requireNonNull(in_password, "in_password");
    this.filter =
      Objects.requireNonNull(in_filter, "filter");

    this.props = new Properties();
    this.props.setProperty("login", this.username);
    this.props.setProperty("password", this.password);
    this.formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ");
  }

  /**
   * Create a new repository source.
   *
   * @param in_username The GitHub user
   * @param in_password The user's password
   * @param in_filter   The repository filter
   *
   * @return A new source
   */

  public static GTRepositorySourceType newSource(
    final String in_username,
    final String in_password,
    final GTFilterProgram in_filter)
  {
    return new GTGithubRepositories(in_username, in_password, in_filter);
  }

  @Override
  public SortedMap<GTRepositoryGroupName, GTRepositoryGroupType> get(
    final GTGitExecutableType in_git)
    throws IOException
  {
    Objects.requireNonNull(in_git, "Git");

    try {
      final var ghb = GitHubBuilder.fromProperties(this.props);
      final var gh = ghb.build();

      LOG.debug(
        "github api rate limit {} remaining {} reset {}",
        Integer.valueOf(gh.getRateLimit().limit),
        Integer.valueOf(gh.rateLimit().remaining),
        this.formatter.format(gh.rateLimit().getResetDate()));

      final var me = gh.getMyself();

      SortedMap<GTRepositoryGroupName, SortedMap<GTRepositoryName, GTRepositoryType>> groups =
        TreeMap.empty();

      final var rs = me.listRepositories(100, GHMyself.RepositoryListFilter.ALL);
      final var rsi = rs.iterator();

      while (rsi.hasNext()) {
        final var r = rsi.next();

        LOG.debug(
          "repository: {}/{} {}",
          r.getOwnerName(),
          r.getName(),
          r.getGitTransportUrl());

        if (!this.repositoryIsIncluded(r.getOwnerName(), r.getName())) {
          LOG.debug(
            "repository {}/{} is not included",
            r.getOwnerName(),
            r.getName());
          continue;
        }

        final var group =
          GTRepositoryGroupName.of(r.getOwnerName());
        final var name =
          GTRepositoryName.of(r.getName());
        final var base_clone_url =
          new URI(r.gitHttpTransportUrl());

        final var clone_url =
          new URI(
            base_clone_url.getScheme(),
            this.username,
            base_clone_url.getHost(),
            base_clone_url.getPort(),
            base_clone_url.getPath(),
            base_clone_url.getQuery(),
            base_clone_url.getFragment());

        SortedMap<GTRepositoryName, GTRepositoryType> repositories;
        if (groups.containsKey(group)) {
          repositories = groups.get(group).get();
        } else {
          repositories = TreeMap.empty();
        }

        final GTRepositoryType repository =
          new GTGithubRepository(
            in_git, this.username, this.password, group, name, clone_url);

        repositories = repositories.put(name, repository);
        groups = groups.put(group, repositories);
      }

      return groups.map(
        (group_name, repositories) ->
          Tuple.of(group_name, GTRepositoryGroup.of(group_name, repositories)));
    } catch (final URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private boolean repositoryIsIncluded(
    final String owner_name,
    final String name)
  {
    final var repos_name =
      new StringBuilder(128)
        .append(owner_name)
        .append("/")
        .append(name)
        .toString();

    return this.filter.includes(LOG, repos_name);
  }
}
