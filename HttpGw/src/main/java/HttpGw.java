import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpGw {
    public final static int BUFFER_SIZE = 256;

    private final Files files;
    private final DatagramSocket socket;
    private final List<HostAddress> hostAddresses;

    public HttpGw() throws SocketException, UnknownHostException {
        files = new Files();
        socket = new DatagramSocket();
        hostAddresses = new ArrayList<>();
        hostAddresses.add(new HostAddress("localhost", 4445));
    }

    public static void main(String[] args) throws IOException {
        HttpGw httpGw = new HttpGw();
        httpGw.run();
        httpGw.close();
    }

    public void sendPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    public void sendMessage(Message msg, HostAddress address) throws IOException {
        byte[] buffer = msg.serialize();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address.getAddress(), address.getPort());
        sendPacket(packet);
    }

    public DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public Message receiveMessage() throws IOException {
        DatagramPacket packet = receivePacket();
        return Message.deserialize(packet.getData());
    }

    public Message getFileSize(String fileHash, String filename, HostAddress hostAddress) throws IOException {
        Message msg = Message.newFileSizeRequest(fileHash, filename);
        sendMessage(msg, hostAddress);
        return receiveMessage();
    }

    public long requestChunks(long file_size, String fileHash) throws IOException {
        int maxChunkSize = BUFFER_SIZE - 15;

        //1-> calcular quantas divisões têm de ser feitas em função do tamanho do buffer
        long chunks = file_size / maxChunkSize + 1;

        //2-> criação do limite inferior e superior dos chunks
        long lim_inf = 0;
        long lim_sup = maxChunkSize;

        //3-> fazer ciclo com tantas iterações quantos forem os chunks
        int i;
        for (i = 0; (i < chunks) && (lim_sup < file_size); i++) {
            // 3.1 requestChunk
            requestChunk(fileHash, i, lim_inf, lim_sup);

            // 3.2-> incrementar os limites superior e inferior
            lim_inf = lim_sup + 1;
            lim_sup = lim_sup + maxChunkSize;
        }
        //4-> verificar se saiu do ciclo por acabarem os chunks ou por haver um chunk "incompleto"
        if (lim_sup > file_size) {
            // 4.1 requestChunk
            requestChunk(fileHash, i, lim_inf, file_size);
        }
        //5-> devolver os chunks para o pedido de dados
        return chunks;
    }

    public void requestChunk(String fileHash, long chunk, long b, long e) throws IOException {
        //1.1-> criar pedido de chunk
        Message chunk_request = Message.newChunkRequest(fileHash, chunk, b, e);

        //1.2-> fazer o pedido do ultimo chunk
        sendMessage(chunk_request, hostAddresses.get(0));
    }

    public Map<Long, byte[]> receiveChunks(long chunks, String fileHash) throws IOException {
        Map<Long, byte[]> ordered_chunks = new HashMap<>();
        //1-> fazer ciclo para receber os chunks pedidos
        for (long i = 0; i < chunks; i++) {
            //1.1-> receber chunk
            Message data = receiveMessage();

            //1.2-> colocar o chunk na position do array
            ordered_chunks.put(data.getChunk_number(), data.getData());
        }

        files.deleteFile(fileHash);
        //6-> devolver o array com os chunks compilados
        return ordered_chunks;
    }

    public void requestFile(String input) throws IOException, ClassNotFoundException {
        String fileHash = files.addFile(input);


        //1-> pedir tamanho do ficheiro
        Message file_size = getFileSize(fileHash, input, hostAddresses.get(0));

        if (file_size.getQuery_type() != 'e') {
            //2-> dividir em chunks e pedir os respetivos chunks
            long chunks = requestChunks(file_size.getSize(), fileHash);

            //3-> receber os dados em chunks e compilar numa lista
            Map<Long, byte[]> data = receiveChunks(chunks, fileHash);
            writeToFile(input, data);
        } else {
            System.out.println("Requested file does not exist.");
        }
    }

    private void writeToFile(String filename, Map<Long, byte[]> data) throws IOException {
        String[] parts = filename.split("/");
        File file = new File("/tmp/" + parts[parts.length - 1]);
        file.createNewFile();

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            for (long i = 0; i < data.size(); i++) {
                outputStream.write(data.get(i));
            }
        }
    }

    public void run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print(">> ");
            String in = reader.readLine();

            try {
                requestFile(in);
            } catch (ClassNotFoundException e) {
                System.out.println();
            }

            if (in.equals("end")) {
                break;
            }
        }
    }

    public void close() {
        socket.close();
    }

}
