package tp1.server.resources.requester;

import tp1.api.User;
import tp1.api.service.util.Result;

import java.net.URI;

public interface Requester {

    Result<User> requestUser(URI serverURI, String userId, String password);

    Result<String[][]> requestSpreadsheetRangeValues(String sheetURL, String userEmail, String range);

    Result<Void> deleteUserSheets(URI serverURI, String userId);
}
