package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    // parent znode for the service registry.
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZooKeeper zooKeeper;
    private String currentZNode = null;
    private List<String> allServiceAddresses = null;

    public ServiceRegistry(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        // every node that will create the ServiceRegistry object will check that the ServiceRegistry znode exists, if not - it will be created.
        createServiceRegistry();
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        // create a workerZnode under the RegistryZnode to register to the service registry.
        this.currentZNode = zooKeeper.create(REGISTRY_ZNODE + "/n_", metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("Registered to service registry");
    }

    public void registerForUpdates() {
        try {
            updateAddresses();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getAllServiceAddresses() throws InterruptedException, KeeperException {
        // in case caller forgot to register for updates.
        if (allServiceAddresses == null) {
            updateAddresses();
        }
        return allServiceAddresses;
    }

    // In case the node wants to gracefully unregister, or the node becomes the leader itself - and needs to unregister from the group of worker nodes.
    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        // first check if we indeed have an existing znode registered with the cluster.
        if (currentZNode != null && zooKeeper.exists(currentZNode, false) != null) {
            zooKeeper.delete(currentZNode, -1);
        }
    }

    private void createServiceRegistry() {
        // Check if znode already exists, if not then create it.
        try {
            if (zooKeeper.exists(REGISTRY_ZNODE, false) == null) {
                zooKeeper.create(REGISTRY_ZNODE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateAddresses() throws InterruptedException, KeeperException {
        // Get all the nodes registered in the cluster
        List<String> workerZnodes = zooKeeper.getChildren(REGISTRY_ZNODE, this);

        // temporary list of addresses.
        List<String> addresses = new ArrayList<>(workerZnodes.size());

        for (String workerZnode : workerZnodes) {
            String workerZnodeFullPath = REGISTRY_ZNODE + "/" + workerZnode;
            // Get the status of the workerZnode
            Stat stat = zooKeeper.exists(workerZnodeFullPath, false);
            // in case while we we reached here, and the worker went down, we simply skip this iteration.
            if (stat == null) {
                continue;
            }

            // get the service data from the node.
            byte[] addressBytes = zooKeeper.getData(workerZnodeFullPath, false, stat);
            String address = new String(addressBytes);
            // add it to the temporary list of addresses.
            addresses.add(address);
        }
        // update the cache of the addresses.
        this.allServiceAddresses = Collections.unmodifiableList(addresses);
        System.out.println("The cluster addresses are : " + this.allServiceAddresses);
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            // in case there is any change in the workerZnodes, we update the registry.
            updateAddresses();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
