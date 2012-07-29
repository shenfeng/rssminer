package rssminer.db;

/**
 * google export
 *
 * @author feng
 *
 */
public class SubItem {
    private String title;
    private String url;
    private String category;

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String toString() {
        return "Item [title=" + title + ", url=" + url + ", category="
                + category + "]";
    }
}
