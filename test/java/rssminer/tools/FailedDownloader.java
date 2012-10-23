/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

public class FailedDownloader {

    public static void main(String[] args) throws IOException,
            InterruptedException, URISyntaxException, SQLException {
        Downloader d = new Downloader(
                "/home/feng/workspace/rssminer/test/failed_rss/",
                "select id, url from rss_links where total_feeds = 0", 15);
        d.start();
    }
}
