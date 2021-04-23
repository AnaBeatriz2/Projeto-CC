
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FastFileServer {
    private final DatagramSocket socket;

    public FastFileServer() throws SocketException {
        socket = new DatagramSocket(4445);
    }

    public void run() throws IOException {
        boolean running = true;

        while (running) {
            DatagramPacket packet = Message.receivePacket(socket);

            new Thread(new RequestHandler(packet, socket)).start();
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        FastFileServer fastFileServer = new FastFileServer();
        fastFileServer.run();
    }
}
