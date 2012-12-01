/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class TestTest {

    public static void main(String[] args) throws SocketException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        byte[] key = null;
        while (interfaces.hasMoreElements()) {
            NetworkInterface in = interfaces.nextElement();
            String name = in.getDisplayName();
            if ("eth0".equalsIgnoreCase(name)) {
                key = in.getHardwareAddress();
            }

            System.out.println(name);

            Enumeration<NetworkInterface> subs = in.getSubInterfaces();
            while (subs.hasMoreElements()) {
                System.out.println(subs.nextElement().getName());
            }
        }
        key = Arrays.copyOf(key, 16);

        long start = System.currentTimeMillis();
        int times = 10;
        for (int i = 0; i < times; ++i) {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            byte[] bytes = cipher.doFinal(Integer.toString(112132).getBytes());
            // System.out.println(bytes.length);

            String s = DatatypeConverter.printBase64Binary(bytes);
            System.out.println(s);
            // continue;
            bytes = DatatypeConverter.parseBase64Binary(s);
            //
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            //
            int id = Integer.parseInt(new String(cipher.doFinal(bytes)));
            // System.out.println(input);
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("time " + time + "ms" + "; " + time / (double) times);
    }
}
