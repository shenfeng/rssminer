package rssminer.utils;

public class GCTest {

    public static void main(String[] args) {

        long start = System.currentTimeMillis();
        byte[] buffer = null;
        for (int i = 0; i < 10000; i++) {

            buffer = new byte[1024 * 1024];
            for (int j = 0; j < buffer.length; j++) {
                buffer[j] = 1;
            }
        }

        System.out.println(System.currentTimeMillis() - start);
        System.out.println(buffer);

    }
}
