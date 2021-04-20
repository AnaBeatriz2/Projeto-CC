public class Chunk {
    private final String filename;
    private final long chunk;
    private final long start;
    private final long end;

    public Chunk(String filename, long chunk, long start, long end) {
        this.filename = filename;
        this.chunk = chunk;
        this.start = start;
        this.end = end;
    }

    public String getFilename() {
        return filename;
    }

    public long getChunk() {
        return chunk;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
