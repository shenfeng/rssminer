/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;

public class Watch {

    long start;

    public Watch start() {
        start = System.currentTimeMillis();
        return this;
    }

    public long time() {
        long tmp = System.currentTimeMillis();
        long time = tmp - start;
        start = tmp;
        return time;
    }
}
