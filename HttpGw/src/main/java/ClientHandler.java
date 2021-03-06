import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements HttpHandler {
    private final HostAddresses hostAddresses;

    public ClientHandler(HostAddresses hostAddresses) {
        this.hostAddresses = hostAddresses;
    }

    public void handle(HttpExchange httpExchange) throws IOException {
        String filename = null;

        if ("GET".equals(httpExchange.getRequestMethod())) {
            filename = handleGetRequest(httpExchange);
            System.out.println("Client wants: " + filename);
            handleResponse(httpExchange, filename);
        } else {
            httpExchange.sendResponseHeaders(404, 0);
            httpExchange.getRequestBody().close();
        }
    }

    private String handleGetRequest(HttpExchange httpExchange) {
        return httpExchange
            .getRequestURI()
            .toString();
    }

    private void handleResponse(HttpExchange httpExchange, String filename) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();

        try {
            byte[] response = request(filename);
            httpExchange.getResponseHeaders().add("Content-Type", "application/pdf");
            httpExchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + filename);
            httpExchange.sendResponseHeaders(200, response.length);
            outputStream.write(response);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            httpExchange.sendResponseHeaders(404, 0);
        }

        outputStream.flush();
        outputStream.close();
        httpExchange.getRequestBody().close();

        outputStream.flush();
        outputStream.close();
    }

    private byte[] request(String filename) throws IOException, ClassNotFoundException {
        FileRequest fileRequest = new FileRequest(hostAddresses, filename);
        OrderedChunks orderedChunks = fileRequest.requestFile();

        List<Byte> byteList = new ArrayList<>();


        for (int i = 0; i < orderedChunks.getChunks(); i++) {
            for (byte b : orderedChunks.get(i)) {
                byteList.add(b);
            }
        }

        byte[] bytes = new byte[byteList.size()];

        for (int i = 0; i < byteList.size(); i++) {
            bytes[i] = byteList.get(i);
        }

        return bytes;
    }
}
