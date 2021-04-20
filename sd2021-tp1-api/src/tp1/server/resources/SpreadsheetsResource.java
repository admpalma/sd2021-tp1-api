package tp1.server.resources;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;
import tp1.api.service.util.Spreadsheets;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SpreadsheetsResource implements Spreadsheets {

    private interface Requester {

        Result<User> requestUser(URI serverURI, String userId, String password);

        Result<String[][]> requestSpreadsheetRangeValues(String sheetURL, String userEmail, String range);
    }

    private static class RestRequester implements Requester {

        private final Client client;

        RestRequester() {
            client = ClientBuilder.newClient(new ClientConfig());
        }

        @Override
        public Result<User> requestUser(URI serverURI, String userId, String password) {
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
        }

        @Override
        public Result<String[][]> requestSpreadsheetRangeValues(String sheetURL, String userEmail, String range) {
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
        }
    }

    private static class SoapRequester implements Requester {

        public final static String USERS_WSDL = "/users/?wsdl";
        public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

        public final static int MAX_RETRIES = 3;
        public final static long RETRY_PERIOD = 1000;
        public final static int CONNECTION_TIMEOUT = 1000;
        public final static int REPLY_TIMEOUT = 600;
        private final QName usersQName;
        private final QName spreadsheetsQName;

        SoapRequester() {
            usersQName = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
            spreadsheetsQName = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
        }

        @Override
        public Result<User> requestUser(URI serverURI, String userId, String password) {
            try {
                Service service = Service.create(new URL(serverURI + USERS_WSDL), usersQName);
                SoapUsers users = service.getPort(SoapUsers.class);

                //Set timeouts for executing operations
                ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
                ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

                return Result.ok(users.getUser(userId, password));
            } catch (UsersException e) {
                return Result.error(Result.ErrorCode.valueOf(e.getMessage()));
            } catch (WebServiceException | MalformedURLException e) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }

        @Override
        public Result<String[][]> requestSpreadsheetRangeValues(String sheetURL, String userEmail, String range) {
            try {
                int idSplitIndex = sheetURL.lastIndexOf('/');
                Service service = Service.create(new URL(sheetURL.substring(0, idSplitIndex) + "/?wsdl"), spreadsheetsQName);
                SoapSpreadsheets spreadsheets = service.getPort(SoapSpreadsheets.class);

                //Set timeouts for executing operations
                ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
                ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

                String[][] spreadsheetRangeValues = spreadsheets.getSpreadsheetRangeValues(sheetURL.substring(idSplitIndex + 1), userEmail, range);
                return Result.ok(spreadsheetRangeValues);
            } catch (SheetsException e) {
                return Result.error(Result.ErrorCode.valueOf(e.getMessage()));
            } catch (WebServiceException | MalformedURLException e) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    URI uri;
    private final Map<String, Spreadsheet> spreadsheets;
    private final String ownUri;
    private final String domain;
    private final Discovery discovery;
    private final AtomicInteger totalSpreadsheets;
    private final RestRequester restRequester;
    private final SoapRequester soapRequester;

    private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

    public SpreadsheetsResource(String domain, String ownUri, Discovery discovery) {
        spreadsheets = new HashMap<>();
        this.discovery = discovery;
        discovery.startEmitting();
        discovery.startReceiving();
        this.ownUri = ownUri;
        this.domain = domain;
        totalSpreadsheets = new AtomicInteger(0);
        restRequester = new RestRequester();
        soapRequester = new SoapRequester();
    }

    @Override
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
        URI[] selfOther = discovery.knownUrisOf(domain + ":users");

        if (selfOther == null || sheet == null || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);


        Result<User> userResult = requesterFromURI(selfOther[0])
                .requestUser(selfOther[0], sheet.getOwner(), password);
        if (!userResult.isOK()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        if (sheet.getColumns() < 1 || sheet.getRows() < 1) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        String id = String.valueOf(totalSpreadsheets.incrementAndGet());

        sheet.setSheetId(id);
        sheet.setSheetURL(ownUri + RestSpreadsheets.PATH + "/" + id);
        spreadsheets.put(id, sheet);
        return Result.ok(id);
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        Result<Spreadsheet> sheetResult = getSpreadsheet(sheetId, sheet.getOwner(), password);
        if (sheetResult.isOK()) {
            spreadsheets.remove(sheet.getSheetId());
            return Result.ok();
        } else {
            return Result.error(sheetResult.error());
        }
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        URI[] selfOther = discovery.knownUrisOf(domain + ":users");
        Result<User> userResult = requesterFromURI(selfOther[0])
                .requestUser(selfOther[0], userId, password);

        if (!userResult.isOK()) {
            return Result.error(userResult.error());
        }
        User u = userResult.value();
        Spreadsheet spreadsheet = spreadsheets.get(sheetId);
        if (spreadsheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Set<String> sharedWith = spreadsheet.getSharedWith();
        if (!spreadsheet.getOwner().equals(u.getUserId()) && (sharedWith == null || sharedWith.stream().noneMatch(u.getEmail()::equals))) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        return Result.ok(spreadsheet);
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        Result<Spreadsheet> sheetResult = getSpreadsheet(sheetId, sheet.getOwner(), password);
        if (!sheetResult.isOK()) {
            return Result.error(sheetResult.error());
        }
        sheet = sheetResult.value();
        Set<String> shared = sheet.getSharedWith();
        if (shared == null) {
            shared = new HashSet<>();
        }
        if (!shared.add(userId))
            return Result.error(Result.ErrorCode.CONFLICT);
        sheet.setSharedWith(shared);
        return Result.ok();
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        Result<Spreadsheet> sheetResult = getSpreadsheet(sheetId, sheet.getOwner(), password);
        if (!sheetResult.isOK()) {
            return Result.error(sheetResult.error());
        }
        sheet = sheetResult.value();
        Set<String> shared = sheet.getSharedWith();
        if (shared == null || !shared.remove(userId))
            return Result.error(Result.ErrorCode.NOT_FOUND);
        sheet.setSharedWith(shared);
        return Result.ok();
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        Result<Spreadsheet> sheetResult = getSpreadsheet(sheetId, userId, password);
        if (!sheetResult.isOK()) {
            return Result.error(sheetResult.error());
        }
        Spreadsheet sheet = sheetResult.value();
        Pair<Integer, Integer> c;
        try {
            c = Cell.CellId2Indexes(cell);
        } catch (InvalidCellIdException e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        if (c.getLeft() < 0 || c.getLeft() > sheet.getRows() ||
                c.getRight() < 0 || c.getRight() > sheet.getColumns())
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        sheet.setCellRawValue(cell, rawValue);
        return Result.ok();
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        URI[] selfOther = discovery.knownUrisOf(domain + ":users");

        Result<User> userResult = requesterFromURI(selfOther[0]).requestUser(selfOther[0], userId, password);

        if (!userResult.isOK()) {
            return Result.error(userResult.error());
        }
        User u = userResult.value();
        Spreadsheet spreadsheet = spreadsheets.get(sheetId);
        if (spreadsheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Set<String> sharedWith = spreadsheet.getSharedWith();
        if (!spreadsheet.getOwner().equals(u.getUserId()) && (sharedWith == null || sharedWith.stream().noneMatch(u.getEmail()::equals))) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        return Result.ok(SpreadsheetEngineImpl.getInstance()
                .computeSpreadsheetValues(newAbstractSpreadsheet(spreadsheet, u.getEmail())));
    }

    @Override
    public Result<String[][]> getSpreadsheetRangeValues(String sheetId, String userEmail, String range) {
        System.out.println(sheetId + " " + userEmail + " " + range);
        Spreadsheet spreadsheet = spreadsheets.get(sheetId);
        if (spreadsheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        String userId = userEmail.split("@")[0];
        Set<String> sharedWith = spreadsheet.getSharedWith();
        if (!spreadsheet.getOwner().equals(userId) && (sharedWith == null || sharedWith.stream().noneMatch(userEmail::equals))) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        return Result.ok(new CellRange(range).extractRangeValuesFrom(SpreadsheetEngineImpl.getInstance()
                .computeSpreadsheetValues(newAbstractSpreadsheet(spreadsheet, spreadsheet.getOwner() + "@" + domain))));
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
                System.out.println(sheetURL+ userEmail+ range);
                Result<String[][]> rangeValuesResult = requesterFromURI(sheetURL)
                        .requestSpreadsheetRangeValues(sheetURL, userEmail, range);
                if (!rangeValuesResult.isOK()) {
                    return null;
                }
                return rangeValuesResult.value();
            }
        };
    }

    private Requester requesterFromURI(String uri) {
        return requesterFromURI(URI.create(uri));
    }

    private Requester requesterFromURI(URI uri) {
        String serverType = uri.getPath().substring(1, 5);
        return switch (serverType) {
            case "soap" -> soapRequester;
            case "rest" -> restRequester;
            default -> throw new IllegalArgumentException("Unexpected value: " + serverType);
        };
    }

}
