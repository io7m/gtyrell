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

package com.io7m.gtyrell.server;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.io7m.gtyrell.filter.GTFilterCompilerException;
import com.io7m.gtyrell.filter.GTFilterCompilers;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyException;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Clock;

/**
 * The main server frontend.
 */

public final class GTServerMain
{
  private GTServerMain()
  {

  }

  /**
   * Main function for starting a server.
   *
   * @param args Command line arguments
   *
   * @throws IOException               On I/O errors
   * @throws JPropertyException        On configuration errors
   * @throws GTFilterCompilerException On compilation errors
   */

  public static void main(final String[] args)
    throws IOException, JPropertyException, GTFilterCompilerException
  {
    try {
      if (args.length < 1) {
        System.err.println("usage: server.conf [logback.xml]");
        System.exit(1);
      }

      if (args.length > 1) {
        final var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
          final var configurator = new JoranConfigurator();
          configurator.setContext(context);
          configurator.doConfigure(args[1]);
        } catch (final Exception ex) {
          System.err.println("Could not load logback.xml: ");
          ex.printStackTrace();
        }
      }

      final var config =
        GTServerConfigurations.fromProperties(
          GTFilterCompilers.create(Clock.systemDefaultZone()),
          JProperties.fromFile(new File(args[0])));

      final var server = GTServer.newServer(config);
      server.run();
    } catch (final JPropertyException | IOException e) {
      throw e;
    } catch (final GTFilterCompilerException e) {
      final var log = LoggerFactory.getLogger(GTServerMain.class);
      for (final var error : e.errors()) {
        final var position = error.position();
        log.error(
          "{}:{}:{}: {}",
          position.file(),
          Integer.valueOf(position.line()),
          Integer.valueOf(position.column()),
          error.message());
        error.exception().ifPresent(ex -> log.error("exception: ", ex));
      }
      throw e;
    }
  }
}
