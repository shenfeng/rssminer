# RSSMiner

* Rssminer - an intelligent RSS reader
* Live version [rssminer.net](http://rssminer.net)
* Write in Clojure & Javascript & Java, by [shen feng](http://shenfeng.me)
* Simple, fast, responsive. It's a javascript app

## Features

* Read the original, not the provided `abstract`: eg: `Hacker news`,
`IBM developerWorks : Java technology`, `Peter Norvig`
* Read blogspot, wordpress, feedburner etc in China
* Learn from your `like` `dislike`, then rank feeds accordingly
* Google Chrome plugin
* Clean and compact code.

## Technologies

### Server-side

* Java. Clojure.
* [MySQL](http://www.mysql.com/), data store
* [Redis](http://redis.io/), session store, Message Queue
* [http-kit](https://github.com/shenfeng/http-kit), event driven http
  server and http client especially written for Rssminer

### Client-side
* [jQuery](http://jquery.com/), nice api to user
* [Underscore](http://documentcloud.github.com/underscore/), be functional
* [sass](http://sass-lang.com/), fix css

## How to run it

### Dependencies

jdk, redis, [lein](https://github.com/technomancy/leiningen),
mysql-server, [sass](http://sass-lang.com/), rake

### Instructions

1. clone this repo, fetch deps

```sh
git clone git://github.com/shenfeng/rssminer.git && cd rssminer && lein deps
```

3. Initialize database, create user, import schema

```sh
cd rssminer && ./scripts/admin init-db
```

4. Run it

```sh
rake run:dev # run server in dev profile, view it: http://127.0.0.1:9090
```

### Run unit test

```sh
rake test
```

It will create a temp MySQL database for each test. If MySQL's db
path is in `tmpfs`, It will much faster(12s vs 40+s).

```sh
rake mysql_dev # replace my.cnf will a dev one, run mysql in /tmp, run it after understand it.
```

### Command line args

```sh
./scripts/run --help

Usage:

 Switches                 Default                          Desc
 --------                 -------                          ----
 -p, --port               9090                             Port to listen
 --worker                 2                                Http worker count
 --fetcher-queue          20                               queue size
 --fetch-size             100                              Bulk fetch size
 --profile                :dev                             dev or prod
 --redis-host             127.0.0.1                        Redis for session store
 --proxy-server           //192.168.1.4                    proxy server
 --static-server          //192.168.1.4                    static server
 --db-url                 jdbc:mysql://localhost/rssminer  Mysql Database url
 --db-user                feng                             Mysql Database user name
 --index-path             /var/rssminer/index              Path to store lucene index
 --no-fetcher, --fetcher  true                             Start rss fetcher
 --no-proxy, --proxy      true                             Enable Socks proxy
 --no-help, --help        false                            Print this help
```
