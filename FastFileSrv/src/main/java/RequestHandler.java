import Common.HostAddress;
import Common.Message.Message;

import java.io.*;
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
            size = Files.size(new File(input.getFile_name()).toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return Message.newErrorMessage("file-does-not-exist");
        }
        return Message.newFileSizeResponse(input.getFile_name(), size);
    }

    public Message getFileChunk(Message message) {
        FileInputStream f;
        int range = (int) (message.getChunk_end() - message.getChunk_start() + 1);
        byte[] buffer = new byte[range];

        try {
            f = new FileInputStream(message.getFile_name());
            long s = f.skip(message.getChunk_start());
            if (s != message.getChunk_start()) {
                System.out.println("Skipped " + s + " of " + message.getChunk_start());
            }
            long r = f.read(buffer, 0, range);
            if (r != range) {
                System.out.println("(" + message.getChunk_number() + ") Read " + r + " of " + range);
            }
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            return Message.newErrorMessage("file-does-not-exist");
        }

        return Message.newChunkResponse(message.getFile_name(), message.getChunk_number(), buffer);
    }

    public void parseInput(Message input, HostAddress hostAddress) throws IOException {
        Message message = switch (input.getQuery_type()) {
            case 'h' -> Message.newHello();
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
            parseInput(in, hostAddress);
        } catch (ClassNotFoundException ignored) {} catch (IOException e) {
            e.printStackTrace();
        }
    }
}
