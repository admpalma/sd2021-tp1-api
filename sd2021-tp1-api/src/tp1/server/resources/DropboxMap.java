package tp1.server.resources;


import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.files.*;
import com.google.gson.Gson;
import tp1.api.Spreadsheet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import com.dropbox.core.v2.DbxClientV2;

public class DropboxMap implements ConcurrentMap<String, Spreadsheet> {


//    private static final String CREATE_FOLDER_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
//    private static final String UPLOAD_FILE_URL = "https://content.dropboxapi.com/2/files/upload";
//    private static final String GET_FILE_URL = "https://content.dropboxapi.com/2/files/download";
//
//    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
//    private static final String OCTET_CONTENT_TYPE = "application/octet-stream; charset=utf-8";

    private Gson json;

    @SuppressWarnings("SpellCheckingInspection")
    private static final String apiKey = "zlf67qhgv2h9w9v";
    private static final String apiSecret = "hg677c1o66w9s2c";

    @SuppressWarnings("SpellCheckingInspection")
    private static final String accessTokenStr = "STBuIpf86CQAAAAAAAAAAYBVTmzLp2W70p-EVzoTJ8lr8u7Y_xAz0dv8CRKMomIn";

    private final String baseDir;

    private final DbxClientV2 client;

    public static void main(String[] args) {
        DropboxMap map = new DropboxMap("/test");

        map.put("haha",new Spreadsheet());
        Spreadsheet s = map.get("hahe");
        System.out.println(map.containsKey("hahe"));
        System.out.println(map.remove("hahe"));
    }

    public DropboxMap(String baseDir) {
        json = new Gson();

        this.baseDir = baseDir;
        client = getClient();
        createFolder(baseDir);
    }

    private DbxClientV2 getClient() {
        DbxRequestConfig dbxRequestConfig = DbxRequestConfig.newBuilder(
                "SD").build();

        return new DbxClientV2(dbxRequestConfig, DropboxMap.accessTokenStr);
    }


    private void createFolder(String folder) {
        try {
            client.files().createFolderV2(folder);
        } catch (CreateFolderErrorException ignored){
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int size() {
        //TODO Not exhausting "has_more"
        try {
            return client.files().listFolder(baseDir).getEntries().size();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object keyObj) {
        String key = (String) keyObj;
        try {
            return client.files().listFolder(baseDir).getEntries()
                    .stream().anyMatch(
                            metadata -> key.equals(metadata.getName())
                    );
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {

        return false;
    }

    @Override
    public Spreadsheet get(Object keyObj) {
        String key = (String)keyObj;
        Spreadsheet s = null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            client.files().download(baseDir + "/" + key).download(outputStream);
            s = json.fromJson(outputStream.toString(),Spreadsheet.class);

        } catch (DownloadErrorException ignored){
        } catch (DbxException | IOException e) {
            e.printStackTrace();
        }

        return s;
    }

    @Override
    public Spreadsheet put(String key, Spreadsheet value) {

        Spreadsheet s = get(key);

        try (InputStream in = new ByteArrayInputStream(json.toJson(value).getBytes(StandardCharsets.UTF_8))) {
            client.files().uploadBuilder(baseDir+"/"+key)
                    .withAutorename(false)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
        } catch (IOException | DbxException e) {
            e.printStackTrace();
        }

        return s;
    }

    @Override
    public Spreadsheet remove(Object keyObj) {
        Spreadsheet s = get(keyObj);
        String key = (String) keyObj;

        if (s!=null)
            try {
                client.files().deleteV2(baseDir+"/"+key);
            } catch (DeleteErrorException e) {
                e.printStackTrace();
            } catch (DbxException e) {
                e.printStackTrace();
            }
        return s;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Spreadsheet> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public Collection<Spreadsheet> values() {
        return null;
    }

    @Override
    public Set<Entry<String, Spreadsheet>> entrySet() {
        return null;
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

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public boolean replace(String key, Spreadsheet oldValue, Spreadsheet newValue) {
        return false;
    }

    @Override
    public Spreadsheet replace(String key, Spreadsheet value) {
        return null;
    }
}
