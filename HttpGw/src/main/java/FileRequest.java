import java.io.*;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class FileRequest {
    private static final int MAX_THREADS = 10;

    private final HostAddresses hostAddresses;
    private final String filename;
    private final List<Thread> threads;
    private OrderedChunks orderedChunks;

    public FileRequest(HostAddresses hostAddresses, String filename) {
        this.hostAddresses = hostAddresses;

        this.filename = filename;
        this.threads = new ArrayList<>();
    }

    public OrderedChunks requestFile() throws IOException {
        Message file_size = getFileSize(hostAddresses.get(0));

        if (file_size.getQuery_type() != 'e') {
            getChunks(file_size.getSize());
        } else {
            System.out.println("Requested file does not exist.");
        }

        return orderedChunks;
    }

    private Message getFileSize(HostAddress hostAddress) throws IOException {
        Message msg = Message.newFileSizeRequest(filename);
        DatagramSocket socket = new DatagramSocket();

        Message.sendMessage(msg, hostAddress, socket);
        Message message = Message.receiveMessage(socket);
        socket.close();

        return message;
    }

    private void getChunks(long file_size) {
        int maxChunkSize = Message.BUFFER_SIZE - filename.length() - 50;

        long chunks = file_size / maxChunkSize + 1;
        orderedChunks = new OrderedChunks(chunks);

        List<Long> missingChunks = orderedChunks.missingChunks();
        requestMissingChunks(missingChunks, maxChunkSize, chunks, file_size);
        waitForThreads();

        while (!orderedChunks.hasAllChunks()) {
            missingChunks = orderedChunks.missingChunks();
            requestMissingChunks(missingChunks, maxChunkSize, chunks, file_size);
            waitForThreads();
        }
    }

    private void requestMissingChunks(List<Long> missingChunks, int maxChunkSize, long chunks, long file_size) {
        long lim_inf = 0;
        long lim_sup = maxChunkSize;

        long i;
        for (i = 0; (i < chunks) && (lim_sup < file_size) && (threads.size() < MAX_THREADS); i++) {
            if (missingChunks.contains(i)) {
                requestChunk(i ,lim_inf, lim_sup);
            }

            lim_inf = lim_sup + 1;
            lim_sup = lim_sup + maxChunkSize;
        }

        if (lim_sup > file_size) {
            if (missingChunks.contains(i)) {
                requestChunk(i, lim_inf, file_size - 1);
            }
        }
    }

    private void requestChunk(long i, long lim_inf, long lim_sup) {
        Chunk chunk = new Chunk(filename, i, lim_inf, lim_sup);
        Thread t = new Thread(new ChunkRequest(hostAddresses, chunk, orderedChunks));
        t.start();
        threads.add(t);
    }

    private void waitForThreads() {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Fallen brother. We have lost thy soul.");
            }
        }
        threads.clear();
    }
}
