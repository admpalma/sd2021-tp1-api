package tp1.server.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.api.service.util.SpreadsheetDatabase;
import tp1.server.resources.Discovery;
import tp1.server.resources.SpreadsheetsManager;
import tp1.server.rest.client.RestSpreadsheetsClient;
import tp1.util.Leader;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static tp1.server.resources.SpreadsheetsManager.serverSecret;


@Singleton
public class SpreadsheetsRepResource implements RestSpreadsheets {

    private static final String PASSWORD = "password";
    private static final String USERID = "userId";
    public static final String SHEETS = ":sheets";
    public static final String SHARE = "share";

    private final SpreadsheetsManager spreadsheetsManager;

    private static final Logger Log = Logger.getLogger(SpreadsheetsRepResource.class.getName());
    private final String domain;
    private final String url;
    private final Discovery discovery;
    private final Leader leader;
    private final ExecutorService executorService;

    public SpreadsheetsRepResource(String domain, String ownUri, Discovery discovery, SpreadsheetDatabase sdb, Leader leader) {
        this.domain = domain;
        url = ownUri;
        this.discovery = discovery;
        this.leader = leader;
        spreadsheetsManager = new SpreadsheetsManager(domain, ownUri, discovery, sdb);
        executorService = Executors.newCachedThreadPool();
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

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestSpreadsheets.PATH)
                            .queryParam(PASSWORD, password)
                            .build(sheet))
                    .build());
        } else {
            String s = extractResult(spreadsheetsManager.createSpreadsheet(sheet, password));
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).createSpreadsheet(sheet, serverSecret));
            return s;
        }
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestSpreadsheets.PATH)
                            .path("/" + sheetId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        } else {
            extractResult(spreadsheetsManager.deleteSpreadsheet(sheetId, password));
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).deleteSpreadsheet(sheetId, serverSecret));
        }
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
        return extractResult(spreadsheetsManager.getSpreadsheet(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
        return extractResult(spreadsheetsManager.getSpreadsheetValues(sheetId, userId, password));
    }

    @Override
    public String[][] getSpreadsheetRangeValues(String sheetId, String userEmail, String range, String serverSecret) {
        return extractResult(spreadsheetsManager.getSpreadsheetRangeValues(sheetId, userEmail, range, serverSecret));
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestSpreadsheets.PATH)
                            .path(sheetId)
                            .path(cell)
                            .queryParam(USERID, userId)
                            .queryParam(PASSWORD, password)
                            .build(rawValue))
                    .build());
        } else {
            extractResult(spreadsheetsManager.updateCell(sheetId, cell, rawValue, userId, password));
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).updateCell(sheetId, cell, rawValue, userId, serverSecret));
        }
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestSpreadsheets.PATH)
                            .path(sheetId)
                            .path(SHARE)
                            .path(userId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        } else {
            extractResult(spreadsheetsManager.shareSpreadsheet(sheetId, userId, password));
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).shareSpreadsheet(sheetId, userId, serverSecret));
        }
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {
        if (cannotWrite(password)) {
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestSpreadsheets.PATH)
                            .path(sheetId)
                            .path("share")
                            .path(userId)
                            .queryParam(PASSWORD, password)
                            .build())
                    .build());
        } else {
            extractResult(spreadsheetsManager.unshareSpreadsheet(sheetId, userId, password));
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).unshareSpreadsheet(sheetId, userId, serverSecret));
        }
    }

    @Override
    public void deleteUserSheets(String userId, String serverSecret) {
        if (cannotWrite(serverSecret)) {
            System.out.println("i wanna write FFS: " + url);
            System.out.println("serverSecret FFS: " + serverSecret);
            throw new WebApplicationException(Response.temporaryRedirect(
                    UriBuilder.fromPath(leader.getUrl() + RestSpreadsheets.PATH)
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
            replicateIfPrimary(uri -> () -> new RestSpreadsheetsClient(uri).deleteUserSheets(userId, SpreadsheetsManager.serverSecret));
            System.out.println("i should return ffs");
        }
    }
}
