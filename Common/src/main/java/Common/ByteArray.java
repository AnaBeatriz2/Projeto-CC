package Common;

public class ByteArray {
    // values: 0 - 255
     public static byte toByte(int value) {
        return (byte) value;
     }
     public static int fromByte(byte value) {
         return value & 0xFF;
     }

     // values: 0 - 65535
     public static byte[] toByte2(int value) {
         assert value >= 0 && value <= 65535;
         return new byte[] {
                 (byte) (value >>> 8),
                 (byte) value
         };
     }
    public static int fromByte2(byte[] value) {
         assert value.length == 2;
         return ((value[0] & 0xFF) << 8 ) | ((value[1] & 0xFF));
    }

    // values: 0 - 4294967295
    public static byte[] toByte4(long value) {
         assert value >= 0 && value <= 4294967295L;
         return new byte[] {
                 (byte) (value >>> 24),
                 (byte) (value >>> 16),
                 (byte) (value >>> 8),
                 (byte) value
         };
    }
    public static long fromByte4(byte[] value) {
         assert value.length == 4;
         return (
                 ((long) (value[0] & 0xFF) << 24) |
                 ((long) (value[1] & 0xFF) << 16) |
                 ((long) (value[2] & 0xFF) << 8 ) |
                 ((long) (value[3] & 0xFF))
         );
    }
}
