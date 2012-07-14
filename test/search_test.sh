#! /bin/bash

TERMS=( 'java' 'ios' "clojure" "谷歌四面树敌", "史诗般的战争", "debian", "做正确的加法", "“编程", "网页中的平面构成")
COUNT=${#TERMS[@]}

for i in {1..200}; do
    IDX=$(expr $i % $COUNT)
    TERM=${TERMS[$IDX]}

    if [ $IDX -eq 1 ]; then
        echo "recompute all"
        wget --header "Cookie:_id_={{id}}" http://127.0.0.1:9090/admin/compute > /dev/null 2>&1
    fi
    QPS=$(ab -n 3000 -c 10 -C _id_={{id}} "http://127.0.0.1:9090/api/search?q=${TERM}&limit=11" 2>&1 | grep "Requests")
    echo -e $TERM ":\t"  $QPS
    sleep 1
done
