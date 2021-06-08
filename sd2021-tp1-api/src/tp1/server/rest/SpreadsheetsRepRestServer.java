package tp1.server.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetHashMap;
import tp1.util.GenericExceptionMapper;
import tp1.util.VersionFilter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SpreadsheetsRepRestServer extends AbstractRestServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetsRepRestServer.class.getName());
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            String serverURI = initServer(serverURI1 -> {
                ResourceConfig config = new ResourceConfig();
                config.register(
                        new SpreadsheetsRepResource(
                                domain,
                                serverURI1,
                                new Discovery(SERVICE, serverURI1, domain),
                                new SpreadsheetHashMap(),
                                args[1])
                );
                config.register(GenericExceptionMapper.class);
                config.register(new VersionFilter());
                return config;
            });
            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
