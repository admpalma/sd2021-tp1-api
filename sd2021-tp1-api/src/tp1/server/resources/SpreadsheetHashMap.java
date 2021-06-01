package tp1.server.resources;

import org.checkerframework.checker.units.qual.C;
import tp1.api.Spreadsheet;
import tp1.api.service.util.SpreadsheetDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpreadsheetHashMap implements SpreadsheetDatabase {

    ConcurrentHashMap<String,Spreadsheet> map;
    public SpreadsheetHashMap(){
        map = new ConcurrentHashMap<String,Spreadsheet>();

    }
    @Override
    public Spreadsheet get(String sheetId) {
        return map.get(sheetId);
    }

    @Override
    public Spreadsheet remove(String sheetId) {
        return map.remove(sheetId);
    }

    @Override
    public boolean containsKey(String sheetId) {
        return map.containsKey(sheetId);
    }

    @Override
    public Spreadsheet putIfAbsent(String sheetId, Spreadsheet sheet) {
        return map.putIfAbsent(sheetId,sheet);
    }

    @Override
    public Spreadsheet put(String sheetId, Spreadsheet sheet) {
        return map.put(sheetId,sheet);
    }

    @Override
    public boolean removeUserSpreadsheets(String userId) {
        boolean hadSheet = false;
        for (Spreadsheet sheet : map.values()) {
            if(sheet.getOwner().equals(userId)){
                map.remove(sheet.getSheetId());
                hadSheet = true;
            }
        }

        return hadSheet;
    }
}
