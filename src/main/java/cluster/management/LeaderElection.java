package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    //    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
//    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;
    private static final String ELECTION_NAMESPACE = "/election";
    private String currentZnodeName;
    private final OnElectionCallback onElectionCallback;

    public LeaderElection(ZooKeeper zooKeeper, OnElectionCallback onElectionCallback) {
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
    }

//    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
//        LeaderElection leaderElection = new LeaderElection();
//
//        leaderElection.connectToZookeeper();
//
//        leaderElection.volunteerForLeadership();
//        leaderElection.reelectLeader();
//
//        leaderElection.run();
//        // call the close method when the main thread wakes up.
//        leaderElection.close();
//        System.out.println("Disconnected from Zookeeper, exiting application.");
//    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        // create a new EPHEMERAL_SEQUENTIAL znode, this means that the node will get deleted if gets disconnected from ZK.
        // Sequential means the name of the znode is going to be appended with the sequence number depending on
        // the order of addition of the znodes under the election parent znode.
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name: " + znodeFullPath);
        // Extract the znode name without the path, and store it in the class member variable.
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");

    }

    public void reelectLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";

        // we run a while loop to avoid the scenario when the else block might have not completely executed, and the predecessor might have already crashed.
        // so we run the while loop until this node gets selected as the leader, or we find an existing znode to watch for failures.
        while (predecessorStat == null) {

            // Get the children znodes of the election znode.
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

            // Now we want to check if this znode is the smallest in the list. For that we first sort the list lexicographically.
            Collections.sort(children);
            // After the sorting, the smallest znode (based on name) is going to be the first node in the sequence.
            String smallestChild = children.get(0);

            // if this znode is the smallest.
            if (smallestChild.equals(currentZnodeName)) {
                System.out.println("I am the leader. [ZNODE: " + currentZnodeName + "]");

                // If this node is elected as the leader, before returning we need to call the onElectedToBeLeader method.
                onElectionCallback.onElectedToBeLeader();
                return;
            } else {
                System.out.println("[ZNODE: " + currentZnodeName + "] I am not the leader, the leader is [ZNODE: " + smallestChild + "]");

                // find the predecessor node in the znode sequence, to figure out what znode we should watch for failures.
                // we find the index of our znode, and subtract 1 from it to get the predecessor.
                // We know we are not the leader, so we know the current node is not the smallest node. So we are safe from OutOfBoundsException
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                // watch the predecessor.
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
            }
        }

        // If this node is a worker, we need to call the onWorker method.
        onElectionCallback.onWorker();

        System.out.println("Watching znode: " + predecessorZnodeName);
        System.out.println();
    }


    /**
     * Called by ZooKeeper on a separate thread whenever there is a new event coming from the zookeeper server.
     *
     * @param event event arriving from the zookeeper server.
     */
    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
//            case None:
//                // General zookeeper connection events have None type.
//                if (event.getState() == Event.KeeperState.SyncConnected) {
//                    // we are successfully connected to Zookeeper.
//                    System.out.println("[EVENT] Successfully connected to ZooKeeper.");
//                } else {
//                    // in case we lose connection with zookeeper.
//                    // Wake up main thread and exit.
//                    synchronized (zooKeeper) {
//                        System.out.println("[EVENT] Disconnected from Zookeeper.");
//                        zooKeeper.notifyAll();
//                    }
//                }
//                break;
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException | KeeperException e) {
                }
        }
    }
}
