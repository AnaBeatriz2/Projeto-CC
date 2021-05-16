
import Common.Message.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FastFileServer {
    private final DatagramSocket socket;
    private final int port;

    public FastFileServer(String port) throws SocketException {
        this.port = Integer.parseInt(port);
        this.socket = new DatagramSocket(this.port);
    }

    public void run() throws IOException {
        boolean running = true;
        System.out.println("Running on port " + this.port + "...");

        while (running) {
            DatagramPacket packet = Message.receivePacket(socket);

            new Thread(new RequestHandler(packet, socket)).start();
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        FastFileServer fastFileServer = new FastFileServer(args[0]);
        fastFileServer.run();
    }
}
