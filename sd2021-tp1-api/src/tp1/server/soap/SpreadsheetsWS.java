package tp1.server.soap;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.api.service.util.SpreadsheetDatabase;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetsResource;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetsWS implements SoapSpreadsheets {

    private final SpreadsheetsResource spreadsheetsResource;

    public SpreadsheetsWS(String domain, String ownUri, Discovery discovery, SpreadsheetDatabase sdb) {
        spreadsheetsResource = new SpreadsheetsResource(domain, ownUri, discovery,sdb);
    }

    private <T> T extractResult(Result<T> result) throws SheetsException {
        if (result.isOK()) {
            return result.value();
        } else {
            throw new SheetsException(result.error().name());
        }
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
        return extractResult(spreadsheetsResource.createSpreadsheet(sheet, password));
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
        extractResult(spreadsheetsResource.deleteSpreadsheet(sheetId, password));
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        return extractResult(spreadsheetsResource.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        extractResult(spreadsheetsResource.shareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
        extractResult(spreadsheetsResource.unshareSpreadsheet(sheetId, userId, password));
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) throws SheetsException {
        extractResult(spreadsheetsResource.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
        return extractResult(spreadsheetsResource.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetRangeValues(String sheetId, String userEmail, String range,String serverSecret) throws SheetsException {
        return extractResult(spreadsheetsResource.getSpreadsheetRangeValues(sheetId, userEmail, range,serverSecret));
    }

    @Override
    public void deleteUserSheets(String userId,String serverSecret) throws SheetsException {
        extractResult(spreadsheetsResource.deleteUserSheets(userId, serverSecret));
    }
}
