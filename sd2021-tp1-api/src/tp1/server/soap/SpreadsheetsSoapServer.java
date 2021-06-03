package tp1.server.soap;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetHashMap;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class SpreadsheetsSoapServer {
    private static Logger Log = Logger.getLogger(SpreadsheetsSoapServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "sheets";
    public static final String SOAP_SPREADSHEETS_PATH = "/soap/spreadsheets";

    public static void main(String[] args) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();

            //This allows client code executed by this server to ignore hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            String serverURI = String.format("https://%s:%s/soap", ip, PORT);

            //Create an https configurator to define the SSL/TLS context
            HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());

            HttpsServer server = HttpsServer.create(new InetSocketAddress(ip, PORT), 0);

            server.setHttpsConfigurator(configurator);

            server.setExecutor(Executors.newCachedThreadPool());


            Endpoint soapUsersEndpoint = Endpoint.create(new SpreadsheetsSoapResource(args[0], serverURI, new Discovery(SERVICE, serverURI, args[0]),new SpreadsheetHashMap()));

            soapUsersEndpoint.publish(server.createContext(SOAP_SPREADSHEETS_PATH));

            server.start();

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
