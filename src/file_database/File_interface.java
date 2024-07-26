package file_database;

import gui.*;
import network.Server;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class File_interface extends Database {
    public static String jar_path;

    private static final SecureFile ServerList; //protetto
    private static final SecureFile DNS_CA_list; //protetto contiene la lista di tutti gli ip dei dns assieme alle chiavi pubbliche delle ca (base64)
    private static final SecureFile TerminalLog; //publico

    public static final int SERVER_LIST = 0;
    public static final int TERMINAL_LOG = 1;
    public static final int DNS_CA_LIST = 2;

    static { //inizializza tutti i SecureFile esterni al jar e legge il contenuto dei file interni
        try {
            jar_path = File_interface.class.getProtectionDomain().getCodeSource().getLocation().getPath(); //calcola la abs path del jar eseguito
            jar_path = jar_path.substring(0, jar_path.length() - 1); //rimuove l'ultimo /
            jar_path = jar_path.substring(0, jar_path.lastIndexOf('/')); //rimuove Godzilla.jar dalla fine della path

            ServerList = new SecureFile(jar_path + "/database/ServerList.dat", true);
            DNS_CA_list = new SecureFile(jar_path + "/database/DNS_CA_list.dat", true);
            TerminalLog = new SecureFile(jar_path + "/database/TerminalLog.dat", false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init_ca_public_key() throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {
        String key_list_txt = new String(DNS_CA_list.read());

        Pattern sep_key_list_patt = Pattern.compile("[;\n]");
        String[] sep_key_list = sep_key_list_patt.split(key_list_txt); //ottiene la lista separata

        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("inizio a leggere le informazioni di dns e ca\n", false); }
        if (sep_key_list.length != 1) { //se equivale ad 1 significa che contiene sono "" ed il file è vuoto
            for (int i = 0; i < sep_key_list.length; i += 2) {
                add_dns_ca_key(sep_key_list[i], sep_key_list[i + 1]);
                if (Database.DEBUG) {
                    CentralTerminal_panel.terminal_write("memorizzato il dns - " + sep_key_list[i] + "\n", false);
                }
            }
        }
        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("finito di leggere informazioni di dns e ca", false); }

        if (!Database.DNS_CA_KEY.isEmpty()) { //se conosce almeno un dns
            init_server_list();
        }
        else { //se non ne conosce nessuno richiede prima di aggiungerne
            req_dns_ca.success();
        }
    }

    private static void add_dns_ca_key(String ip, String key_b64) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {
        //legge la public key della CA
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey key = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key_b64))); //sep_key_list[i+1] conitiene la chiave pubblica in base64

        //genera il cipher
        Cipher decoder = Cipher.getInstance("RSA");
        decoder.init(Cipher.DECRYPT_MODE, key);

        //memorizza la connessione dns_ip -> (cipher, pub_key)
        Database.DNS_CA_KEY.put(ip, new Pair<>(decoder, key_b64));
    }

    private static TempPanel_action add_dns_ca = new TempPanel_action() {
        @Override
        public void success() {
            try {
                Pattern ip_patt = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
                Matcher match = ip_patt.matcher(input.elementAt(0));

                if (!match.find()) { //se non è un ip
                    retry();
                } else { //è stato inserito un indirizzo ip, controlla la chiave pubblica
                    if (input.elementAt(1).isEmpty()) { //controlla solo che sia stato effettivamente inserito qualcosa
                        retry();
                    } else { //input validi!
                        add_dns_ca_key(input.elementAt(0), input.elementAt(1)); //aggiunge il nuovo dns
                        init_server_list();
                    }
                }
            }
            catch (Exception e) {
                retry();
            }
        }

        private void retry() {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "dati inseriti non validi, riprovare"
            ), req_dns_ca);
        }

        @Override
        public void fail() {}
    };

    public static TempPanel_action req_dns_ca = new TempPanel_action() {
        @Override
        public void success() {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    !Database.DNS_CA_KEY.isEmpty(), //se sta aggiungendo un nuovo dns mostra annulla, altrimenti no
                    "indirizzo ip del dns:",
                    "chiave pubblica della CA:"
            ), add_dns_ca);
        }

        @Override
        public void fail() {} //non può essere premuto annulla
    };

    public static void init_server_list() throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        if (Database.DEBUG) { CentralTerminal_panel.terminal_write("leggo il file contenente la server list\n", false); }
        /*
         * il testo è formattato in questo modo:
         * <nome1>;<indirizzo1>
         * <nome2>;<indirizzo2>
         * ...
         * ...
         *
         * viene diviso ad ogni ';' o '\n' ricavando un array:
         * {<nome1>, <indirizzo1>, <nome2>, <indirizzo2>, ...}
         * che poi verrà inserito nel database
         */
        String server_list = new String(ServerList.read());

        Pattern p = Pattern.compile("[;\n]");
        String[] info = p.split(server_list);

        if (info.length != 1) { //il file è vuoto, Pattern.split("") ritorna un array con un solo elemento vuoto
            for (int i = 0; i < info.length; i += 3) {
                Database.serverList.put(info[i], new Pair<>(info[i+1], info[i+2]));
            }

            if (Database.DEBUG) { CentralTerminal_panel.terminal_write("aggiorno la gui con le informazioni dei database\n", false); }
            ServerList_panel.update_gui();
            ButtonTopBar_panel.init_buttons();
        }
    }

    public static String read_file(int file_id) throws IOException, IllegalBlockSizeException, BadPaddingException {
        switch (file_id) {
            case SERVER_LIST:
                return new String(ServerList.read());

            case TERMINAL_LOG:
                return new String(TerminalLog.read());

            case DNS_CA_LIST:
                return new String(DNS_CA_list.read());

            default:
                throw new RuntimeException("impossibile trovare il file con id: " + file_id);
        }
    }

    public static void append_to_file(int file_id, String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        switch (file_id) {
            case SERVER_LIST:
                ServerList.append(txt);
                break;

            case TERMINAL_LOG:
                TerminalLog.append(txt);
                break;

            case DNS_CA_LIST:
                DNS_CA_list.append(txt);
                break;

            default:
                throw new RuntimeException("impossibile trovare il file con id: " + file_id);
        }
    }

    public static void overwrite_file(int file_id, String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        switch (file_id) {
            case SERVER_LIST:
                ServerList.replace(txt);
                break;

            case TERMINAL_LOG:
                TerminalLog.replace(txt);
                break;

            case DNS_CA_LIST:
                DNS_CA_list.replace(txt);
                break;

            default:
                throw new RuntimeException("impossibile trovare il file con id: " + file_id);
        }
    }

    public static void close() throws IOException {
        ServerList.close();
        TerminalLog.close();
        DNS_CA_list.close();
    }

}

