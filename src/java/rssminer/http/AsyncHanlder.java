package rssminer.http;

import org.jboss.netty.handler.codec.http.HttpResponse;

public interface AsyncHanlder {

    public void onCompleted(HttpResponse response);
}
