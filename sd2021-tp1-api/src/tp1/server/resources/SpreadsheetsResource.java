package tp1.server.resources;

import org.apache.commons.lang3.tuple.Pair;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.api.service.util.Spreadsheets;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.server.resources.requester.Requester;
import tp1.server.resources.requester.RestRequester;
import tp1.server.resources.requester.SoapRequester;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SpreadsheetsResource implements Spreadsheets {

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

        if (sheet == null || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        if (sheet.getColumns() < 1 || sheet.getRows() < 1) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        URI[] selfOther = discovery.knownUrisOf(domain + ":users");

        Result<User> userResult = requesterFromURI(selfOther[0])
                .requestUser(selfOther[0], sheet.getOwner(), password);
        if (!userResult.isOK()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        String id = String.valueOf(totalSpreadsheets.incrementAndGet());

        sheet.setSheetId(id);
        sheet.setSheetURL(ownUri + RestSpreadsheets.PATH + "/" + id);
        synchronized (spreadsheets) {
            spreadsheets.put(id, sheet);
        }

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
