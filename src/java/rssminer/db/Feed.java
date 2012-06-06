package rssminer.db;

public class Feed implements Comparable<Feed> {
    private int id;
    private int rssid;
    private double score;
    private int vote;
    private String link;
    private String title;
    private String author;
    private String tags;
    private int publishedts;
    private int readts;
    private int votets;

    public String getAuthor() {
        return author;
    }

    public int getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public int getPublishedts() {
        return publishedts;
    }

    public int getReadts() {
        return readts;
    }

    public int getRssid() {
        return rssid;
    }

    public double getScore() {
        return score;
    }

    public String getTags() {
        return tags;
    }

    public String getTitle() {
        return title;
    }

    public int getVote() {
        return vote;
    }

    public int getVotets() {
        return votets;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setPublishedts(int publishedts) {
        this.publishedts = publishedts;
    }

    public void setReadts(int readts) {
        this.readts = readts;
    }

    public void setRssid(int rssid) {
        this.rssid = rssid;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVote(int vote) {
        this.vote = vote;
    }

    public void setVotets(int votets) {
        this.votets = votets;
    }

    public int compareTo(Feed o) {
        if (o.score > score)
            return 1;
        else if (score > o.score)
            return -1;
        return 0;
    }

}
