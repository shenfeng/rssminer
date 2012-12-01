/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import org.junit.Test;

public class TestTest {

    @Test
    public void testChar() {
        String s = "thisto是;ss1111fsd";
        for (int i = 0; i < s.length(); ++i) {
            System.out.println(s.charAt(i) + "\t" + Character.isLetter(s.charAt(i)));
        }
    }

}
