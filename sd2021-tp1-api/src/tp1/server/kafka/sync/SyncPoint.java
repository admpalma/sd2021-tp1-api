package tp1.server.kafka.sync;


import java.util.HashMap;
import java.util.Map;

public class SyncPoint {
    private static SyncPoint instance;

    public static SyncPoint getInstance() {
        if (instance == null)
            instance = new SyncPoint();
        return instance;
    }

    private final Map<Long, String> result;

    public long getVersion() {
        return version;
    }

    private long version;


    private SyncPoint() {
        result = new HashMap<>();
        version = -1L;
    }

    /**
     * Waits for version to be at least equals to n
     */
    public synchronized void waitForVersion(long n) {
        while (version < n) {
            try {
                wait();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    /**
     * Assuming that results are added sequentially, returns null if the result is not available.
     */
    public synchronized String waitForResult(long n) {
        while (version < n) {
            try {
                wait();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        return result.remove(n);
    }

    /**
     * Updates the version and stores the associated result
     */
    public synchronized void setResult(long n, String res) {
        if (res != null)
            result.put(n, res);
        version = n;
        notifyAll();
    }

    /**
     * Cleans up results that will not be consumed
     */
    public synchronized void cleanupUntil(long n) {
        result.entrySet().removeIf(longStringEntry -> longStringEntry.getKey() < n);
    }

}
