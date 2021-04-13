import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostAddress {
    private final InetAddress address;
    private final int port;

    public HostAddress(String address, int port) throws UnknownHostException {
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
