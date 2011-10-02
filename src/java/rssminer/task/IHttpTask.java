package rssminer.task;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;

import org.jboss.netty.handler.codec.http.HttpResponse;

public interface IHttpTask {

    URI getUri();

    Map<String, Object> getHeaders();

    Object doTask(HttpResponse response) throws Exception;

    Proxy getProxy();
}
