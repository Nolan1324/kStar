package structure;

public class Cache {
    public String type;
    public int siteHash;
    public byte[] compiledData;

    public Cache(String type, int siteHash, byte[] compiledData) {
        this.type = type;
        this.siteHash = siteHash;
        this.compiledData = compiledData;
    }

    public Cache() {

    }
}
