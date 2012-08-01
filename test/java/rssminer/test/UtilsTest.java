package rssminer.test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.Assert;
import org.junit.Test;

import rssminer.Utils;

public class UtilsTest {

    @Test
    public void testEncripyDecripy() throws InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InterruptedException {
        int total = 200;
        int start = new Random().nextInt(Integer.MAX_VALUE - total);
        for (int i = start; i < start + total; ++i) {
            String e = Utils.encrytUserID(i);
            int id = Utils.descryUserID(e);
            Assert.assertEquals(i, id);
        }

        final int key = new Random().nextInt();
        final String e = Utils.encrytUserID(key);

        ExecutorService execs = Executors.newFixedThreadPool(40);
        for (int i = 0; i < total; i++) {
            execs.submit(new Runnable() {
                public void run() {
                    try {
                        if (Utils.descryUserID(e) != key) {
                            throw new Exception();
                        }
                        if (!Utils.encrytUserID(key).equals(e)) {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        execs.shutdown();
        execs.awaitTermination(10, TimeUnit.DAYS);
    }
}
