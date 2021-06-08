package tp1.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import tp1.api.service.rest.RestRepSpreadsheets;

import java.util.concurrent.atomic.AtomicInteger;

@Provider
public class VersionFilter implements ContainerResponseFilter {

    private final AtomicInteger version;

    public VersionFilter(AtomicInteger version) {
        this.version = version;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().add(RestRepSpreadsheets.HEADER_VERSION, version.get());
    }
}
