import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class OrderedChunks {
    ReentrantLock reentrantLock = new ReentrantLock();

    private final Map<Long, byte[]> ordered_chunks;
    private final long chunks;
    private long received_chunks;

    public OrderedChunks(long chunks) {
        this.ordered_chunks = new HashMap<>();
        this.chunks = chunks;
        this.received_chunks = 0;
    }

    public long getChunks() {
        return chunks;
    }

    public void put(long chunk, byte[] data) {
        reentrantLock.lock();
        if (!this.ordered_chunks.containsKey(chunk)) {
            this.ordered_chunks.put(chunk, data);
            this.received_chunks++;
        }
        reentrantLock.unlock();
    }

    public byte[] get(long n_chunk) {
        reentrantLock.lock();
        byte[] chunk = this.ordered_chunks.get(n_chunk);
        reentrantLock.unlock();
        return chunk;
    }

    public boolean hasAllChunks() {
        reentrantLock.lock();
        boolean hasAll = this.chunks == this.received_chunks;
        reentrantLock.unlock();
        return hasAll;
    }

    public List<Long> missingChunks() {
        List<Long> missingChunks = new ArrayList<>();

        reentrantLock.lock();

        for (long i = 0; i < chunks; i++) {
            if (!ordered_chunks.containsKey(i)) {
                missingChunks.add(i);
            }
        }

        reentrantLock.unlock();

        return missingChunks;
    }
}
