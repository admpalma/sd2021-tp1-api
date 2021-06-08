package tp1.server.resources;


import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.Spreadsheet;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import tp1.api.service.util.SpreadsheetDatabase;
import tp1.server.resources.dropbox.*;

public class SpreadsheetDropbox implements SpreadsheetDatabase {
    private static final int MAX_RETRIES = 5;

    //    private static final String CREATE_FOLDER_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
    private static final String UPLOAD_FILE_URL = "https://content.dropboxapi.com/2/files/upload";
    private static final String GET_FILE_URL = "https://content.dropboxapi.com/2/files/download";
    private static final String DELETE_FILE_URL = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String LIST_FOLDER_URL = "https://api.dropboxapi.com/2/files/list_folder";
    private static final String LIST_FOLDER_CONTINUE_URL = "https://api.dropboxapi.com/2/files/list_folder/continue";
    private static final String BULK_DELETE_URL = "https://api.dropboxapi.com/2/files/delete_batch";

    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String OCTET_CONTENT_TYPE = "application/octet-stream";

    private Gson json;

    private final String accessTokenStr;

    private final String baseDir;


    private final OAuth20Service service;
    private OAuth2AccessToken accessToken;

    // Since there is only a single instance per domain, a cache ends up always being up to date,
    // except for when there is previous information already there, therefore its read only.
    private final ConcurrentMap<String, Spreadsheet> cache;

    /*
    Folder structure, where the files inside each user are empty
    and serve only as a listing of which files belong to who

    ├── sheets
    │   ├── 1
    │   ├── 2
    │   ├── 3
    │   ├── 4
    │   └── 5
    └── users
        ├── user-1
        │   ├── 1
        │   ├── 2
        │   └── 3
        └── user-2
            ├── 4
            └── 5
     */


    public SpreadsheetDropbox(String baseDir, boolean refresh, String apiKey,String apiSecret, String accessTokenStr) {
        json = new Gson();
        this.accessTokenStr = accessTokenStr;
        cache = new ConcurrentHashMap<>();
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        this.baseDir = baseDir;
        if (refresh)
            pathRequest(baseDir, DELETE_FILE_URL);
    }

    private void pathRequest(String path, String requestURL) {
        OAuthRequest createFolder = new OAuthRequest(Verb.POST, requestURL);
        createFolder.addHeader("Content-Type", JSON_CONTENT_TYPE);
        createFolder.setPayload(json.toJson(new PathArg(path)));

        executeRequest(createFolder);
    }

    @Override
    public boolean containsKey(String key) {
        boolean contains = cache.containsKey(key);
        if (contains)
            return true;
        return Objects.requireNonNull(listDirectory("sheets")).contains(key);
    }

    private List<String> listDirectory(String path) {

        List<String> directoryContents = new ArrayList<>();

        OAuthRequest listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_URL);
        listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
        listDirectory.setPayload(json.toJson(new ListFolderArgs(baseDir + "/" + path, false)));

        service.signRequest(accessTokenStr, listDirectory);

        Response r;

        try {
            while (true) {
                r = service.execute(listDirectory);

                if (r.getCode() == 409) {
                    return directoryContents;
                }
                if (r.getCode() != 200) {
                    System.err.println("Failed to list directory '" + path + "'. Status " + r.getCode() + ": " + r.getMessage());
                    System.err.println(r.getBody());
                    return directoryContents;
                }

                ListFolderReturn reply = json.fromJson(r.getBody(), ListFolderReturn.class);

                for (ListFolderReturn.FolderEntry e : reply.getEntries()) {
                    String[] nodes = e.toString().split("/");
                    directoryContents.add(nodes[nodes.length - 1]);
                }

                if (reply.has_more()) {
                    //There are more elements to read, prepare a new request (now a continuation)
                    listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_CONTINUE_URL);
                    listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
                    //In this case the arguments is just an object containing the cursor that was returned in the previous reply.
                    listDirectory.setPayload(json.toJson(new ListFolderContinueArgs(reply.getCursor())));
                    service.signRequest(accessToken, listDirectory);
                } else {
                    break; //There are no more elements to read. Operation can terminate.
                }
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return directoryContents;
    }

