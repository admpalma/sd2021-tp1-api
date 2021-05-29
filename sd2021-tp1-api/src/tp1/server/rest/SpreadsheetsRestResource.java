package tp1.server.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetsResource;

import java.util.logging.Logger;


@Singleton
public class SpreadsheetsRestResource implements RestSpreadsheets {

    private final SpreadsheetsResource spreadsheetsResource;

    private static Logger Log = Logger.getLogger(SpreadsheetsRestResource.class.getName());

    public SpreadsheetsRestResource(String domain, String ownUri, Discovery discovery) {
        spreadsheetsResource = new SpreadsheetsResource(domain, ownUri, discovery);
    }

    private <T> T extractResult(Result<T> result) {
        if (result == null) {
            return null;
        }
        if (result.isOK()) {
            return result.value();
        } else {
            throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
        }
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {
        return extractResult(spreadsheetsResource.createSpreadsheet(sheet, password));
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {
        extractResult(spreadsheetsResource.deleteSpreadsheet(sheetId, password));
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
        return extractResult(spreadsheetsResource.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
        return extractResult(spreadsheetsResource.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetRangeValues(String sheetId, String userEmail, String range,String serverSecret) {
        return extractResult(spreadsheetsResource.getSpreadsheetRangeValues(sheetId, userEmail, range,serverSecret));
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        extractResult(spreadsheetsResource.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {
        extractResult(spreadsheetsResource.shareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {
        extractResult(spreadsheetsResource.unshareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void deleteUserSheets(String userId,String serverSecret) {
        extractResult(spreadsheetsResource.deleteUserSheets(userId,serverSecret));
    }
}
