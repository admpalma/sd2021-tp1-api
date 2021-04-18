package tp1.impl.engine;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Users;
import tp1.api.service.util.UsersImpl;
import tp1.server.resources.Discovery;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RestSpreadsheetsImpl implements tp1.api.service.rest.RestSpreadsheets{
    private final Map<String, Spreadsheet> sheets;
    private final String selfUri;
    private Discovery disc;
    private Users userServer;
//    http://srv2:8080/rest/spreadsheets/7830020
    public RestSpreadsheetsImpl(String domain) throws UnknownHostException {
        selfUri = "http://" + InetAddress.getLocalHost().getHostAddress()+"/rest";
        disc = new Discovery("sheets",selfUri ,domain);
        disc.startEmitting();
        disc.startReceiving();
        sheets = new HashMap<String,Spreadsheet>();
//        userServer = new UsersImpl(disc.sheet.getOwner()selfOther());

    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {
        return null;
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {

    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
        return null;
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
        return new String[0][];
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {

    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {

    }
}
