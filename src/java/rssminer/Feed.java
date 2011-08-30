package rssminer;

public class Feed {
    private String title;
    private int docId;
    private String snippet;
    private String author;
    private String categories;
    private String feedid;

    public String getAuthor() {
        return author;
    }

    public String getCategories() {
        return categories;
    }

    public int getDocId() {
        return docId;
    }

    public String getFeedid() {
        return feedid;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getTitle() {
        return title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public void setFeedid(String feedid) {
        this.feedid = feedid;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
