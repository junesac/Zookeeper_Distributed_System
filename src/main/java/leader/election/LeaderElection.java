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
import org.apache.zookeeper.data.Stat;

public class LeaderElection implements Watcher {

	private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
	private static final int SESSION_TIMEOUT = 3000;
	private ZooKeeper zooKeeper;
	private String currentZnodeName;

	private static final String ELECTION_NAMESPACE = "/election";

	private static final String TARGET_ZNODE = "/target_znode";

	public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

		LeaderElection leaderElection = new LeaderElection();
		leaderElection.connectToZookeeper();

		// changes for leader election
		leaderElection.volunteerForLeadership();
		leaderElection.reelectLeader();

		leaderElection.run();
		leaderElection.close();
		System.out.println("Disconnected from zookeeper, exiting app");

	}

	public void watchTargetZnode() throws KeeperException, InterruptedException {
		Stat stat = zooKeeper.exists(TARGET_ZNODE, this);
		if (stat == null) {
			return;
		}
		byte[] data = zooKeeper.getData(TARGET_ZNODE, this, stat);
		List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this);
		System.out.println("Data:  " + new String(data) + " Children are : " + children);

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

	/**
	 * To add all the events we are subscribe to
	 */
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

		// In exists method of zoo keeper (Method name: watchTargetZnode()) we
		// register for events of node creation & deletion
		case NodeDeleted:
			try {
				reelectLeader();
			} catch (KeeperException | InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			break;
		/*
		 * case NodeCreated: System.out.println(TARGET_ZNODE + " node created");
		 * break; // In getData method of zoo keeper (Method name:
		 * watchTargetZnode()) we // register for events of change of data in
		 * node case NodeDataChanged: System.out.println(TARGET_ZNODE +
		 * " data changed"); break;
		 * 
		 * // In getChildren method of zoo keeper (Method name:
		 * watchTargetZnode()) // we register for changes in list of childrens
		 * case NodeChildrenChanged: System.out.println(TARGET_ZNODE +
		 * " children changed"); break;
		 */

		}

		// to re register same events again.
		try {
			watchTargetZnode();
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
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

	public void reelectLeader() throws KeeperException, InterruptedException {
		Stat predecessorStat = null;
		String predecessorZnodeName = "";

		while (predecessorStat == null) {
			List<String> childrens = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

			Collections.sort(childrens);
			String smallestChild = childrens.get(0);

			if (smallestChild.equals(currentZnodeName)) {
				System.out.println("I am the leader");
				return;
			} else {
				System.out.println("I am not the leader.");
				int predecessorZnodeIndex = Collections.binarySearch(childrens, currentZnodeName) - 1;
				predecessorZnodeName = childrens.get(predecessorZnodeIndex);
				predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);

			}
		}

		System.out.println("Watching Znode: " + predecessorZnodeName);
		System.out.println();
	}

}
