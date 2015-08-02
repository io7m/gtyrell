io7m-gtyrell
=========

`gtyrell` is a tiny server that periodically synchronizes a set of
[Git](http://git-scm.com) repositories.

## Requirements

+ [Git](http://git-scm.com)
+ A JRE supporting Java 7 or greater.

## Building

```
$ mvn clean package
```

## Running

Compilation produces a jar file containing all of the dependencies.

```
$ java -jar io7m-gtyrell-server/target/io7m-gtyrell-server-*-main.jar
usage: server.conf [logback.xml]
```

The program accepts a configuration file and an optional
[Logback](http://logback.qos.ch) configuration file to control
logging. The default is to log everything.

The `gtyrell` configuration file is in [Java Properties](https://en.wikipedia.org/wiki/.properties)
format:

```
# The base directory to which repository directories will be written
com.io7m.gtyrell.server.directory       = /git/com.github

# The path to the Git executable
com.io7m.gtyrell.server.git_executable  = /usr/bin/git

# The time to pause between synchronization attempts
com.io7m.gtyrell.server.pause_duration  = 0h 15m 0s

# A list of repository sources
com.io7m.gtyrell.server.repository_sources = github0

# A GitHub repository source
com.io7m.gtyrell.server.repository_source.github0.type     = github
com.io7m.gtyrell.server.repository_source.github0.user     = someone
com.io7m.gtyrell.server.repository_source.github0.password = enoemos123
```

The above configuration will cause the server to authenticate to the
[GitHub API](https://developer.github.com/v3/) as `someone` using the
password `enoemos123`, and retrieve a list of all of the repositories
owned by the user. For each repository `r`, it will then clone `r`
to `/git/com.github/someone/r.git` if it has not yet been cloned, or
fetch updates to `r` if it has. When all updates have been performed,
it will pause for 15 minutes before trying again.

Currently, only GitHub is supported as a repository source, and only
in the authenticated per-user manner specified.

