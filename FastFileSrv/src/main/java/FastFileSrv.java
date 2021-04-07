import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FastFileSrv {
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];

    public FastFileSrv() throws SocketException {
        socket = new DatagramSocket(4445);
    }

    public void sendPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public void sendMessage(String msg, HostAddress address) throws IOException {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address.getAddress(), address.getPort());
        sendPacket(packet);
    }

    public DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public String receiveMessage() throws IOException {
        DatagramPacket packet = receivePacket();
        return new String(packet.getData(), 0, packet.getLength());
    }

    public void run() throws IOException {
        running = true;

        while (running) {
            DatagramPacket packet = receivePacket();

            HostAddress hostAddress = new HostAddress(packet.getAddress(), packet.getPort());
            String received = new String(packet.getData(), 0, packet.getLength());
            sendMessage(received, hostAddress);

            if (received.equals("end")) {
                running = false;
                continue;
            }
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        FastFileSrv fastFileSrv = new FastFileSrv();
        fastFileSrv.run();
    }
}
