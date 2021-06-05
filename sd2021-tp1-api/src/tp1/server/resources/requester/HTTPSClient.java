package tp1.server.resources.requester;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;

import static tp1.server.resources.requester.AbstractRequester.CONNECTION_TIMEOUT;
import static tp1.server.resources.requester.AbstractRequester.REPLY_TIMEOUT;

public class HTTPSClient {
    public static Client make(){
        HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

        return ClientBuilder.newClient(new ClientConfig())
        .property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT)
        .property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
    }
}
