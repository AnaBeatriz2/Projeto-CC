import java.io.IOException;
import java.net.DatagramSocket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ChunkRequest implements Runnable{
    private final List<HostAddress> hostAddresses;
    private final Chunk chunk;
    private final OrderedChunks orderedChunks;

    public ChunkRequest(List<HostAddress> hostAddresses, Chunk chunk, OrderedChunks orderedChunks) {
        this.hostAddresses = hostAddresses;
        this.chunk = chunk;
        this.orderedChunks = orderedChunks;
    }

    private HostAddress getRandomHostAddress() {
        Random rand = new Random();
        return this.hostAddresses.get(rand.nextInt(hostAddresses.size()));
    }

    public void run() {
        chunkRequest();
    }

    private void chunkRequest() {
        Message chunk_request = Message.newChunkRequest(chunk);

        Message data = null;

        for (int i = 0; data == null && i < 10; i++) {
            try {
                data = sendRequest(chunk_request);
            } catch (IOException ignored) { }
        }

        if (data != null) {
            orderedChunks.put(data.getChunk_number(), data.getData());
        }

    }

    private Message sendRequest(Message chunk_request) throws IOException {
        HostAddress hostAddress = this.getRandomHostAddress();
        DatagramSocket socket = new DatagramSocket();

        Message.sendMessage(chunk_request, hostAddress, socket);

        return sendMessage(socket);
    }

    private Message sendMessage(DatagramSocket socket) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return Message.receiveMessage(socket);
                } catch (IOException e) {
                    return null;
                }
            }).get(1, TimeUnit.SECONDS);
        } catch (Exception ignored) { }

        return null;
    }
}
