package tp1.server.rest;

import com.google.gson.Gson;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestRepSpreadsheets;
import tp1.api.service.util.Result;
import tp1.api.service.util.SpreadsheetDatabase;
import tp1.server.kafka.KafkaPublisher;
import tp1.server.kafka.KafkaSubscriber;
import tp1.server.kafka.sync.SyncPoint;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetsManager;
import tp1.util.ParameterizedCommand;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static tp1.util.ParameterizedCommand.Command;


@Singleton
public class SpreadsheetsRepResource implements RestRepSpreadsheets {

    public static final String SHEETS = ":sheets";
    public static final String SHARE = "share";
    public static final String VALUES = "values";
    public static final String RANGE = "range";
    public final String serverSecret;

    private final SpreadsheetsManager spreadsheetsManager;
    private long version;

    private static final Logger Log = Logger.getLogger(SpreadsheetsRepResource.class.getName());
    private static final Gson json = new Gson();
    private final String domain;
    private final SyncPoint syncPoint;
    private final KafkaPublisher publisher;

    public SpreadsheetsRepResource(String domain, String ownUri, Discovery discovery, SpreadsheetDatabase sdb, String serverSecret) {
        this.domain = domain;
        this.version = -1L;
        spreadsheetsManager = new SpreadsheetsManager(domain, ownUri, discovery, sdb, serverSecret);
        this.serverSecret = serverSecret;
        syncPoint = SyncPoint.getInstance();
        List<String> topicLst = new LinkedList<>();
        topicLst.add(domain);
        publisher = KafkaPublisher.createPublisher("localhost:9092, kafka:9092");

        KafkaSubscriber.createSubscriber("localhost:9092, kafka:9092", topicLst).start(r -> {
            ParameterizedCommand command = json.fromJson(r.value(), ParameterizedCommand.class);
            String[] args = command.args();
            switch (command.type) {
                case createSpreadsheet -> spreadsheetsManager.createSpreadsheet(command.spreadsheet(), args[0]);
                case deleteSpreadsheet -> spreadsheetsManager.deleteSpreadsheet(args[0], args[1]);
                case deleteUserSheets -> spreadsheetsManager.deleteUserSheets(args[0], args[1]);
                case unshareSpreadsheet -> spreadsheetsManager.unshareSpreadsheet(args[0], args[1], args[2]);
                case updateCell -> spreadsheetsManager.updateCell(args[0], args[1], args[2], args[3], args[4]);
                case shareSpreadsheet -> spreadsheetsManager.shareSpreadsheet(args[0], args[1], args[2]);
            }
            version = r.offset();
            syncPoint.setResult(version, command.spreadsheet().getSheetId());
        });
    }

    private <T> T extractResult(Result<T> result) {
        if (result == null) {
            return null;
        }
        if (result.isOK()) {
            return result.value();
        } else {
            throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
        }
    }

    private long updateVersion(Command command, String[] args, Spreadsheet sheet) {
        return publisher.publish(domain, json.toJson(new ParameterizedCommand(
                command,
                args,
                sheet)));
    }

    private long updateVersion(Command command, String[] args) {
        return publisher.publish(domain, json.toJson(new ParameterizedCommand(
                command,
                args)));
    }

    @Override
    public String createSpreadsheet(Long version, Spreadsheet sheet, String password) {
        if (version == null) {
            version = -1L;
        }
        extractResult(spreadsheetsManager.createSpreadsheetValidator(sheet, password));
        long newVersion = updateVersion(Command.createSpreadsheet, new String[]{password}, sheet);
        return syncPoint.waitForResult(Math.max(newVersion, version));
    }

    @Override
    public void deleteSpreadsheet(Long version, String sheetId, String password) {
        if (version == null) {
            version = -1L;
        }
        extractResult(spreadsheetsManager.deleteSpreadsheetValidator(sheetId, password));
        long newVersion = updateVersion(Command.deleteSpreadsheet, new String[]{sheetId, password});
        syncPoint.waitForResult(Math.max(newVersion, version));
    }

    @Override
    public Spreadsheet getSpreadsheet(Long version, String sheetId, String userId, String password) {
        if (version == null) {
            version = -1L;
        }
        if (version > this.version) {
            syncPoint.waitForVersion(version);
        }
        return extractResult(spreadsheetsManager.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetValues(Long version, String sheetId, String userId, String password) {
        if (version == null) {
            version = -1L;
        }
        if (version > this.version) {
            syncPoint.waitForVersion(version);
        }
        return extractResult(spreadsheetsManager.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetRangeValues(Long version, String sheetId, String userEmail, String range, String serverSecret) {
        if (version == null) {
            version = -1L;
        }
        if (version > this.version) {
            syncPoint.waitForVersion(version);
        }
        return extractResult(spreadsheetsManager.getSpreadsheetRangeValues(sheetId, userEmail, range, serverSecret));
    }

    @Override
    public void updateCell(Long version, String sheetId, String cell, String rawValue, String userId, String password) {
        if (version == null) {
            version = -1L;
        }
        extractResult(spreadsheetsManager.commonSpreadsheetValidator(sheetId, password));
        long newVersion = updateVersion(Command.updateCell, new String[]{sheetId, cell, rawValue, userId, password});
        syncPoint.waitForResult(Math.max(newVersion, version));
    }

    @Override
    public void shareSpreadsheet(Long version, String sheetId, String userId, String password) {
        if (version == null) {
            version = -1L;
        }
        extractResult(spreadsheetsManager.commonSpreadsheetValidator(sheetId, password));
        long newVersion = updateVersion(Command.shareSpreadsheet, new String[]{sheetId, userId, password});
        syncPoint.waitForResult(Math.max(newVersion, version));
    }

    @Override
    public void unshareSpreadsheet(Long version, String sheetId, String userId, String password) {
        if (version == null) {
            version = -1L;
        }
        extractResult(spreadsheetsManager.commonSpreadsheetValidator(sheetId, password));
        long newVersion = updateVersion(Command.unshareSpreadsheet, new String[]{sheetId, userId, password});
        syncPoint.waitForResult(Math.max(newVersion, version));
    }

    @Override
    public void deleteUserSheets(Long version, String userId, String serverSecret) {
        if (version == null) {
            version = -1L;
        }
        long newVersion = updateVersion(Command.deleteUserSheets, new String[]{userId, serverSecret});
        syncPoint.waitForResult(Math.max(newVersion, version));
    }

}
