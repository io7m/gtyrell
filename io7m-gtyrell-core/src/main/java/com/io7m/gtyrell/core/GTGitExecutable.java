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

package com.io7m.gtyrell.core;

import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
    this.exec = NullCheck.notNull(in_exec);
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

  @Override public void clone(
    final URI url,
    final File output)
    throws IOException
  {
    NullCheck.notNull(url);
    NullCheck.notNull(output);

    final File output_full = output.getCanonicalFile();

    final List<String> args = new ArrayList<>(4);
    args.add(this.exec.toString());
    args.add("clone");
    args.add("--progress");
    args.add("--mirror");
    args.add(url.toString());
    args.add(output_full.toString());
    GTGitExecutable.LOG.debug("execute {}", args);

    final ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    pb.redirectErrorStream(true);

    final List<String> out_lines = new ArrayList<>(16);
    GTProcessUtilities.executeLogged(
      GTGitExecutable.LOG, pb.start(), out_lines);
  }

  @Override public void fetch(final File repository)
    throws IOException
  {
    NullCheck.notNull(repository);

    final File repository_dir = repository.getCanonicalFile();
    if (repository_dir.isDirectory() == false) {
      throw new IOException(
        String.format(
          "Not a directory: %s",
          repository_dir));
    }

    final List<String> args = new ArrayList<>(4);
    args.add(this.exec.toString());
    args.add("fetch");
    args.add("--progress");
    args.add("--prune");
    GTGitExecutable.LOG.debug("execute {} in {}", args, repository_dir);

    final ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    pb.directory(repository_dir);
    pb.redirectErrorStream(true);

    final List<String> out_lines = new ArrayList<>(16);
    GTProcessUtilities.executeLogged(
      GTGitExecutable.LOG, pb.start(), out_lines);
  }
}
