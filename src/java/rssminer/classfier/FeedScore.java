/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;

public class FeedScore implements Comparable<FeedScore> {
    public final int feedID;
    public final int subid;
    public final int publishTs;
    double score;

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public FeedScore(int feedID, int subid, int publishTs) {
        this.feedID = feedID;
        this.subid = subid;
        this.publishTs = publishTs;
    }

    // sort by rssLinkID to group commit, delete from redis
    public int compareTo(FeedScore o) {
        if (o.subid > this.subid)
            return -1;
        if (o.subid < this.subid)
            return 1;
        return 0;
    }

    public String toString() {
        return "FeedScore [id=" + feedID + ", subid=" + subid + ", score="
                + score + "]";
    }

}