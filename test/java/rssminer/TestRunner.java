/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;

import org.junit.runner.JUnitCore;

import rssminer.test.HtmlUtilTest;
import rssminer.test.MapperTest;

public class TestRunner {

    public static void main(String[] args) {

        String[] classes = new String[] { HtmlUtilTest.class.getName(),
                rssminer.test.UtilsTest.class.getName(), MapperTest.class.getName() };

        JUnitCore.main(classes);
    }
}
