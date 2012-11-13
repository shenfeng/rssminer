/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;


// user vote, read a feed.
public class UserEvent implements Event {

    final int userID;
    // -1 means recompute for user
    final int feedID;

    public UserEvent(int userID, int feedID) {
        // do it now
        this.userID = userID;
        this.feedID = feedID;
    }

    public String toString() {
        return "user:" + userID + ", feedid:" + feedID;

    }
}
