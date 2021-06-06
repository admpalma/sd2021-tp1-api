package tp1.server.rest.client;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.api.service.util.Spreadsheets;

import java.net.URI;

public class RestSpreadsheetsClient extends RestClient implements Spreadsheets {
    private static final String PASSWORD = "password";
    private static final String USERID = "userId";
    private static final String SHEETS = "/sheets";

    public RestSpreadsheetsClient(URI serverUri) {
        super(serverUri, RestSpreadsheets.PATH);
    }

    @Override
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
        Response r = target
                .queryParam(PASSWORD, password)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(sheet, MediaType.APPLICATION_JSON));
        return super.responseContents(r, Status.OK, new GenericType<>() {
        });
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        Response r = target
                .path(sheetId)
                .queryParam(PASSWORD, password)
                .request()
                .delete();

        return verifyResponse(r, Status.NO_CONTENT);
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
        Response r = target.path(String.format("/%s/share/%s", sheetId, userId))
                .queryParam(PASSWORD, password)
                .request()
                .post(Entity.json(""));
        return verifyResponse(r, Status.NO_CONTENT);
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        Response r = target.path(String.format("/%s/share/%s", sheetId, userId))
                .queryParam(PASSWORD, password)
                .request()
                .delete();
        return verifyResponse(r, Status.NO_CONTENT);
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        Response r = target
                .path(sheetId)
                .path(cell)
                .queryParam(USERID, userId)
                .queryParam(PASSWORD, password)
                .request()
                .put(Entity.entity(rawValue, MediaType.APPLICATION_JSON));

        return verifyResponse(r, Status.NO_CONTENT);
    }

    @Override
    public Result<Void> deleteUserSheets(String userId, String serverSecret) {
        Response r = target
                .path("deleteUserSheets")
                .path(userId)
                .queryParam("serverSecret", serverSecret)
                .request()
                .delete();

        return verifyResponse(r, Status.NO_CONTENT);//TODO check this lmao
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        throw new RuntimeException();
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        throw new RuntimeException();
    }

    @Override
    public Result<String[][]> getSpreadsheetRangeValues(String sheetId, String userEmail, String range, String serverSecret) {
        throw new RuntimeException();
    }
}
