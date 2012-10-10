package rssminer.classfier;

import java.util.concurrent.TimeUnit;

public class UserEvent extends Event {

    final int userID;
    final int feedID;

    public UserEvent(int userID, int feedID) {
        super(0);
        this.userID = userID;
        this.feedID = feedID;
    }

    public String toString() {
        return "user:" + userID + ", feedid:" + feedID + " due in "
                + getDelay(TimeUnit.SECONDS) + " s";
    }
}
