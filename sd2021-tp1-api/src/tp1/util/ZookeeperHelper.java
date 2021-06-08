package tp1.util;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class ZookeeperHelper {

    private static final String CHILD = "/child_";

    private final ZooKeeper zk;
    private final Leader leader;
    private final String root;
    private final String ownPath;
    private String watchedPath;


    public ZookeeperHelper(String hostPort, String domain, Leader leader, String serverURI) throws IOException {
        this.root = "/" + domain;
        this.leader = leader;
        zk = new ZooKeeper(hostPort, 3000, null);
        try {
            zk.create(root, serverURI.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException ignored) {
        }
        try {
            ownPath = zk.create(root + CHILD, serverURI.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            updateLeaderOnChange();
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
        updateWatch();
    }

    private void updateLeaderOnChange() {
        try {
            String primaryPath = zk.getChildren(root, false).stream()
                    .min(String::compareTo)
                    .get();

            leader.setUrl(new String(zk.getData(root + "/" + primaryPath, event -> updateLeaderOnChange(), null)));
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void updateWatch() {
        try {
            var previousPath = zk.getChildren(root, false).stream()
                    .filter(s -> s.compareTo(ownPath) < 0)
                    .max(String::compareTo);
            if (previousPath.isPresent()) {
                watchedPath = root + "/" + previousPath.get();
                zk.exists(watchedPath, event -> updateWatch());
            }
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