class SecureFile {
    private final boolean IS_PROTECTED;
    private FileOutputStream fos;

    private final File F;

    public SecureFile(String pathname, boolean is_prot) throws FileNotFoundException {
        F = new File(pathname);

        this.IS_PROTECTED = is_prot;

        fos = new FileOutputStream(F, true);
    }

    protected byte[] read() throws IOException, IllegalBlockSizeException, BadPaddingException {
        FileInputStream fis = new FileInputStream(F);
        byte[] txt = fis.readAllBytes();
        fis.close();

        if (IS_PROTECTED) {
            txt = File_cipher.decrypt(txt);
        }

        return txt;
    }

    protected void append(String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        if (IS_PROTECTED) {
            String file_txt = read() + txt;
            replace(file_txt);
        } else {
            fos.write(txt.getBytes());
        }
    }

    protected void replace(String txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        clear_file();
        byte[] txt_b = txt.getBytes();

        if (IS_PROTECTED) {
            txt_b = File_cipher.crypt(txt_b);
        }

        fos.write(txt_b);
    }

    protected void replace(byte[] txt) throws IOException, IllegalBlockSizeException, BadPaddingException {
        clear_file();

        if (IS_PROTECTED) {
            txt = File_cipher.crypt(txt);
        }

        fos.write(txt);
    }

    private void clear_file() throws IOException {
        new FileOutputStream(F, false).close();
    }

    protected void close() throws IOException {
        fos.close();
    }

}