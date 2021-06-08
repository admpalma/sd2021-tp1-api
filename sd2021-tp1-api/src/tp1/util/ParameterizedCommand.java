package tp1.util;

import com.google.gson.Gson;
import tp1.api.Spreadsheet;

public class ParameterizedCommand {

    public enum Command {
        createSpreadsheet, deleteSpreadsheet, updateCell, shareSpreadsheet, unshareSpreadsheet, deleteUserSheets
    }

    private static final Gson gson = new Gson();
    private static final String DELIMITER = "\t";
    public final Command type;
    private final String jsonArgs;
    private String sheet;

    public ParameterizedCommand(Command type, String[] args) {
        this.type = type;
        this.jsonArgs = gson.toJson(args);
    }

    public ParameterizedCommand(Command type, String[] args, Spreadsheet spreadsheet) {
        this(type,args);
        sheet = gson.toJson(spreadsheet);
    }

    public ParameterizedCommand(String encoding) {
        String[] tokens = encoding.split(DELIMITER);
        this.type = Command.valueOf(tokens[0]);
        this.jsonArgs = tokens[1];
        this.sheet = tokens[2];
    }

    public String encode() {
        return type.name() + DELIMITER +
                jsonArgs + DELIMITER +
                sheet;
    }

    public Spreadsheet spreadsheet() {
        return gson.fromJson(sheet, Spreadsheet.class);
    }

    public String[] args() {
        return gson.fromJson(jsonArgs, String[].class);
    }
}