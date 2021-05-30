package tp1.server.resources.dropbox;

public class UploadFileArgs {
    final String path;
    final boolean autorename;
    final String mode;
    final String mute;

    public UploadFileArgs(String path, boolean autorename, String mode, String mute) {
        this.path = path;
        this.autorename = autorename;
        this.mode = mode;
        this.mute = mute;
    }
}
