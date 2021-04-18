package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


@Singleton
public class SpreadsheetsResource implements RestSpreadsheets {

    private final Map<String, Spreadsheet> spreadsheets;
    private final String ownUri;
    private final String domain;
    private RestUsers userServer;
    private final Discovery discovery;
    private final ClientConfig config;
    private AtomicInteger totalSpreadsheets;

    private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

    public SpreadsheetsResource(String domain, String ownUri, Discovery discovery) {
        config = new ClientConfig();
        spreadsheets = new HashMap<>();
        this.discovery = discovery;
        discovery.startEmitting();
        discovery.startReceiving();
        this.ownUri = ownUri;
        this.domain = domain;
        totalSpreadsheets = new AtomicInteger(0);
    }


    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {

        URI[] selfOther = discovery.knownUrisOf(domain + ":users");

        if (selfOther == null || sheet == null || password == null)
            throw new WebApplicationException(Response.Status.fromStatusCode(400));

        Client client = ClientBuilder.newClient(config);
        System.out.println("http://" + selfOther[0] + ":8080" + RestUsers.PATH + "/" + sheet.getOwner());
        WebTarget target = client.target(selfOther[0]).path(RestUsers.PATH);


        Response r = target.path(sheet.getOwner()).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Success:");
            User u = r.readEntity(User.class);
            System.out.println("User : " + u);
            if (!u.getPassword().equals(password)) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        } else {
            System.out.println("Error, HTTP error status: " + r.getStatus());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (sheet.getColumns() < 1 || sheet.getRows() < 1) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        String id = String.valueOf(totalSpreadsheets.incrementAndGet());

        sheet.setSheetId(id);
        sheet.setSheetURL(ownUri + RestSpreadsheets.PATH + "/" + id);
        spreadsheets.put(id, sheet);
        return id;

    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {

    }

    private Response getWithPassword(String sheetId, String userId, String password) {

        return null;
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

        URI[] selfOther = discovery.knownUrisOf(domain + ":users");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        System.out.println("http://" + selfOther[0] + ":8080" + RestUsers.PATH + "/" + userId);
        WebTarget target = client.target(selfOther[0]).path(RestUsers.PATH);

        Response r = target.path("/" + userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Success:");
            User u = r.readEntity(User.class);
            System.out.println("User : " + u);
            Spreadsheet spreadsheet = spreadsheets.get(sheetId);
            if (spreadsheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            if (!spreadsheet.getOwner().equals(u.getUserId())) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            return spreadsheet;
        } else {
            System.out.println("Error, HTTP error status: " + r.getStatus());
            throw new WebApplicationException(Response.Status.fromStatusCode(r.getStatus()));
        }
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
        return new String[0][];
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {

    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {

    }
}
