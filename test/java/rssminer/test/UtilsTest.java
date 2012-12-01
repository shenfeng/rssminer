/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.test;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import rssminer.Utils;

public class UtilsTest {

    @Test
    public void testSpaceSplit() {
        List<String> results = Utils.simpleSplit("  a  b   c");
        System.out.println(results);
        for (String str : results) {
            System.out.println(str.length());
        }
        Assert.assertEquals(3, results.size());
        results = Utils.simpleSplit("what; are you doing;");
        System.out.println(results);
        Assert.assertEquals(4, results.size());
        results = Utils.simpleSplit("我所在的是10号车厢，满载118人，分排坐");
        System.out.println(results);
        Assert.assertEquals(3, results.size());
        results = Utils.simpleSplit("a");
        Assert.assertEquals(1, results.size());

        results = Utils.simpleSplit(" a ");
        Assert.assertEquals(1, results.size());

        results = Utils.simpleSplit("");
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void testsplit() {
        List<String> list = Utils.split("abc; abcd; ", ';');
        System.out.println(list);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("abcd", list.get(1));

        list = Utils.split("abc ", ';');
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("abc", list.get(0));

        list = Utils.split("abc; ", ';');
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("abc", list.get(0));

        list = Utils.split(";abc; ", ';');
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("abc", list.get(0));

    }
}
