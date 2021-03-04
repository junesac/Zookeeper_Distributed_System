package leader.election;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class LeaderElection implements Watcher {

	private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
	private static final int SESSION_TIMEOUT = 3000;
	private ZooKeeper zooKeeper;
	private String currentZnodeName;

	private static final String ELECTION_NAMESPACE = "/election";

	public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

		LeaderElection leaderElection = new LeaderElection();
		leaderElection.connectToZookeeper();

		// changes for leader election
		leaderElection.volunteerForLeadership();
		leaderElection.electLeader();

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

	// changes for leader election
	public void volunteerForLeadership() throws KeeperException, InterruptedException {
		String znodePrefix = ELECTION_NAMESPACE + "/c_";
		String znodeFullPath = zooKeeper.create(znodePrefix, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.EPHEMERAL_SEQUENTIAL);

		System.out.println("znode name : " + znodeFullPath);
		this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
	}

	public void electLeader() throws KeeperException, InterruptedException {
		List<String> childrens = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

		Collections.sort(childrens);
		String smallestChild = childrens.get(0);

		if (smallestChild.equals(currentZnodeName)) {
			System.out.println(" I am the leader");
			return;
		}
		System.out.println(" I am not the leader. " + smallestChild + " is the leader");

	}

}
