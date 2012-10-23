/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;

import org.apache.commons.io.IOUtils;
import rssminer.jsoup.HtmlUtils;
import rssminer.tools.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;

public class EmitTextFile {
    public static void main(String[] args) throws SQLException, IOException {

        Connection db = Utils.getRssminerDB();
        Statement stat = db.createStatement();
        ResultSet rs = stat.executeQuery("select * from feed_data where id > 100000 limit 20000");
        while (rs.next()) {
            int id = rs.getInt("id");
            String summary = rs.getString("summary");
            String text = HtmlUtils.text(summary);
            FileOutputStream fs = new FileOutputStream("/home/feng/test/data/" + id);
            IOUtils.write(text, fs);
            fs.close();
        }
    }
}
