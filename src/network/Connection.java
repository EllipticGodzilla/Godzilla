package network;

import files.Logger;
import gui.*;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public abstract class Connection {
    private static Socket sck;
    private static BufferedOutputStream output;
    private static BufferedInputStream input;

    private static String paired_usr;

    private static AESCipher cipher = null;
    private static boolean set_seed = false;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static boolean secure = false;
    private static boolean dynamic = false;

    public static boolean init(String ip, int port) throws IOException {
        if (!set_seed) { //imposta il seed di random
            RANDOM.setSeed(System.currentTimeMillis());
            set_seed = true;
        }

        if (Connection.isClosed()) {
            sck = new Socket(ip, port);

            output = new BufferedOutputStream(sck.getOutputStream());
            input = new BufferedInputStream(sck.getInputStream());

            return true;
        }
        else {
            return false;
        }
    }

    public static synchronized void write(String msg) {
        write(msg.getBytes());
    }

    public static synchronized void write(byte[] msg) { //non si aspetta nessuna risposta e non invia una risposta
        write(msg, null);
    }

    public static synchronized void write(byte[] msg, On_arrival action) { //non invia una risposta, e si aspetta una risposta dal server
        byte conv_code = (action == null)? 0x00 : register_conv(action);

        write(conv_code, msg);
    }

    public static synchronized void write(byte conv_code, byte[] msg, On_arrival action) { //invia una risposta e si aspetta una risposta
        //registra la nuova azione da eseguire una volta ricevuta la risposta
        Receiver.new_conv(conv_code, action);
        write(conv_code, msg); //invia la risposta
    }

    public static synchronized void write(byte conv_code, byte[] msg) { //invia una risposta
        try {
            if (!isClosed()) { //se è effettivamente connesso a qualcuno
                Logger.log("invio al server [" + (int)conv_code + "]: " + new String(msg));

                byte[] msg_prefix = concat_array(new byte[] {conv_code}, msg); //concatena conv_code e msg[]

                direct_write(msg_prefix); //se possibile cifra e invia il messaggio
            }
            else {
                Logger.log("non si è connessi a nessun server, impossibile inviare: " + new String(msg), true, '\n');
            }
        } catch (IllegalBlockSizeException | IOException | BadPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    //copia dei metodi write(...) per inviare messaggi dalle mod, l'unica differenza è che prima del messaggio viene aggiunto "<nome mod>:"
    public static synchronized void mod_write(String msg) {
        write(ButtonTopBar_panel.active_mod + ":" + msg);
    }

    public static synchronized void mod_write(byte[] msg) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(prefix);
    }

    public static synchronized void mod_write(byte[] msg, On_arrival action) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(prefix, action);
    }

    public static synchronized void mod_write(byte conv_code, byte[] msg) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(conv_code, prefix);
    }

    public static synchronized void mod_write(byte conv_code, byte[] msg, On_arrival action) {
        byte[] prefix = (ButtonTopBar_panel.active_mod + ":").getBytes();
        prefix = concat_array(prefix, msg); //concatena i due array prefix[] e msg[]

        write(conv_code, prefix, action);
    }
    //fine dei metodi per inviare messaggi dalle mod

    public static void direct_write(String msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        direct_write(msg.getBytes());
    }

    public static void direct_write(byte[] msg) throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (secure) { //è una connessione sicura cifra il messaggio
            msg = cipher.encode(msg);
        }

        output.write(new byte[]{(byte) (msg.length & 0xff), (byte) ((msg.length >> 8) & 0xff)}); //invia 2 byte che indicano la dimensione del messaggio
        output.write(msg); //invia il messaggio

        output.flush();
    }

    public static String wait_for_string() { //se non è una connessione dinamica, attende che venga inviata una stringa
        if (!dynamic) {
            return new String(wait_for_bytes());
        }
        else {
            Logger.log("wait_for_string() può essere utilizzato solamente quando la connessione non è dinamica!", true, '\n');
            throw new RuntimeException("wait_for_string() is a only non-dynamic connection function!");
        }
    }

    public static byte[] wait_for_bytes() { //se non è una connessione dinamica, attende che vengano inviati dei bytes
        if (!dynamic) {
            try {
                Logger.log("attendo per dei bytes dal server...");

                byte[] msg_size_byte = input.readNBytes(2); //legge la dimensione del messaggio che sta arrivando
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8); //trasforma i due byte appena letti in un intero

                byte[] msg = input.readNBytes(msg_len); //legge il messaggio appena arrivato

                if (secure) { //se può decifra il messaggio
                    msg = cipher.decode(msg);
                }

                Logger.log("ricevuto dal server i bytes: " + new String(msg));

                return msg; //se la connessione non è dinamica non viene aggiunto il byte conv_code quindi ritorna il messaggio così come è arrivato
            } catch (IOException | IllegalBlockSizeException | BadPaddingException e) { //connessione con il server chiusa
                Logger.log("persa la connessione con il server", true, '\n');
                return null;
            }
        }
        else { //se la connessione è dinamica non si possono ricevere bytes in questo modo
            Logger.log("wait_for_bytes() può essere utilizzato solamente quando la connessione non è dinamica!", true, '\n');
            throw new RuntimeException("wait_for_bytes() is a only non-dynamic connection function!");
        }
    }

    public static void set_cipher(AESCipher cipher) {
        if (!secure) {
            Connection.cipher = cipher;
            Receiver.set_cipher(cipher);

            secure = true;
        }
    }

    public static void start_dinamic() {
        Receiver.init(input);
        Receiver.start();
        dynamic = true;
    }

    public static void close() throws IOException {
        //chiude la connessione con il server
        sck.close();
        sck = null;

        input.close();
        output.close();
        Receiver.stop();

        //resetta tutte le variabili
        secure = false;
        dynamic = false;
        paired_usr = null;
    }

    public static void pair(String usr) {
        Connection.paired_usr = usr; //imposta il nome dell'utente con cui si è connessi

        //attiva il pannello con i bottoni per attivare le mod
        Godzilla_frame.enable_panel(Godzilla_frame.BUTTON_TOPBAR);
        ButtonTopBar_panel.setEnabled(true);

        //aggiorna i pulsanti in Client_panel
        ClientList_panel.update_buttons();

        Logger.log("connesso con il client: " + Connection.paired_usr);
    }

    public static void unpair(boolean notify_server) {
        if (is_paired()) { //se è connesso ad un client
            if (notify_server) {
                Connection.write("EOP"); //invia una notifica al server per lo scollegamento
            }
            String usr = Connection.paired_usr; //ricorda il nome dell'utente a cui era collegato
            Connection.paired_usr = null;

            ButtonTopBar_panel.end_mod(notify_server); //chiude qualsiasi mod, se ce ne è una attiva

            //disattiva i pulsanti per le mod e resetta il terminal
            ButtonTopBar_panel.end_mod(false);
            Godzilla_frame.disable_panel(Godzilla_frame.BUTTON_TOPBAR);
            ButtonTopBar_panel.setEnabled(false);

            //aggiorna i pulsanti in Client_panel
            ClientList_panel.update_buttons();

            Logger.log("disconnesso dal client: " + usr);
        }
        else {
            Logger.log("impossibile scollegarsi da un client, non c'è nessun client appaiato", true, '\n');
        }
    }

    public static void register_to_bus(String bus_name, On_arrival action) {
        Receiver.register_action(bus_name, action);
    }

    public static String get_paired_usr() {
        return (paired_usr == null)? "" : paired_usr;
    }

    public static boolean is_paired() {
        return paired_usr != null;
    }

    public static boolean isClosed() { //ritorna true se non si è mai connesso (sck == null) o se si è connesso ma è stato disconnesso (sck.isClosed())
        return sck == null || sck.isClosed();
    }

    private static byte register_conv(On_arrival action) { //registra una nuova conversazione e ritorna il conv_code associato
        byte[] conv_code = new byte[1];
        do { //per evitare di avere conv_code duplicati o 0x00
            RANDOM.nextBytes(conv_code); //genera un byte casuale che utilizzerà come conv_code
        } while (conv_code[0] == 0x00 || !Receiver.new_conv(conv_code[0], action));

        return conv_code[0];
    }

    private static byte[] concat_array(byte[] arr1, byte[] arr2) {
        int arr1_len = arr1.length;

        arr1 = Arrays.copyOf(arr1, arr1.length + arr2.length);
        System.arraycopy(arr2, 0, arr1, arr1_len, arr2.length);

        return arr1;
    }
}

