package tp1.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import tp1.api.service.rest.RestRepSpreadsheets;
import tp1.server.kafka.sync.SyncPoint;

@Provider
public class VersionFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().add(RestRepSpreadsheets.HEADER_VERSION, SyncPoint.getInstance().getVersion());
    }
}
