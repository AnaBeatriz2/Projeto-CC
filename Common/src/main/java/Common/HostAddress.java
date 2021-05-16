package Common;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostAddress {
    private static int nextId = 0;

    private int id;
    private InetAddress address;
    private int port;

    public HostAddress(String address, int port) throws UnknownHostException {
        this.id = nextId++;

        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    public HostAddress(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }
}
