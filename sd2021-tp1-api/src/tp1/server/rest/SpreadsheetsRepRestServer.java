package tp1.server.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetHashMap;
import tp1.util.GenericExceptionMapper;
import tp1.util.Leader;
import tp1.util.VersionFilter;
import tp1.util.ZookeeperHelper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SpreadsheetsRepRestServer extends AbstractRestServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetsRepRestServer.class.getName());
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            Leader leader = new Leader();
            String serverURI = initServer(serverURI1 -> {
                ResourceConfig config = new ResourceConfig();
                AtomicInteger version = new AtomicInteger(0);
                config.register(new SpreadsheetsRepResource(domain, serverURI1, new Discovery(SERVICE, serverURI1, domain), new SpreadsheetHashMap(), leader, version));
                config.register(GenericExceptionMapper.class);
                config.register(new VersionFilter(version));
                return config;
            });
            ZookeeperHelper zookeeperHelper = new ZookeeperHelper("kafka:2181", domain, leader, serverURI);
            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
