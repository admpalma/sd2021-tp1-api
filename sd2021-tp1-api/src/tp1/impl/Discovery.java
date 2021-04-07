package tp1.impl;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * <p>A class to perform service discovery, based on periodic service contact endpoint
 * announcements over multicast communication.</p>
 *
 * <p>Servers announce their *name* and contact *uri* at regular intervals. The server actively
 * collects received announcements.</p>
 *
 * <p>Service announcements have the following format:</p>
 *
 * <p>&lt;service-name-string&gt;&lt;delimiter-char&gt;&lt;service-uri-string&gt;</p>
 */
public class Discovery {
    private static final Logger Log = Logger.getLogger(Discovery.class.getName());

    static {
        // addresses some multicast issues on some TCP/IP stacks
        System.setProperty("java.net.preferIPv4Stack", "true");
        // summarizes the logging format
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
    }


    // The pre-agreed multicast endpoint assigned to perform discovery.
    static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
    static final int DISCOVERY_PERIOD = 1000;
    static final int DISCOVERY_TIMEOUT = 5000;

    // Used separate the two fields that make up a service announcement.
    private static final String DELIMITER = "\t";

    private final InetSocketAddress addr;
    private final String serviceName;
    private final String serviceURI;
    private final Map<String, Map<URI, Long>> serviceURIs;
    private MulticastSocket multicastSocket;

    // Main just for testing purposes
    public static void main(String[] args) throws Exception {
        Discovery discovery = new Discovery(DISCOVERY_ADDR, "test", "http://" + InetAddress.getLocalHost().getHostAddress());
        discovery.startEmitting();
        discovery.startReceiving();
    }

    /**
     * @param serviceName the name of the service to announce
     * @param serviceURI  an uri string - representing the contact endpoint of the service being announced
     */
    Discovery(InetSocketAddress addr, String serviceName, String serviceURI) {
        this.addr = addr;
        this.serviceName = serviceName;
        this.serviceURI = serviceURI;
        serviceURIs = new HashMap<>();
    }

    /**
     * @param serviceName the name of the service to announce
     * @param serviceURI  an uri string - representing the contact endpoint of the service being announced
     */
    Discovery(String serviceName, String serviceURI) {
        this.addr = DISCOVERY_ADDR;
        this.serviceName = serviceName;
        this.serviceURI = serviceURI;
        serviceURIs = new ConcurrentHashMap<>();
    }

    /**
     * Creates and configures {@link Discovery#multicastSocket} in case it hadn't yet been done
     *
     * @throws IOException
     */
    private void initMulticastSocket() throws IOException {
        if (multicastSocket == null) {
            multicastSocket = new MulticastSocket(addr.getPort());
            multicastSocket.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
        }
    }

    public void startEmitting() {
        Log.info(String.format("Starting Discovery emission on: %s for: %s -> %s\n", addr, serviceName, serviceURI));

        byte[] announceBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
        DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

        try {
            initMulticastSocket();
            //TODO cancelable emission?
            ScheduledExecutorService emitterExecutor = Executors.newSingleThreadScheduledExecutor();
            ScheduledFuture<?> emitter = emitterExecutor.scheduleAtFixedRate(() -> {
                try {
                    multicastSocket.send(announcePkt);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 0, DISCOVERY_PERIOD, TimeUnit.MILLISECONDS);
            ScheduledFuture<?> gc = emitterExecutor.scheduleAtFixedRate(() -> {
                long currentTimeMillis = System.currentTimeMillis();
                for(Map<URI, Long> s: serviceURIs.values()){
                    for(Map.Entry<URI,Long> e:s.entrySet()){
                        if (e.getValue()+DISCOVERY_TIMEOUT<currentTimeMillis){
                            s.remove(e.getKey());
                        }
                    }
                }
            }, DISCOVERY_TIMEOUT, DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts sending service announcements at regular intervals...
     */
    public void startReceiving() {
        Log.info(String.format("Starting Discovery reception on: %s for: %s -> %s\n", addr, serviceName, serviceURI));

        try {
            initMulticastSocket();
            //TODO cancelable reception?
            ExecutorService receiverExecutor = Executors.newSingleThreadExecutor();
            Future<?> receiver = receiverExecutor.submit(() -> {
                final int maxLength = 1024;
                DatagramPacket pkt = new DatagramPacket(new byte[maxLength], maxLength);
                while (true) {
                    try {
                        pkt.setLength(maxLength);
                        multicastSocket.receive(pkt);
                        //TODO: we could dump this onto the other thread but it is so fast that it doesn't matter
                        String msg = new String(pkt.getData(), 0, pkt.getLength());
                        String[] msgElems = msg.split(DELIMITER);
                        if (msgElems.length == 2) {    //periodic announcement
                            String hostName = pkt.getAddress().getCanonicalHostName();
                            String hostAddress = pkt.getAddress().getHostAddress();
                            URI uri = new URI(hostAddress);
                            serviceURIs.computeIfAbsent(hostName, k -> new HashMap<>())
                                    .put(uri, System.currentTimeMillis());
                        }
                    } catch (IOException | URISyntaxException e) {
                        // do nothing
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the known servers for a service.
     *
     * @param serviceName the name of the service being discovered
     * @return an array of URI with the service instances discovered.
     */
    public URI[] knownUrisOf(String serviceName) {
        Map<URI, Long> serviceNameURIs = serviceURIs.get(serviceName);
        if (serviceNameURIs == null) {
            //TODO: exception or static cached array?
            return new URI[0];
        } else {
            long currentTimeMillis = System.currentTimeMillis();
            //TODO: actually clean the map (will cause concurrency issues)
            //probs good idea to use ConcurrentHashMap and filter every x time on the emitter thread, since it's free most of the time
            //and here we'd just map, not filter, it doesn't need to be perfect anyway
            return serviceNameURIs.keySet().toArray(new URI[0]);
        }
    }
}
