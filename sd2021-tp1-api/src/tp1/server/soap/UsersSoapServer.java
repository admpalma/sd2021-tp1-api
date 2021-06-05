package tp1.server.soap;

import tp1.server.resources.Discovery;

import java.util.logging.Logger;

public class UsersSoapServer extends AbstractSoapServer {

    private static final Logger Log = Logger.getLogger(UsersSoapServer.class.getName());
    public static final String SERVICE = "users";
    public static final String SOAP_USERS_PATH = "/soap/users";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            String serverURI = initServer(URI -> new UsersWS(new Discovery(SERVICE, URI, domain), domain), SOAP_USERS_PATH);

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
