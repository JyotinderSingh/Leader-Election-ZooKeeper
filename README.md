# Leader-Election Implementation with ZooKeeper

This repository shows a leader-election implementation in a distributed system using ZooKeeper, in Java.

## Features

* **Fault Tolerant** - Any number of nodes can fail, and the cluster remains functional.
* **Horizontally Scalable** - Nodes can be added dynamically, and they would join the cluster without any issues.
* **No performance bottlenecks** caused by herd effect, since each node only watches its one single predecessor, instead
  of all the nodes watching one leader.
  
As soon as the leader goes down a new leader is elected.

## Steps to run
Make sure ZooKeeper is running, and it contains an election parent.
```bash
# Launch zk (from the appropriate directory)
$ ./zkServer.sh start

# Create an election parent
$ ./zkCli.sh
[zk]> create /election  
```

Inside the project directory, build the package. This will create a Jar file in a new 'target' directory.
```bash
$ mvn clean package
```

Run the Jar file produced by maven to add new nodes to the cluster.
Run the Jar in multiple terminals to simulate multiple nodes.
```bash
$ java -jar target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Try stopping any of the created nodes, and the cluster will auto heal. Add more nodes, and they will quietly join the cluster.
