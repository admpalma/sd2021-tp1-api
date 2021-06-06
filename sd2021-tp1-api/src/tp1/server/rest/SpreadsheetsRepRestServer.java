package tp1.server.rest;

import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetHashMap;
import tp1.util.Leader;
import tp1.util.ZookeeperHelper;

import java.util.logging.Logger;

public class SpreadsheetsRepRestServer extends AbstractRestServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetsRepRestServer.class.getName());
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            Leader leader = new Leader();
            String serverURI = initServer(URI -> new SpreadsheetsRepResource(domain, URI, new Discovery(SERVICE, URI, domain), new SpreadsheetHashMap(), leader));
            ZookeeperHelper zookeeperHelper = new ZookeeperHelper("kafka:2181", domain, leader, serverURI);
            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
