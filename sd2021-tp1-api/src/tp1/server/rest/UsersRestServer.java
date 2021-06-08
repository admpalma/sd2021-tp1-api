package tp1.server.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.Discovery;

import java.util.logging.Logger;

public class UsersRestServer extends AbstractRestServer {

    public static final String SERVICE = "users";
    private static final Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    public static void main(String[] args) {
        try {
            String domain = args[0];
            String serverURI = initServer(serverURI1 -> {
                ResourceConfig config1 = new ResourceConfig();
                config1.register(new UsersResource(new Discovery(SERVICE, serverURI1, domain), domain, args[1]));
                return config1;
            });

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
