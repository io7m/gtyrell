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

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * The interface exposed by the {@code git} executable.
 */

public interface GTGitExecutableType
{
  /**
   * Clone a repository.
   *
   * @param url    The remote repository.
   * @param output The output file (a directory, typically).
   *
   * @throws IOException On execution and I/O errors
   */

  void clone(
    URI url,
    File output)
    throws IOException;

  /**
   * Fetch all changes from the repository's remotes.
   *
   * @param repository The repository
   *
   * @throws IOException On execution and I/O errors
   */

  void fetch(File repository)
    throws IOException;
}
