package rssminer.classfier;

public class VoteEvent {

	final int userID;
	final int feedID;
	final boolean like;

	public VoteEvent(int userID, int feedID, boolean like) {
		this.userID = userID;
		this.feedID = feedID;
		this.like = like;
	}
}
