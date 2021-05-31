package tp1.server.resources.dropbox;


public class ListFolderContinueArgs {
    final String cursor;

    public ListFolderContinueArgs(String cursor) {
        this.cursor = cursor;
    }
}
