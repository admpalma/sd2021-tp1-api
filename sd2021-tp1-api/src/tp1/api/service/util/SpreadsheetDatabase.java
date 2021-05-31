package tp1.api.service.util;

import tp1.api.Spreadsheet;

public interface SpreadsheetDatabase {
    Spreadsheet get(String sheetId);
    Spreadsheet remove(String sheetId);
    boolean containsKey(String sheetId);
    Spreadsheet putIfAbsent(String sheetId,Spreadsheet sheet);
    Spreadsheet put(String sheetId,Spreadsheet sheet);
    boolean removeUserSpreadsheets(String userId);
}
