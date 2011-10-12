package rssminer;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import rssminer.Utils.Pair;

/**
 * executed too often, it's better to be implemented for performance
 * 
 * @author feng
 * 
 */
public class Links {

    public static class LinksConf {
        List<String> acceptedTopDomains;
        List<Pattern> badDomainPattens;
        List<Pattern> badRssTitlePattens;
        List<Pattern> badRssUrlPattens;
        List<String> blackDomainStr;
        List<String> ignoredExtensions;

        public void setAcceptedTopDomains(List<String> acceptedTopDomains) {
            this.acceptedTopDomains = acceptedTopDomains;
        }

        public void setBadDomainPattens(List<Pattern> badDomainPattens) {
            this.badDomainPattens = badDomainPattens;
        }

        public void setBadRssTitlePattens(List<Pattern> badRssTitlePattens) {
            this.badRssTitlePattens = badRssTitlePattens;
        }

        public void setBadRssUrlPattens(List<Pattern> badRssUrlPattens) {
            this.badRssUrlPattens = badRssUrlPattens;
        }

        public void setBlackDomainStr(List<String> blackDomainStr) {
            this.blackDomainStr = blackDomainStr;
        }

        public void setIgnoredExtensions(List<String> ignoredExtensions) {
            this.ignoredExtensions = ignoredExtensions;
        }

    }

    final String[] mAcceptedTopDomains;
    final Pattern[] mBadDomainPattens;
    final Pattern[] mBadRssTitlePattens;
    final Pattern[] mBadRssUrlPattens;
    final String[] mBlackWords;
    final String[] mIgnoredExtensions;

    public Links(LinksConf conf) {
        mIgnoredExtensions = new String[conf.ignoredExtensions.size()];
        conf.ignoredExtensions.toArray(mIgnoredExtensions);

        mBadDomainPattens = new Pattern[conf.badDomainPattens.size()];
        conf.badDomainPattens.toArray(mBadDomainPattens);

        mBlackWords = new String[conf.blackDomainStr.size()];
        conf.blackDomainStr.toArray(mBlackWords);

        mAcceptedTopDomains = new String[conf.acceptedTopDomains.size()];
        conf.acceptedTopDomains.toArray(mAcceptedTopDomains);

        mBadRssUrlPattens = new Pattern[conf.badRssUrlPattens.size()];
        conf.badRssUrlPattens.toArray(mBadRssUrlPattens);

        mBadRssTitlePattens = new Pattern[conf.badRssTitlePattens.size()];
        conf.badRssTitlePattens.toArray(mBadRssTitlePattens);
    }

    public boolean keep(URI uri) {

        if (uri.getScheme() == null || !uri.getScheme().startsWith("http"))
            return false;

        String host = uri.getHost();
        if (host == null)
            return false;

        boolean keep = false;
        for (String topDomain : mAcceptedTopDomains) {
            if (host.endsWith(topDomain)) {
                keep = true;
                break;
            }
        }

        if (!keep)
            return false;

        String path = uri.getPath();
        for (String extension : mIgnoredExtensions) {
            if (path.endsWith(extension)) {
                // in the black list, return early
                return false;
            }
        }

        for (String black : mBlackWords) {
            if (host.contains(black)) {
                return false;
            }
        }

        for (Pattern bad : mBadDomainPattens) {
            if (bad.matcher(host).find()) {
                return false;
            }
        }

        return true;
    }

    public URI resolve(String base, String part) {
        try {
            URI uri = new URI(base);
            if (uri.getPath().length() == 0) {
                return uri.resolve("/" + part);
            } else {
                return uri.resolve(part);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Pair resolveRss(String base, String part, String title) {
        if (part == null)
            return null;
        if (title != null) {
            for (Pattern p : mBadRssTitlePattens) {
                if (p.matcher(title).find()) {
                    return null;
                }
            }
        }

        for (Pattern p : mBadRssUrlPattens) {
            if (p.matcher(part).find()) {
                return null;
            }
        }

        URI uri = resolve(base, part);

        return uri == null ? null : new Pair(uri.toString(), title);
    }

    public URI resoveAndClean(String base, String part) {

        if (part.isEmpty() || part.startsWith("#")
                || part.startsWith("mailto") || part.startsWith("javascript"))
            return null;

        URI result = resolve(base, part);
        if (result != null && keep(result)) {
            return result;
        } else {
            return null;
        }
    }
}
