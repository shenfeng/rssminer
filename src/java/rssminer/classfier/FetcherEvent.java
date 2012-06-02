package rssminer.classfier;

import java.util.List;

public class FetcherEvent implements Event {
    final int subid;
    final List<Integer> feedids;

    public FetcherEvent(int subid, List<Integer> feedids) {
        this.subid = subid;
        this.feedids = feedids;
    }
}