    // Generalised file text getter, could be used for other types of files that would not be spreadsheets
    private String getFileText(String path) {
        OAuthRequest request = new OAuthRequest(Verb.POST, GET_FILE_URL);
        request.addHeader("Content-Type", OCTET_CONTENT_TYPE);
        request.addHeader("Dropbox-API-Arg", json.toJson(
                new PathArg(baseDir + "/" + path)
        ));

        return executeRequest(request);
    }


    // This method could internally be named getSheet, but externally it would be redundant,
    // as this is already a sheets service plus it returns Spreadsheet
    @Override
    public Spreadsheet get(String key) {
        Spreadsheet s = cache.get(key);
        if (s != null)
            return s;
        return json.fromJson(getFileText("sheets/" + key), Spreadsheet.class);
    }

    // Could be used for any Jsonable class
    private void uploadFile(String path, Object file) {
        OAuthRequest request = new OAuthRequest(Verb.POST, UPLOAD_FILE_URL);
        request.addHeader("Content-Type", OCTET_CONTENT_TYPE);
        request.addHeader("Dropbox-API-Arg", json.toJson(
                new UploadFileArgs(baseDir + "/" + path, false, "overwrite", true)
        ));
        request.setPayload(json.toJson(file));

        executeRequest(request);
    }

    // Simple request wrapper
    private String executeRequest(OAuthRequest request) {
        service.signRequest(accessTokenStr, request);
        String body = null;


        Response r = null;
        try {
            r = service.execute(request);
            body = r.getBody();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (r == null)
            throw new RuntimeException("Dropbox query failed with null");
        if (r.getCode() == 409)
            return null;
        if (r.getCode() != 200) {
            try {
                throw new RuntimeException("Dropbox query failed with " + r.getCode() + "\n" + r.getBody());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return body;
    }

    // Puts and returns old value
    @Override
    public Spreadsheet put(String key, Spreadsheet value) {

        Spreadsheet s = cache.put(key, value);
        if (s == null)
            s = get(key);

        // Could be replaced by a request queue maybe?
        Thread a = new Thread(() -> uploadFile("sheets/" + key, value));
        Thread b = new Thread(() -> uploadFile("users/" + value.getOwner() + "/" + value.getSheetId(), ""));
        a.start();
        b.start();

        try {
            a.join();
            b.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return s;
    }

    // This method deletes stuff asynchronously
    private void deleteBulk(List<String> paths) {
        List<PathArg> argList = new LinkedList<>();
        for (String path : paths) {
            argList.add(new PathArg(path));
        }
        BulkDeleteArgs arg = new BulkDeleteArgs(argList);

        OAuthRequest request = new OAuthRequest(Verb.POST, BULK_DELETE_URL);
        request.addHeader("Content-Type", JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(arg));
        executeRequest(request);
    }

    @Override
    public boolean removeUserSpreadsheets(String userId) {

        cache.values().removeIf(spreadsheet -> spreadsheet.getOwner().equals(userId));

        List<String> sheets = listDirectory("users/" + userId);
        assert sheets != null;
        deleteBulk(sheets.stream().map(s -> baseDir + "/sheets/" + s).collect(Collectors.toList()));
        pathRequest(baseDir + "/users/" + userId, DELETE_FILE_URL);

        return sheets.size() != 0;
    }


    public Spreadsheet remove(String sheetId) {
        Spreadsheet s = cache.remove(sheetId);
        if (s == null)
            s = get(sheetId);

        if (s != null) {
            pathRequest(baseDir + "/sheets/" + sheetId, DELETE_FILE_URL);
            pathRequest(baseDir + "/users/" + s.getOwner() + "/" + sheetId, DELETE_FILE_URL);
        }
        return s;
    }

    // Here we are using god as our concurrency check
    @Override
    public Spreadsheet putIfAbsent(String key, Spreadsheet value) {
        Spreadsheet s = cache.putIfAbsent(key, value);
        if (s == null)
            s = get(key);
        if (s == null) {
            put(key, value);
        }
        return s;
    }

}
