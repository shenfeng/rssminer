import os
import feedparser

DIRNAME = "failed_rss/"

if __name__ == '__main__':
    files = os.listdir(DIRNAME)
    ok = 0
    fail = 0
    for f in files:
        try:
            f = open(DIRNAME + f)
            content = f.read()
            f.close()
            result =feedparser.parse(content)
            # print result.feed
            if len(result.entries) > 1:
                print f, "ok", len(result.entries)
                ok +=1
            else:
                # print f, "empty"
                fail += 1
        except Exception, e:
            # print f, 'fail', e
            fail += 1
    print "ok", ok, "fail", fail
    # print content
        # os.open(f)
        # print f


