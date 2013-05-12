import httplib
import random

HOST = "127.0.0.1:7167"


def send_and_get(id):
    data = "thisisatest" * random.randint(1024, 10240)
    con = httplib.HTTPConnection(HOST)
    con.request('POST', '/d/feeds?id=' + str(id) + "&len=" + str(len(data)), data)
    resp = con.getresponse()
    r = resp.read()
    if r != "OK":
        print "POST expect OK:", id, "get:", r
    # con.close()

    con = httplib.HTTPConnection(HOST)
    con.request('GET', '/d/feeds?id=' + str(id))
    resp = con.getresponse()
    status = resp.status
    if status != 200:
        print "GET data expect 200:", id, "get:", status
    r = resp.read()

    if r != data:
        print "Did not get expected", len(r), len(data)
        print r, "=-----------=-", data
    con.close()

if __name__ == '__main__':
    for i in xrange(1, 100000):
        send_and_get(random.randint(1, 1024 * 10240))
