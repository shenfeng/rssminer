#!/bin/sh

ABSOLUTE_PATH=$(readlink -f $0)
HOME="${ABSOLUTE_PATH%/*/*/*}"
cd "${HOME}" && java -server \
    -Dfile.encoding=UTF-8 \
    -Djava.net.preferIPv4Stack=true \
    -Dsun.jnu.encoding=UTF-8 \
    -Dsun.net.inetaddr.ttl=0 \
    -Xms88m -Xmx88m -XX:PermSize=32M -XX:MaxPermSize=32M \
    -server -cp "classes:lib/*:src/:test/" clojure.main \
    -m rssminer.tools $@
