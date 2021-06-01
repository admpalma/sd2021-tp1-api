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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import tp1.api.service.util.SpreadsheetDatabase;
import tp1.server.resources.dropbox.*;

public class SpreadsheetDropbox implements SpreadsheetDatabase {

    private static final String CREATE_FOLDER_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
    private static final String UPLOAD_FILE_URL = "https://content.dropboxapi.com/2/files/upload";
    private static final String GET_FILE_URL = "https://content.dropboxapi.com/2/files/download";
    private static final String DELETE_FILE_URL = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String LIST_FOLDER_URL = "https://api.dropboxapi.com/2/files/list_folder";
    private static final String LIST_FOLDER_CONTINUE_URL = "https://api.dropboxapi.com/2/files/list_folder/continue";

    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String OCTET_CONTENT_TYPE = "application/octet-stream";

    private Gson json;

    @SuppressWarnings("SpellCheckingInspection")
    private static final String apiKey = "zlf67qhgv2h9w9v";
    private static final String apiSecret = "hg677c1o66w9s2c";

    @SuppressWarnings("SpellCheckingInspection")
    private static final String accessTokenStr = "STBuIpf86CQAAAAAAAAAAYBVTmzLp2W70p-EVzoTJ8lr8u7Y_xAz0dv8CRKMomIn";

    private final String baseDir;


    private final OAuth20Service service;
    private OAuth2AccessToken accessToken;

    public static void main(String[] args) {
        SpreadsheetDropbox map = new SpreadsheetDropbox("/test",true);
        Spreadsheet s = new Spreadsheet();
        s.setOwner("despacito");
        s.setSheetId("kek");
        map.put("kek",s);
        map.removeUserSpreadsheets("despacito");

//        map.put("haha",new Spreadsheet());
//        Spreadsheet s = map.get("hahe");
//        System.out.println(map.containsKey("hahe"));
//        System.out.println(map.remove("hahe"));
    }

    public SpreadsheetDropbox(String baseDir, boolean refresh) {
        json = new Gson();

        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        this.baseDir = baseDir;
        if (refresh)
            deleteFolder(baseDir);
    }

    private void deleteFolder(String folder) {
        pathRequest(folder, DELETE_FILE_URL);
    }

    private void pathRequest(String path, String requestURL) {
        OAuthRequest createFolder = new OAuthRequest(Verb.POST, requestURL);
        createFolder.addHeader("Content-Type",JSON_CONTENT_TYPE);
        createFolder.setPayload(json.toJson(new PathArg(path)));

        executeRequest(createFolder);
    }

    private void createFolder(String folder) {
        pathRequest(folder, CREATE_FOLDER_URL);
    }

    @Override
    public boolean containsKey(String key) {
        return Objects.requireNonNull(listDirectory(baseDir + "/sheets/" + key)).contains(key);
    }

    private List<String> listDirectory(String path){

        List<String> directoryContents = new ArrayList<>();

        OAuthRequest listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_URL);
        listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
        listDirectory.setPayload(json.toJson(new ListFolderArgs(baseDir + "/"+path, false)));

        service.signRequest(accessTokenStr, listDirectory);

        Response r = null;

        try {
            while(true) {
                r = service.execute(listDirectory);

                if(r.getCode() == 409){
                    return directoryContents;
                }
                if(r.getCode() != 200) {
                    System.err.println("Failed to list directory '" + path + "'. Status " + r.getCode() + ": " + r.getMessage());
                    System.err.println(r.getBody());
                    return directoryContents;
                }

                ListFolderReturn reply = json.fromJson(r.getBody(), ListFolderReturn.class);

                for(ListFolderReturn.FolderEntry e: reply.getEntries()) {
                    directoryContents.add(e.toString());
                }

                if(reply.has_more()) {
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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return directoryContents;
    }

    private String getFileText(String path){
        OAuthRequest request = new OAuthRequest(Verb.POST, GET_FILE_URL);
        request.addHeader("Content-Type",OCTET_CONTENT_TYPE);
        request.addHeader("Dropbox-API-Arg",json.toJson(
                new GetFileArgs(baseDir +"/"+ path)
        ));

        service.signRequest(accessTokenStr,request);

        Response r=null;
        String fileText = null;
        try{
            r = service.execute(request);
            fileText = r.getBody();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if ( r==null)
            throw new RuntimeException("Dropbox query failed with null");
        if (r.getCode()!=200 && r.getCode()!=409)
            throw new RuntimeException("Dropbox query failed with "+r.getCode());

        return fileText;
    }


    @Override
    public Spreadsheet get(String key) {
        return json.fromJson(getFileText("sheets/"+key),Spreadsheet.class);
    }

    private void uploadFile(String path,Object file){
        OAuthRequest request = new OAuthRequest(Verb.POST, UPLOAD_FILE_URL);
        request.addHeader("Content-Type",OCTET_CONTENT_TYPE);
        request.addHeader("Dropbox-API-Arg",json.toJson(
                new GetFileArgs(baseDir + "/" +path)
        ));
        request.setPayload(json.toJson(file));

        executeRequest(request);
    }

    private void executeRequest(OAuthRequest request) {
        service.signRequest(accessTokenStr,request);

        Response r=null;
        try{
            r = service.execute(request);
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if ( r==null)
            throw new RuntimeException("Dropbox query failed with null");
        if (r.getCode()!=200 && r.getCode()!=409) {
            try {
                throw new RuntimeException("Dropbox query failed with "+r.getCode()+"\n"+r.getBody());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Spreadsheet put(String key, Spreadsheet value) {

        Spreadsheet s = get(key);
        uploadFile("sheets/"+key,value);
        uploadFile("users/"+value.getOwner()+"/"+value.getSheetId(),"");

        return s;
    }

    @Override
    public boolean removeUserSpreadsheets(String userId) {
        List<String> sheets = listDirectory("users/" + userId);
        assert sheets != null;
        Thread[] t = new Thread[sheets.size()];
        for (int i = 0;i<t.length;i++) {
            int finalI = i;
            t[i] = new Thread(() -> {
                String[] pathParts = sheets.get(finalI).split("/");
                String path = pathParts[pathParts.length-1];
                remove(path);
            });
            t[i].start();
        }
        for (Thread k:t){
            try {
                k.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        pathRequest(baseDir + "/users/" + userId, DELETE_FILE_URL);
        return sheets.size()!=0;
    }


    public Spreadsheet remove(String sheetId) {
        Spreadsheet s = get(sheetId);
        if (s!=null) {
            pathRequest(baseDir+"/sheets/" + sheetId, DELETE_FILE_URL);
            pathRequest(baseDir+"/users/" + s.getOwner(), DELETE_FILE_URL);
        }
        return s;
    }

    @Override
    public Spreadsheet putIfAbsent(String keyObj, Spreadsheet value) {
//        String key = (String) keyObj;
//
//        List<LockFileArg> file = new LinkedList();
//        file.add(new LockFileArg(baseDir+"/"+key));
//        try {
//            List<LockFileResultEntry> s = client.files().getFileLockBatch(file).getEntries();
//            System.out.println();
//        } catch (DbxException e) {
//            e.printStackTrace();
//        }
        return null;
    }

}
