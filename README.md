# RSSMiner

* Rssminer - an simple, but intelligent RSS reader
* Live version [http://rssminer.net](http://rssminer.net)
* Write in Clojure & Javascript & Java, by [shen feng](http://shenfeng.me)
* Simple, fast, Responsive UI. Features combines naturally, and make
  perfect sense.

## Features

* Read the original, not the provided `abstract`: eg: `Hacker news`,
`IBM developerWorks : Java technology`, `Peter Norvig`
* Read blogspot, wordpress, feedburner etc in China
* Learn from your reading history, your vote `like` `dislike`, rank feeds accordingly. Realtime.
* Google Chrome plugin to add subscription
* Clean and compact code.

## Technologies

### Server-side

* Java. Clojure.
* [Apache Lucene](http://lucene.apache.org/), use it to do fulltext,
  realtime, instant search.
* [Ring](https://github.com/mmcgrana/ring),
  [compojure](https://github.com/weavejester/compojure),
  easier http.
* [http-kit](https://github.com/shenfeng/http-kit), super fast event driven HTTP
  server and HTTP client. Especially written for Rssminer.
* [dbcp](https://github.com/shenfeng/dbcp), Simple database connection
  pool. Especially written for Rssminer
* [mmseg](https://github.com/shenfeng/mmseg), A java implementation of
  MMSEG. Especially written for Rssminer
* [MySQL](http://www.mysql.com/), data store
* [Redis](http://redis.io/), Message Queue; Per user per feed score Store. Proxy cache.

### Client-side
* [jQuery](http://jquery.com/), nicer API
* [Underscore](http://documentcloud.github.com/underscore/), be functional
* [sass](http://sass-lang.com/), fix css
* [mustache.js](https://github.com/janl/mustache.js), I rely it do all
  the client side templating. For faster UI, this is one way to go.

## How to run it

### Dependencies

jdk, redis, [lein](https://github.com/technomancy/leiningen),
mysql-server, [sass](http://sass-lang.com/), rake

### Instructions

1. clone this repo, fetch deps

```sh
git clone git://github.com/shenfeng/rssminer.git && cd rssminer && lein deps
```

2. Initialize database, create user, import schema

```sh
cd rssminer && ./scripts/admin init-db
```

3. Run it

```sh
rake run:dev # run server in dev profile, view it: http://127.0.0.1:9090
```

### Run unit test

```sh
rake test
```

It will create/drop a temp MySQL database for each test. If MySQL's db
path is in `tmpfs`, It will much faster(12s vs 40+s).

```sh
rake mysql_dev # replace my.cnf will a dev one, run mysql in /tmp, run it after understand it.
```

### Command line args

```sh
Usage:

 Switches                 Default                          Desc
 --------                 -------                          ----
 -p, --port               9090                             Port to listen
 --worker                 2                                Http worker thread count
 --fetcher-concurrency    20
 --fetch-size             100                              Bulk fetch size
 --profile                :dev                             dev or prod
 --redis-host             127.0.0.1                        Redis for session store
 --proxy-server           //192.168.1.3                    proxy server
 --static-server          //192.168.1.3                    static server
 --db-url                 jdbc:mysql://localhost/rssminer  Mysql Database url
 --db-user                feng                             Mysql Database user name
 --bind-ip                0.0.0.0                          Which ip to bind
 --events-threshold       2                                How many user feed events buffered before recompute again
 --index-path             /var/rssminer/index              Path to store lucene index
 --no-fetcher, --fetcher  false                            Start rss fetcher
 --no-proxy, --proxy      false                            Enable Socks proxy
 --no-help, --help        false                            Print this help

```
