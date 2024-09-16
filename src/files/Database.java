package files;

import network.Server_info;

import javax.crypto.Cipher;
import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    public static Map<String, Server_info> server_list = new LinkedHashMap<>(); //lega l'indirizzo (ip o link) di ogni server di cui si è a conoscenza all'oggetto Server_info con tutte le sue specifiche
    public static byte[] FileCypherKey_test = new byte[32]; //ultimi 32 byte dell hash SHA3-512 della chiave utilizzata per cifrare i file
    public static Map<String, Pair<Cipher, String>> dns_ca_key = new LinkedHashMap<>(); //associa ogni dns di cui è a conoscenza con la coppia Cipher e base64 della sua chiave pubblica
}
