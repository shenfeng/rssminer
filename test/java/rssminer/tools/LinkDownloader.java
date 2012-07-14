package rssminer.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

public class LinkDownloader {

    public static void main(String[] args) throws IOException,
            InterruptedException, URISyntaxException, SQLException {
        Downloader d = new Downloader("/home/feng/Downloads/htmls/",
                "select id, link from feeds", 15);
        d.start();
    }
}
