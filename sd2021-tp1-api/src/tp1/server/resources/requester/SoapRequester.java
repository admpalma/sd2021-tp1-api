package tp1.server.resources.requester;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.User;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class SoapRequester extends AbstractRequester implements Requester {

    public final static String USERS_WSDL = "/users/?wsdl";
    public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

    private final QName usersQName;
    private final QName spreadsheetsQName;

    public SoapRequester() {
        usersQName = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
        spreadsheetsQName = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
    }

    @Override
    public Result<User> requestUser(URI serverURI, String userId, String password) {
        return defaultRetry(() -> {
            try {
                Service service = Service.create(new URL(serverURI + USERS_WSDL), usersQName);
                SoapUsers users = service.getPort(SoapUsers.class);

                //Set timeouts for executing operations
                ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
                ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

                return Result.ok(users.getUser(userId, password));
            } catch (UsersException e) {
                return Result.error(Result.ErrorCode.valueOf(e.getMessage()));
            } catch (WebServiceException | MalformedURLException e) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @Override
    public Result<String[][]> requestSpreadsheetRangeValues(String sheetURL, String userEmail, String range) {
        return defaultRetry(() -> {
            try {
                int idSplitIndex = sheetURL.lastIndexOf('/');
                Service service = Service.create(new URL(sheetURL.substring(0, idSplitIndex) + "/?wsdl"), spreadsheetsQName);
                SoapSpreadsheets spreadsheets = service.getPort(SoapSpreadsheets.class);

                //Set timeouts for executing operations
                ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
                ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

                String[][] spreadsheetRangeValues = spreadsheets.getSpreadsheetRangeValues(sheetURL.substring(idSplitIndex + 1), userEmail, range);
                return Result.ok(spreadsheetRangeValues);
            } catch (SheetsException e) {
                return Result.error(Result.ErrorCode.valueOf(e.getMessage()));
            } catch (WebServiceException | MalformedURLException e) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        });
    }
}