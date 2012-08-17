/*
 * Copyright (c) 2012. shenedu@gmail.com
 */

package rssminer.classfier;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rssminer.jsoup.HtmlUtils;
import rssminer.search.RssminerAnalyzer;
import rssminer.tools.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class Counter {

    private HashMap<String, Integer> counter;

    public Counter(int capacity) {
        this.counter = new HashMap<String, Integer>(capacity);
    }

    public void add(String key) {
        Integer i = counter.get(key);
        if (i == null) {
            i = 0;
        }
        i += 1;
        counter.put(key, i);
    }

    public HashMap<String, Integer> getCounter() {
        return counter;
    }

    public String toString() {
        return counter.toString();
    }
}

/**
 * Date: 8/15/12
 * Time: 8:09 PM
 */
public class DocumentFrenquency {

    static Counter df = new Counter(617514 * 3);

    static Counter tmp = new Counter(408640);

    static RssminerAnalyzer analyzer = new RssminerAnalyzer();

    public static void addDocument(String text) throws IOException {
        TokenStream stream = analyzer.tokenStream("", new StringReader(text));
        CharTermAttribute term = stream.getAttribute(CharTermAttribute.class);
        Counter c = new Counter(4000);
        String prev = "<p>";
        while (stream.incrementToken()) {
            String t = new String(term.buffer(), 0, term.length());
            c.add(t);
//            tmp.add(prev + "_" + t);
            prev = t;
        }

        for (String t : c.getCounter().keySet()) {
            df.add(t);
        }
    }

    static Logger logger = LoggerFactory.getLogger(DocumentFrenquency.class);

    private static void remove() {
        HashMap<String, Integer> map = df.getCounter();
        Iterator<Map.Entry<String, Integer>> ite = map.entrySet().iterator();
        while (ite.hasNext()) {
            Map.Entry<String, Integer> en = ite.next();
            if (en.getValue() <= 1) {
                ite.remove();
            }
        }
        for (Map.Entry<String, Integer> entry : tmp.getCounter().entrySet()) {
            if (entry.getValue() > 1) {
                df.add(entry.getKey());
            }
        }
        tmp.getCounter().clear();
    }

    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
        long start = System.currentTimeMillis();
        Connection con = Utils.getRssminerDB();
        int maxID = Utils.getMaxID();
//        int maxID = 100000;
        String sql = "select summary from feed_data where id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        for (int i = 1; i < maxID; i++) {
            if (i % 20000 == 0) {
//                remove();
                logger.info("handle {}, max {}, term: {}",
                        new Object[]{i, maxID,
                                df.getCounter().size()});

            }

            ps.setInt(1, i);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String summary = rs.getString(1);
                summary = HtmlUtils.text(summary);
//                summary = Mapper.toSimplified(HtmlUtils.text(summary));
                addDocument(summary);
            }
            rs.close();
        }
        //        remove();
        FileOutputStream fout = new FileOutputStream("/tmp/df");
        for (Map.Entry<String, Integer> entry : df.getCounter().entrySet()) {
            String str = entry.getValue() + "\t" + entry.getKey() + "\n";
            // System.out.print(str);
            fout.write(str.getBytes());
        }
        fout.close();
        System.out.println((System.currentTimeMillis() - start) + "ms");
    }
}
