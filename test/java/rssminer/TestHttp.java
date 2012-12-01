/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import me.shenfeng.http.DynamicBytes;
import me.shenfeng.http.client.BinaryRespListener;
import me.shenfeng.http.client.HttpClient;
import me.shenfeng.http.client.HttpClientConfig;
import me.shenfeng.http.client.IBinaryHandler;

/**
 * Created with IntelliJ IDEA. User: feng Date: 9/16/12 Time: 11:34 AM To change
 * this template use File | Settings | File Templates.
 */
public class TestHttp {

    public static void main(String[] args) throws IOException, URISyntaxException,
            InterruptedException {
        HttpClient client = new HttpClient(new HttpClientConfig());

        client.get(new URI("http://www.gtdlife.com/favicon.ico"),
                new HashMap<String, String>(), Proxy.NO_PROXY, new BinaryRespListener(
                        new IBinaryHandler() {
                            @Override
                            public void onSuccess(int status, Map<String, String> headers,
                                    DynamicBytes bytes) {
                                System.out.println(headers);
                            }

                            @Override
                            public void onThrowable(Throwable t) {

                            }
                        }));

        Thread.sleep(10000);
    }

}
