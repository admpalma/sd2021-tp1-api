package tp1.api.service.util;

import tp1.api.Spreadsheet;

import java.net.URI;

public class SpreadsheetsImpl implements Spreadsheets {
    URI uri;
    public SpreadsheetsImpl(URI uri){
        this.uri = uri;
    }

    @Override
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
        return null;
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        return null;
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        return null;
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
        return null;
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        return null;
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        return null;
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        return null;
    }
}
