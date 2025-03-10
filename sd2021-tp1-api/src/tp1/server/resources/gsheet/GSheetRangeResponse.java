package tp1.server.resources.gsheet;

public class GSheetRangeResponse {
    private String range;
    private String majorDimension;
    private String[][] values;

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getMajorDimension() {
        return majorDimension;
    }

    public void setMajorDimension(String majorDimension) {
        this.majorDimension = majorDimension;
    }

    public String[][] getValues(){
        return values;
    }

    public void setValues(String[][] values) {
        this.values = values;
    }
}
