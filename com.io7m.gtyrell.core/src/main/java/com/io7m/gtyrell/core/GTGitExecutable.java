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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The default implementation of the {@link GTGitExecutableType} interface.
 */

public final class GTGitExecutable implements GTGitExecutableType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GTGitExecutable.class);
  }

  private final File exec;

  private GTGitExecutable(
    final File in_exec)
  {
    this.exec = Objects.requireNonNull(in_exec, "in_exec");
  }

  /**
   * Construct a new executable.
   *
   * @param exec The path to the executable.
   *
   * @return A new executable
   */

  public static GTGitExecutableType newExecutable(
    final File exec)
  {
    return new GTGitExecutable(exec);
  }

  @Override
  public void clone(
    final URI url,
    final File output)
    throws IOException
  {
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(output, "output");

    final List<String> args = new ArrayList<>(4);
    args.add(this.exec.toString());
    args.add("clone");
    args.add("--progress");
    args.add("--mirror");
    args.add(url.toString());
    final File output_full = output.getCanonicalFile();
    args.add(output_full.toString());
    LOG.debug("execute {}", args);

    final ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    configureEnvironment(pb.environment());
    pb.redirectErrorStream(true);

    final List<String> out_lines = new ArrayList<>(16);
    final Process process = pb.start();
    GTProcessUtilities.executeLogged(LOG, process, out_lines);
  }

  private static void configureEnvironment(
    final Map<String, String> environment)
  {
    environment.put("GIT_TERMINAL_PROMPT", "0");
  }

  @Override
  public void fetch(final File repository)
    throws IOException
  {
    Objects.requireNonNull(repository, "repository");

    final File repository_dir = repository.getCanonicalFile();
    if (!repository_dir.isDirectory()) {
      throw new IOException(
        String.format("Not a directory: %s", repository_dir));
    }

    final List<String> args = new ArrayList<>(4);
    args.add(this.exec.toString());
    args.add("fetch");
    args.add("--progress");
    args.add("--prune");
    LOG.debug("execute {} in {}", args, repository_dir);

    final ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    pb.directory(repository_dir);
    configureEnvironment(pb.environment());
    pb.redirectErrorStream(true);

    final List<String> out_lines = new ArrayList<>(16);
    final Process process = pb.start();
    GTProcessUtilities.executeLogged(LOG, process, out_lines);
  }
}
