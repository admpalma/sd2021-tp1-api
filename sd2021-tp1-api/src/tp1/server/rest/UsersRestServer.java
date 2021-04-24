package tp1.server.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.Discovery;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class UsersRestServer {

    private static final Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "users";

    public static void main(String[] args) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format("http://%s:%s/rest", ip, PORT);

            ResourceConfig config = new ResourceConfig();
            config.register(new UsersRestResource(new Discovery(SERVICE, serverURI, args[0]),args[0]));

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
