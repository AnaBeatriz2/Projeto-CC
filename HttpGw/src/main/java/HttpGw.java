import java.io.*;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class HttpGw {
    private final List<HostAddress> hostAddresses;


    public HttpGw() throws UnknownHostException {
        hostAddresses = new ArrayList<>();
        hostAddresses.add(new HostAddress("localhost", 4445));
    }

    public static void main(String[] args) throws IOException {
        HttpGw httpGw = new HttpGw();
        httpGw.run();
    }

    public Message getFileSize(String filename, HostAddress hostAddress) throws IOException {
        Message msg = Message.newFileSizeRequest(filename);
        DatagramSocket socket = new DatagramSocket();

        Message.sendMessage(msg, hostAddress, socket);
        Message message = Message.receiveMessage(socket);
        socket.close();

        return message;
    }

    public OrderedChunks requestChunks(long file_size, String filename) {
        int maxChunkSize = Message.BUFFER_SIZE - filename.length() - 15;

        List<Thread> threads = new ArrayList<>();

        //1-> calcular quantas divisões têm de ser feitas em função do tamanho do buffer
        long chunks = file_size / maxChunkSize + 1;

        //2-> criação do limite inferior e superior dos chunks
        long lim_inf = 0;
        long lim_sup = maxChunkSize;

        //3-> receber os dados em chunks e colocar na estrutura de dados OrderedChunks
        OrderedChunks orderedChunks = new OrderedChunks(chunks);

        //3-> fazer ciclo com tantas iterações quantos forem os chunks
        int i;
        for (i = 0; (i < chunks) && (lim_sup < file_size); i++) {
            // 3.1 requestChunk
            Chunk chunk = new Chunk(filename, i, lim_inf, lim_sup);
            Thread thread = new Thread(new ChunkRequest(hostAddresses, chunk, orderedChunks));
            threads.add(thread);
            thread.start();

            // 3.2-> incrementar os limites superior e inferior
            lim_inf = lim_sup + 1;
            lim_sup = lim_sup + maxChunkSize;
        }
        //4-> verificar se saiu do ciclo por acabarem os chunks ou por haver um chunk "incompleto"
        if (lim_sup > file_size) {
            // 4.1 requestChunk
            Chunk chunk = new Chunk(filename, i, lim_inf, file_size);
            Thread thread = new Thread(new ChunkRequest(hostAddresses, chunk, orderedChunks));
            threads.add(thread);
            thread.start();
        }

        //5-> aguardar que todas as threads terminem
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Fallen brother. We have lost thy soul.");
            }
        }

        //6-> devolver os chunks para o pedido de dados
        return orderedChunks;
    }

    public void requestFile(String input) throws IOException, ClassNotFoundException {
        //1-> pedir tamanho do ficheiro
        Message file_size = getFileSize(input, hostAddresses.get(0));

        if (file_size.getQuery_type() != 'e') {
            //2-> dividir em chunks e pedir os respetivos chunks
            OrderedChunks orderedChunks = requestChunks(file_size.getSize(), input);

            if (orderedChunks.hasAllChunks()) {
                writeToFile(input, orderedChunks);
            }
        } else {
            System.out.println("Requested file does not exist.");
        }
    }

    private void writeToFile(String filename, OrderedChunks data) throws IOException {
        String[] parts = filename.split("/");
        File file = new File("/tmp/" + parts[parts.length - 1]);
        file.createNewFile();

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            for (long i = 0; i < data.getChunks(); i++) {
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
}
