package tp1.server.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

public abstract class AbstractRestServer {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String HTTPS_S_S_REST = "https://%s:%s/rest";

    protected static String initServer(Function<String, ResourceConfig> function) throws NoSuchAlgorithmException, UnknownHostException {
        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format(HTTPS_S_S_REST, ip, PORT);
        //This allows client code executed by this server to ignore hostname verification
        HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

        ResourceConfig config = function.apply(serverURI);

        JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());
        return serverURI;
    }
}
