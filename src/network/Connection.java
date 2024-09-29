package network;

import files.Logger;
import gui.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public abstract class Connection {
    private static Socket sck; //oggetti per la connessione con il server
    private static BufferedInputStream input_stream;
    private static BufferedOutputStream output_stream;

    private static final Map<Byte, On_arrival> conv_map = new LinkedHashMap<>(); //mappa fra tutti i conv code attivi e le azioni da eseguire una volta ricevuta la risposta
    private static final Map<Byte, Thread> waiting_map = new LinkedHashMap<>(); //mappa fra tutti i conv code per cui c'è un thread che attende la risposta
    private static final Map<Byte, byte[]> waiting_reply = new LinkedHashMap<>(); //una volta ricevuta la risposta a un conv code a cui un thread era in attesa, viene memorizzata qui la risposta
    private static final Map<String, Vector<On_arrival>> prefix_actions = new LinkedHashMap<>(); //a ogni prefisso collega un azione da eseguire

    private static Connection_encoder encoder; //specifica come cifrare/decifrare la conversazione con il server
    private static final SecureRandom random_generator = new SecureRandom(); //utilizzato solo per generare numeri delle conversazioni

    private static boolean conv_based = false; //se la connessione con il server si basa su conversazioni o se è diretta

    static { //imposta un seed diverso a ogni lancio
        random_generator.setSeed(System.currentTimeMillis());
    }

    public static void init(String ip, int port) throws IOException {
        //controlla che non sia già aperta un altra connessione
        if (sck != null && !sck.isClosed()) {
            Logger.log("tentativo di inizializzare la classe Connection con il server ad indirizzo ip: " + ip + ", mentre si è già connessi ad un server", true);
            return;
        }

        sck = new Socket(ip, port);
        input_stream = new BufferedInputStream(sck.getInputStream());
        output_stream = new BufferedOutputStream(sck.getOutputStream());
    }

    //chiude la connessione con il server
    protected static void close() {
        if (sck != null) {
            try {
                //chiude il socket
                sck.close();

                //resetta tutte le variabili
                sck = null;
                conv_map.clear();
                conv_based = false;
            } catch (IOException _) {
                Logger.log("errore nella chiusura del socket con il server", true);
            }
        }
    }

    //direct connection
    public static boolean send_directly(byte[] msg) {
        if (!conv_based) {
            try {
                output_stream.write(new byte[]{(byte) (msg.length & 0xff), (byte) ((msg.length >> 8) & 0xff)}); //invia 2 byte che indicano la dimensione del messaggio
                output_stream.write(msg);

                output_stream.flush();
                return true;
            } catch (IOException _) {
                Logger.log("impossibile inviare messaggi al server, output stream è chiuso", true);
                return false;
            }
        }
        else {
            Logger.log("impossibile utilizzare direct_write mentre la connessione con il server utilizza conversazioni", true);
            return false;
        }
    }

    public static byte[] wait_for_bytes() {
        if (!conv_based) {
            byte[] msg;
            try {
                byte[] msg_size_byte = input_stream.readNBytes(2); //legge la dimensione del messaggio che sta arrivando
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8); //trasforma i due byte appena letti in un intero

                msg = input_stream.readNBytes(msg_len); //legge il messaggio appena arrivato
            } catch (IOException _) { //connessione con il server chiusa
                Logger.log("impossibile ricevere bytes dal server, input stream è stato chiuso", true);
                return null;
            }

            return msg;
        }
        else { //se la connessione è dinamica non si possono ricevere bytes in questo modo
            Logger.log("wait_for_bytes() può essere utilizzato solamente quando la connessione non si basa su conversazioni", true);
            return null;
        }
    }

    //start using conversation for the connection
    public static void start_conv(Connection_encoder encoder) {
        if (!conv_based) { //se non è gia una connessione basata su conversazioni
            Connection.encoder = encoder;
            if (prefix_actions.isEmpty()) { //non c'è nessuna azione registrata
                init_std_rAction();
            }

            new Thread(Connection::start_reading_conv).start();
            conv_based = true;
        }
        else {
            Logger.log("tentativo di inizializzare le conversazioni per una connessione dove sono già utilizzate", true);
        }
    }

    //inizializza la mappa receiving_action con i prefissi standard
    private static void init_std_rAction() {
        On_arrival EOC_std = ((_, msg) -> {
            if (msg.length == 0) {
                Logger.log("il server ha chiuso la connessione");
                if (Server_manager.is_paired()) { //se è connesso a un client si disconnette
                    Server_manager.unpair(false);
                }

                Server_manager.close(false);
            }
        });
        register_action("EOC", EOC_std); //se riceve EOC chiude la connessione

        On_arrival EOP_std = ((_, msg) -> { //end of pair
            if (msg.length == 0 && Server_manager.is_paired()) {
                Logger.log("il client: " + Server_manager.get_paired_usr() + " si è disconnesso");
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "disconnessione dal client avvenuta con successo"
                ), null);

                Server_manager.unpair(false);
            }
            else if (msg.length == 0) { //non è appaiato a nessuno
                Logger.log("ricevuto dal server la richiesta di scollegarsi da un client mentre non si è appaiati a nessuno", true);
            }
        });
        register_action("EOP", EOP_std);

        On_arrival EOM_std = ((_, msg) -> { //end of mod (chiude la mod attiva al momento)
            if (msg.length == 0 && !ButtonTopBar_panel.active_mod.isEmpty() && Server_manager.is_paired()) {
                Logger.log("il client: " + Server_manager.get_paired_usr() + " ha chiuso la mod: " + ButtonTopBar_panel.active_mod);

                ButtonTopBar_panel.end_mod(false);
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "il client " + Server_manager.get_paired_usr() + " ha chiuso la mod"
                ), null);
            }
            else if (msg.length == 0 && Server_manager.is_paired()) {
                Logger.log("ricevuto dal client: " + Server_manager.get_paired_usr() + " la richiesta di chiudere la mod mentre nessuna mod è attiva", true);
            }
            else if (msg.length == 0) {
                Logger.log("ricevuto dal server la richiesta di chiudere la mod mentre non si è appaiati a nessun client", true);
            }
        });
        register_action("EOM", EOM_std);

        On_arrival pair_std = (conv_code, msg) -> {
            if (Server_manager.is_paired()) { //se è già appaiato a un client
                Logger.log("l'utente: " + new String(msg) + " ha tentato di collegarsi mentre si è già collegati a: " + Server_manager.get_paired_usr());
                Connection.send(conv_code, "den".getBytes()); //rifiuta il collegamento
            }
            else { //se non è appaiato a nessun altro client
                String pairing_usr = new String(msg);
                Logger.log("il client " + pairing_usr + " ha chiesto di collegarsi");

                Server_manager.pair_with(pairing_usr, conv_code);
            }
        };
        register_action("pair", pair_std);

        On_arrival connChk_std = ((conv_code, msg) -> { //il server controlla che il client sia ancora connesso
            if (msg.length == 0) {
                Connection.send(conv_code, "ack".getBytes());
            }
        });
        register_action("con_ck", connChk_std);

        On_arrival cList_std = ((_, msg) -> { //aggiornamenti alla lista di client
            String msg_str = new String(msg);

            Logger.log("ricevuta la lista aggiornata dei client connessi al server: " + msg_str);
            ClientList_panel.update_client_list(msg_str);
        });
        register_action("clList", cList_std);

        //il client a cui è connesso richiede di far partire una mod
        On_arrival SOM_std = ((conv_code, msg) -> {
            String mod_name = new String(msg);
            if (Server_manager.is_paired()) {
                Logger.log("il client appaiato: " + Server_manager.get_paired_usr() + " ha richiesto di attivare la mod: " + mod_name);

                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        true,
                        "il client " + Server_manager.get_paired_usr() + " ha richiesto di attivare la mod " + mod_name
                ), new StartMod(conv_code, mod_name));
            }
            else {
                Logger.log("ricevuto dal server una richiesta di attivare la mod: " + mod_name + " mentre non si è appaiati a nessun client", true);
            }
        });
        register_action("SOM", SOM_std);
    }

    //registra un nuova azione per un dato prefisso
    public static void register_action(String prefix, On_arrival action) {
        Vector<On_arrival> action_list = prefix_actions.get(prefix);

        if (action_list != null) { //c'è già almeno un azione registrata per questo prefisso
            action_list.add(action);
        }
        else { //non c'è ancora nessun azione registrata al prefisso
            Vector<On_arrival> actions = new Vector<>();
            actions.add(action);

            prefix_actions.put(prefix, actions);
        }
    }

    private static void start_reading_conv() {
        while (conv_based) {
            try {
                //riceve 2 byte con la lunghezza del messaggio in arrivo
                byte[] msg_size_byte = input_stream.readNBytes(2);
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8);

                //legge msg_len bytes ricevendo il messaggio dal server
                byte[] msg = input_stream.readNBytes(msg_len);
                msg = encoder.decode(msg); //decifra il messaggio

                process_msg(msg); //processa il messaggio
            }
            catch (IOException _) { //chiusa la connessione con il server
                Server_manager.close(false);
                break;
            }
        }
    }

    //processa messaggi arrivati dal server
    private static void process_msg(byte[] msg) {
        byte conv_code = msg[0]; //memorizza il codice della conversazione
        msg = Arrays.copyOfRange(msg, 1, msg.length); //elimina il conv_code dal messaggio

        Logger.log("ricevuto dal server [" + (int) conv_code + "]: " + new String(msg));

        On_arrival conv_action = conv_map.get(conv_code); //ricava l'azione da eseguire per questa conversazione
        Thread waiting_thread = waiting_map.get(conv_code); //cerca un thread che stia attendendo una risposta a questa conversazione
        if (conv_action == null && waiting_thread == null) { //se non è registrata nessuna azione processa il messaggio utilizzando i prefissi
            prefix_msg(msg, conv_code);
        }
        else if (waiting_thread == null){ //se è specificata un azione la esegue
            convCode_msg(msg, conv_code, conv_action);
        }
        else { //se c'è un thread in attesa per questa risposta
            thread_msg(msg, conv_code, waiting_thread);
        }
    }

    //processa messaggi arrivati dal server utilizzando il prefisso
    private static void prefix_msg(byte[] msg, byte conv_code) {
        String msg_str = new String(msg);

        //divide il messaggio in prefisso e payload
        String prefix;
        byte[] payload;

        int prefix_len = msg_str.indexOf(':');
        if (prefix_len == -1) { //non è specificato nessun payload, tutto il messaggio è il prefisso
            prefix = msg_str;
            payload = new byte[0];
        }
        else {
            //CONTROLLARE CHE PREFIX E PAYLOAD VENGANO RITAGLIATI IN MODO CORRETTO
            prefix = msg_str.substring(0, prefix_len - 1); //ritaglia il prefisso dal messaggio
            payload = Arrays.copyOfRange(msg, prefix_len + 1, msg.length);
        }

        //esegue tutte le azioni che sono state registrate a questo prefisso
        Vector<On_arrival> actions = prefix_actions.get(prefix);

        if (actions == null) { //non ci sono azioni registrate a questo prefisso
            Logger.log("ricevuto dal server il payload: " + new String(payload) + " per il prefisso: " + prefix + ", ma nessuna azioni è registrata a tale prefisso", true);
        }
        else {
            for (On_arrival action : actions) {
                action.on_arrival(conv_code, payload);
            }
        }
    }

    //processa messaggi arrivati dal server utilizzando il conv_code
    private static void convCode_msg(byte[] msg, byte conv_code, On_arrival action) {
        conv_map.remove(conv_code);
        action.on_arrival(conv_code, msg);
    }

    //processa messaggi arrivati dal server a cui un thread è in attesa
    private static void thread_msg(byte[] msg, byte conv_code, Thread thread) {
        waiting_reply.put(conv_code, msg);

        synchronized (thread) {
            thread.notify();
        }
    }

    //invia un messaggio al server utilizzando le conversazioni
    public static synchronized void send(byte[] msg, On_arrival action) {
        if (action == null) {
            Logger.log("impossibile registrare una conversazione con azione nulla", true);
        }
        else {
            byte conv_code = new_conv_code();
            conv_map.put(conv_code, action);

            send(conv_code, msg);
        }
    }

    //invia un messaggio al server senza specificare un azione da eseguire una volta ricevuta la risposta
    public static synchronized byte send(byte[] msg) {
        byte conv_code = new_conv_code();
        send(conv_code, msg);

        return conv_code;
    }

    //invia un messaggio combinando conv_code e msg
    public static synchronized void send(byte conv_code, byte[] msg) {
        msg = encoder.encode(msg); //cifra il messaggio per il server
        int msg_len = msg.length + 1;

        try {
            //invia la lunghezza del messaggio
            output_stream.write(new byte[]{(byte) (msg_len & 0xff), (byte) ((msg_len >> 8) & 0xff)}); //invia 2 byte che indicano la dimensione del messaggio

            //invia conv_code e poi il messaggio
            output_stream.write(conv_code);
            output_stream.write(msg);

            output_stream.flush();
        }
        catch (IOException _) {
            Logger.log("impossibile inviare messaggi al server, output stream è chiuso", true);
        }
    }

    //genera un nuovo conv_code casuale e unico
    private static byte new_conv_code() {
        byte[] conv_code = new byte[1];
        do { //si assicura sia random e non duplicato per un altra conversazione
            random_generator.nextBytes(conv_code);
        } while (conv_map.get(conv_code[0]) != null);

        return conv_code[0];
    }

    //attende una risposta dal server a un dato conv_code
    public static byte[] wait_for_reply(byte conv_code) {
        if (!waiting_map.containsKey(conv_code)) { //se non c'è già un altro thread che attende una risposta a questo conv code
            Thread c_thread = Thread.currentThread();

            //aggiunge questo thread alla lista di thread che attendono una risposta dal server
            waiting_map.put(conv_code, c_thread);

            //attende che il thread che legge i messaggi arrivati dal server lo notifichi
            synchronized (c_thread) {
                try {
                    c_thread.wait();
                } catch (InterruptedException e) {
                    Logger.log("thread interrotto attendendo una risposta alla conversazione: " + conv_code, true);
                    return null;
                }
            }

            byte[] reply = waiting_reply.get(conv_code);

            //se ha ricevuto una risposta la elimina da waiting_reply
            if (reply != null) {
                waiting_reply.remove(conv_code);
            }

            return reply;
        }
        else {
            return null;
        }
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
            Connection.send(CONV_CODE, "start".getBytes());
            ButtonTopBar_panel.start_mod(MOD_NAME);
        }

        @Override
        public void fail() {
            Connection.send(CONV_CODE, "stop".getBytes());
        }
    }
}