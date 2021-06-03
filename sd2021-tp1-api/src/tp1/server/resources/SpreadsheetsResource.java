package tp1.server.resources;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.api.service.util.SpreadsheetDatabase;
import tp1.api.service.util.Spreadsheets;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.server.resources.requester.Requester;
import tp1.server.resources.requester.RestRequester;
import tp1.server.resources.requester.SoapRequester;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SpreadsheetsResource implements Spreadsheets {

    protected static final String serverSecret = "sheet_%|WRDLdwA4Bp_/EsUw%oj9";

    URI uri;
    private final static int CACHE_FAIL_TTL = 60000;
    private final static int CACHE_VALID_TTL = 200;
    private final SpreadsheetDatabase spreadsheets;
    private final ConcurrentMap<Spreadsheet, Pair<String[][], Long>> spreadsheetValues;
    private final ConcurrentMap<String, Pair<String[][], Long>> rangeCache;
    private final String ownUri;
    private final String domain;
    private final Discovery discovery;
    private final AtomicInteger totalSpreadsheets;
    private final RestRequester restRequester;
    private final SoapRequester soapRequester;

    private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

    public SpreadsheetsResource(String domain, String ownUri, Discovery discovery,SpreadsheetDatabase spreadsheetDatabase) {
        spreadsheets = spreadsheetDatabase;
        spreadsheetValues = new ConcurrentHashMap<>();
        rangeCache = new ConcurrentHashMap<>();
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

        Result<User> userResult = authenticateUser(sheet.getOwner(), password);
        if (!userResult.isOK()) {
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
        if (sheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        synchronized (sheet) {
            Result<User> userResult = authenticateUser(sheet.getOwner(), password);

            if (!userResult.isOK()) {
                return Result.error(userResult.error());
            } else {
                spreadsheets.remove(sheet.getSheetId());
                spreadsheetValues.remove(sheet);
            }
        }
        return Result.ok();
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        URI[] selfOther = discovery.knownUrisOf(domain + ":users");
        Spreadsheet sheet = spreadsheets.get(sheetId);
        if (sheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Result<User> userResult = requesterFromURI(selfOther[0])
                .requestUser(selfOther[0], userId, password);
        if (!userResult.isOK()) {
            return Result.error(userResult.error());
        }
        User u = userResult.value();

        synchronized (sheet) {
            // Shouldn't be needed?
//            if (!spreadsheets.containsKey(sheetId)) {
//                return Result.error(Result.ErrorCode.NOT_FOUND);
//            }
            Set<String> sharedWith = sheet.getSharedWith();
            if (!sheet.getOwner().equals(u.getUserId()) && (sharedWith == null || !sharedWith.contains(u.getEmail()))) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
        }
        return Result.ok(sheet);
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        if (sheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Result<User> userResult = authenticateUser(sheet.getOwner(), password);
        if (!userResult.isOK()) {
            return Result.error(userResult.error());
        }
        synchronized (sheet) {
            // Shouldnt be needed
//            if (sheetId == null) {
//                return Result.error(Result.ErrorCode.NOT_FOUND);
//            }
            Set<String> shared = sheet.getSharedWith();
            if (shared == null) {
                shared = new HashSet<>();
            }
            if (!shared.add(userId))
                return Result.error(Result.ErrorCode.CONFLICT);
            sheet.setSharedWith(shared);
            spreadsheets.put(sheetId,sheet);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        if (sheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Result<User> userResult = authenticateUser(sheet.getOwner(), password);
        if (!userResult.isOK()) {
            return Result.error(userResult.error());
        }

        synchronized (sheet) {
            if (!spreadsheets.containsKey(sheetId)) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            Set<String> shared = sheet.getSharedWith();
            if (shared == null || !shared.remove(userId))
                return Result.error(Result.ErrorCode.NOT_FOUND);
            sheet.setSharedWith(shared);
            spreadsheets.put(sheetId,sheet);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        if (sheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Result<User> userResult = authenticateUser(userId, password);
        if (!userResult.isOK()) {
            return Result.error(userResult.error());
        }
        synchronized (sheet) {
            if (!spreadsheets.containsKey(sheetId)) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
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
            spreadsheetValues.remove(sheet);
            // Needed for DropboxMap
            spreadsheets.put(sheetId,sheet);
        }
        return Result.ok();
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        Spreadsheet sheet = spreadsheets.get(sheetId);
        if (sheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Result<User> userResult = authenticateUser(userId, password);
        if (!userResult.isOK()) {
            return Result.error(userResult.error());
        }
        User u = userResult.value();

        synchronized (sheet) {
            if (!spreadsheets.containsKey(sheetId)) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            Set<String> sharedWith = sheet.getSharedWith();
            if (!sheet.getOwner().equals(u.getUserId()) && (sharedWith == null || !sharedWith.contains(u.getEmail()))) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            return Result.ok(SpreadsheetEngineImpl.getInstance()
                    .computeSpreadsheetValues(newAbstractSpreadsheet(sheet, u.getEmail())));
        }
    }

    @Override
    public Result<String[][]> getSpreadsheetRangeValues(String sheetId, String userEmail, String range,String serverSecret) {
        if(!SpreadsheetsResource.serverSecret.equals(serverSecret)){
            Log.severe("Wrong server secret on getSpreadsheetRangeValues");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        Spreadsheet sheet = spreadsheets.get(sheetId);
        if (sheet == null) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        String userId = userEmail.split("@")[0];
        synchronized (sheet) {
            if (!spreadsheets.containsKey(sheetId)) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            Set<String> sharedWith = sheet.getSharedWith();
            if (!sheet.getOwner().equals(userId) && (sharedWith == null || !sharedWith.contains(userEmail))) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            String[][] values;
            Pair<String[][], Long> result = spreadsheetValues.get(sheet);
            if (result == null || result.getRight() + CACHE_VALID_TTL < System.currentTimeMillis()) {
                values = SpreadsheetEngineImpl.getInstance()
                        .computeSpreadsheetValues(newAbstractSpreadsheet(sheet, sheet.getOwner() + "@" + domain));
                spreadsheetValues.put(sheet, new ImmutablePair<>(values, System.currentTimeMillis()));
            } else {
                values = result.getLeft();
            }
            return Result.ok(new CellRange(range).extractRangeValuesFrom(values));
        }
    }

    @Override
    public Result<Void> deleteUserSheets(String userId, String serverSecret) {
        if (!UsersResource.serverSecret.equals(serverSecret)){
            Log.severe("Wrong server secret on delUserSheets");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        if (spreadsheets.removeUserSpreadsheets(userId)) {
            spreadsheetValues.entrySet().removeIf(sheetValueEntry -> sheetValueEntry.getKey().getOwner().equals(userId));
            return Result.ok();
        }
        return Result.error(Result.ErrorCode.NOT_FOUND);
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
                Pair<String[][], Long> cells = rangeCache.get(sheetURL + range);
                if (cells != null && cells.getRight() + CACHE_VALID_TTL > System.currentTimeMillis()) {
                    return cells.getLeft();
                }

                Result<String[][]> rangeValuesResult = requesterFromURI(sheetURL)
                        .requestSpreadsheetRangeValues(sheetURL, userEmail, range,serverSecret);

                if (!rangeValuesResult.isOK()) {
                    if (cells != null && cells.getRight() + CACHE_FAIL_TTL > System.currentTimeMillis()) {
                        return cells.getLeft();
                    }
                    return null;
                }
                rangeCache.put(sheetURL + range, new ImmutablePair<>(rangeValuesResult.value(), System.currentTimeMillis()));
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

    private Result<User> authenticateUser(String userId, String password) {
        URI userUri = discovery.knownUrisOf(domain + ":users")[0];
        return requesterFromURI(userUri).requestUser(userUri, userId, password);
    }

}
