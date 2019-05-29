gtyrell
===

[![Build Status](https://img.shields.io/travis/io7m/gtyrell.svg?style=flat-square)](https://travis-ci.org/io7m/gtyrell)
[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.gtyrell/com.io7m.gtyrell.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.gtyrell%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.io7m.gtyrell/com.io7m.gtyrell.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/com/io7m/gtyrell/)
[![Codacy Badge](https://img.shields.io/codacy/grade/1a59ea6bf43c4f5896a3b0195037be64.svg?style=flat-square)](https://www.codacy.com/app/github_79/gtyrell?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=io7m/gtyrell&amp;utm_campaign=Badge_Grade)
[![Codecov](https://img.shields.io/codecov/c/github/io7m/gtyrell.svg?style=flat-square)](https://codecov.io/gh/io7m/gtyrell)

![gtyrell](./src/site/resources/gtyrell.jpg?raw=true)

## Usage

Create a server configuration file (`server.conf`):

```
com.io7m.gtyrell.server.directory      = /tmp/gh
com.io7m.gtyrell.server.git_executable = /usr/bin/git
com.io7m.gtyrell.server.pause_duration = 0h 15m 0s

com.io7m.gtyrell.server.repository_sources = github0

com.io7m.gtyrell.server.repository_source.github0.type     = github
com.io7m.gtyrell.server.repository_source.github0.user     = yourgithubusername
com.io7m.gtyrell.server.repository_source.github0.password = yourgithubpassword
```

The above will attempt to sync repositories from GitHub to `/tmp/gh`
every 15 minutes.

Run the server:

```
$ java -jar com.io7m.gtyrell.server-1.0.0-main.jar server.conf
```

The server will not fork into the background and can be safely used under
a process supervision system such as [s6](http://www.skarnet.org/software/s6/)
without issues.

## Inclusions/Exclusion filters

It's possible to explicitly include and exclude repositories from being
cloned and/or updated. This is achieved by specifying a filter program:

```
com.io7m.gtyrell.server.repository_source.github0.type     = github
com.io7m.gtyrell.server.repository_source.github0.user     = yourgithubusername
com.io7m.gtyrell.server.repository_source.github0.password = yourgithubpassword
com.io7m.gtyrell.server.repository_source.github0.filter   = /etc/filter.conf
```

The file referenced by `com.io7m.gtyrell.server.repository_source.github0.filter`
must contain a filter program. Filter programs are specified with a line-based
format consisting of rules evaluated from top to bottom. An example filter
file:

```
include ^.*$
exclude ^x/.*$
include-and-halt ^x/y$
exclude-and-halt ^z/.*$
```

A _filter rule_ must be one of `include`, `exclude`, `include-and-halt`, or
`exclude-and-halt`. The incoming repository names are matched against the patterns
given in the filter rules. A repository is included or excluded based on the
result of the _last_ rule that matched the repository.

The `include` command marks a repository as included if the pattern matches the
repository name. Evaluation of other rules continues if the pattern matches.

The `exclude` command marks a repository as excluded if the pattern matches the
repository name. Evaluation of other rules continues if the pattern matches.

The `include-and-halt` command marks a repository as included if the pattern
matches the  repository name. Evaluation of other rules halts if the pattern
matches.

The `exclude-and-halt` command marks a repository as included if the pattern
matches the  repository name. Evaluation of other rules halts if the pattern
matches.

If no rules are specified at all, no repositories are included. If no rules
match at all for a given repository, the repository is not included.

Patterns are given in [Java regular expression syntax](https://docs.oracle.com/javase/9/docs/api/java/util/regex/Pattern.html)
and are matched against the incoming repository owner and name separated by a slash.

Given this example:

```
include ^.*$
exclude ^x/.*$
include-and-halt ^x/y$
exclude-and-halt ^z/.*$
include ^z/a$
```

A repository `a/b`:

  1. Will match rule 1 and therefore will, currently, be included
  2. Will not match rule 2 and therefore its inclusion status will not change
  3. Will not match rule 3 and therefore its inclusion status will not change
  4. Will not match rule 4 and therefore its inclusion status will not change
  5. Will not match rule 5 and therefore its inclusion status will not change

As a result, `a/b` will be included as rule 1 included it and no other subsequent
rules changed this.

A repository `x/q`:

  1. Will match rule 1 and therefore will, currently, be included
  2. Will match rule 2 and therefore its inclusion status will be changed to _excluded_
  3. Will not match rule 3 and therefore its inclusion status will not change
  4. Will not match rule 4 and therefore its inclusion status will not change
  5. Will not match rule 5 and therefore its inclusion status will not change

As a result, `x/q` will be not included as rule 2 excluded it and no other subsequent
rules changed this.

A repository `x/y`:

  1. Will match rule 1 and therefore will, currently, be included
  2. Will not match rule 2 and therefore its inclusion status will not change
  3. Will match rule 3 and therefore its inclusion status will be changed to _included_ and evaluation will be halted here
  4. Rule 4 is not evaluated because rule 3 matched and halted evaluation
  5. Rule 5 is not evaluated because rule 3 matched and halted evaluation

As a result, `x/y` will be included as rule 3 included it and evaluation was
halted at that point.

A repository `z/a`:

  1. Will match rule 1 and therefore will, currently, be included
  2. Will not match rule 2 and therefore its inclusion status will not change
  3. Will not match rule 3 and therefore its inclusion status will not change
  4. Will match rule 4 and therefore its inclusion status will be changed to _excluded_ and evaluation will be halted here
  5. Rule 5 is not evaluated because rule 4 matched and halted evaluation

As a result, `z/a` will be excluded as rule 4 excluded it and evaluation was
halted at that point. Note that rule 5 would have matched the input exactly,
but rule 4 prevented that rule from being evaluated.

The filter rules are inspired by [OpenBSD](https://www.openbsd.org)'s [pf](https://www.openbsd.org/faq/pf/)
packet filter.
