package tp1.server.soap;

import com.sun.net.httpserver.HttpServer;
import jakarta.xml.ws.Endpoint;
import tp1.server.resources.Discovery;

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
            String serverURI = String.format("http://%s:%s/soap", ip, PORT);

            HttpServer server = HttpServer.create(new InetSocketAddress(ip, PORT), 0);

            server.setExecutor(Executors.newCachedThreadPool());

            Endpoint soapUsersEndpoint = Endpoint.create(new SpreadsheetsSoapResource(args[0], serverURI, new Discovery(SERVICE, serverURI, args[0])));

            soapUsersEndpoint.publish(server.createContext(SOAP_SPREADSHEETS_PATH));

            server.start();

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            //More code can be executed here...
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
