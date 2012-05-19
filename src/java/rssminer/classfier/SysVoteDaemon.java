package rssminer.classfier;

import static java.lang.System.currentTimeMillis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysVoteDaemon implements Runnable {

	private final DataSource ds;
	private static final Logger logger = LoggerFactory
			.getLogger(SysVoteDaemon.class);
	private volatile boolean running = false;
	private LinkedBlockingQueue<VoteEvent> queue = new LinkedBlockingQueue<VoteEvent>();
	Thread t;

	public SysVoteDaemon(DataSource ds) {
		this.ds = ds;
	}

	public void start() {
		t = new Thread(this);
		t.setName("user-vote");
		t.setDaemon(true);
		t.start();
		running = true;
		logger.debug("user vote daemon started");
	}

	public void stop() {
		running = false;
		t.interrupt();
	}

	public double getLikeScore(int userid) {
		return 1D;
	}

	public double getNetral(int userid) {
		return 0;
	}

	public void onUserVote(int userID, int feedID, boolean like) {
		queue.offer(new VoteEvent(userID, feedID, like));
	}

	public void onFeedFeched(int feedid, int subid) {

	}

	private void saveScores(int userID, double[] scores) throws SQLException {
		if (scores == null) {
			return;
		}
		Connection con = ds.getConnection();
		try {
			PreparedStatement ps = con
					.prepareStatement("update users set scores = ? where id = ?");
			ps.setString(1, scores[0] + "," + scores[1]);
			ps.setInt(2, userID);
			ps.executeUpdate();
			ps.close();
		} finally {
			con.close();
		}
	}

	public void run() {
		VoteEvent event = null;
		while (running) {
			try {
				event = queue.take();
				long start = currentTimeMillis();
				// TODO hard code 45 day
				int ts = (int) (start / 1000) - 3600 * 24 * 45;
				double[] scores = new UserSysVote(event.userID, ts, ds)
						.reCompute();
				saveScores(event.userID, scores);
			} catch (InterruptedException ignore) {
			} catch (Exception e) {
				if (event != null) {
					// queue.offer(event); // retry loop forever
				}
				logger.error(e.getMessage(), e);
			}
		}
		logger.debug("user vote daemon stopped");
	}
}
