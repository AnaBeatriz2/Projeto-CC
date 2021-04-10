import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Files;

public class FastFileSrv {
    private final DatagramSocket socket;

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket(4445);
    }

    public void sendPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public void sendMessage(Message msg, HostAddress address) throws IOException {
        System.out.println(msg);
        byte[] buffer = SerializationUtils.serialize(msg);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address.getAddress(), address.getPort());
        sendPacket(packet);
    }

    public DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public Message receiveMessage(DatagramPacket packet) throws ClassNotFoundException {
        return SerializationUtils.deserialize(packet.getData());
    }

    public Message getFileSize(String filename) {
        long size;
        try {
            size = Files.size(new File(filename).toPath());
        } catch (IOException e) {
            return Message.newErrorMessage("file-does-not-exist");
        }
        return Message.newFileSizeResponse(filename, size);
    }

    public Message getFileChunk(String filename, long begin, long end) {
        RandomAccessFile f;
        int range = Math.toIntExact(end - begin);
        byte[] buffer = new byte[range];
        try {
            f = new RandomAccessFile(filename,"r");
            f.seek(Math.toIntExact(begin));
            f.read(buffer, 0, range);
        } catch (IOException | IndexOutOfBoundsException e) {
            return Message.newErrorMessage("file-does-not-exist");
        }

        return Message.newChunkResponse(filename, begin, end, buffer);
    }

    public void parseInput(Message input, HostAddress hostAddress) throws IOException {
        Message message = switch (input.getQuery_type()) {
            case "t" -> getFileSize(input.getFilename());
            case "c" -> getFileChunk(input.getFilename(), input.getChunk_start(), input.getChunk_end());
            default -> Message.newErrorMessage("unexpected-request");
        };

        sendMessage(message, hostAddress);
    }

    public void run() throws IOException {
        boolean running = true;

        while (running) {
            DatagramPacket packet = receivePacket();

            HostAddress hostAddress = new HostAddress(packet.getAddress(), packet.getPort());

            Message in = null;
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
