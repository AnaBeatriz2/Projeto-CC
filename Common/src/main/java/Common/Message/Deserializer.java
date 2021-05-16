package Common.Message;

import Common.ByteArray;

public class Deserializer extends MessageContent {
    public static Message deserialize(byte[] data) {
        try {
            return switch ((char) data[0]) {
                case 'h' -> deserializeHello(data);
                case 'f' -> deserializeFileSizeRequest(data);
                case 's' -> deserializeFileSizeResponse(data);
                case 'c' -> deserializeChunkRequest(data);
                case 'd' -> deserializeChunkResponse(data);
                case 'e' -> deserializeError(data);
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Message deserializeHello(byte[] packet) {
        Message m = new Message();
        m.query_type = 'h';
        return m;
    }

    private static Message deserializeFileSizeRequest(byte[] packet) {
        Message m = new Message();
        m.query_type = 'f';
        m.file_name = new String(getFileNameBytes(packet));
        return m;
    }

    private static Message deserializeFileSizeResponse(byte[] packet) {
        Message m = new Message();
        m.query_type = 's';
        m.file_name = new String(getFileNameBytes(packet));

        byte[] file_size_bytes = new byte[4];
        System.arraycopy(packet, 11, file_size_bytes, 0, 4);
        m.file_size = ByteArray.fromByte4(file_size_bytes);

        return m;
    }

    private static Message deserializeChunkRequest(byte[] packet) {
        Message m = new Message();
        m.query_type = 'c';
        m.file_name = new String(getFileNameBytes(packet));
        m.chunk_number = getChunkNumberBytes(packet);
        m.chunk_start = getChunkStartBytes(packet);
        m.chunk_end = getChunkEndBytes(packet);
        return m;
    }

    private static Message deserializeChunkResponse(byte[] packet) {
        Message m = new Message();
        m.query_type = 'd';

        byte[] file_name_bytes = getFileNameBytes(packet);
        m.file_name = new String(file_name_bytes);

        m.chunk_number = getChunkNumberBytes(packet);

        int dataLength = getDataLengthBytes(packet);
        m.data = new byte[dataLength];
        System.arraycopy(
                packet, 17 + file_name_bytes.length,
                m.data, 0,
                dataLength
        );

        return m;
    }

    private static Message deserializeError(byte[] data) {
        Message m = new Message();
        m.query_type = 'e';
        return m;
    }


    // --- Helpers ---


    private static byte[] getFileNameBytes(byte[] packet) {
        byte[] file_name_size_bytes = new byte[2];
        System.arraycopy(packet, 1, file_name_size_bytes, 0, 2);
        int file_name_size = ByteArray.fromByte2(file_name_size_bytes);

        byte[] file_name_bytes = new byte[file_name_size];
        System.arraycopy(packet, 17, file_name_bytes, 0, file_name_size);

        return file_name_bytes;
    }

    private static long getChunkNumberBytes(byte[] packet) {
        byte[] chunk_number_bytes = new byte[4];
        System.arraycopy(packet, 3, chunk_number_bytes, 0, 4);
        return ByteArray.fromByte4(chunk_number_bytes);
    }

    private static long getChunkStartBytes(byte[] packet) {
        byte[] chunk_start_bytes = new byte[4];
        System.arraycopy(packet, 7, chunk_start_bytes, 0, 4);
        return ByteArray.fromByte4(chunk_start_bytes);
    }

    private static long getChunkEndBytes(byte[] packet) {
        byte[] chunk_end_bytes = new byte[4];
        System.arraycopy(packet, 11, chunk_end_bytes, 0, 4);
        return ByteArray.fromByte4(chunk_end_bytes);
    }

    private static int getDataLengthBytes(byte[] packet) {
        byte[] data_length_bytes = new byte[2];
        System.arraycopy(packet, 15, data_length_bytes, 0, 2);
        return ByteArray.fromByte2(data_length_bytes);
    }
}
