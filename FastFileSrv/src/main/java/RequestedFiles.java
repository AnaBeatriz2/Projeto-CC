import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.Map;

public class RequestedFiles {
    private final Map<String, String> files;

    public RequestedFiles() {
        this.files = new HashMap<>();
    }

    public String addFile(String filename) {
        String fileHash = null;
        boolean hasKey = true;

        while (hasKey) {
            fileHash = RandomStringUtils.random(4, true, true);
            hasKey = files.containsKey(fileHash);
        }

        files.put(fileHash, filename);
        return fileHash;
    }

    public void put(String fileHash, String filename) {
        files.put(fileHash, filename);
    }

    public String getFile(String fileHash) {
        return files.get(fileHash);
    }

    public void deleteFile(String fileHash) {
        files.remove(fileHash);
    }
}

