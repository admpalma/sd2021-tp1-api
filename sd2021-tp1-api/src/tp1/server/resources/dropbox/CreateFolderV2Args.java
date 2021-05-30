package tp1.server.resources.dropbox;

import com.google.gson.JsonElement;

public class CreateFolderV2Args {
    final String path;
    final boolean autorename;

    public CreateFolderV2Args(String folder, boolean b) {
        path = folder;
        autorename = b;
    }
}
