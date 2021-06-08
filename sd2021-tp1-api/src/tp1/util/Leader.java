package tp1.util;

public class Leader {
    private String url;

    public Leader() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        System.out.println("we love this kind of turd like debugginhpdosfgiuhpouhidfsgohijdfgv ophijdfs gophijudfg oij");
        System.out.println("anyway, moi: " + url);
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < 3; i++) {
            System.out.println(stackTrace[i]);
        }
        this.url = url;
    }
}
