package tp1.server.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.Discovery;

import java.util.logging.Logger;

public class UsersRestServer extends AbstractRestServer {

    private static final Logger Log = Logger.getLogger(UsersRestServer.class.getName());
    public static final String SERVICE = "users";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            final java.util.function.Function<String, Object> stringObjectFunction = URI -> new UsersResource(new Discovery(SERVICE, URI, domain), domain);
            String serverURI = initServer(serverURI1 -> {
                ResourceConfig config1 = new ResourceConfig();
                config1.register(stringObjectFunction.apply(serverURI1));
                return config1;
            });

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
