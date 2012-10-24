#!/bin/sh

READLINK=readlink
if which greadlink > /dev/null; then
    READLINK=greadlink
fi

ABSOLUTE_PATH=$(${READLINK} -f $0)
HOME="${ABSOLUTE_PATH%/*/*/*}"
cd "${HOME}"
rake javac_debug
java -server \
    -Dfile.encoding=UTF-8 \
    -Djava.net.preferIPv4Stack=true \
    -Dsun.jnu.encoding=UTF-8 \
    -Dsun.net.inetaddr.ttl=0 \
    -Xms288m -Xmx288m -XX:PermSize=32M -XX:MaxPermSize=32M \
    -server -cp "classes:lib/*:src/:test/" clojure.main \
    -m rssminer.tools $@
