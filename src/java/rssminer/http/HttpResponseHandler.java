package rssminer.http;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class HttpResponseHandler extends SimpleChannelUpstreamHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        NettyResponseFuture future = (NettyResponseFuture) ctx
                .getAttachment();
        HttpResponse response = (HttpResponse) e.getMessage();
        ctx.getChannel().close();
        future.done(response);
    }
}
