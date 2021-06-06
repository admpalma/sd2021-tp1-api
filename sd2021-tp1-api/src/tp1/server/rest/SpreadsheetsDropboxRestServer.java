package tp1.server.rest;

import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetDropbox;

import java.util.logging.Logger;

public class SpreadsheetsDropboxRestServer extends AbstractRestServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetsDropboxRestServer.class.getName());
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            String serverURI = initServer(URI -> new SpreadsheetsResource(
                    domain,
                    URI,
                    new Discovery(SERVICE, URI, domain),
                    new SpreadsheetDropbox("/" + domain, "true".equals(args[1]))
            ));
            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
