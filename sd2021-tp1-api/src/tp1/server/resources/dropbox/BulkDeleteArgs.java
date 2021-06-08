package tp1.server.resources.dropbox;

import java.util.List;

public class BulkDeleteArgs {
    List<PathArg> entries;
    public BulkDeleteArgs(List<PathArg> entries){
        this.entries = entries;
    }
}
