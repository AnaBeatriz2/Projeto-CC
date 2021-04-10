import com.google.common.primitives.Bytes;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class Message implements Serializable {
    private String query_type;
    private String filename;
    private long chunk_start;
    private long chunk_end;
    private List<Byte> data;
    private long size;
    private String error_message;

    public static Message newFileSizeRequest(String filename) {
        Message message = new Message();
        message.query_type = "t";
        message.filename = filename;
        return message;
    }

    public static Message newFileSizeResponse(String filename, long size) {
        Message message = new Message();
        message.query_type = "st";
        message.filename = filename;
        message.size = size;
        return message;
    }

    public static Message newErrorMessage(String error_message) {
        Message message = new Message();
        message.query_type = "err";
        message.error_message = error_message;
        return message;
    }

    public static Message newChunkRequest(String filename, long start, long end) {
        Message message = new Message();
        message.query_type = "c";
        message.filename = filename;
        message.chunk_start = start;
        message.chunk_end = end;
        return message;
    }

    public static Message newChunkResponse(String filename, long start, long end, byte[] data) {
        Message message = new Message();
        message.query_type = "sc";
        message.filename = filename;
        message.chunk_start = start;
        message.chunk_end = end;
        message.data = Bytes.asList(data);
        return message;
    }

    public String getQuery_type() {
        return query_type;
    }

    public void setQuery_type(String query_type) {
        this.query_type = query_type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getChunk_start() {
        return chunk_start;
    }

    public void setChunk_start(long chunk_start) {
        this.chunk_start = chunk_start;
    }

    public long getChunk_end() {
        return chunk_end;
    }

    public void setChunk_end(long chunk_end) {
        this.chunk_end = chunk_end;
    }

    public byte[] getData() {
        return Bytes.toArray(data);
    }

    public void setData(byte[] data) {
        this.data = Bytes.asList(data);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "query_type='" + query_type + '\'' +
                ", filename='" + filename + '\'' +
                ", chunk_start=" + chunk_start +
                ", chunk_end=" + chunk_end +
                ", size=" + size +
                ", error_message='" + error_message + '\'' +
                '}';
    }
}
