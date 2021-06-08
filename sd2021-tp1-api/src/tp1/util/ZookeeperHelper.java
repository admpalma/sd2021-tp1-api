package tp1.util;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ZookeeperHelper {

    private static final String CHILD = "/child_";

    private final ZooKeeper zk;
    private final Leader leader;
    private final String serverURI;
    private final String root;
    private final String ownPath;
    private String watchedPath;
    private Watcher watcher;


    public ZookeeperHelper(String hostPort, String domain, Leader leader, String serverURI) throws IOException {
        this.root = "/" + domain;
        this.leader = leader;
        this.serverURI = serverURI;
        zk = new ZooKeeper(hostPort, 3000, null);
        try {
            zk.create(root, serverURI.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("dosfigjiojçsdfgfoijdsfgijff VAI OPEN THE TCHEKA VAI AIN AIN E ISSO AI JUSTIN EI JUSTIN JUSTIN");
            leader.setUrl(serverURI);
        } catch (KeeperException | InterruptedException ignored) {
        }
        System.out.println("wtf matem-me");
        try {
            ownPath = zk.create(root + CHILD, serverURI.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
//            leader.setUrl(new String(zk.getData(root, updateLeaderOnChange(), null)));
//            for (int i = 0; i < 10; i++) {
//                System.out.println(new String(zk.getData(root, updateLeaderOnChange(), null)));
//            }
            updateLeaderOnChange().process(null);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        System.out.println("BLYAAAAAAAAT");
        updateWatch();
    }

    private Watcher updateLeaderOnChange() {
        System.out.println("opijdfsgiopjdfgsiopjfdgsoijpdfgsopijdfgsopijudfghsijopdfgsoijpdgsfijopdfsgijopdfgsijopdsfgs");
        if (watcher == null) {
            watcher = event -> {
                System.out.println("elect me sdfgopij");
//                System.out.println(event.getType());
//            if (EventType.NodeDataChanged.equals(event.getType())) {
                try {
                    System.out.println(root);
                    List<String> children = zk.getChildren(root, false);
                    System.out.println(children);
                    var previousPath = children.stream()
                            .min(String::compareTo).get();

                    System.out.println(root + "/" + previousPath);

                    byte[] data = zk.getData(root + "/" + previousPath, updateLeaderOnChange(), null);
                    System.out.println(Arrays.toString(data));
                    leader.setUrl(new String(data));
                    System.out.println("a serio");
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                    System.out.println("isto e bue fixe");
                    throw new IllegalStateException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                    System.out.println("isto e ainda mais fixe");
                    throw e;
                }
//            }
            };
        }
        System.out.println("4564264624624646");
        return watcher;
    }

    private void updateWatch() {
        try {
            System.out.println("caralho");
            List<String> children = zk.getChildren(root, false);
            System.out.println("CH324234234234234234234234234F");
            System.out.println(children);
            var previousPath = children.stream()
                    .filter(s -> s.compareTo(ownPath) < 0)
                    .max(String::compareTo);
            System.out.println("CHILDRENEFOISEFJSOIDF");
            System.out.println(children);
            if (previousPath.isPresent()) {
                watchedPath = root + "/" + previousPath.get();
                System.out.println(watchedPath);
                zk.exists(watchedPath, event -> {
                    System.out.println("dont call us we call you");
                    System.out.println(event.getType());
                    System.out.println(event.getPath());
                    //if (EventType.NodeDeleted == event.getType()) {
                        updateWatch();
                    System.out.println("fds");
                    //}
                });
            }
//            else {
//                //TODO im leader
//                zk.setData(root, serverURI.getBytes(), -1);
//                leader.setUrl(serverURI);
//            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println("...");
            throw new IllegalStateException(e);
        } catch (Exception fml) {
            fml.printStackTrace();
            System.out.println(fml.getMessage());
            System.out.println("idk");
            throw fml;
        }
    }

}
