/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpTest {

    public static void main(String[] args) throws IOException,
            InterruptedException {
        URL url = new URL("http://127.0.0.1:10000");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(3500);
        con.setConnectTimeout(3500);
        con.setRequestMethod("POST");
        con.setRequestProperty("aaa", "bbbbb");
        con.setDoOutput(true);

        OutputStream out = con.getOutputStream();
        out.write("abcdefg".getBytes());
        out.close();
        con.connect();

        try {
            InputStream is = con.getInputStream();
            is.close();
        } catch (Exception e) {
            con.disconnect();
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Thread.sleep(10000);

        // con.
    }
}
