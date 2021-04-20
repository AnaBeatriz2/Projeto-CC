import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class Message implements Serializable {
    public final static int BUFFER_SIZE = 256;

    public static Message newFileSizeRequest(String filename) {
        Message message = new Message();
        message.query_type = 'f';
        message.filename = filename;
        return message;
    }

    public static Message newFileSizeResponse(String filename, long size) { // check usages
        Message message = new Message();
        message.query_type = 's';
        message.filename = filename;
        message.size = size;
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
        message.filename = chunk.getFilename();
        message.chunk_number = chunk.getChunk();
        message.chunk_start = chunk.getStart();
        message.chunk_end = chunk.getEnd();
        return message;
    }

    public static Message newChunkResponse(String filename, long chunk_number, byte[] data) {
        Message message = new Message();
        message.query_type = 'd';
        message.filename = filename;
        message.chunk_number = chunk_number;
        message.data = data;
        return message;
    }

    private byte query_type; // size: 1 + 1

    private String filename; // size: ? max -> HttpGw.BUFFER_SIZE - 5 - 1
    private long size; // size: ? but not a problem for 256 plus buffer

    private long chunk_number; // size: ? but not a problem for 256 plus buffer
    private long chunk_start; // size: ? but not a problem for 256 plus buffer
    private long chunk_end; // size: ? but not a problem for 256 plus buffer

    private byte[] data; // size: ? max -> HttpGw.BUFFER_SIZE - 15

    private String error_message; // size: ? max HttpGw.BUFFER_SIZE - 1 - 1

    public static void sendPacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public static void sendMessage(Message msg, HostAddress address, DatagramSocket socket) throws IOException {
        byte[] buffer = msg.serialize();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address.getAddress(), address.getPort());
        sendPacket(socket, packet);
    }

    public static DatagramPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public static Message receiveMessage(DatagramSocket socket) throws IOException {
        DatagramPacket packet = receivePacket(socket);
        return Message.deserialize(packet.getData());
    }

    public byte getQuery_type() {
        return query_type;
    }

    public String getFilename() {
        return filename;
    }

    public long getChunk_start() {
        return chunk_start;
    }

    public long getChunk_end() {
        return chunk_end;
    }

    public long getChunk_number() {
        return chunk_number;
    }

    public byte[] getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    public String getError_message() {
        return error_message;
    }

    public byte[] serialize() {
        return switch (query_type) {
            case 'f' -> serializeFileSizeRequest();
            case 's' -> serializeFileSizeResponse();
            case 'c' -> serializeChunkRequest();
            case 'd' -> serializeChunkResponse();
            case 'e' -> serializeError();
            default -> new byte[0];
        };
    }

    private byte[] serializeFileSizeRequest() {
        String t = ";" + filename + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    private byte[] serializeFileSizeResponse() {
        String t = ";" + filename + ";" + size + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    private byte[] serializeChunkRequest() {
        String t =";" + filename + ";" + chunk_number + ";" + chunk_start + ";" + chunk_end + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    private byte[] serializeChunkResponse() {
        String t = ";" + filename.length() + ";" + filename + ";" + Long.toString(chunk_number).length() + ";" + chunk_number + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes(), data);
    }

    private byte[] serializeError() {
        String t = ";" + error_message + ";";
        return Bytes.concat(new byte[]{query_type}, t.getBytes());
    }

    public static Message deserialize(byte[] data) {
        return switch (data[0]) {
            case 'f' -> deserializeFileSizeRequest(data);
            case 's' -> deserializeFileSizeResponse(data);
            case 'c' -> deserializeChunkRequest(data);
            case 'd' -> deserializeChunkResponse(data);
            case 'e' -> deserializeError(data);
            default -> null;
        };
    }

    private static Message deserializeFileSizeRequest(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        String[] ts = t.split(";");
        Message m = new Message();
        m.query_type = 'f';
        m.filename = ts[0];
        return m;
    }

    private static Message deserializeFileSizeResponse(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        String[] ts = t.split(";");
        Message m = new Message();
        m.query_type = 's';
        m.filename = ts[0];
        m.size = Long.parseLong(ts[1]);
        return m;
    }

    private static Message deserializeChunkRequest(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        String[] ts = t.split(";");
        Message m = new Message();
        m.query_type = 'c';
        m.filename = ts[0];
        m.chunk_number = Long.parseLong(ts[1]);
        m.chunk_start = Long.parseLong(ts[2]);
        m.chunk_end = Long.parseLong(ts[3]);
        return m;
    }

    private static Message deserializeChunkResponse(byte[] data) {
        int usefulSize = 0;

        for (; usefulSize < data.length; usefulSize++) {
            if (data[usefulSize] == '\0') {
                break;
            }
        }

        int from = 2, to = 4;
        int filenameSize = Integer.parseInt(new String(Arrays.copyOfRange(data, from, to)));

        from = 5;
        to = 5 + filenameSize;
        String filename = new String(Arrays.copyOfRange(data, from, to));

        from = to + 1;
        to = from + 1;
        int chunkNumberSize = Integer.parseInt(new String(Arrays.copyOfRange(data, from, to)));

        from = to + 1;
        to = from + chunkNumberSize;
        long chunkNumber = Long.parseLong(new String(Arrays.copyOfRange(data, from, to)));

        from = to + 1;
        to = usefulSize;
        byte[] content = Arrays.copyOfRange(data, from, to);

        Message m = new Message();
        m.query_type = 'd';
        m.filename = filename;
        m.chunk_number = chunkNumber;
        m.data = content;
        return m;
    }

    private static Message deserializeError(byte[] data) {
        String t = new String(Arrays.copyOfRange(data, 2, data.length));
        Message m = new Message();
        m.query_type = 'e';
        m.error_message = t;
        return m;
    }


    @Override
    public String toString() {
        return "Message{" +
                "query_type=" + query_type +
                ", filename='" + filename + '\'' +
                ", chunk_start=" + chunk_start +
                ", chunk_end=" + chunk_end +
                ", chunk_number=" + chunk_number +
                ", data=" + Arrays.toString(data) +
                ", size=" + size +
                ", error_message='" + error_message + '\'' +
                '}';
    }
}
