/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;

import rssminer.search.Searcher;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

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
