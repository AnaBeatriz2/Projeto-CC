import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class HttpGw {
    private DatagramSocket socket;
    private List<HostAddress> hostAddresses;

    public HttpGw() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        hostAddresses = new ArrayList<>();
        hostAddresses.add(new HostAddress("localhost", 4445));
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

    public void requestFile(String input) {
        //t-<filename> -> pedir o tamanho do ficheiro
        //getFileSize()
        //t-<filename>-size -> devolução do tamanho do ficheiro

        //divideIntoChunks() -> fazer as contas de quantas divisões têm de ser feitas e fazer pedidos

        //fazer ciclo com tantas iterações quantos chunks
        //b-<filename>-<i>-<f> -> pedir chunks de intervalos
        //askForChunks()
        //organizá-los numa lista porque podem chegar desordenados
    }

    public void run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            String in = reader.readLine();

            sendMessage(in, hostAddresses.get(0));
            System.out.println(receiveMessage());
            //FIXME: substituir ^ por v
            //requestFile(in);

            if (in.equals("end")) {
                break;
            }
        }
    }

    public void close() {
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        HttpGw httpGw = new HttpGw();
        httpGw.run();
        httpGw.close();
    }

}
