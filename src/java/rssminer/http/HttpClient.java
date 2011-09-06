package rssminer.http;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_ENCODING;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.USER_AGENT;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static rssminer.Utils.getHost;
import static rssminer.Utils.getPath;
import static rssminer.Utils.getPort;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class HttpClient {
    private final ClientBootstrap bootstrap;
    private final ChannelGroup allChannels;

    public HttpClient() {
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(), 1));

        bootstrap.setPipelineFactory(new HttpClientPipelineFactory());
        allChannels = new DefaultChannelGroup("client");
    }

    public void close() {
        allChannels.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    public NettyResponseFuture get(URI uri, AsyncHanlder hanlder) {
        HttpRequest request = new DefaultHttpRequest(HTTP_1_1, GET,
                getPath(uri));
        request.setHeader(HOST, getHost(uri));
        request.setHeader(USER_AGENT,
                "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)");
        request.setHeader(ACCEPT, "*/*");
        request.setHeader(ACCEPT_ENCODING, "gzip, deflate");
        request.setHeader(CONNECTION, Values.CLOSE);

        // System.out.println(request + "\n");

        ChannelFuture future = bootstrap.connect(new InetSocketAddress(
                getHost(uri), getPort(uri)));

        NettyResponseFuture f = new NettyResponseFuture(hanlder);
        allChannels.add(future.getChannel());

        future.addListener(new NettyConnectListener(request, f));
        return f;
    }
}
