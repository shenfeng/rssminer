/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rssminer.search.Searcher;

public class FetcherEvent extends Event {
    final int subid;
    final List<Integer> feedids;
    
    public static final int DELAY = Searcher.DELAY + 2;

    public FetcherEvent(int subid, List<Integer> feedids) {
        // delay compute, wait for Lucence index
        super(DELAY);
        this.subid = subid;
        this.feedids = feedids;
    }

    public String toString() {
        return "fetcher:" + subid + ", count:" + feedids.size() + " due in "
                + getDelay(TimeUnit.SECONDS) + " s";
    }
}
