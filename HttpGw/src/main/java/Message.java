import com.google.common.primitives.Bytes;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {
    private byte query_type; // size: 1 + 1

    private String file_hash; // size: 4 + 1
    private String filename; // size: ? max -> HttpGw.BUFFER_SIZE - 5 - 1
    private long size; // size: ? but not a problem for 256 plus buffer

    private long chunk_number; // size: ? but not a problem for 256 plus buffer
    private long chunk_start; // size: ? but not a problem for 256 plus buffer
    private long chunk_end; // size: ? but not a problem for 256 plus buffer

    private byte[] data; // size: ? max -> HttpGw.BUFFER_SIZE - 15

    private String error_message; // size: ? max HttpGw.BUFFER_SIZE - 1 - 1

    public static Message newFileSizeRequest(String file_hash, String filename) {
        Message message = new Message();
        message.query_type = 'f';
        message.file_hash = file_hash;
        message.filename = filename;
        return message;
    }

    public static Message newFileSizeResponse(String file_hash, long size) {
        Message message = new Message();
        message.query_type = 's';
        message.file_hash = file_hash;
        message.size = size;
        return message;
    }

    public static Message newErrorMessage(String error_message) {
        Message message = new Message();
        message.query_type = 'e';
        message.error_message = error_message;
        return message;
    }

    public static Message newChunkRequest(String file_hash, long chunk_number, long chunk_start, long chunk_end) {
        Message message = new Message();
        message.query_type = 'c';
        message.file_hash = file_hash;
        message.chunk_number = chunk_number;
        message.chunk_start = chunk_start;
        message.chunk_end = chunk_end;
        return message;
    }

    public static Message newChunkResponse(String file_hash, long chunk_number, byte[] data) {
        Message message = new Message();
        message.query_type = 'd';
        message.file_hash = file_hash;
        message.chunk_number = chunk_number;
        message.data = data;
        return message;
    }

    public byte getQuery_type() {
        return query_type;
    }

    public String getFile_hash() {
        return file_hash;
    }

    public String getFilename() {
        return filename;
    }

    public long getChunk_start() {
        return chunk_start;
    }

    public long getChunk_end() {
        return chunk_end;
    }

    public long getChunk_number() {
        return chunk_number;
    }

    public byte[] getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    public String getError_message() {
        return error_message;
    }

    public byte[] serialize() {
        return switch (query_type) {
            case 'f' -> serializeFileSizeRequest();
            case 's' -> serializeFileSizeResponse();
            case 'c' -> serializeChunkRequest();
            case 'd' -> serializeChunkResponse();
            case 'e' -> serializeError();
            default -> new byte[0];
        };
    }

    private byte[] serializeFileSizeRequest() {
        String t = ";" + file_hash + ";" + filename + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    private byte[] serializeFileSizeResponse() {
        String t = ";" + file_hash + ";" + size + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    private byte[] serializeChunkRequest() {
        String t =";" + file_hash + ";" + chunk_number + ";" + chunk_start + ";" + chunk_end + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    private byte[] serializeChunkResponse() {
        String t = ";" + file_hash + ";" + Long.toString(chunk_number).length() + ";" + chunk_number + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes(), data);
    }

    private byte[] serializeError() {
        String t = ";" + error_message + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    public static Message deserialize(byte[] data) {
        return switch (data[0]) {
            case 'f' -> deserializeFileSizeRequest(data);
            case 's' -> deserializeFileSizeResponse(data);
            case 'c' -> deserializeChunkRequest(data);
            case 'd' -> deserializeChunkResponse(data);
            case 'e' -> deserializeError(data);
            default -> null;
        };
    }

    private static Message deserializeFileSizeRequest(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        String[] ts = t.split(";");
        Message m = new Message();
        m.query_type = 'f';
        m.file_hash = ts[0];
        m.filename = ts[1];
        return m;
    }

    private static Message deserializeFileSizeResponse(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        String[] ts = t.split(";");
        Message m = new Message();
        m.query_type = 's';
        m.file_hash = ts[0];
        m.size = Long.parseLong(ts[1]);
        return m;
    }

    private static Message deserializeChunkRequest(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        String[] ts = t.split(";");
        Message m = new Message();
        m.query_type = 'c';
        m.file_hash = ts[0];
        m.chunk_number = Long.parseLong(ts[1]);
        m.chunk_start = Long.parseLong(ts[2]);
        m.chunk_end = Long.parseLong(ts[3]);
        return m;
    }

    private static Message deserializeChunkResponse(byte[] data) {
        int usefulSize = 0;

        for (; usefulSize < data.length; usefulSize++) {
            if (data[usefulSize] == '\0') {
                break;
            }
        }

        String fileHash = new String(Arrays.copyOfRange(data, 2, 6));
        int chunkNumberSize = Integer.parseInt(new String(Arrays.copyOfRange(data, 7, 8)));
        long chunkNumber = Long.parseLong(new String(Arrays.copyOfRange(data, 9, 9 + chunkNumberSize)));
        byte[] content = Arrays.copyOfRange(data, 10 + chunkNumberSize, usefulSize);

        Message m = new Message();
        m.query_type = 'd';
        m.file_hash = fileHash;
        m.chunk_number = chunkNumber;
        m.data = content;
        return m;
    }

    private static Message deserializeError(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        Message m = new Message();
        m.query_type = 'e';
        m.error_message = t;
        return m;
    }


    @Override
    public String toString() {
        return "Message{" +
                "query_type=" + query_type +
                ", file_hash='" + file_hash + '\'' +
                ", filename='" + filename + '\'' +
                ", chunk_start=" + chunk_start +
                ", chunk_end=" + chunk_end +
                ", chunk_number=" + chunk_number +
                ", data=" + Arrays.toString(data) +
                ", size=" + size +
                ", error_message='" + error_message + '\'' +
                '}';
    }
}
