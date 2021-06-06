package tp1.util;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.io.IOException;

public class ZookeeperHelper {

    private static final String CHILD = "/child_";

    private final ZooKeeper zk;
    private final Leader leader;
    private final String serverURI;
    private final String root;
    private final String ownPath;
    private String watchedPath;


    public ZookeeperHelper(String hostPort, String domain, Leader leader, String serverURI) throws IOException {
        this.root = "/" + domain;
        this.leader = leader;
        this.serverURI = serverURI;
        zk = new ZooKeeper(hostPort, 3000, null);
        try {
            zk.create(root, serverURI.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException ignored) {
        }
        try {
            leader.setUrl(serverURI);
            ownPath = zk.create(root + CHILD, serverURI.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            zk.exists(root, updateLeaderOnChange(root, leader));
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        updateWatch();
    }

    private Watcher updateLeaderOnChange(String root, Leader leader) {
        return event -> {
            if (EventType.NodeDataChanged.equals(event.getType())) {
                try {
                    leader.setUrl(new String(zk.getData(root, updateLeaderOnChange(root, leader), null)));
                } catch (KeeperException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    private void updateWatch() {
        try {
            var previousPath = zk.getChildren(root, false).stream()
                    .filter(s -> s.compareTo(ownPath) < 0)
                    .max(String::compareTo)
                    .map(s -> s.substring(s.lastIndexOf('/')));

            if (previousPath.isPresent()) {
                watchedPath = root + previousPath.get();
                zk.exists(watchedPath, event -> {
                    if (EventType.NodeDeleted.equals(event.getType())) {
                        updateWatch();
                    }
                });
            } else {
                //TODO im leader
                zk.setData(root, serverURI.getBytes(), -1);
                leader.setUrl(serverURI);
            }
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
