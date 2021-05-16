import Common.Chunk;
import Common.HostAddress;
import Common.Message.Message;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ChunkRequest implements Runnable{
    private final HostAddresses hostAddresses;
    private final Chunk chunk;
    private final OrderedChunks orderedChunks;

    public ChunkRequest(HostAddresses hostAddresses, Chunk chunk, OrderedChunks orderedChunks) {
        this.hostAddresses = hostAddresses;
        this.chunk = chunk;
        this.orderedChunks = orderedChunks;
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (data != null) {
            orderedChunks.put(data.getChunk_number(), data.getData());
        }

    }

    private Message sendRequest(Message chunk_request) throws IOException {
        HostAddress hostAddress = hostAddresses.getRandomHostAddress();

        if (hostAddress == null) {
            return null;
        }

        DatagramSocket socket = new DatagramSocket();

        System.out.println("requested chunk " + chunk_request.getChunk_number());
        Message.sendMessage(chunk_request, hostAddress, socket);

        return sendMessage(socket);
    }

    private Message sendMessage(DatagramSocket socket) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Message m = Message.receiveMessage(socket);
                    System.out.println("received chunk " + m.getChunk_number());
                    return m;
                } catch (IOException e) {
                    //e.printStackTrace();
                    return null;
                }
            }).get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return null;
    }
}
