# Service Registry/Discovery with Leader-Election using ZooKeeper

This repository implements a fully automated Service Registry/Discovery implementation backed by leader-election algorithm in a distributed
system using ZooKeeper, in Java.

Using this service registry nodes can:
* Register to the cluster by publishing their address.
* Register for updates tp get any node's address.

The leader keeps track of all the nodes registered under the Service Registry parent, and updates the list of available
service addresses as the nodes come up / go down.

## Features

* **Fault Tolerant** - Any number of nodes can fail, and the cluster remains functional.
* **Horizontally Scalable** - Nodes can be added dynamically, and they would join the cluster without any issues.
* **No performance bottlenecks** caused by herd effect, since each node only watches its one single predecessor, instead
  of all the nodes watching one leader.

As soon as the leader goes down a new leader is elected.

## Steps to run

Make sure ZooKeeper is installed and running on port 2181. (The sample default zookeeper_sample.cfg works fine.)

```bash
# Launch zk (from the appropriate directory)
$ ./zkServer.sh start
```

Inside the project directory, build the package. This will create a Jar file in a new 'target' directory.

```bash
$ mvn clean package
```

Run the Jar file produced by maven to add new nodes to the cluster. Run the Jar in multiple terminals to simulate
multiple nodes.

Terminal 1

```bash
$ java -jar target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8080
```

Terminal 2

```bash
$ java -jar target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8081
```

Terminal 3

```bash
$ java -jar target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar 8082
```

Try stopping any of the created nodes, and the cluster will gracefully recover, add more nodes, and they will quietly
join the cluster and show up in the service registry.

In case the leader goes down, another node will unregister itself from the workers and take role of the leader and start
tracking the nodes in the registry.
