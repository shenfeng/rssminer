/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;

import java.util.concurrent.TimeUnit;

// user vote, read a feed.
public class UserEvent extends Event {

    final int userID;
    // -1 means recompute for user
    final int feedID;

    public UserEvent(int userID, int feedID) {
        // do it now
        super(0);
        this.userID = userID;
        this.feedID = feedID;
    }

    public String toString() {
        return "user:" + userID + ", feedid:" + feedID + " due in "
                + getDelay(TimeUnit.SECONDS) + " s";
    }
}
