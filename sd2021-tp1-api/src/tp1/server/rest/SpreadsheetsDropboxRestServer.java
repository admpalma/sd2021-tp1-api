package tp1.server.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetDropbox;
import tp1.server.resources.SpreadsheetHashMap;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class SpreadsheetsDropboxRestServer {

    private static Logger Log = Logger.getLogger(SpreadsheetsDropboxRestServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            //This allows client code executed by this server to ignore hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            String serverURI = String.format("https://%s:%s/rest", ip, PORT);

            ResourceConfig config = new ResourceConfig();
            config.register(
                new SpreadsheetsRestResource(
                    args[0],
                    serverURI,
                    new Discovery(SERVICE, serverURI, args[0]),
                    new SpreadsheetDropbox("/"+args[0],"true".equals(args[1]))
                )
            );

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
