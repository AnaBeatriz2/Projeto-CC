package Common.Message;

public class MessageContent {
    protected byte query_type;

    protected String file_name;
    protected String error_message;

    protected long file_size;
    protected long chunk_number;
    protected long chunk_start;
    protected long chunk_end;

    protected byte[] data;

    public byte getQuery_type() {
        return query_type;
    }

    public String getFile_name() {
        return file_name;
    }

    public String getError_message() {
        return error_message;
    }

    public long getFile_size() {
        return file_size;
    }

    public long getChunk_number() {
        return chunk_number;
    }

    public long getChunk_start() {
        return chunk_start;
    }

    public long getChunk_end() {
        return chunk_end;
    }

    public byte[] getData() {
        return data;
    }
}
