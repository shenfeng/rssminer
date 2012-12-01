/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.IOUtils;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class ExtractTextHandler extends DefaultHandler {
    private int i, end;
    private boolean keep = true;
    private boolean prev = false, current = false;
    private StringBuilder sb = new StringBuilder();

    public void characters(char[] ch, int start, int length) throws SAXException {
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

    public void endElement(String uri, String localName, String qName) throws SAXException {
        keep = true;
    }

    public String getText() {
        return sb.toString();
    }

    public void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException {
        if (localName.equalsIgnoreCase("script") || localName.equalsIgnoreCase("style")) {
            keep = false;
        }
    }
}

public class Utils {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";

    public static int getMaxID() throws SQLException {
        Connection con = getRssminerDB();
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery("select max(id) from feed_data");
        rs.next();
        int max = rs.getInt(1);
        con.close();
        return max;
    }

    public static Connection getRssminerDB() throws SQLException {
        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");

        return con;
    }

    public static String extractText(String html) throws IOException, SAXException {
        Parser p = new Parser();
        ExtractTextHandler h = new ExtractTextHandler();
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getText();
    }

    public static String readFile(String file) throws FileNotFoundException, IOException {
        FileInputStream is = new FileInputStream(file);
        String txt = IOUtils.toString(is);
        is.close();
        return txt;
    }

}
