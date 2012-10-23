/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.tools;

import me.shenfeng.http.DynamicBytes;
import me.shenfeng.http.HttpUtils;
import me.shenfeng.http.client.*;
import me.shenfeng.http.client.TextRespListener.IFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.net.Proxy.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class Downloader {
    static Logger logger = LoggerFactory.getLogger(Downloader.class);

    static final private String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11";
    public static Proxy SOCKS = new Proxy(Type.SOCKS, new InetSocketAddress(
            "127.0.0.1", 3128));

    int producerCount = 5;
    int concurrency = 5;
    String sql;
    String destFolder = "test/failed_rss/";

    private IFilter filter = new IFilter() {

        public boolean accept(DynamicBytes partialBody) {
            if (partialBody.length() > 2 * 1024 * 1024) {
                return false;
            }
            return true;
        }

        public boolean accept(Map<String, String> headers) {
            String ct = headers.get(HttpUtils.CONTENT_TYPE);
            if (ct != null) {
                ct = ct.toLowerCase();
                if (ct.indexOf("text") == -1 && ct.indexOf("index") == -1) {
                    return false;
                }
            }
            return true;
        }
    };

    public Downloader(String destFoder, String sql, int concurrency) {
        this.destFolder = destFoder;
        this.sql = sql;
        this.concurrency = concurrency;
        this.producerCount = this.concurrency / 3;
    }

    private List<Job> getAllFailed() throws SQLException {
        Connection con = Utils.getRssminerDB();
        Statement state = con.createStatement();
        // "select id, url from rss_links where total_feeds  = 0"
        ResultSet rs = state.executeQuery(sql);
        List<Job> jobs = new ArrayList<Job>();
        while (rs.next()) {
            int id = rs.getInt(1);
            String url = rs.getString(2);
            File file = new File(destFolder + id);
            Job job = new Job(url, id, file);
            if (file.exists()) {
                logger.info("{}:{} already downloaded", job.id, job.url);
            } else {
                jobs.add(job);
            }
        }
        con.close();
        Collections.shuffle(jobs);
        return jobs;
    }

    public void start() throws IOException, InterruptedException,
            URISyntaxException, SQLException {
        final HttpClient client = new HttpClient(new HttpClientConfig(60000,
                userAgent));
        final Semaphore s = new Semaphore(concurrency);
        final ConcurrentLinkedQueue<Job> jobs = new ConcurrentLinkedQueue<Job>(
                getAllFailed());
        logger.info("get {} jobs", jobs.size());

        // DNS is slow
        ExecutorService execs = Executors.newFixedThreadPool(producerCount);
        for (int i = 0; i < producerCount; i++) {
            execs.submit(new Runnable() {
                public void run() {
                    Job job = jobs.poll();
                    while (job != null) {
                        try {
                            Map<String, String> map = new TreeMap<String, String>();

                            TextRespListener listener = new TextRespListener(
                                    new TextHandler(job, jobs, s), filter);
                            s.acquire();
                            client.get(new URI(job.url), map, job.proxy,
                                    listener);
                        } catch (Exception e) {
                            s.release();
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

class Job {
    final String url;
    final int id;
    final File file;
    Proxy proxy = Proxy.NO_PROXY;

    public Job(String url, int id, File file) {
        this.url = url;
        this.id = id;
        this.file = file;
    }

    public Job(String url, int id, File file, Proxy proxy) {
        this(url, id, file);
        this.proxy = proxy;
    }
}

class TextHandler implements ITextHandler {

    private Semaphore semaphore;
    private ConcurrentLinkedQueue<Job> jobs;
    private Job job;

    static Logger logger = LoggerFactory.getLogger(TextHandler.class);

    public TextHandler(Job job, ConcurrentLinkedQueue<Job> jobs, Semaphore s) {
        this.job = job;
        this.semaphore = s;
        this.jobs = jobs;
    }

    public void onSuccess(int status, Map<String, String> headers, String body) {
        semaphore.release();
        logger.info("{}, {}:{}", new Object[]{status, job.id, job.url});
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
        String msg = t.getMessage(); // maybe null
        if (msg != null && msg.indexOf("reset") != -1) {
            jobs.add(new Job(job.url, job.id, job.file, Downloader.SOCKS));
        }
    }
}
