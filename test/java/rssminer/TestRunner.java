package rssminer;

import org.junit.runner.JUnitCore;

import rssminer.test.HtmlUtilTest;
import rssminer.test.MapperTest;

public class TestRunner {

    public static void main(String[] args) {

        String[] classes = new String[] { HtmlUtilTest.class.getName(), MapperTest.class.getName()};

        JUnitCore.main(classes);
    }
}
