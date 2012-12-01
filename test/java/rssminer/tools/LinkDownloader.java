/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

public class LinkDownloader {

    static final String NO_CONTNET = "select f.id, f.link from feeds f join feed_data d on f.id = d.id where length(compact) < 600";

    public static void main(String[] args) throws IOException, InterruptedException,
            URISyntaxException, SQLException {
        Downloader d = new Downloader("/home/feng/Downloads/htmls/", NO_CONTNET, 150);
        d.start();
    }
}
