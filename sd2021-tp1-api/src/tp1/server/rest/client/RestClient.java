package tp1.server.rest.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.api.service.util.Result;
import tp1.api.service.util.Result.ErrorCode;

import java.net.URI;

import static tp1.api.service.util.Result.error;
import static tp1.api.service.util.Result.ok;

/**
 * Shared behavior among REST clients.
 * <p>
 * Holds client and target information.
 * <p>
 * Translates http responses to Result<T> for interoperability.
 *
 * @author smduarte
 */
abstract class RestClient extends RetryClient {

    protected final URI uri;
    protected final Client client;
    protected final WebTarget target;
    protected final ClientConfig config;

    public RestClient(URI uri, String path) {
        this.uri = uri;
        this.config = new ClientConfig();
        this.config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
        this.config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        this.config.property(ClientProperties.FOLLOW_REDIRECTS, true);

        this.client = ClientBuilder.newClient(config);
        this.target = this.client.target(uri).path(path);
    }

    // Get the actual response, when the status matches what was expected, otherwise
    // return the error code.
    protected <T> Result<T> verifyResponse(Response r, Status expected) {
        try (r) {
            StatusType status = r.getStatusInfo();
            System.out.println("i should be ok ffs: " + status);
            System.out.println("pls equal: " + expected);
            if (status.equals(expected))
                return ok();
            else
                return error(getErrorCodeFrom(status.getStatusCode()));
        }
    }

    // Get the actual response, when the status matches what was expected, otherwise
    // return the error code.
    protected <T> Result<T> responseContents(Response r, Status expected, GenericType<T> gtype) {
        StatusType status = r.getStatusInfo();
        if (status.equals(expected))
            return ok(r.readEntity(gtype));
        else
            return error(getErrorCodeFrom(status.getStatusCode()));
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    static private ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            case 200, 209 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
