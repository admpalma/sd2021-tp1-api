package tp1.server.soap;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetsResource;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetsSoapResource implements SoapSpreadsheets {

    private final SpreadsheetsResource spreadsheetsResource;

    public SpreadsheetsSoapResource(String domain, String ownUri, Discovery discovery) {
        spreadsheetsResource = new SpreadsheetsResource(domain, ownUri, discovery);
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
        Spreadsheet spreadsheet = extractResult(spreadsheetsResource.getSpreadsheet(sheetId, userId, password));
        if (userId.equals("jackie.rau") && sheetId.equals("1")) {
            System.out.println(spreadsheet);
            System.out.println(spreadsheet.getSheetId());
            System.out.println(spreadsheet.getSheetURL());
        }
        return spreadsheet;
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
    public String[][] getSpreadsheetRangeValues(String sheetId, String userEmail, String range) throws SheetsException {
        return extractResult(spreadsheetsResource.getSpreadsheetRangeValues(sheetId, userEmail, range));
    }

    @Override
    public void deleteUserSheets(String userId) throws SheetsException {
        extractResult(spreadsheetsResource.deleteUserSheets(userId));
    }
}
