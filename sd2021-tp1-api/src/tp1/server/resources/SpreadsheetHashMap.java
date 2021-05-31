package tp1.server.resources;

import tp1.api.Spreadsheet;
import tp1.api.service.util.SpreadsheetDatabase;

public class SpreadsheetHashMap implements SpreadsheetDatabase {

    @Override
    public Spreadsheet get(String sheetId) {
        return null;
    }

    @Override
    public Spreadsheet remove(String sheetId) {
        return null;
    }

    @Override
    public boolean containsKey(String sheetId) {
        return false;
    }

    @Override
    public Spreadsheet putIfAbsent(String sheetId, Spreadsheet sheet) {
        return null;
    }

    @Override
    public Spreadsheet put(String sheetId, Spreadsheet sheet) {
        return null;
    }

    @Override
    public boolean removeUserSpreadsheets(String userId) {
        return false;
    }
}
