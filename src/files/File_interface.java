package files;

import gui.*;
import network.Server_info;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.xml.crypto.Data;
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
            //genera il testo da salvare nel file da tutti i server registrati in Database.server_list
            StringBuilder server_list = new StringBuilder();
            for (String key : Database.server_list.keySet()) {
                Server_info info = Database.server_list.get(key);

                server_list
                        .append( key )
                        .append( ":" )
                        .append( (info.get_link() == null)? "" : info.get_link() )
                        .append( ";" )
                        .append( (info.get_ip() == null)? "" : info.get_ip() )
                        .append( ";" )
                        .append( info.get_port() )
                        .append( ";" )
                        .append( info.get_dns_ip() )
                        .append( ";" )
                        .append( info.get_encoder_name() )
                        .append( "\n" );
            }

            //riscrive tutte le informazioni nel file con le nuove
            File_interface.overwrite_file("database/ServerList.dat", server_list.toString());

            //genera il testo da scrivere nel file da tutti i dns salvati in Database.dns_ca_key
            StringBuilder dns_list = new StringBuilder();
            for (String ip : Database.dns_ca_key.keySet()) { //per ogni dns riconosciuto
                dns_list.append( ip )
                        .append( ":" )
                        .append( Database.dns_ca_key.get(ip).el2 )
                        .append( "\n" );
            }

            //riscrive tutte le informazioni salvate nel file con le nuve
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

        if (!Database.dns_ca_key.isEmpty()) { //se conosce almeno un dns
            init_server_list();
        }
        else { //se non ne conosce nessuno richiede prima di aggiungerne
            req_dns_ca.success();
        }
    }

    public static void reload_from_disk() {
        if (initialized) {
            Database.dns_ca_key.clear(); //rimuove tutti i DNS/CA memorizzati
            Database.server_list.clear(); //rimuove tutti i server memorizzati

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
            Database.dns_ca_key.put(ip, new Pair<>(decoder, key_b64));
        }
        catch (InvalidKeySpecException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException _) {} //exception ignorate
    }

    private static final TempPanel_action ADD_DNSCA_ACTION = new TempPanel_action() {
        @Override
        public void success() {
            try {
                String dns_ip = (String) input.elementAt(0);
                String base64_pKey = (String) input.elementAt(1);

                //controlla che l'indirizzo ip sia formattato correttamente
                Pattern ip_patt = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
                Matcher match = ip_patt.matcher(dns_ip);

                if (!match.find()) { //se non è stato inserito correttamente
                    retry();
                }
                else {
                    add_dns_ca_key(dns_ip, base64_pKey);
                    init_server_list();
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
                    !Database.dns_ca_key.isEmpty(), //se sta aggiungendo un nuovo dns mostra annulla, altrimenti no
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
         * <nome1>:<indirizzo1>;<ip1>;<porta1>;<dns_ip1>;<encoder1>
         * <nome2>:<indirizzo2>;<ip2>;<porta2>;<dns_ip2>;<encoder2>
         * ...
         * ...
         */
        String file_content = read_file("database/ServerList.dat");

        if (file_content == null) {
            Logger.log("impossibile leggere il contenuto del file ServerList.dat", true);
            initialized = true;

            return;
        }

        if (!file_content.isEmpty()) { //se è contenuto qualcosa nel file
            Pattern line_pattern = Pattern.compile("([^:]+):([^;]+);([^;]+);([^;]+);([^;]+);([^;]+)");
            String[] lines = file_content.split("\n");

            for (String line : lines) {
                Matcher matcher = line_pattern.matcher(line);

                //la linea non è formattata in modo corretto
                if (!matcher.matches()) {
                    Logger.log("impossibile comprendere la linea: " + line + " nel file ServerList.dat", true);
                }
                else {
                    try {
                        String name = matcher.group(1);
                        Server_info info = new Server_info(
                                (matcher.group(2).isEmpty()) ? null : matcher.group(1),
                                (matcher.group(3).isEmpty()) ? null : matcher.group(2),
                                Integer.parseInt(matcher.group(4)),
                                matcher.group(5),
                                matcher.group(6)
                        );

                        Database.server_list.put(name, info);
                        Logger.log("aggiunto un nuovo server alla lista: " + name);
                    }
                    catch (NumberFormatException _) {
                        Logger.log("impossibile comprendere il numero della porta nella linea: " + line + " nel file ServerList.dat", true);
                    }
                }
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
            Logger.log("impossibile leggere il file: " + name + ", file non esistente", true);
            return -1;
        }
        else {
            return file.is_protected()? 1 : 0;
        }
    }

    public static String read_file(String name) {
        SecureFile file = files.get(name);

        if (file == null) {
            Logger.log("impossibile leggere il contenuto del file: " + name + ", file non esistente", true);
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
            Logger.log("impossibile aggiungere il testo: " + txt + " al file: " + name + ", file non esistente", true);
        }
        else {
            file.append(txt);
        }
    }

    public static void overwrite_file(String name, String txt) {
        SecureFile file = files.get(name);

        if (file == null) {
            Logger.log("impossibile impostare: \"" + txt + "\" come contenuto del file: " + name + ", il file non esiste", true);
        }
        else {
            file.replace(txt);
        }
    }

    public static void create_file(String name, boolean encoded) {
        if (files.containsKey(name)) //se esiste già un file con questo nome
            Logger.log("impossibile creare il file: " + name + ", esiste già un file con questo nome", true);

        files.put(name, new SecureFile(jar_path + "/" + name, encoded));
    }

    public static void delete_file(String name) {
        if (!files.containsKey(name))
            Logger.log("impossibile eliminare il file: " + name + ", il file non esistente", true);

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

