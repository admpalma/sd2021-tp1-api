package tp1.util;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.server.resources.gsheet.GSheetRangeResponse;
import tp1.server.resources.requester.HTTPSClient;

public class GSheets {
    private static final String GET_CELL_URL =
            "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s?key=AIzaSyA9Iz1-yBvyhMWQrXHeFG-v1ONUyqJKM1o";

    public static String[][] getSheetRange(String sheetId, String range) {
        Client cl = HTTPSClient.make();
        Response r = cl.target(String.format(GET_CELL_URL, sheetId, range))
                .request()
                .accept(MediaType.APPLICATION_JSON).get();
        return r.readEntity(GSheetRangeResponse.class).getValues();
    }
}
