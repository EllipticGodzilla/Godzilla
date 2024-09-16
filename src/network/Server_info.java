package network;

public class Server_info {
    private final String SERVER_IP;
    private final int SERVER_PORT;
    private final String SERVER_LINK;
    private final String DNS_IP;
    private final String ENCODER_NAME;

    public Server_info(String link, String ip, int port, String dns_ip, String encoder_name) {
        this.SERVER_IP = ip;
        this.SERVER_PORT = port;
        this.SERVER_LINK = link;
        this.DNS_IP = dns_ip;
        this.ENCODER_NAME = encoder_name;
    }

    public String get_ip() {
        return SERVER_IP;
    }

    public int get_port() {
        return SERVER_PORT;
    }

    public String get_dns_ip() {
        return DNS_IP;
    }

    public String get_link() {
        return SERVER_LINK;
    }

    public String get_encoder_name() {
        return ENCODER_NAME;
    }
}