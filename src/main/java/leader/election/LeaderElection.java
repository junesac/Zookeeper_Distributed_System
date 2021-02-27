package leader.election;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class LeaderElection implements Watcher {

	private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
	private static final int SESSION_TIMEOUT = 3000;
	private ZooKeeper zooKeeper;

	public static void main(String[] args) throws IOException, InterruptedException {

		LeaderElection leaderElection = new LeaderElection();
		leaderElection.connectToZookeeper();
		leaderElection.run();

		leaderElection.close();
		System.out.println("Disconnected from zookeeper, exiting app");

	}

	private void close() throws InterruptedException {
		zooKeeper.close();
	}

	public void connectToZookeeper() throws IOException {
		this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);

	}

	public void run() throws InterruptedException {
		synchronized (zooKeeper) {
			zooKeeper.wait();
		}
	}

	@Override
	public void process(WatchedEvent event) {
		switch (event.getType()) {
		case None:
			if (event.getState() == Event.KeeperState.SyncConnected) {
				System.out.println("Connected to zookeeper successfully !!");
			} else {
				synchronized (zooKeeper) {
					System.out.println("disconnected from zookeeper event !!");
					zooKeeper.notifyAll();
				}
			}
			break;
		}
	}
}
