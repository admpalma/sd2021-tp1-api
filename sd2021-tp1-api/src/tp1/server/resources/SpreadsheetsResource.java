package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
        //System.out.println("http://" + selfOther[0] + ":8080" + RestUsers.PATH + "/" + sheet.getOwner());
        WebTarget target = client.target(selfOther[0]).path(RestUsers.PATH);


        Response r = target.path(sheet.getOwner()).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            //System.out.println("Success:");
            User u = r.readEntity(User.class);
            //System.out.println(u);
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
        Spreadsheet sheet = spreadsheets.get(sheetId);
        sheet = getSpreadsheet(sheetId, sheet.getOwner(), password);
        spreadsheets.remove(sheet.getSheetId());
    }

    private Response getWithPassword(String sheetId, String userId, String password) {

        return null;
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

        URI[] selfOther = discovery.knownUrisOf(domain + ":users");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        //System.out.println("http://" + selfOther[0] + ":8080" + RestUsers.PATH + "/" + userId);
        WebTarget target = client.target(selfOther[0]).path(RestUsers.PATH);

        Response r = target.path(userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
//            System.out.println("Success:");
            User u = r.readEntity(User.class);
//            System.out.println(u);
            Spreadsheet spreadsheet = spreadsheets.get(sheetId);
            if (spreadsheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            if (!spreadsheet.getOwner().equals(u.getUserId()) && spreadsheet.getSharedWith().stream().noneMatch(u.getEmail()::equals)) {
                //TODO fml i was right again
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
        URI[] selfOther = discovery.knownUrisOf(domain + ":users");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget target = client.target(selfOther[0]).path(RestUsers.PATH);

        Response r = target.path(userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
            User u = r.readEntity(User.class);
            Spreadsheet spreadsheet = spreadsheets.get(sheetId);
            if (spreadsheet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            if (!spreadsheet.getOwner().equals(u.getUserId()) && spreadsheet.getSharedWith().stream().noneMatch(u.getEmail()::equals)) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            return SpreadsheetEngineImpl.getInstance()
                    .computeSpreadsheetValues(newAbstractSpreadsheet(spreadsheet, u.getEmail()));
        } else {
            System.out.println("Error, HTTP error status: " + r.getStatus());
            throw new WebApplicationException(Response.Status.fromStatusCode(r.getStatus()));
        }
    }

    @Override
    public String[][] getSpreadsheetRangeValues(String sheetId, String userEmail, String range) {
        Spreadsheet spreadsheet = spreadsheets.get(sheetId);
        if (spreadsheet == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        String userId = userEmail.split("@")[0];
        if (!spreadsheet.getOwner().equals(userId) && spreadsheet.getSharedWith().stream().noneMatch(userEmail::equals)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return new CellRange(range).extractRangeValuesFrom(SpreadsheetEngineImpl.getInstance()
                .computeSpreadsheetValues(newAbstractSpreadsheet(spreadsheet, spreadsheet.getOwner() + "@" + domain)));
    }

    private AbstractSpreadsheet newAbstractSpreadsheet(Spreadsheet sheet, String userEmail) {
        return new AbstractSpreadsheet() {
            @Override
            public int rows() {
                return sheet.getRows();
            }

            @Override
            public int columns() {
                return sheet.getColumns();
            }

            @Override
            public String sheetId() {
                return sheet.getSheetId();
            }

            @Override
            public String cellRawValue(int row, int col) {
                try {
                    return sheet.getRawValues()[row][col];
                } catch (IndexOutOfBoundsException e) {
                    return "#ERROR?";
                }
            }

            @Override
            public String[][] getRangeValues(String sheetURL, String range) {
                // get remote range values
                try {
                    ClientConfig config = new ClientConfig();
                    Client client = ClientBuilder.newClient(config);
                    URL url = new URL(sheetURL);

                    //TODO this is horrible
                    WebTarget target = client.target("http://" + url.getHost() + ":" + url.getPort()).path("rest" + RestSpreadsheets.PATH);

                    Response r = target.path(url.getPath().split("/")[3])
                            .path("rangeValues")
                            .queryParam("userEmail", userEmail)
                            .queryParam("range", range).request()
                            .accept(MediaType.APPLICATION_JSON)
                            .get();

                    if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
                        return r.readEntity(String[][].class);
                    } else {
                        return null;
                    }
                } catch (MalformedURLException e) {
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            }
        };
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        Spreadsheet sheet = getSpreadsheet(sheetId, userId, password);
        Pair<Integer, Integer> c = null;
        try {
            c = Cell.CellId2Indexes(cell);
        } catch (InvalidCellIdException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (c.getLeft() < 0 || c.getLeft() > sheet.getRows() ||
                c.getRight() < 0 || c.getRight() > sheet.getColumns())
            throw new WebApplicationException();
        sheet.setCellRawValue(cell, rawValue);
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        sheet = getSpreadsheet(sheetId, sheet.getOwner(), password);
        Set<String> shared = sheet.getSharedWith();
        if (!shared.add(userId))
            throw new WebApplicationException(Response.Status.CONFLICT);
        sheet.setSharedWith(shared);
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        sheet = getSpreadsheet(sheetId, sheet.getOwner(), password);
        Set<String> shared = sheet.getSharedWith();
        if (!shared.remove(userId))
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        sheet.setSharedWith(shared);
    }
}
