package tp1.server.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestRepSpreadsheets;
import tp1.api.service.util.Result;
import tp1.api.service.util.SpreadsheetDatabase;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetsManager;
import tp1.server.rest.client.RestSpreadsheetsClient;
import tp1.util.Leader;
import tp1.util.ParameterizedCommand;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static tp1.server.resources.SpreadsheetsManager.serverSecret;
import static tp1.util.ParameterizedCommand.Command;


@Singleton
public class SpreadsheetsRepResource implements RestRepSpreadsheets {

    private static final String PASSWORD = "password";
    private static final String USERID = "userId";
    public static final String SHEETS = ":sheets";
    public static final String SHARE = "share";
    public static final String VALUES = "values";
    public static final String RANGE_VALUES = "rangeValues";
    public static final String USER_EMAIL = "userEmail";
    public static final String RANGE = "range";
    public static final String SERVER_SECRET = "serverSecret";

    private final SpreadsheetsManager spreadsheetsManager;
    private final List<ParameterizedCommand> commands;
    private final AtomicInteger version;

    private static final Logger Log = Logger.getLogger(SpreadsheetsRepResource.class.getName());
    private final String domain;
    private final String url;
    private final Discovery discovery;
    private final Leader leader;
    private final ExecutorService executorService;

    public SpreadsheetsRepResource(String domain, String ownUri, Discovery discovery, SpreadsheetDatabase sdb, Leader leader, AtomicInteger version) {
        this.domain = domain;
        url = ownUri;
        this.discovery = discovery;
        this.leader = leader;
        this.version = version;
        spreadsheetsManager = new SpreadsheetsManager(domain, ownUri, discovery, sdb);
        executorService = Executors.newCachedThreadPool();
        commands = new ArrayList<>();
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

    private Runnable getRunnable(AtomicBoolean aBoolean, Supplier<Result<?>> supplier) {
        return () -> {
            Result<?> result = supplier.get();
            if (result.isOK()) {
                aBoolean.set(true);
            }
        };
    }

    private void replicateIfPrimary(Function<URI, Supplier<Result<?>>> function) {
        if (isPrimary()) {
            AtomicBoolean replicated = new AtomicBoolean(false);
            Arrays.stream(discovery.knownUrisOf(domain + SHEETS))
                    .filter(uri -> !uri.toString().equals(url))
                    .forEach(uri -> {
                        System.out.println("1: " + uri);
                        System.out.println("2: " + url);
                        executorService.submit(getRunnable(replicated, function.apply(uri)));
                    });
            while (!replicated.get()) {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private boolean cannotWrite(String password) {
        System.out.println("i wanna write: " + url);
        System.out.println("password: " + password);
        System.out.println(!isPrimary() && !serverSecret.equals(password));
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < 3; i++) {
            System.out.println(stackTrace[i]);
        }
        return !isPrimary() && !serverSecret.equals(password);
    }

    private boolean isPrimary() {
        return url.equals(leader.getUrl());
    }

    private void updateVersion(Command command, String[] args, Spreadsheet sheet) {
        synchronized (commands) {
            commands.add(new ParameterizedCommand(
                    command,
                    version.incrementAndGet(),
                    args,
                    sheet));
        }
    }

    private void updateVersion(Command command, String[] args) {
        synchronized (commands) {
            commands.add(new ParameterizedCommand(
                    command,
                    version.incrementAndGet(),
                    args));
        }
    }

    @Override
    public String createSpreadsheet(Long version, Spreadsheet sheet, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .queryParam(PASSWORD, password)
                            .build(sheet))
                    .build());
        } else {
            String s = extractResult(spreadsheetsManager.createSpreadsheet(sheet, password));
            updateVersion(Command.createSpreadsheet, new String[]{password}, sheet);
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).createSpreadsheet(sheet, serverSecret));
            return s;
        }
    }

    @Override
    public void deleteSpreadsheet(Long version, String sheetId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path("/" + sheetId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        } else {
            extractResult(spreadsheetsManager.deleteSpreadsheet(sheetId, password));
            updateVersion(Command.deleteSpreadsheet, new String[]{sheetId, password});
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).deleteSpreadsheet(sheetId, serverSecret));
        }
    }

    @Override
    public Spreadsheet getSpreadsheet(Long version, String sheetId, String userId, String password) {
        if (version > this.version.get()) {
            updateVersion(version, serverSecret);
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path(sheetId)
                            .queryParam(USERID, userId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        }
        return extractResult(spreadsheetsManager.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetValues(Long version, String sheetId, String userId, String password) {
        if (version > this.version.get()) {
            updateVersion(version, serverSecret);
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path(sheetId)
                            .path(VALUES)
                            .queryParam(USERID, userId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        }
        return extractResult(spreadsheetsManager.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetRangeValues(Long version, String sheetId, String userEmail, String range, String serverSecret) {
        if (version > this.version.get()) {
            updateVersion(version, serverSecret);
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path(sheetId)
                            .path(RANGE_VALUES)
                            .queryParam(USER_EMAIL, userEmail)
                            .queryParam(RANGE, range)
                            .queryParam(SERVER_SECRET, serverSecret)
                            .build())
                    .build());
        }
        return extractResult(spreadsheetsManager.getSpreadsheetRangeValues(sheetId, userEmail, range, serverSecret));
    }

    private void updateVersion(Long version, String serverSecret) {
        executorService.submit(() -> {
            Result<String[]> commands = new RestSpreadsheetsClient(URI.create(leader.getUrl())).getCommands(version, serverSecret);
            if (commands.isOK()) {
                Arrays.stream(commands.value()).map(ParameterizedCommand::new).forEach(command -> {
                    String[] args = command.args();
                    switch (command.type) {
                        case createSpreadsheet -> spreadsheetsManager.createSpreadsheet(command.spreadsheet(), args[0]);
                        case deleteSpreadsheet -> spreadsheetsManager.deleteSpreadsheet(args[0], args[1]);
                        case deleteUserSheets -> spreadsheetsManager.deleteUserSheets(args[0], args[1]);
                        case unshareSpreadsheet -> spreadsheetsManager.unshareSpreadsheet(args[0], args[1], args[2]);
                        case updateCell -> spreadsheetsManager.updateCell(args[0], args[1], args[2], args[3], args[4]);
                        case shareSpreadsheet -> spreadsheetsManager.shareSpreadsheet(args[0], args[1], args[2]);
                    }
                });
            }
            return commands;
        });
    }

    @Override
    public void updateCell(Long version, String sheetId, String cell, String rawValue, String userId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path(sheetId)
                            .path(cell)
                            .queryParam(USERID, userId)
                            .queryParam(PASSWORD, password)
                            .build(rawValue))
                    .build());
        } else {
            extractResult(spreadsheetsManager.updateCell(sheetId, cell, rawValue, userId, password));
            updateVersion(Command.updateCell, new String[]{sheetId, cell, rawValue, userId, password});
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).updateCell(sheetId, cell, rawValue, userId, serverSecret));
        }
    }

    @Override
    public void shareSpreadsheet(Long version, String sheetId, String userId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path(sheetId)
                            .path(SHARE)
                            .path(userId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        } else {
            extractResult(spreadsheetsManager.shareSpreadsheet(sheetId, userId, password));
            updateVersion(Command.shareSpreadsheet, new String[]{sheetId, userId, password});
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).shareSpreadsheet(sheetId, userId, serverSecret));
        }
    }

    @Override
    public void unshareSpreadsheet(Long version, String sheetId, String userId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path(sheetId)
                            .path("share")
                            .path(userId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        } else {
            extractResult(spreadsheetsManager.unshareSpreadsheet(sheetId, userId, password));
            updateVersion(Command.unshareSpreadsheet, new String[]{sheetId, userId, password});
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).unshareSpreadsheet(sheetId, userId, serverSecret));
        }
    }

    @Override
    public void deleteUserSheets(Long version, String userId, String serverSecret) {
        if (cannotWrite(serverSecret)) {
            System.out.println("i wanna write FFS: " + url);
            System.out.println("serverSecret FFS: " + serverSecret);
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path("deleteUserSheets")
                            .path(userId)
                            .queryParam("serverSecret", serverSecret)
                            .build())
                    .build());
        } else {
            System.out.println("cest moi: " + url);
            extractResult(spreadsheetsManager.deleteUserSheets(userId, serverSecret));
            System.out.println("is it hard to do this turd?");
            System.out.println("userId: " + userId + " serverSecret: " + serverSecret);
            updateVersion(Command.deleteUserSheets, new String[]{userId, serverSecret});
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).deleteUserSheets(userId, SpreadsheetsManager.serverSecret));
            System.out.println("i should return ffs");
        }
    }

    @Override
    public String[] getCommands(Long version, String serverSecret) {
        if (cannotWrite(serverSecret)) {
            System.out.println("i wanna write FFS: " + url);
            System.out.println("serverSecret FFS: " + serverSecret);
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestRepSpreadsheets.PATH)
                            .path("commands")
                            .queryParam("serverSecret", serverSecret)
                            .build())
                    .build());
        } else {
            System.out.println("is it hard to do this turd? dfghdfhgfdghfghddfgh");
            return commands.subList(Math.toIntExact(version), commands.size()).stream()
                    .map(ParameterizedCommand::encode)
                    .toArray(String[]::new);
        }
    }

}
