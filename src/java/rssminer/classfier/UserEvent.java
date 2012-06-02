package rssminer.classfier;

public class UserEvent implements Event {

    final int userID;
    final int feedID;

    public UserEvent(int userID, int feedID) {
        this.userID = userID;
        this.feedID = feedID;
    }
}
