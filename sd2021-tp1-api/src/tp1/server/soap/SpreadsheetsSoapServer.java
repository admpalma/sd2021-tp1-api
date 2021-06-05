package tp1.server.soap;

import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetHashMap;

import java.util.logging.Logger;

public class SpreadsheetsSoapServer extends AbstractSoapServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetsSoapServer.class.getName());
    public static final String SERVICE = "sheets";
    public static final String SOAP_SPREADSHEETS_PATH = "/soap/spreadsheets";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            String serverURI = initServer(URI -> new SpreadsheetsWS(domain, URI, new Discovery(SERVICE, URI, domain), new SpreadsheetHashMap()), SOAP_SPREADSHEETS_PATH);

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
