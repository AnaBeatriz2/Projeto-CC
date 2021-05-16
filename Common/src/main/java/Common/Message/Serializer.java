package Common.Message;

import Common.ByteArray;
import com.google.common.primitives.Bytes;

public class Serializer extends Deserializer {
    public byte[] serialize() {
        try {
            return switch ((char) this.query_type) {
                case 'h' -> serializeHello();
                case 'f' -> serializeFileSizeRequest();
                case 's' -> serializeFileSizeResponse();
                case 'c' -> serializeChunkRequest();
                case 'd' -> serializeChunkResponse();
                case 'e' -> serializeError();
                default -> new byte[0];
            };
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    private byte[] serializeHello() {
        byte[] packet = new byte[17];
        packet[0] = 'h';
        return packet;
    }

    private byte[] serializeFileSizeRequest() {
        return initSerialized();
    }

    private byte[] serializeFileSizeResponse() {
        byte[] packet = initSerialized();
        byte[] file_size_bytes = ByteArray.toByte4(this.file_size);
        System.arraycopy(file_size_bytes, 0, packet, 11, 4);

        return packet;
    }

    private byte[] serializeChunkRequest() {
        byte[] packet = initSerialized();

        setChunkNumberBytes(packet);
        setChunkStartBytes(packet);
        setChunkEndBytes(packet);

        return packet;
    }

    private byte[] serializeChunkResponse() {
        byte[] packet = initSerialized();

        setChunkNumberBytes(packet);
        setDataLengthBytes(packet);

        return Bytes.concat(packet, this.data);
    }

    private byte[] serializeError() {
        byte[] packet = new byte[14];
        packet[0] = 'e';
        return packet;
    }


    // --- Helpers ---


    private byte[] initSerialized() {
        byte[] file_name_bytes = this.file_name.getBytes();
        byte[] file_name_size = ByteArray.toByte2(file_name_bytes.length);

        byte[] packet = new byte[17 + file_name_bytes.length];
        packet[0] = this.query_type;
        System.arraycopy(file_name_size, 0, packet, 1, 2);
        System.arraycopy(file_name_bytes, 0, packet, 17, file_name_bytes.length);

        return packet;
    }

    private void setChunkNumberBytes(byte[] packet) {
        byte[] chunk_number_bytes = ByteArray.toByte4(this.chunk_number);
        System.arraycopy(chunk_number_bytes, 0, packet, 3, 4);
    }

    private void setChunkStartBytes(byte[] packet) {
        byte[] chunk_start_bytes = ByteArray.toByte4(this.chunk_start);
        System.arraycopy(chunk_start_bytes, 0, packet, 7, 4);
    }

    private void setChunkEndBytes(byte[] packet) {
        byte[] chunk_end_bytes = ByteArray.toByte4(this.chunk_end);
        System.arraycopy(chunk_end_bytes, 0, packet, 11, 4);
    }

    private void setDataLengthBytes(byte[] packet) {
        byte[] data_length_bytes = ByteArray.toByte2(this.data.length);
        System.arraycopy(data_length_bytes, 0, packet, 15, 2);
    }
}
