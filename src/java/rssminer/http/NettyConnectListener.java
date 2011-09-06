package rssminer.http;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class NettyConnectListener implements ChannelFutureListener {

    private final HttpRequest request;
    private NettyResponseFuture future;

    public NettyConnectListener(HttpRequest request, NettyResponseFuture f) {
        this.request = request;
        this.future = f;
    }

    public void operationComplete(ChannelFuture f) throws Exception {
        if (f.isSuccess()) {
            Channel channel = f.getChannel();
            channel.getPipeline().getContext(HttpResponseHandler.class)
                    .setAttachment(future);
            channel.write(request);
        } else {
            Throwable cause = f.getCause();
            cause.printStackTrace();
            // TODO
        }
    }
}
