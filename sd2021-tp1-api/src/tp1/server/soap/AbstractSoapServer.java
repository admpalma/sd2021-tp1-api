package tp1.server.soap;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.function.Function;

public abstract class AbstractSoapServer {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    protected static final String HTTPS_S_S_SOAP = "https://%s:%s/soap";
    public static final int PORT = 8080;

    protected static String initServer(Function<String, Object> wsFunction, String soapPath) throws NoSuchAlgorithmException, IOException {

        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format(HTTPS_S_S_SOAP, ip, PORT);
        Object webService = wsFunction.apply(serverURI);

        //This allows client code executed by this server to ignore hostname verification
        HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

        //Create an https configurator to define the SSL/TLS context
        HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());

        HttpsServer server = HttpsServer.create(new InetSocketAddress(ip, PORT), 0);

        server.setHttpsConfigurator(configurator);

        server.setExecutor(Executors.newCachedThreadPool());

        Endpoint soapUsersEndpoint = Endpoint.create(webService);

        soapUsersEndpoint.publish(server.createContext(soapPath));

        server.start();
        return serverURI;
    }

}
