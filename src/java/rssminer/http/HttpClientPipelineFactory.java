package rssminer.http;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;

public class HttpClientPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() throws Exception {

        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("inflater", new HttpContentDecompressor());
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("handler", new HttpResponseHandler());

        return pipeline;

    }
}
