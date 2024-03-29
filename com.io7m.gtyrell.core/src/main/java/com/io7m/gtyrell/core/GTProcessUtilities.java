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

package com.io7m.gtyrell.core;

import com.io7m.junreachable.UnreachableCodeException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class GTProcessUtilities
{
  private GTProcessUtilities()
  {
    throw new UnreachableCodeException();
  }

  static void executeLogged(
    final Logger log,
    final Process p,
    final List<String> out_lines)
    throws IOException
  {
    try (InputStream p_stdout = p.getInputStream()) {
      try (BufferedReader r_stdout = new BufferedReader(
        new InputStreamReader(p_stdout, StandardCharsets.UTF_8))) {

        while (true) {
          final String out_line = r_stdout.readLine();
          if (out_line == null) {
            break;
          }

          out_lines.add(out_line);
          log.debug("execute: {}", out_line);
        }
      }

      try {
        p.waitFor();
      } catch (final InterruptedException e) {
        log.error(
          "interrupted whilst waiting for process: ", e);
      }

      if (p.exitValue() > 0) {
        try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
          IOUtils.writeLines(out_lines, "\n", bao, StandardCharsets.UTF_8);
          throw new IOException(bao.toString("UTF-8"));
        }
      }
    }
  }

}
