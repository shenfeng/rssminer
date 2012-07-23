package rssminer;

import org.junit.runner.JUnitCore;

import rssminer.test.HtmlUtilTest;

public class TestRunner {

    public static void main(String[] args) {

        String[] classes = new String[] { HtmlUtilTest.class.getName(), };

        JUnitCore.main(classes);
    }
}
