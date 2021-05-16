package Common.Message;

import Common.Chunk;
import Common.HostAddress;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Message extends Serializer implements Serializable {
    public final static int BUFFER_SIZE = 512;

    public static Message newHello() {
        Message message = new Message();
        message.query_type = 'h';
        return message;
    }

    public static Message newFileSizeRequest(String filename) {
        Message message = new Message();
        message.query_type = 'f';
        message.file_name = filename;
        return message;
    }

    public static Message newFileSizeResponse(String file_name, long size) {
        Message message = new Message();
        message.query_type = 's';
        message.file_name = file_name;
        message.file_size = size;
        return message;
    }

    public static Message newErrorMessage(String error_message) {
        Message message = new Message();
        message.query_type = 'e';
        message.error_message = error_message;
        return message;
    }

    public static Message newChunkRequest(Chunk chunk) {
        Message message = new Message();
        message.query_type = 'c';
        message.file_name = chunk.getFilename();
        message.chunk_number = chunk.getChunk();
        message.chunk_start = chunk.getStart();
        message.chunk_end = chunk.getEnd();
        return message;
    }

    public static Message newChunkResponse(String file_name, long chunk_number, byte[] data) {
        Message message = new Message();
        message.query_type = 'd';
        message.file_name = file_name;
        message.chunk_number = chunk_number;
        message.data = data;
        return message;
    }

    public static void sendMessage(Message msg, HostAddress address, DatagramSocket socket) throws IOException {
        byte[] buffer = msg.serialize();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address.getAddress(), address.getPort());
        sendPacket(socket, packet);
        System.out.println("Sent message: " + (char) buffer[0]);
    }

    private static void sendPacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public static Message receiveMessage(DatagramPacket packet) throws ClassNotFoundException {
        Message m = Message.deserialize(packet.getData());
        if (m != null) {
            System.out.println("Received message: " + (char) m.getQuery_type());
        }
        return m;
    }

    public static Message receiveMessage(DatagramSocket socket) throws IOException {
        DatagramPacket packet = receivePacket(socket);
        Message m = Message.deserialize(packet.getData());
        if (m != null) {
            System.out.println("Received message: " + (char) m.getQuery_type());
        }
        return m;
    }

    public static DatagramPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }


}
