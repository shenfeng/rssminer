package rssminer.classfier;

public class FeedScore implements Comparable<FeedScore> {
    public final int feedID;
    public final int subid;
    double score;

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public FeedScore(int feedID, int subid) {
        this.feedID = feedID;
        this.subid = subid;
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