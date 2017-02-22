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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.gtyrell.core.GTGitExecutableType;
import com.io7m.gtyrell.core.GTRepositoryGroupName;
import com.io7m.gtyrell.core.GTRepositoryName;
import com.io7m.gtyrell.core.GTRepositoryType;
import com.io7m.jnull.NullCheck;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.input.ProxyInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class GTGithubRepository implements GTRepositoryType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GTGithubRepository.class);
  }

  private final GTGitExecutableType git;
  private final URI url;
  private final GTRepositoryGroupName group;
  private final GTRepositoryName name;
  private final String username;
  private final String password;

  GTGithubRepository(
    final GTGitExecutableType in_git,
    final String in_username,
    final String in_password,
    final GTRepositoryGroupName in_group,
    final GTRepositoryName in_name,
    final URI in_url)
  {
    this.username = NullCheck.notNull(in_username);
    this.password = NullCheck.notNull(in_password);
    this.git = NullCheck.notNull(in_git, "Git");
    this.url = NullCheck.notNull(in_url, "URL");
    this.group = NullCheck.notNull(in_group, "Group");
    this.name = NullCheck.notNull(in_name, "Name");
  }

  private static GZIPOutputStream createOutput(
    final Path path)
    throws IOException
  {
    return new GZIPOutputStream(Files.newOutputStream(
      path,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE));
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder(128);
    sb.append(this.group.text());
    sb.append("/");
    sb.append(this.name.text());
    sb.append(" @ ");
    sb.append(this.url);
    return sb.toString();
  }

  @Override
  public void update(
    final File output)
    throws IOException
  {
    if (output.isDirectory()) {
      this.git.fetch(output);
    } else {
      final File parent = output.getParentFile();
      if (!parent.mkdirs()) {
        if (!parent.isDirectory()) {
          throw new IOException(String.format("Not a directory: %s", parent));
        }
      }
      this.git.clone(this.url, output);
    }

    this.fetchIssues(
      new File(output.toString() + ".issues.json.gz"),
      new File(output.toString() + ".issues.json.gz.tmp"));
  }

  private void fetchIssues(
    final File file,
    final File file_tmp)
    throws IOException
  {
    LOG.debug("fetching issues: {}", file);

    final StringBuilder sb = new StringBuilder(128);
    sb.append("https://api.github.com/repos/");
    sb.append(this.group.text());
    sb.append("/");
    sb.append(this.name.text());
    sb.append("/issues?state=all");

    final URL issue_url = new URL(sb.toString());
    final String token = this.username + ":" + this.password;
    final Base64.Encoder enc = Base64.getMimeEncoder();
    final String encoding =
      enc.encodeToString(token.getBytes(StandardCharsets.UTF_8));

    final HttpURLConnection conn = (HttpURLConnection) issue_url.openConnection();
    conn.setRequestProperty("Authorization", "Basic " + encoding);
    conn.setRequestProperty("Accept-Encoding", "gzip");
    conn.connect();

    final Path path = file.toPath();
    final Path path_tmp = file_tmp.toPath();

    try (final CountedMaybeCompressedStream input =
           CountedMaybeCompressedStream.fromHTTPConnection(conn)) {
      try (final OutputStream output = createOutput(path_tmp)) {
        IOUtils.copy(input, output);
        LOG.debug(
          "received {} octets",
          Long.toUnsignedString(input.inner.getByteCount()));
        output.flush();
      }

      if (this.parseJSON(path_tmp)) {
        Files.move(path_tmp, path, StandardCopyOption.ATOMIC_MOVE);
      }
    }
  }

  private boolean parseJSON(
    final Path file)
    throws IOException
  {
    final ObjectMapper m = new ObjectMapper();
    try {
      try (final GZIPInputStream is =
             new GZIPInputStream(Files.newInputStream(file))) {
        m.readTree(is);
        LOG.debug("parsed issues correctly, replacing");
        return true;
      }
    } catch (final JsonProcessingException e) {
      LOG.error(
        "could not parse issues for {}/{}: ",
        this.group.text(),
        this.name.text(),
        e);
      return false;
    }
  }

  private static final class CountedMaybeCompressedStream extends
    ProxyInputStream
  {
    private final CountingInputStream inner;

    private CountedMaybeCompressedStream(
      final CountingInputStream in_inner,
      final InputStream in_outer)
    {
      super(in_outer);
      this.inner = NullCheck.notNull(in_inner, "Inner");
    }

    static CountedMaybeCompressedStream fromHTTPConnection(
      final HttpURLConnection conn)
      throws IOException
    {
      final InputStream raw = conn.getInputStream();
      final CountingInputStream counter = new CountingInputStream(raw);
      final InputStream outer;
      if (Objects.equals("gzip", conn.getContentEncoding())) {
        outer = new GZIPInputStream(counter);
      } else {
        outer = counter;
      }
      return new CountedMaybeCompressedStream(counter, outer);
    }
  }
}
