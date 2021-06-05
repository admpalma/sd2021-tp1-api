package tp1.server.rest;

import tp1.server.resources.Discovery;

import java.util.logging.Logger;

public class UsersRestServer extends AbstractRestServer {

    private static final Logger Log = Logger.getLogger(UsersRestServer.class.getName());
    public static final String SERVICE = "users";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            String serverURI = initServer(URI -> new UsersResource(new Discovery(SERVICE, URI, domain), domain));

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
