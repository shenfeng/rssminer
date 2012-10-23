/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.tools;

import org.ccil.cowan.tagsoup.Parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

class Cmp implements Comparator<Entry<String, Integer>> {

    public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
        return o1.getValue().compareTo(o2.getValue());
    }

}

class TextHandler3 extends DefaultHandler {
    private int i, end;
    private boolean keep = true;
    private boolean prev = false, current = false;
    private StringBuilder sb = new StringBuilder();

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (keep) {
            end = start + length;
            for (i = start; i < end; ++i) {
                current = Character.isWhitespace(ch[i]);
                if (!prev || !current) {
                    sb.append(ch[i]);
                }
                prev = current;
            }
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        keep = true;
    }

    public String getText() {
        return sb.toString();
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes atts) throws SAXException {
        if (localName.equalsIgnoreCase("script")
                || localName.equalsIgnoreCase("style")) {
            keep = false;
        }
    }
}

public class HtmlParserBench {

    static List<File> files = new ArrayList<File>();
    static Logger logger = LoggerFactory.getLogger(HtmlParserBench.class);
    static List<String> summarys = new ArrayList<String>();

    @BeforeClass
    public static void setup() throws SQLException {
        File folder = new File("/home/feng/Downloads/htmls");
        for (File f : folder.listFiles()) {
            if (f.length() < 1024 * 1024) { // 1M
                files.add(f);
            }
        }
        logger.info("total files: " + files.size());
        Connection con = Utils.getRssminerDB();
        Statement stat = con.createStatement();
        ResultSet rs = stat
                .executeQuery("select summary from feed_data limit 1000");
        while (rs.next()) {
            String str = rs.getString(1);
            if (str != null && !str.isEmpty()) {
                summarys.add(str);
            }
        }
    }

    @Test
    public void testTagsoupSummary() {
        Parser p = new Parser();
        for (String html : summarys) {
            try {
                TextHandler3 handler = new TextHandler3();
                StringReader sr = new StringReader(html);
                p.setContentHandler(handler);
                p.parse(new InputSource(sr));
            } catch (Exception e) {
                logger.error(html, e);
            }
        }
    }

    @Test
    public void testTagsoup() {
        Parser p = new Parser();
        for (File f : files) {
            try {
                TextHandler3 handler = new TextHandler3();
                FileInputStream fs = new FileInputStream(f);
                p.setContentHandler(handler);
                p.parse(new InputSource(fs));
                fs.close();
            } catch (Exception e) {
                logger.error(f.toString(), e);
            }
        }
    }

    @Test
    public void testJsoup() {
        for (File f : files) {
            try {
                Document d = Jsoup.parse(f, "utf8");
            } catch (Exception e) {
                logger.error(f.toString(), e);
            }
        }
    }
}