class Receiver {
    private static BufferedInputStream input;

    private static Map<Byte, On_arrival> conv_map = new LinkedHashMap<>(); //memorizza tutte le conversazioni che ha aperto con il server
    private static final Map<String, Vector<On_arrival>> RECEIVING_ACTIONS = new LinkedHashMap<>(); //mappa con tutti i bus registrati e le varie azioni che devono avviare una volta ricevuto un messaggio per quel dato bus

    private static String pairing_usr = "";
    private static boolean secure = false;
    private static AESCipher cipher;

    public static void init(BufferedInputStream input) {
        Receiver.input = input;

        if (RECEIVING_ACTIONS.isEmpty()) { // se ancora non sono stati creati i bus std
            init_std_bus();
        }
    }

    private static void init_std_bus() {
        On_arrival EOC_std = ((_, _) -> { //end o connection
            if (Connection.is_paired()) { //se è connesso ad un client si disconnette
                Connection.unpair(false);
            }

            Server.disconnect(false); //si scollega dal server
        });
        register_action("EOC", EOC_std);

        On_arrival EOP_std = ((_, _) -> { //end of pair
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "disconnessione dal client avvenuta con successo"
            ), null);

            Connection.unpair(false);
        });
        register_action("EOP", EOP_std);

        On_arrival EOM_std = ((_, _) -> { //end of mod (chiude la mod attiva al momento)
            ButtonTopBar_panel.end_mod(false);
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "il client " + Connection.get_paired_usr() + " ha chiuso la mod"), null);
        });
        register_action("EOM", EOM_std);

        On_arrival pair_std = ((conv_code, msg) -> { //richieste di pairing
            pairing_usr = new String(msg);

            if (!Connection.is_paired()) { //se non è appaiato con nessuno
                Logger.log("il client " + pairing_usr + " ha chiesto di collegarsi");

                pair(pairing_usr, conv_code);
            } else { //se è già appaiato con un client
                Logger.log("l'utente: " + pairing_usr + " ha tentato di collegarsi mentre si è già collegati a: " + Connection.get_paired_usr());
                Connection.write(conv_code, "den".getBytes()); //rifiuta
            }
        });
        register_action("pair", pair_std);

        On_arrival connChk_std = ((conv_code, _) -> { //controlli della connessione
            Connection.write(conv_code, "ack".getBytes());
        });
        register_action("con_ck", connChk_std);

        On_arrival cList_std = ((_, msg) -> { //aggiornamenti alla lista di client
            try {
                String msg_str = new String(msg);

                Logger.log("ricevuta la lista aggiornata dei client connessi al server: " + msg_str);
                ClientList_panel.update_client_list(msg_str);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        register_action("clList", cList_std);

        On_arrival SOM_std = ((conv_code, msg) -> { //start of mod
            String mod_name = new String(msg);

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    true,
                    "il client " + Connection.get_paired_usr() + " ha richiesto di attivare la mod " + mod_name
            ), new StartMod(conv_code, mod_name));
        });
        register_action("SOM", SOM_std);
    }

    public static void start() {
        if (!reading && input != null) { //se non sta leggendo da un altro server
            new Thread(READER).start(); //inizia a leggere da questo server
        } else if (input == null) {
            throw new RuntimeException("non è stato inizializzato il receiver");
        } else if (reading) {
            throw new RuntimeException("sto già ascoltando da un server");
        }
    }

    public static boolean new_conv(byte conv_code, On_arrival action) {
        return conv_map.putIfAbsent(conv_code, action) == null;
    }

    public static void set_cipher(AESCipher cipher) {
        if (!secure) {
            Receiver.cipher = cipher;
            secure = true;
        }
    }

    public static void register_action(String bus_name, On_arrival action) {
        Vector<On_arrival> bus_acions = RECEIVING_ACTIONS.get(bus_name);
        if (bus_acions == null) { //non è ancora stato registrata nessuna azione al dato bus
            bus_acions = new Vector<>();
            bus_acions.add(action);

            RECEIVING_ACTIONS.put(bus_name, bus_acions);
        } else {
            bus_acions.add(action);
        }
    }

    public static void stop() {
        input = null; //genera un errore nel thread reader e lo stoppa
        conv_map = new LinkedHashMap<>(); //resetta le conversazioni

        //resetta tutte le variabili
        secure = false;
    }

    private static boolean reading = false;
    private static final Runnable READER = () -> {
        reading = true;

        try {
            while (!Connection.isClosed()) {
                byte[] msg_size_byte = input.readNBytes(2);
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8);

                byte[] msg = input.readNBytes(msg_len);
                if (secure) {
                    msg = cipher.decode(msg);
                }

                byte conv_code = msg[0]; //memorizza il codice della conversazione
                msg = Arrays.copyOfRange(msg, 1, msg.length); //elimina il conv_code dal messaggio

                Logger.log("ricevuto dal server [" + (int) conv_code + "]: " + new String(msg));

                On_arrival conv_action = conv_map.get(conv_code); //ricava l'azione da eseguire per questa conversazione
                if (conv_action == null) { //se non è registrata nessuna azione processa il messaggio normalmente
                    String bus_name = get_bus_name(new String(msg));

                    byte[] payload;
                    if (bus_name.length() != msg.length) { //se oltre al bus_name c'è altro in msg
                        payload = Arrays.copyOfRange(msg, bus_name.length() + 1, msg.length); //ritaglia solo il contenuto del messaggio
                    }
                    else { //se msg = bus_name
                        payload = new byte[0];
                    }

                    send_bus_signal(bus_name, conv_code, payload);
                } else { //se è specificata un azione la esegue
                    conv_map.remove(conv_code);
                    conv_action.on_arrival(conv_code, msg);
                }
            }
        }
        catch (IOException _) {
            Server.disconnect(true);
        }
        catch (IllegalBlockSizeException | BadPaddingException _) {
            Logger.log("impossibile decifrare il messaggio ricevuto dal server, mi disconnetto", true, '\n');
            Server.disconnect(false);
        }
        catch (ArrayIndexOutOfBoundsException _) {}

        reading = false;
    };

    private static String get_bus_name(String msg) {
        char[] msg_charArray = msg.toCharArray();
        StringBuilder bus_name = new StringBuilder();

        for (int i = 0; i < msg.length(); i++) {
            if (msg_charArray[i] == ':') {
                break;
            } else {
                bus_name.append(msg_charArray[i]);
            }
        }

        return bus_name.toString();
    }

    private static synchronized void send_bus_signal(String bus_name, byte conv_code, byte[] payload) {
        Vector<On_arrival> actions = RECEIVING_ACTIONS.get(bus_name);

        if (actions == null) {
            Logger.log("è stato ricevuto dal server un messaggio per il bus: " + bus_name + " ma non c'è nessuno registrato", true, '\n');
        } else {
            for (On_arrival action : RECEIVING_ACTIONS.get(bus_name)) {
                action.on_arrival(conv_code, payload);
            }
        }
    }

    public static void pair(String pairing_usr, byte conv_code) {
        StartMod.Pair_request req = new StartMod.Pair_request(conv_code);
        TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, true, "l'utente " + pairing_usr + " ha chiesto di collegarsi"), req);
    }

    private static class StartMod implements TempPanel_action {
        public final byte CONV_CODE;
        private final String MOD_NAME;

        public StartMod(byte conv_code, String mod_name) {
            this.CONV_CODE = conv_code;
            this.MOD_NAME = mod_name;
        }

        @Override
        public void success() {
            Connection.write(CONV_CODE, "start".getBytes());
            ButtonTopBar_panel.start_mod(MOD_NAME);
        }

        @Override
        public void fail() {
            Connection.write(CONV_CODE, "stop".getBytes());
        }

        private static class Pair_request implements TempPanel_action {
            public byte conv_code;

            public Pair_request(byte conv_code) {
                this.conv_code = conv_code;
            }

            @Override
            public synchronized void success() { //connessione accettata
                if (!Connection.is_paired()) { // se non è appaiato con nessun client
                    Logger.log("accettata la connessione con il client, attendo conferma dal server");
                    Connection.write(conv_code, "acc".getBytes(), PAIRING_CHECK);
                } else {
                    Logger.log("tentativo di accettare la connessione con il client fallito, si è già connessi al client: " + Connection.get_paired_usr(), true, '\n');
                    TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "impossibile accettare più di una connessione"), null);
                    Connection.write(conv_code, "den".getBytes());
                }
            }

            @Override
            public synchronized void fail() { //connessione rifiutata
                Logger.log("rifiutata la connessione con il client");
                Connection.write(this.conv_code, "den".getBytes());
            }

            //attende una conferma dell'appaiamento dal server
            private final On_arrival PAIRING_CHECK = (_, msg) -> {
                if (new String(msg).equals("acc")) {
                    Connection.pair(pairing_usr);

                    Logger.log("connessione con il client: " + pairing_usr + " instaurata con successo");
                    TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "connessione con il client " + pairing_usr + " avvenuta con successo"), null);
                } else {
                    Logger.log("tentativo di connessione con il client: " + pairing_usr + " fallito");
                    TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "connessione con il client " + pairing_usr + " fallito"), null);
                }
            };
        }
    }
}