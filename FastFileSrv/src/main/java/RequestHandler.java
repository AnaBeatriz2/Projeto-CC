import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;

public class RequestHandler implements Runnable{
    private final DatagramPacket packet;
    private final DatagramSocket socket;

    public RequestHandler(DatagramPacket packet, DatagramSocket socket) {
        this.packet = packet;
        this.socket = socket;
    }

    public Message getFileSize(Message input) {
        long size;
        try {
            size = Files.size(new File(input.getFilename()).toPath());
        } catch (IOException e) {
            return Message.newErrorMessage("file-does-not-exist");
        }
        return Message.newFileSizeResponse(input.getFilename(), size);
    }

    public Message getFileChunk(Message message) {
        RandomAccessFile f;
        int range = (int) (message.getChunk_end() - message.getChunk_start() + 1);
        byte[] buffer = new byte[range];
        try {
            f = new RandomAccessFile(message.getFilename(),"r");
            f.seek(message.getChunk_start());
            f.read(buffer, 0, range);
        } catch (IOException | IndexOutOfBoundsException e) {
            return Message.newErrorMessage("file-does-not-exist");
        }

        return Message.newChunkResponse(message.getFilename(), message.getChunk_number(), buffer);
    }

    public void parseInput(Message input, HostAddress hostAddress) throws IOException {
        Message message = switch (input.getQuery_type()) {
            case 'f' -> getFileSize(input);
            case 'c' -> getFileChunk(input);
            default -> Message.newErrorMessage("unexpected-request");
        };

        Message.sendMessage(message, hostAddress, socket);
    }

    public void run() {
        HostAddress hostAddress = new HostAddress(packet.getAddress(), packet.getPort());

        Message in;
        try {
            in = Message.receiveMessage(packet);
            System.out.println(in);
            parseInput(in, hostAddress);
        } catch (ClassNotFoundException ignored) {} catch (IOException e) {
            e.printStackTrace();
        }
    }
}
