package tp1.server.rest;

import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetHashMap;

import java.util.logging.Logger;

public class SpreadsheetsRestServer extends AbstractRestServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetsRestServer.class.getName());
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            String serverURI = initServer(URI -> new SpreadsheetsResource(domain, URI, new Discovery(SERVICE, URI, domain), new SpreadsheetHashMap()));
            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
