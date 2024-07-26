package file_database;

import javax.crypto.Cipher;
import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    public static Map<String, Pair<String, String>> serverList = new LinkedHashMap<>(); //lega il nome del server alla coppia (indirizzo, dns)(non necessariamente all'ip, anche link per il dns)
    public static byte[] FileCypherKey_test = new byte[32]; //ultimi 32 byte dell hash SHA3-512 della chiave utilizzata per cifrare i file
    public static Map<String, Pair<Cipher, String>> DNS_CA_KEY = new LinkedHashMap<>(); //associa ogni dns di cui è a conoscenza con la coppia Cipher e base64 della sua chiave pubblica
    public static boolean DEBUG = true;
}
