package rssminer.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import me.shenfeng.http.HttpUtils;
import me.shenfeng.http.client.HttpClient;
import me.shenfeng.http.client.HttpClientConfig;
import me.shenfeng.http.client.ITextHandler;
import me.shenfeng.http.client.TextRespListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TextHandler implements ITextHandler {

    private Semaphore semaphore;
    private ConcurrentLinkedQueue<Job> jobs;
    private Job job;

    public TextHandler(Job job, ConcurrentLinkedQueue<Job> jobs, Semaphore s) {
        this.job = job;
        this.semaphore = s;
        this.jobs = jobs;
    }

    static Logger logger = LoggerFactory.getLogger(TextHandler.class);

    public void onSuccess(int status, Map<String, String> headers, String body) {
        semaphore.release();
        logger.info("{}, {}:{}", new Object[] { status, job.id, job.url });
        if (status == 200) {
            try {
                FileOutputStream fo = new FileOutputStream(job.file);
                fo.write(body.getBytes());
                fo.close();
            } catch (Exception e) {
                logger.error(job.url, e);
            }
        } else if (status == 302 || status == 301) {
            String location = headers.get(HttpUtils.LOCATION);
            if (location != null) {
                try {
                    location = new URI(job.url).resolve(location).toString();
                    jobs.add(new Job(location, job.id, job.file));
                } catch (URISyntaxException e) {
                    logger.error("resove", e);
                }
                // jobs.add(new Ojb)
            }
        }
    }

    public void onThrowable(Throwable t) {
        semaphore.release();
        logger.error(job.url, t);
    }

}

class Job {
    final String url;
    final String id;
    final File file;

    public Job(String url, String id, File file) {
        this.url = url;
        this.id = id;
        this.file = file;
    }
}

public class FetcherFailed {
    static Logger logger = LoggerFactory.getLogger(FetcherFailed.class);

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer";

    static final int PRODUCER = 5;
    static final int HTTP_CONCURRENCY = 5;

    // static Proxy PROXY = new Proxy(Type.SOCKS, new InetSocketAddress(
    // "127.0.0.1", 3128));
    static Proxy PROXY = Proxy.NO_PROXY;
    static final String DEST_FOLDER = "test/failed_rss/";

    private static List<Job> getAllFailed() throws SQLException {
        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");
        Statement state = con.createStatement();
        ResultSet rs = state
                .executeQuery("select id, url from rss_links where total_feeds  = 0");
        List<Job> jobs = new ArrayList<Job>();
        while (rs.next()) {
            String id = rs.getString("id");
            String url = rs.getString("url");
            File file = new File(DEST_FOLDER + id);
            Job job = new Job(url, id, file);
            if (file.exists()) {
                logger.info("{}:{} already downloaded", job.id, job.url);
            } else {
                jobs.add(job);
            }
        }
        con.close();
        return jobs;
    }

    public static void main(String[] args) throws IOException,
            InterruptedException, URISyntaxException, SQLException {
        final HttpClient client = new HttpClient(new HttpClientConfig());
        final Semaphore s = new Semaphore(HTTP_CONCURRENCY);
        final ConcurrentLinkedQueue<Job> jobs = new ConcurrentLinkedQueue<Job>(
                getAllFailed());
        logger.info("get {} jobs", jobs.size());

        // DNS is slow
        ExecutorService execs = Executors.newFixedThreadPool(PRODUCER);
        for (int i = 0; i < PRODUCER; i++) {
            execs.submit(new Runnable() {
                public void run() {
                    Job job = jobs.poll();
                    while (job != null) {
                        try {
                            Map<String, String> map = new TreeMap<String, String>();

                            TextRespListener listener = new TextRespListener(
                                    new TextHandler(job, jobs, s));
                            s.acquire();
                            client.get(new URI(job.url), map, PROXY, listener);
                        } catch (Exception e) {
                            logger.info("exception...", e);
                        } finally {
                            job = jobs.poll();
                        }
                    }
                }
            });
        }
        execs.shutdown();
        execs.awaitTermination(1000, TimeUnit.DAYS);
    }
}
