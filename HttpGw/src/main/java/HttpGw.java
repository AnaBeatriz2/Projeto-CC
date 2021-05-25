import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;

public class HttpGw {
    public static void main(String[] args) throws IOException {
        HttpGw httpGw = new HttpGw();
        httpGw.start(args);
    }

    public void start(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
        HostAddresses hostAddresses = new HostAddresses(args);

        server.createContext("/", new ClientHandler(hostAddresses));
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port 8080");

        while(true) {}
    }
}
