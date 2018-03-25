gtyrell
===

[![Build Status](https://travis-ci.org/io7m/gtyrell.png)](https://travis-ci.org/io7m/gtyrell)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.io7m.gtyrell/io7m-gtyrell/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.io7m.gtyrell/io7m-gtyrell)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1a59ea6bf43c4f5896a3b0195037be64)](https://www.codacy.com/app/github_79/gtyrell?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=io7m/gtyrell&amp;utm_campaign=Badge_Grade)

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
$ java -jar io7m-gtyrell-server-1.0.0-main.jar server.conf
```

The server will not fork into the background and can be safely used under
a process supervision system such as [s6](http://www.skarnet.org/software/s6/)
without issues.

