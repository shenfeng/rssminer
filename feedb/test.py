import httplib
import random

HOST = "127.0.0.1:7167"


def test_ok(con, id, data):
    # con = httplib.HTTPConnection(HOST)
    con.request('GET', '/d/test/' + str(id))
    resp = con.getresponse()
    status = resp.status
    if status != 200:
        print "ERROR: expect 200:", id, "get:", status
    r = resp.read()

    if data:
        if r != data:
            print "ERROR: not get expected", len(r), len(data)
            print r, "=-----------=-", data
    elif len(r) < 1:
        print "Error: length is 0"


def send_and_get(id, ids=[]):
    data = "abc1234" * random.randint(1, 10)
    con = httplib.HTTPConnection(HOST)
    con.request('POST', '/d/test?id=' + str(id) + "&len=" + str(len(data)), data)
    resp = con.getresponse()
    r = resp.read()
    if r != "OK":
        print "ERROR: POST expect OK:", id, "get:", r
        # con.close()
    test_ok(con, id, data)
    ids.append(id)

    test_ok(con, ids[random.randint(0, len(ids) - 1)], None)
    test_ok(con, "-".join(map(str, ids)), None)
    con.close()


if __name__ == '__main__':
    for i in xrange(1, 5):
        send_and_get(random.randint(1, 1024 * 10240))
