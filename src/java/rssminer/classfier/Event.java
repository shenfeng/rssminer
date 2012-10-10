package rssminer.classfier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import rssminer.search.Searcher;

// just interface
public abstract class Event implements Delayed {
    private final long due;

    // more than two minute
    public static final int DELAY = Searcher.DELAY + 2;

    public Event(int minute) {
        due = System.currentTimeMillis() + MINUTES.toMillis(minute);
    }

    public long getDelay(TimeUnit unit) {
        return unit.convert(due - System.currentTimeMillis(), MILLISECONDS);
    }

    public int compareTo(Delayed o) {
        return (int) (due - ((Event) o).due);
    }
}
