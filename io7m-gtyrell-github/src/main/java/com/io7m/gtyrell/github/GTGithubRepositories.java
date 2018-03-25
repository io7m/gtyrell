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
import com.io7m.jnull.NullCheck;
import javaslang.Tuple;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
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
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private final String username;
  private final String password;
  private final SimpleDateFormat formatter;
  private final Pattern inclusion;
  private final Pattern exclusion;

  private GTGithubRepositories(
    final String in_username,
    final String in_password,
    final Pattern in_inclusion,
    final Pattern in_exclusion)
  {
    this.username =
      NullCheck.notNull(in_username);
    this.password =
      NullCheck.notNull(in_password);
    this.inclusion =
      NullCheck.notNull(in_inclusion, "in_inclusion");
    this.exclusion =
      NullCheck.notNull(in_exclusion, "in_exclusion");

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
   *                       @param in_exclusion The exclusion pattern
   * @param in_inclusion The inclusion pattern
   *
   * @return A new source
   */

  public static GTRepositorySourceType newSource(
    final String in_username,
    final String in_password,
    final Pattern in_inclusion,
    final Pattern in_exclusion)
  {
    return new GTGithubRepositories(
      in_username, in_password, in_inclusion, in_exclusion);
  }

  @Override
  public SortedMap<GTRepositoryGroupName, GTRepositoryGroupType> get(
    final GTGitExecutableType in_git)
    throws IOException
  {
    NullCheck.notNull(in_git, "Git");

    try {
      final GitHubBuilder ghb = GitHubBuilder.fromProperties(this.props);
      final GitHub gh = ghb.build();

      LOG.debug(
        "github api rate limit {} remaining {} reset {}",
        Integer.valueOf(gh.getRateLimit().limit),
        Integer.valueOf(gh.rateLimit().remaining),
        this.formatter.format(gh.rateLimit().getResetDate()));

      final GHMyself me = gh.getMyself();

      SortedMap<GTRepositoryGroupName, SortedMap<GTRepositoryName, GTRepositoryType>> groups =
        TreeMap.empty();

      final PagedIterable<GHRepository> rs =
        me.listRepositories(100, GHMyself.RepositoryListFilter.ALL);
      final PagedIterator<GHRepository> rsi = rs.iterator();

      while (rsi.hasNext()) {
        final GHRepository r = rsi.next();

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

        final GTRepositoryGroupName group =
          GTRepositoryGroupName.of(r.getOwnerName());
        final GTRepositoryName name =
          GTRepositoryName.of(r.getName());
        final URI base_clone_url =
          new URI(r.gitHttpTransportUrl());

        final URI clone_url =
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
    final String repos_name =
      new StringBuilder(128)
        .append(owner_name)
        .append("/")
        .append(name)
        .toString();

    final Matcher inclusion_matcher =
      this.inclusion.matcher(repos_name);
    final Matcher exclusion_matcher =
      this.exclusion.matcher(repos_name);

    final boolean include = inclusion_matcher.matches();
    LOG.debug(
      "include {} with {}: {}",
      repos_name,
      this.inclusion.pattern(),
      Boolean.valueOf(include));

    final boolean exclude = exclusion_matcher.matches();
    LOG.debug(
      "exclude {} with {}: {}",
      repos_name,
      this.exclusion.pattern(),
      Boolean.valueOf(exclude));

    return include && !exclude;
  }
}
