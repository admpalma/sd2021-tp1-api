package tp1.server.resources.requester;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.util.Result;

import java.net.URI;

public class RestRequester extends AbstractRequester implements Requester {

    private final Client client;

    public RestRequester() {
        client = ClientBuilder.newClient(new ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT)
                .property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT));
    }

    @Override
    public Result<User> requestUser(URI serverURI, String userId, String password) {
        return defaultRetry(() -> {
            WebTarget target = client.target(serverURI).path(RestUsers.PATH);
            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();
            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
                User user = r.readEntity(User.class);
                if (!user.getPassword().equals(password)) {
                    return Result.error(Result.ErrorCode.BAD_REQUEST);
                }
                return Result.ok(user);
            } else {
                System.out.println("Error, HTTP error status: " + r.getStatus());
                return Result.error(Result.ErrorCode.valueOf(Response.Status.fromStatusCode(r.getStatus()).name()));
            }
        });
    }

    @Override
    public Result<String[][]> requestSpreadsheetRangeValues(String sheetURL, String userEmail, String range) {
        return defaultRetry(() -> {
            WebTarget target = client.target(sheetURL);
            Response r = target.path("rangeValues")
                    .queryParam("userEmail", userEmail)
                    .queryParam("range", range).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
                return Result.ok(r.readEntity(String[][].class));
            } else {
                return Result.error(Result.ErrorCode.valueOf(Response.Status.fromStatusCode(r.getStatus()).name()));
            }
        });
    }
}
