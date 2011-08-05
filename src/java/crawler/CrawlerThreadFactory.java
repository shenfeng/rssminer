package crawler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerThreadFactory implements ThreadFactory {
	final AtomicInteger threadNumber = new AtomicInteger(1);
	final String prefix = "crawler-worker-";

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, prefix + threadNumber.getAndIncrement());
		t.setDaemon(true);
		return t;
	}
}
