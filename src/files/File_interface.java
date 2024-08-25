package files;

import gui.*;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class File_interface extends Database {
    public static String jar_path;
    public static boolean initialized = false;

    private static final Map<String, SecureFile> files = new LinkedHashMap<>();
    private static final Vector<Runnable> file_updater = new Vector<>(); //azioni da eseguire quando si aggiornano i dati di tutti i file

    public static void init() {
        get_jarpath();
        check_essential_files();
        init_file_updater();

        load_all_files();
    }

    private static void get_jarpath() {
        String tmp_jar_path = File_interface.class.getProtectionDomain().getCodeSource().getLocation().getPath(); //calcola l'abs path del jar
        tmp_jar_path = tmp_jar_path.substring(0, tmp_jar_path.length() - 1); //rimuove l'ultimo /
        jar_path = tmp_jar_path.substring(0, tmp_jar_path.lastIndexOf('/')); //rimuove Godzilla.jar dalla fine della path
    }

    private static void check_essential_files() {
        try {
            new File(jar_path + "/database").mkdir();
            new File(jar_path + "/database/graphics").mkdir();
            new File(jar_path + "/database/ServerList.dat").createNewFile();
            new File(jar_path + "/database/TerminalLog.dat").createNewFile();
            new File(jar_path + "/database/DNS_CA_list.dat").createNewFile();
        }
        catch (IOException _) {
            System.out.println("impossibile creare la cartella: " + jar_path + "/database, o ai file al suo interno");
            System.exit(0);
        }
    }

    private static void init_file_updater() {
        file_updater.add(() -> { //aggiorna tutti i file standard
            //salva la lista dei server memorizzati
            StringBuilder server_list = new StringBuilder();
            for (String key : Database.serverList.keySet()) {
                Pair<String, String> s_info = Database.serverList.get(key);
                server_list.append( key )
                        .append( ";" )
                        .append( s_info.el1 ) //server link/ip
                        .append( ";" )
                        .append( s_info.el2 ) //dns ip
                        .append( "\n" );
            }

            File_interface.overwrite_file("database/ServerList.dat", server_list.toString());

            //salva la lista di dns registrati
            StringBuilder dns_list = new StringBuilder();
            for (String ip : Database.DNS_CA_KEY.keySet()) { //per ogni dns riconosciuto
                dns_list.append( ip )
                        .append( ";" )
                        .append( Database.DNS_CA_KEY.get(ip).el2 )
                        .append( "\n" ); //aggiunge al file la linea ip;Base64(pub_key)
            }

            File_interface.overwrite_file("database/DNS_CA_list.dat", dns_list.toString());

            //salva i log nel terminale
            File_interface.overwrite_file("database/TerminalLog.dat", Logger.get_log());
        });
    }

    public static void update_servers_info() {
        String key_list_txt = read_file("database/DNS_CA_list.dat");

        Pattern sep_key_list_patt = Pattern.compile("[;\n]");
        String[] sep_key_list = sep_key_list_patt.split(key_list_txt); //ottiene la lista separata

        /*
        *  se sep_key_list non ha un numero pari di elementi significa che il file finisce con una linea vuota (o che è corrotto)
        *  e di conseguenza l'ultimo elemento dell'array non contiene nessun dato per un dns
         */
        if ((sep_key_list.length & 1) != 0) {
            sep_key_list = Arrays.copyOfRange(sep_key_list, 0, sep_key_list.length - 1); //rimuove l'ultimo elemento
        }

        Logger.log("apro il file database/CA_DNS_list.dat e memmorizzo i dati dei dns registrati");
        for (int i = 0; i < sep_key_list.length; i += 2) {
            add_dns_ca_key(sep_key_list[i], sep_key_list[i + 1]);
            Logger.log("memorizzato dns con indirizzo: " + sep_key_list[i]);
        }
        Logger.log("memorizzati tutti i dns registrati");

        if (!Database.DNS_CA_KEY.isEmpty()) { //se conosce almeno un dns
            init_server_list();
        }
        else { //se non ne conosce nessuno richiede prima di aggiungerne
            req_dns_ca.success();
        }
    }

    public static void reload_from_disk() {
        if (initialized) {
            Database.DNS_CA_KEY.clear(); //rimuove tutti i DNS/CA memorizzati
            Database.serverList.clear(); //rimuove tutti i server memorizzati

            close(); //chiude tutti i file e svuola la mappa files
            files.clear();

            //elimina tutti i server memorizzatti dalla list
            ServerList_panel.clear();

            update_servers_info();
        }
    }

    public static void update_files() {
        if (initialized) {
            Logger.log("inizio updating dei file sul disco");

            for (Runnable update : file_updater) {
                update.run();
            }

            Logger.log("file aggiornati");
        }
    }

    public static void add_updater(Runnable updater) {
        file_updater.add(updater);
    }

    public static String[] get_file_list() {
        return files.keySet().toArray(new String[0]);
    }

    //    LOADING FILES FROM DISK

    private static void load_all_files() {
        File[] files = new File(jar_path).listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                load_folder(file);
            }
            else if (!file.getName().equals("Godzilla.jar")) {
                load_file(file);
            }
        }
    }

    private static void load_folder(File folder) {
        if (folder.listFiles() != null) { //se questa cartella non è vuota
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    load_folder(file);
                } else {
                    load_file(file);
                }
            }
        }
    }

    private static void load_file(File file) {
        String file_name = file.getAbsolutePath().replaceAll(jar_path + "/", "");
        files.put(file_name, new SecureFile(file.getPath()));
    }

    //    MANAGE OF CA PUBLIC KEY

    private static void add_dns_ca_key(String ip, String key_b64) {
        try {
            //legge la public key della CA
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey key = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key_b64))); //sep_key_list[i+1] conitiene la chiave pubblica in base64

            //genera il cipher
            Cipher decoder = Cipher.getInstance("RSA");
            decoder.init(Cipher.DECRYPT_MODE, key);

            //memorizza la connessione dns_ip -> (cipher, pub_key)
            Database.DNS_CA_KEY.put(ip, new Pair<>(decoder, key_b64));
        }
        catch (InvalidKeySpecException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException _) {} //exception ignorate
    }

    private static final TempPanel_action ADD_DNSCA_ACTION = new TempPanel_action() {
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
            ), ADD_DNSCA_ACTION);
        }

        @Override
        public void fail() {} //non può essere premuto annulla
    };

    public static void init_server_list() {
        Logger.log("apro il file database/ServerList.dat con la lista di server memorizzati");
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
        String server_list = read_file("database/ServerList.dat");

        Pattern p = Pattern.compile("[;\n]");
        String[] info = p.split(server_list);

        if (info.length != 1) { //il file è vuoto, Pattern.split("") ritorna un array con un solo elemento vuoto
            for (int i = 0; i < info.length; i += 3) {
                Database.serverList.put(info[i], new Pair<>(info[i+1], info[i+2]));
            }

            ServerList_panel.update_gui();
            ButtonTopBar_panel.init_buttons();
        }

        Logger.log("lista dei server inizializzata");
        initialized = true;
    }

    // -1 = file non esistente, 0 = file non cifrato, 1 = file cifrato
    public static int is_encoded(String name) {
        SecureFile file = files.get(name);

        if (file == null) {
            Logger.log("impossibile leggere il file: " + name + ", file non esistente", true, '\n');
            return -1;
        }
        else {
            return file.is_protected()? 1 : 0;
        }
    }

    public static String read_file(String name) {
        SecureFile file = files.get(name);

        if (file == null) {
            Logger.log("impossibile leggere il contenuto del file: " + name + ", file non esistente", true, '\n');
            return null;
        }
        else {
            byte[] cont_byte = file.read();
            if (cont_byte == null) { //non è riuscito a decifrare il file
                return null;
            }
            else {
                return new String(cont_byte);
            }
        }
    }

    public static void append_to(String name, String txt) {
        SecureFile file = files.get(name);

        if (file == null) {
            Logger.log("impossibile aggiungere il testo: " + txt + " al file: " + name + ", file non esistente", true, '\n');
        }
        else {
            file.append(txt);
        }
    }

    public static void overwrite_file(String name, String txt) {
        SecureFile file = files.get(name);

        if (file == null) {
            Logger.log("impossibile impostare: \"" + txt + "\" come contenuto del file: " + name + ", il file non esiste", true, '\n');
        }
        else {
            file.replace(txt);
        }
    }

    public static void create_file(String name, boolean encoded) {
        if (files.containsKey(name)) //se esiste già un file con questo nome
            Logger.log("impossibile creare il file: " + name + ", esiste già un file con questo nome", true, '\n');

        files.put(name, new SecureFile(jar_path + "/" + name, encoded));
    }

    public static void delete_file(String name) {
        if (!files.containsKey(name))
            Logger.log("impossibile eliminare il file: " + name + ", il file non esistente", true, '\n');

        files.get(name).delete();
        files.remove(name);
    }

    public static void set_encoded(String file_name, boolean encode) {
        files.get(file_name).set_encoded(encode);
    }

    public static boolean exist(String name) {
        return files.containsKey(name);
    }

    public static void close() {
        for (SecureFile file : files.values()) {
            file.close();
        }
    }
}

