package rssminer.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.Utils;

class Handler implements AsyncHanlder {
    private final static Logger logger = LoggerFactory
            .getLogger(Handler.class);

    @Override
    public void onCompleted(HttpResponse response) {
        ChannelBuffer c = response.getContent();
        String type = response.getHeader(Names.CONTENT_TYPE);
        String s = new String(c.array(), Utils.parseCharset(type));
        // logger.info("{}, \nlength: {}\n", response, s.length());
        // logger.info("length: {}", s.length());
    }
}

public class HttpClientTest {

    @Test
    public void testAsync() throws URISyntaxException, InterruptedException,
            ExecutionException {
        HttpClient client = new HttpClient();
        String str[] = new String[] {
//                "http://gerrit/docs/onycloud/uberdoc.html",
//                "http://gerrit/hp-printer-windows-driver/hppasc16.inf",
//                "http://gerrit/", "http://gerrit/videos/javascript/",
//                "http://gerrit/onyx/style.docx",
//                "http://gerrit/videos/clojure/",

         "http://gerrit:3333/static/62111c40/css/style.css",
         "http://gerrit:3333/static/62111c40/scripts/yui/menu/menu-min.js",
//         "http://localhost:8090", "http://gerrit:8080/login"
        };

        final BlockingQueue<NettyResponseFuture> queue = new ArrayBlockingQueue<NettyResponseFuture>(
                180);
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        NettyResponseFuture f = queue.take();
                        f.get();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();

        while (true) {
            for (String s : str) {
                NettyResponseFuture resp = client.get(new URI(s),
                        new Handler());
                queue.put(resp);
            }
        }

    }
}
