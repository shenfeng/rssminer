package rssminer.test;

import junit.framework.Assert;
import org.junit.Test;
import rssminer.Utils;

public class SimHashTest {
    @Test
    public void testHammingDistance() {
        int diff = 1;
        String strx = "0";
        String stry = "1";
        for (int i = 0; i < 31; i++) {
            int x = Integer.parseInt(strx, 2);
            int y = Integer.parseInt(stry, 2);
            Assert.assertSame(diff, Utils.hammingDistance(x, y));
            double r = Math.random();
            if (r > 0.7) {
                diff += 1;
                strx += "1";
                stry += "0";
            } else if (r > 0.4) {
                diff += 1;
                strx += "0";
                stry += "1";
            } else {
                strx += "1";
                stry += "1";
            }
        }
    }

    private int simhash(String[] terms) {
        int[] bits = new int[32];
        for (String term : terms) {
            int code = term.hashCode();
            for (int i = 0; i < bits.length; i++) {
                if (((code >>> i) & 0x1) == 0x1) {
                    ++bits[i];
                } else {
                    --bits[i];
                }
            }
        }
        int fingerprint = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] > 0) {
                fingerprint += (1 << i);
            }
        }
        return fingerprint;
    }

    @Test
    public void testSimHash() {
        int h1 = simhash("the cat sat on a mat".split(" "));
        int h2 = simhash("the cat sat on the mat".split(" "));
        int h3 = simhash("we all scream for ice cream".split(" "));
        System.out.println(Utils.hammingDistance(h1, h2));
        System.out.println(Utils.hammingDistance(h1, h3));
        System.out.println(Utils.hammingDistance(h2, h3));
    }
}
