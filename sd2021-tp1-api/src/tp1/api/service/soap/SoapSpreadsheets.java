package tp1.api.service.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import tp1.api.Spreadsheet;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public interface SoapSpreadsheets {

    static final String NAME = "spreadsheets";
    static final String NAMESPACE = "http://sd2021";
    static final String INTERFACE = "tp1.api.service.soap.SoapSpreadsheets";

    /**
     * Creates a new spreadsheet. The sheetId and sheetURL are generated by the server.
     *
     * @param sheet    - the spreadsheet to be created.
     * @param password - the password of the owner of the spreadsheet.
     * @throws SheetsException otherwise
     */
    @WebMethod
    String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException;


    /**
     * Deletes a spreadsheet.
     *
     * @param sheetId  - the sheet to be deleted.
     * @param password - the password of the owner of the spreadsheet.
     * @throws SheetsException otherwise
     */
    @WebMethod
    void deleteSpreadsheet(String sheetId, String password) throws SheetsException;

    /**
     * Retrieve a spreadsheet.
     *
     * @param sheetId  - The  spreadsheet being retrieved.
     * @param userId   - The user performing the operation.
     * @param password - The password of the user performing the operation.
     * @throws SheetsException otherwise
     */
    @WebMethod
    Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException;


    /**
     * Adds a new user to the list of shares of a spreadsheet.
     *
     * @param sheetId  - the sheet being shared.
     * @param userId   - the user that is being added to the list of shares.
     * @param password - The password of the owner of the spreadsheet.
     * @throws SheetsException otherwise
     */
    @WebMethod
    void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException;


    /**
     * Removes a user from the list of shares of a spreadsheet.
     *
     * @param sheetId  - the sheet being shared.
     * @param userId   - the user that is being removed from the list of shares.
     * @param password - The password of the owner of the spreadsheet.
     * @throws SheetsException otherwise
     */
    @WebMethod
    void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException;


    /**
     * Updates the raw values of some cells of a spreadsheet.
     *
     * @param userId   - The user performing the update.
     * @param sheetId  - the spreadsheet whose values are being retrieved.
     * @param cell     - the cell being updated
     * @param rawValue - the new raw value of the cell
     * @param password - the password of the owner of the spreadsheet
     * @throws SheetsException otherwise
     **/
    @WebMethod
    void updateCell(String sheetId, String cell, String rawValue, String userId, String password) throws SheetsException;


    /**
     * Retrieves the calculated values of a spreadsheet.
     *
     * @param userId   - The user requesting the values
     * @param sheetId  - the spreadsheet whose values are being retrieved.
     * @param password - the password of the owner of the spreadsheet
     * @throws SheetsException otherwise
     */
    @WebMethod
    String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException;

    /**
     * Retrieves the calculated values of a range within a spreadsheet.
     *
     * @param userEmail The email of the user requesting the values (userId@domain)
     * @param sheetId   the spreadsheet whose values are being retrieved.
     * @param range     The range of calculated values to return.
     * @throws SheetsException otherwise
     */
    @WebMethod
    String[][] getSpreadsheetRangeValues(String sheetId, String userEmail, String range,String serverSecret) throws SheetsException;

    /**
     * Deletes a user's spreadsheets. Only a server can call this method. :^) Hm yes secure
     *
     * @param userId  - the id of the user whomst'dve sheets are to be deleted.
     * @throws SheetsException otherwise
     */
    @WebMethod
    void deleteUserSheets(String userId,String serverSecret) throws SheetsException;
}
