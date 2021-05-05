import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;

public class HttpGw {
    public static void main(String[] args) throws IOException {
        HttpGw httpGw = new HttpGw();
        httpGw.start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);

        server.createContext("/", new ClientHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port 8080");

        while(true) {}
    }
}
