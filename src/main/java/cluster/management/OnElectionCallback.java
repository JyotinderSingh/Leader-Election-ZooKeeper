package cluster.management;

import org.apache.zookeeper.KeeperException;

public interface OnElectionCallback {
    // After every leader election, only one of the two methods will be called on a node.
    void onElectedToBeLeader() throws InterruptedException, KeeperException;

    void onWorker();
}
