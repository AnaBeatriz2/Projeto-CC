import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;

public class FastFileSrv {
    private final static int BUFFER_SIZE = 256;

    private final RequestedFiles files;
    private final DatagramSocket socket;

    public FastFileSrv() throws SocketException {
        files = new RequestedFiles();
        socket = new DatagramSocket(4445);
    }

    public void sendPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public void sendMessage(Message msg, HostAddress address) throws IOException {
        System.out.println(msg);
        byte[] buffer = msg.serialize();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address.getAddress(), address.getPort());
        sendPacket(packet);
    }

    public DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public Message receiveMessage(DatagramPacket packet) throws ClassNotFoundException {
        return Message.deserialize(packet.getData());
    }

    public Message getFileSize(Message input) {
        files.put(input.getFile_hash(), input.getFilename());

        long size;
        try {
            size = Files.size(new File(input.getFilename()).toPath());
        } catch (IOException e) {
            return Message.newErrorMessage("file-does-not-exist");
        }
        return Message.newFileSizeResponse(input.getFile_hash(), size);
    }

    public Message getFileChunk(Message message) {
        String filename = files.getFile(message.getFile_hash());

        RandomAccessFile f;
        int range = (int) (message.getChunk_end() - message.getChunk_start() + 1);
        byte[] buffer = new byte[range];
        try {
            f = new RandomAccessFile(filename,"r");
            f.seek(message.getChunk_start());
            f.read(buffer, 0, range);
        } catch (IOException | IndexOutOfBoundsException e) {
            return Message.newErrorMessage("file-does-not-exist");
        }

        return Message.newChunkResponse(message.getFile_hash(), message.getChunk_number(), buffer);
    }

    public void parseInput(Message input, HostAddress hostAddress) throws IOException {
        Message message = switch (input.getQuery_type()) {
            case 'f' -> getFileSize(input);
            case 'c' -> getFileChunk(input);
            default -> Message.newErrorMessage("unexpected-request");
        };

        sendMessage(message, hostAddress);
    }

    public void run() throws IOException {
        boolean running = true;

        while (running) {
            DatagramPacket packet = receivePacket();

            HostAddress hostAddress = new HostAddress(packet.getAddress(), packet.getPort());

            Message in;
            try {
                in = receiveMessage(packet);
                System.out.println(in);
                parseInput(in, hostAddress);
            } catch (ClassNotFoundException ignored) {}
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        FastFileSrv fastFileSrv = new FastFileSrv();
        fastFileSrv.run();
    }
}
