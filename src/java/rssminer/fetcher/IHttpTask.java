package rssminer.fetcher;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;

public interface IHttpTask {

    URI getUri();

    Map<String, String> getHeaders();

    Object doTask(int status, Map<String, String> headers, String body);

    Object onThrowable(Throwable t);

    Proxy getProxy();
}
