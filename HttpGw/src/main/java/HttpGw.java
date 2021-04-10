import javafx.util.Pair;
import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpGw {
    private final DatagramSocket socket;
    private final List<HostAddress> hostAddresses;

    public HttpGw() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        hostAddresses = new ArrayList<>();
        hostAddresses.add(new HostAddress("localhost", 4445));
    }

    public void sendPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public void sendMessage(Message msg, HostAddress address) throws IOException {
        byte[] buffer = SerializationUtils.serialize(msg);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address.getAddress(), address.getPort());
        sendPacket(packet);
    }

    public DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public Message receiveMessage() throws IOException {
        DatagramPacket packet = receivePacket();
        return SerializationUtils.deserialize(packet.getData());
    }

    public Message getFileSize(String filename, HostAddress hostAddress) throws IOException, ClassNotFoundException {
        Message msg = Message.newFileSizeRequest(filename);
        sendMessage(msg, hostAddress);
        return receiveMessage();
    }

    public long divideIntoChunks(long file_size, String filename) throws IOException {
        //1-> calcular quantas divisões têm de ser feitas em função do tamanho do buffer (256)
        long chunks = (file_size + 255)/256;

        //2-> criação do limite inferior e supoerior dos chunks
        long lim_inf = 0;
        long lim_sup = 255;

        //3-> fazer ciclo com tantas iterações quantos forem os chunks
        for (int i = 0; (i < chunks) && (lim_sup < file_size); i++) {
            //3.1-> criar pedido de chunk
            Message chunk_request = Message.newChunkRequest(filename, lim_inf, lim_sup);

            //3.2-> fazer o pedido de chunk
            sendMessage(chunk_request, hostAddresses.get(0));

            //3.3-> incrementar os limites superior e inferior
            lim_inf = lim_sup + 1;
            lim_sup = lim_sup + 256;
        }
        //4-> verificar se saiu do ciclo por acabarem os chunks ou por haver um chunk "incompleto"
        if (lim_sup > file_size) {
            //4.1-> criar pedido de chunk
            Message chunk_request = Message.newChunkRequest(filename, lim_inf, file_size);

            //4.2-> fazer o pedido do ultimo chunk
            sendMessage(chunk_request, hostAddresses.get(0));
        }
        //5-> devolver os chunks para o pedido de dados
        return chunks;
    }

    public Map<Long, byte[]> askForChunks(long chunks, long file_size) throws IOException, ClassNotFoundException {
        Map<Long, byte[]> ordered_chunks = new HashMap<>();
        //1-> fazer ciclo para receber os chunks pedidos
        for (long i = 0; i < chunks; i++) {
            //1.1-> receber chunk
            Message data = receiveMessage();

            //1.2-> retirar da Message data os limites inferior e superior
            Pair<Long, Long>  interval = new Pair<>(data.getChunk_start(), data.getChunk_end());

            //1.3-> calcular em que posição devem ficar para estarem ordenados
            long position = interval.getKey() * chunks / file_size;

            //1.4-> retirar da Message data o chunk
            byte[] data_chunk = data.getData();

            //1.5-> colocar o chunk na position do array
            ordered_chunks.put(position, data_chunk);
        }
        //6-> devolver o array com os chunks compilados
        return ordered_chunks;
    }

    public void requestFile(String input) throws IOException, ClassNotFoundException {
        //1-> pedir tamanho do ficheiro
        Message file_size = getFileSize(input, hostAddresses.get(0));
        System.out.println(file_size);

        //2-> dividir em chunks e pedir os respetivos chunks
        long chunks = divideIntoChunks(file_size.getSize(), input);

        //3-> receber os dados em chunks e compilar numa lista
        Map<Long, byte[]> data = askForChunks(chunks, file_size.getSize());

        for (long i = 0; i < chunks; i++) {
            System.out.print(new String(data.get(i), StandardCharsets.UTF_8));
        }
    }

    public void run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print(">>");
            String in = reader.readLine();

            try {
                requestFile(in);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

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
