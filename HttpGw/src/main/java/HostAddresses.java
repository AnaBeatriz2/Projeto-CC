import Common.HostAddress;
import Common.Message.Message;
import Common.TimeLimitedCodeBlock;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class HostCheck extends TimerTask {
    private final HostAddresses hostAddresses;

    public HostCheck(HostAddresses hostAddresses) {
        this.hostAddresses = hostAddresses;
    }

    public void run() {
        for (HostAddress h : hostAddresses.getHostAddresses()) {
            try {
                hostCheck(h);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private void hostCheck(HostAddress h) throws Exception {
        try {
            TimeLimitedCodeBlock.runWithTimeout(() -> {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    Message msg = Message.newHello();
                    Message.sendMessage(msg, h, socket);

                    Message.receiveMessage(socket);
                    socket.close();
                } catch (IOException e) {
                    this.hostAddresses.removeActiveHost(h.getId());
                }
            }, 1, TimeUnit.SECONDS);

            this.hostAddresses.putActiveHost(h);
        }
        catch (TimeoutException e) {
            this.hostAddresses.removeActiveHost(h.getId());
        }
    }
}

public class HostAddresses {
    private final Map<Integer, HostAddress> activeHostAddresses;
    private final List<HostAddress> hostAddresses;

    public HostAddresses(String[] args) {
        this.activeHostAddresses = new HashMap<>();
        this.hostAddresses = new ArrayList<>();

        for(String arg : args) {
            String[] host = arg.split(":");
            this.add(host[0], Integer.parseInt(host[1]));
        }

        Timer timer = new Timer();
        timer.schedule(new HostCheck(this), 0, 2000);
    }

    public HostAddress getRandomHostAddress() {
        Random generator = new Random();
        Object[] values = activeHostAddresses.values().toArray();

        if (values.length == 0 ) {
            return null;
        }

        return (HostAddress) values[generator.nextInt(values.length)];
    }

    public void add(String address, int port) {
        try {
            this.hostAddresses.add(new HostAddress(address, port));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    public HostAddress get(int pos) {
        return this.hostAddresses.get(pos);
    }

    public Map<Integer, HostAddress> getActiveHostAddresses() {
        return activeHostAddresses;
    }

    public void removeActiveHost(int id) {
        HostAddress h = this.activeHostAddresses.remove(id);

        if (h != null) {
            System.out.println("Server " + h.getAddress() + ":"+ h.getPort() + " is down.");
        }
    }

    public void putActiveHost(HostAddress h) {
        if (!this.activeHostAddresses.containsKey(h.getId())) {
            this.activeHostAddresses.put(h.getId(), h);
            System.out.println("Server " + h.getAddress() + ":"+ h.getPort() + " is up.");
        }
    }

    public List<HostAddress> getHostAddresses() {
        return hostAddresses;
    }
}
