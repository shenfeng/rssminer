package rssminer;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Locale;

public class HrefRewriter {

    private static final char QUOTE = '\"';

    private StringBuilder sb;
    private URI urlBase;
    private String proxyUrl;
    private int mIndex = 0;
    private String mHtml;
    private String lowerCaseHtml;

    public HrefRewriter(String html, String urlBase, String proxyUrl)
            throws URISyntaxException {
        sb = new StringBuilder(html.length() + 100);
        this.mHtml = html;
        this.lowerCaseHtml = html.toLowerCase(Locale.US);
        this.urlBase = new URI(urlBase);
        this.proxyUrl = proxyUrl;
    }

    private boolean advanceUtil(String str) {
        int idx = lowerCaseHtml.indexOf(str, mIndex + 1);
        int end = lowerCaseHtml.indexOf('>', mIndex + 1);
        if (idx != -1 && end > idx) {
            int e = idx + str.length();
            sb.append(mHtml.substring(mIndex, e));
            mIndex = e;
            return true;
        } else {
            return false;
        }
    }

    private boolean advanceUtil(char c1, char c2) {

        char ch = lowerCaseHtml.charAt(mIndex);
        while ((ch != c1 && ch != c2 && ch != '>') && mIndex < mHtml.length()) {
            sb.append(mHtml.charAt(mIndex));
            ++mIndex;
            ch = mHtml.charAt(mIndex);
        }
        // sb.append(html.charAt(mIndex));
        ++mIndex;// advance to the next char, does not include c1 || c2

        if (ch == '>' || mIndex == mHtml.length()) {
            return false;
        } else {
            return true;
        }
    }

    private String getNextJsSrc() {
        // System.out.println(mHtml.substring(mIndex, mIndex + 60));
        if (!advanceUtil("src")) {
            return null;
        }

        if (!advanceUtil('\"', '\'')) {
            return null;
        }
        int start = mIndex - 1;
        int idx = mHtml.indexOf(mHtml.charAt(start), mIndex);
        if (idx != -1) {
            mIndex = idx + 1; // include ' || "
            return mHtml.substring(start + 1, idx);
        }

        return null;
    }

    String getNextLink() {
        while (true) {
            int idx = lowerCaseHtml.indexOf('<', mIndex);
            if (idx == -1) {
                sb.append(mHtml.substring(mIndex));
                mIndex += mHtml.length() - 1;
                return null;
            }

            sb.append(mHtml.substring(mIndex, idx + 1));

            mIndex = idx + 1;

            if (idx + 6 < mHtml.length()) {
                String tag = lowerCaseHtml.substring(mIndex, idx + 4);
                if ("scr".equals(tag)) { // js(script[src])
                    sb.append(mHtml.substring(mIndex, idx + 7));
                    System.out.println(mHtml.substring(mIndex, mIndex + 70));
                    mIndex += 6;
                    return getNextJsSrc();
                } else if ("lin".equals(tag)) { // css(link[href])
                    sb.append(mHtml.substring(mIndex, idx + 5));
                    mIndex += 4;
                } else if ("img".equals(tag)) { // img(img[src])
                    sb.append(mHtml.substring(mIndex, idx + 4));
                    mIndex += 3;
                }
            }
        }

    }

    public String rewrite() throws URISyntaxException,
            UnsupportedEncodingException {
        String nextLink;
        while ((nextLink = getNextLink()) != null) {
            String s = urlBase.resolve(nextLink).toString();
            String e = URLEncoder.encode(s, "utf8");
            sb.append(QUOTE);
            sb.append(proxyUrl).append(e).append(QUOTE);
        }
        // sb.append(mHtml.substring(mIndex));
        return sb.toString();
    }
}
