package rssminer;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;

import rssminer.jsoup.HtmlUtils;
import rssminer.search.Mapper;
import rssminer.search.Searcher;

// https://github.com/tnm/murmurhash-java
final class MurmurHash {

    // all methods static; private constructor.
    private MurmurHash() {
    }

    /**
     * Generates 32 bit hash from byte array of the given length and seed.
     * 
     * @param data
     *            byte array to hash
     * @param length
     *            length of the array to hash
     * @param seed
     *            initial seed value
     * @return 32 bit hash of the given array
     */
    public static int hash32(final byte[] data, int length, int seed) {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        // Initialize the hash to a random value
        int h = seed ^ length;
        int length4 = length / 4;

        for (int i = 0; i < length4; i++) {
            final int i4 = i * 4;
            int k = (data[i4 + 0] & 0xff) + ((data[i4 + 1] & 0xff) << 8)
                    + ((data[i4 + 2] & 0xff) << 16)
                    + ((data[i4 + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        // Handle the last few bytes of the input array
        switch (length % 4) {
        case 3:
            h ^= (data[(length & ~3) + 2] & 0xff) << 16;
        case 2:
            h ^= (data[(length & ~3) + 1] & 0xff) << 8;
        case 1:
            h ^= (data[length & ~3] & 0xff);
            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    /**
     * Generates 32 bit hash from byte array with default seed value.
     * 
     * @param data
     *            byte array to hash
     * @param length
     *            length of the array to hash
     * @return 32 bit hash of the given array
     */
    public static int hash32(final byte[] data, int length) {
        return hash32(data, length, 0x9747b28c);
    }

    /**
     * Generates 32 bit hash from a string.
     * 
     * @param text
     *            string to hash
     * @return 32 bit hash of the given string
     */
    public static int hash32(final String text) {
        final byte[] bytes = text.getBytes();
        return hash32(bytes, bytes.length);
    }

    /**
     * Generates 32 bit hash from a substring.
     * 
     * @param text
     *            string to hash
     * @param from
     *            starting index
     * @param length
     *            length of the substring to hash
     * @return 32 bit hash of the given string
     */
    public static int hash32(final String text, int from, int length) {
        return hash32(text.substring(from, from + length));
    }

    /**
     * Generates 64 bit hash from byte array of the given length and seed.
     * 
     * @param data
     *            byte array to hash
     * @param length
     *            length of the array to hash
     * @param seed
     *            initial seed value
     * @return 64 bit hash of the given array
     */
    public static long hash64(final byte[] data, int length, int seed) {
        final long m = 0xc6a4a7935bd1e995L;
        final int r = 47;

        long h = (seed & 0xffffffffl) ^ (length * m);

        int length8 = length / 8;

        for (int i = 0; i < length8; i++) {
            final int i8 = i * 8;
            long k = ((long) data[i8 + 0] & 0xff)
                    + (((long) data[i8 + 1] & 0xff) << 8)
                    + (((long) data[i8 + 2] & 0xff) << 16)
                    + (((long) data[i8 + 3] & 0xff) << 24)
                    + (((long) data[i8 + 4] & 0xff) << 32)
                    + (((long) data[i8 + 5] & 0xff) << 40)
                    + (((long) data[i8 + 6] & 0xff) << 48)
                    + (((long) data[i8 + 7] & 0xff) << 56);

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        switch (length % 8) {
        case 7:
            h ^= (long) (data[(length & ~7) + 6] & 0xff) << 48;
        case 6:
            h ^= (long) (data[(length & ~7) + 5] & 0xff) << 40;
        case 5:
            h ^= (long) (data[(length & ~7) + 4] & 0xff) << 32;
        case 4:
            h ^= (long) (data[(length & ~7) + 3] & 0xff) << 24;
        case 3:
            h ^= (long) (data[(length & ~7) + 2] & 0xff) << 16;
        case 2:
            h ^= (long) (data[(length & ~7) + 1] & 0xff) << 8;
        case 1:
            h ^= (long) (data[length & ~7] & 0xff);
            h *= m;
        }
        ;

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        return h;
    }

    /**
     * Generates 64 bit hash from byte array with default seed value.
     * 
     * @param data
     *            byte array to hash
     * @param length
     *            length of the array to hash
     * @return 64 bit hash of the given string
     */
    public static long hash64(final byte[] data, int length) {
        return hash64(data, length, 0xe17a1465);
    }

    /**
     * Generates 64 bit hash from a string.
     * 
     * @param text
     *            string to hash
     * @return 64 bit hash of the given string
     */
    public static long hash64(final String text) {
        final byte[] bytes = text.getBytes();
        return hash64(bytes, bytes.length);
    }

    /**
     * Generates 64 bit hash from a substring.
     * 
     * @param text
     *            string to hash
     * @param from
     *            starting index
     * @param length
     *            length of the substring to hash
     * @return 64 bit hash of the given array
     */
    public static long hash64(final String text, int from, int length) {
        return hash64(text.substring(from, from + length));
    }
}

public class SimHash {
    static final int MIN_TERM = 5;

    public static long simHash(String html) {
        String text = Mapper.toSimplified(HtmlUtils.text(html));
        if (html.isEmpty()) {
            return -1;
        }
        int[] bits = new int[64];
        TokenStream stream = Searcher.analyzer.tokenStream("",
                new StringReader(text));
        CharTermAttribute c = stream.getAttribute(CharTermAttribute.class);
        boolean b = false;
        Set<String> unique = new HashSet<String>();
        try {
            while (stream.incrementToken()) {
                String term = new String(c.buffer(), 0, c.length());
                if (!b) {
                    unique.add(term);
                    b = unique.size() >= MIN_TERM;
                }
                long code = MurmurHash.hash64(term);
                for (int j = 0; j < bits.length; j++) {
                    if (((code >>> j) & 0x1) == 0x1) {
                        bits[j] += 1;
                    } else {
                        bits[j] -= 1;
                    }
                }
            }
        } catch (IOException ignore) { // can not happen
        }

        if (!b) {
            return -1;
        }

        long fingerprint = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] > 0) {
                fingerprint += (1 << i);
            }
        }
        return fingerprint;
    }

    public static long simHash(int feedid) throws IOException {
        IndexReader reader = Searcher.SEARCHER.acquireReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        int docid = Searcher.SEARCHER.feedID2DocID(searcher, feedid);
        if (docid == -1) {
            return -1;
        }
        TermFreqVector tv = reader.getTermFreqVector(docid, Searcher.CONTENT);
        if (tv == null || tv.getTerms().length < MIN_TERM) {
            return -1;
        }
        int[] bits = new int[64];
        String[] terms = tv.getTerms();
        int[] frequencies = tv.getTermFrequencies();
        for (int i = 0; i < terms.length; i++) {
            String term = terms[i];
            long code = MurmurHash.hash64(term);
            for (int j = 0; j < bits.length; j++) {
                if (((code >>> j) & 0x1) == 0x1) {
                    bits[j] += frequencies[i];
                } else {
                    bits[j] -= frequencies[i];
                }
            }
        }
        long fingerprint = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] > 0) {
                fingerprint += (1 << i);
            }
        }
        return fingerprint;
    }

    public static int hammingDistance(long x, long y) {
        int dist = 0;
        long val = x ^ y;
        while (val != 0) {
            ++dist;
            val &= val - 1;
        }
        return dist;
    }

}
