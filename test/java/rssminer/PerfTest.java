package rssminer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class Request {

    static String[] search = new String[]{
            "java", "ios", "clojure", "谷歌四面树敌", "史诗般的战争",
            "debian", "做正确的加法", "编程", "网页中的平面构成", "企业开发者", "Nosql", "google",
            "2从原理分析PHP性能", "nginx", "排名算法", "电子商务", "software",
            "创新工场", "手机", "周鸿祎", "sql", "企业应用测试平台", "融资"
    };

    static String[] cookie = new String[60];

    static {
        for (int i = 0; i < cookie.length; i++) {
            cookie[i] = "zk" + Integer.toString(Integer.MAX_VALUE - (i + 1), 35);
        }
    }


    static String cookie(int userID) {
        return "zk" + Integer.toString(Integer.MAX_VALUE - (userID + 1), 35);
    }

    static String[] getUrls = new String[]{
            "/api/subs",
            "/api/welcome?section=newest&limit=26&offset=0",
            "/api/welcome?section=newest&limit=26&offset=20",
            "/api/welcome?section=recommend&limit=26&offset=0",
            "/api/welcome?section=read&limit=26&offset=0",
            "/api/welcome?section=voted&limit=26&offset=0"
    };
    public static final int RSS_COUNT = 6000;
    public static final int FEED_COUNT = 100000;

    String url;
    int userID;
    Map<String, String> headers = new HashMap<>();
    boolean post = false;
    byte[] body;
//    Map<String, Object> body = new HashMap<>();

    public Request() {
        Random r = new Random();
        userID = r.nextInt(70);
        headers.put("Accept", "*/*");
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_0) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.89 Safari/537.1");
        headers.put("Pragma", "no-cache");
        headers.put("Connection", r.nextBoolean() ? "keep-alive" : "close");
        headers.put("Accept-Encoding", "gzip,deflate,sdch");
        headers.put("Cookie", "_id_=" + cookie(userID));

        int next = r.nextInt(100);
        if (next > 85) {
            url = getUrls[r.nextInt(getUrls.length)];
        } else if (next > 80) { // search
            url = "/api/search?q=" + search[r.nextInt(search.length)] +
                    "&limit=" + (r.nextInt(5) + 11);
        } else if (next > 60) {
            post = true;
            url = "/api/feeds/" + r.nextInt(FEED_COUNT) + "/read";
        } else if (next > 55) { // vote
            post = true;
            url = "/api/feeds/" + r.nextInt(FEED_COUNT) + "/vote";
            body = (r.nextInt(5) > 3 ? "{\"vote\":1}" : "{\"vote\":1}").getBytes();
        } else if (next > 40) {
            url = "/api/subs/" + r.nextInt(RSS_COUNT);
            url += "?offset=0&limit=15&sort=newest";
        } else if (next > 30) {
            url = "/api/subs/" + r.nextInt(RSS_COUNT);
            int c = r.nextInt(7) + 3;
            for (int i = 0; i < c; ++i) {
                url += ("-" + r.nextInt(RSS_COUNT));
            }
            url += "?offset=0&limit=15&sort=newest";
        } else if (next > 25) { // single get
            url = "/api/feeds/" + r.nextInt(FEED_COUNT);
        } else {
            url = "/api/feeds/" + r.nextInt(FEED_COUNT);
            int c = r.nextInt(5) + 3;
            for (int i = 0; i < c; ++i) {
                url += ("-" + r.nextInt(FEED_COUNT));
            }
        }
    }
}

public class PerfTest {
    static int THREAD_COUNT = 20;
    static final int COUNT = 30000;
    static AtomicInteger remaining = new AtomicInteger(COUNT);
    static String host = "http://192.168.1.102:9090";

    static Logger logger = LoggerFactory.getLogger(PerfTest.class);

    static int exec(Request req) throws IOException {
        long start = System.currentTimeMillis();
        HttpURLConnection con = (HttpURLConnection) new URL(host + req.url).openConnection();

        for (Map.Entry<String, String> entry : req.headers.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (req.post) {
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            if (req.body != null)
                con.getOutputStream().write(req.body);
        }

        int code = con.getResponseCode();
        if (code > 400) {
            throw new RuntimeException("status code: " + code + "; req: " + req.url);
        }

        InputStream is = null;
        try {
            is = con.getInputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = (is.read(buffer))) > 0) {
            }
            is.close();
        } catch (IOException e) {
        }

        con.disconnect();

        int time = (int) (System.currentTimeMillis() - start);
        logger.info("{}: {} {} {}, {}ms", new Object[]{
                req.userID, req.post ? "GET" : "POST", code, req.url, time
        });

        return time;
    }

    public static void main(String[] args) throws InterruptedException {
        final AtomicLong total = new AtomicLong(0);
        ExecutorService exector = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            exector.submit(new Runnable() {
                public void run() {
                    while (remaining.decrementAndGet() > 0) {
                        Request req = new Request();
                        try {
                            int time = exec(req);
                            total.getAndAdd(time);
                        } catch (Exception e) {
                            logger.error(req.url, e);
                        }
                    }
                }
            });
        }
        exector.shutdown();
        exector.awaitTermination(1000, TimeUnit.MINUTES);
        logger.info("per request time: {}ms", total.get() / (double) COUNT);
    }
}
