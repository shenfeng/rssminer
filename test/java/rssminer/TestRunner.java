package rssminer;

import org.junit.runner.JUnitCore;

import rssminer.test.HtmlUtilTest;
import rssminer.test.UtilsTest;;

public class TestRunner {

    public static void main(String[] args) {

        String[] classes = new String[] { HtmlUtilTest.class.getName(),
                UtilsTest.class.getName() };

        JUnitCore.main(classes);
    }
}
