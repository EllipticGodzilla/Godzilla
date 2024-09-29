package network;

import files.Database;
import files.Logger;
import gui.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Server_manager {
    private static final Map<String, Connection_encoder> encoders = new LinkedHashMap<>(); //mappa con tutti gli encoders per le connessioni con il server
    private static final int DNS_PORT = 9696;

    private static String server_name = ""; //nome del server a cui ci si sta collegando
    private static Cipher server_pKey_encoder, server_pKey_decoder;

    private static boolean connected = false; //se è connesso a un server
    private static String username = null; //username con cui ci si è registrati nel server, null se non ci si è ancora registrati
    private static String pairing_usr = null; //username del client con cui è in corso il processo di collegamento, null se non si sta appaiando a nessuno
    private static String paired_usr = null; //username del client a cui è appaiato, null se non è appaiato a nessuno

    public static void init_encoder()  {
        if (encoders.containsKey("standard_protocol")) { //se è già registrato lo standard
            return;
        }

        try {
            Connection_encoder std_protocol = new Connection_encoder() {
                private final Cipher encoder = Cipher.getInstance("AES/CBC/PKCS5Padding");
                private final Cipher decoder = Cipher.getInstance("AES/CBC/PKCS5Padding");

                private final Random random = new Random();
                private SecretKey session_key;

                @Override
                public String[] compatible_with() {
                    return new String[] {"standard_protocol"};
                }

                @Override
                public void init(byte[] check_code) {
                    byte[] aes_key = Arrays.copyOf(check_code, 32);
                    int random_seed = (check_code[32] << 24) | ((check_code[33] & 0xFF) << 16) | ((check_code[34] & 0xFF) << 8) | (check_code[35] & 0xFF); //combina gli ultimi 4 byte per fare il seed di random

                    random.setSeed(random_seed);

                    session_key = new SecretKeySpec(aes_key, "AES");
                    IvParameterSpec iv = next_iv();

                    try {
                        encoder.init(Cipher.ENCRYPT_MODE, session_key, iv);
                        decoder.init(Cipher.DECRYPT_MODE, session_key, iv);
                    }
                    catch (InvalidKeyException | InvalidAlgorithmParameterException _) {}
                }

                @Override
                public byte[] encode(byte[] msg) {
                    try {
                        byte[] encoded_msg = encoder.doFinal(msg); //decodifica il messaggio
                        regen_iv(); //rigenera gli iv

                        return encoded_msg;
                    }
                    catch (BadPaddingException | IllegalBlockSizeException _) {
                        Logger.log("errore nel cifrare il messaggio: " + new String(msg), true);
                        return null;
                    }
                }

                @Override
                public byte[] decode(byte[] msg) {
                    try {
                        byte[] plain_msg = decoder.doFinal(msg);
                        regen_iv();

                        return plain_msg;
                    }
                    catch (BadPaddingException | IllegalBlockSizeException _) {
                        Logger.log("errore nel decifrare un messaggio dal server", true);
                        return null;
                    }
                }

                private void regen_iv() {
                    try {
                        IvParameterSpec iv = next_iv();
                        encoder.init(Cipher.ENCRYPT_MODE, session_key, iv);
                        decoder.init(Cipher.DECRYPT_MODE, session_key, iv);
                    }
                    catch (InvalidKeyException | InvalidAlgorithmParameterException _) {} //impossibile rientrare
                }

                private IvParameterSpec next_iv() { //genera un iv casuale
                    byte[] iv_bytes = new byte[encoder.getBlockSize()];
                    random.nextBytes(iv_bytes);

                    return new IvParameterSpec(iv_bytes);
                }
            };
            encoders.put("standard_protocol", std_protocol);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException _) {
            Logger.log("errore nella definizione del protocollo standard per proteggere la comunicazione con il server", true);
        }
    }

    //inizia una nuova connessione con il server
    public static synchronized void connect_to(String server_name) {
        if (connected) { //se è già connesso a un server
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "impossibile iniziare una nuova connessione con il server: " + server_name + ", si è già connessi ad un server"
            ), null);
            Logger.log("tentativo di iniziare una connessione con il server: " + server_name + " mentre si è già connessi ad un altro server", true);

            return;
        }

        //cerca di collegarsi al nuovo server
        Server_manager.server_name = server_name;
        Server_info server_info = Database.server_list.get(server_name);

        if (server_info == null) { //la lista con tutti i server in ServerList_panel e quella in Database non sono sincronizzate
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "impossibile trovare le specifiche del server: " + server_name + ", prova a riavviare Godzilla"
            ), null);
            Logger.log("impossibile trovare le specifiche del server: " + server_name + ", la lista in ServerList_panel e quella in Database sono de sincronizzate", true);

            return;
        }
        Logger.log("trovate le specifiche del server: " + server_name);

        //prova a inizializzare la connessione con il server
        if (!connect_to_server(server_info)) {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "impossibile connettersi al server: " + server_name
            ), null);
            return;
        }
        Logger.log("connessione con il server: " + server_name + " avvenuta con successo");

        //controlla la sicurezza della connessione con il server e concorda un codice di 36 byte random
        byte[] check_code = init_secure_connection(server_info);
        if (check_code == null) {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "errore nel controllo del certificato, impossibile assicurare una connessione sicura"
            ), null);

            return;
        }
        Logger.log("concordato il check code con il server: " + server_name + " in modo sicuro");

        //riceve la lista di protocolli per cifrare le prossime conversazioni che il server supporta
        String[] server_protocols = get_supported_protocols(check_code);
        if (server_protocols == null) {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "errore nell'inizializzazione della connessione con il server"
            ), null);
            Logger.log("impossibile determinare la lista di protocolli supportati dal server: " + server_name, true);

            return;
        }

        //trova il protocollo da utilizzare, cioè preferred_encoder se supportato dal server, o standard se non lo è
        Connection_encoder preferred_encoder = encoders.get(server_info.get_encoder_name());
        String common_protocol = get_common_protocol(server_protocols, preferred_encoder);

        if (common_protocol == null) { //nessun protocollo in comune
            Logger.log("tentativo di instaurare una connessione sicura con il server fallito, nessun protocollo in comune", true);
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "impossibile comunicare con il server, nessun protocollo in comune"
            ), null);

            return;
        } else if (common_protocol.isEmpty()) { //deve utilizzare il protocollo standard
            Logger.log("il protocollo specificato nelle server_info non è supportato dal server: " + server_name + ", utilizzo il protocollo standard");
            use_protocol(encoders.get("standard_protocol"), "standard_protocol", check_code);
        } else { //utilizza preferred_encoder
            Logger.log("il protocollo specificato nelle server_info è supportato dal server: " + server_name);
            use_protocol(preferred_encoder, common_protocol, check_code);
        }

        connected = true;
        Logger.log("connessione con il server e protezione della comunicazione avvenuta con successo");

        //esegue il login o registra un nuovo account nel server
        manage_login();
    }

    //chiude la connessione con il server
    public static void close(boolean notify_server) {
        if (!connected) { //se non è connesso a nessun server
            Logger.log("tentativo di chiudere la connessione con un server mentre non si è connessi", true);
            return;
        }

        connected = false;
        username = null;

        //chiude la connessione
        if (notify_server) { //se deve notificare il server che si sta scollegando
            Connection.send("EOC".getBytes());
        }
        Connection.close();

        //disattiva il pannello client list e resetta la lista dei client
        ClientList_panel.update_client_list("");
        ClientList_panel.setEnabled(false);
        Godzilla_frame.disable_panel(Godzilla_frame.CLIENT_LIST);

        //imposta il pulsante per connettersi a un server attivo e disattiva quello per scollegarsi in ServerList_panel
        ServerList_panel.update_button_enable();

        Logger.log("la connessione con il server è stata terminata con successo");
    }

    public static boolean is_connected() {
        return connected;
    }

    //cerca l'indirizzo ip del server e tenta di connettersi a esso
    private static boolean connect_to_server(Server_info info) {
        //prova a trovare l'indirizzo ip dalle server_info o dal dns a cui il server dovrebbe essere registrato
        String server_ip = get_server_ip(info);
        if (server_ip == null) {
            Logger.log("qualcosa è andato storto nel tentativo di trovare l'indirizzo ip del server: " + server_name);
            return false;
        }
        else if (server_ip.equals("error, the ip is not registered")) {
            Logger.log("il dns: " + info.get_dns_ip() + " legato al server: " + server_name + "  non è più a conoscenza dell'indirizzo ip del server", true);
            return false;
        }
        Logger.log("ip del server: " + server_name + " trovato con successo: " + server_ip);

        //prova a connettersi al server
        try {
            Connection.init(server_ip, info.get_port());
        }
        catch (UnknownHostException _) {
            Logger.log("impossibile comprendere l'indirizzo ip: " + server_ip + " del server: " + server_name, true);
            return false;
        }
        catch (IOException _) {
            Logger.log("impossibile connettersi all'indirizzo: " + server_ip + " alla porta: " + info.get_port() + " del server: " + server_name, true);
            return false;
        }

        Logger.log("connessione con il server: " + server_name + " avvenuta con successo");
        return true; //è riuscito a inizializzare la connessione con il server correttamente
    }

    //cerca l'indirizzo ip del server, che sia scritto direttamente nelle Server_info, o se deve collegarsi al dns per richiederlo
    private static String get_server_ip(Server_info info) {
        String info_ip = info.get_ip();

        if (info_ip.isEmpty()) { //non è memorizzato l'indirizzo ip del server ma solo il link
            try {
                String server_link = info.get_link();

                //si collega al dns a cui è registrato il server per trovare il suo indirizzo ip
                String dns_ip = info.get_dns_ip();
                String dns_msg = "d" + server_link; //messaggio da inviare al dns per ricevere l'indirizzo ip dal link del server

                Connection.init(dns_ip, DNS_PORT);
                Connection.send_directly(dns_msg.getBytes());
                String server_ip = new String(Connection.wait_for_bytes());

                //chiude la connessione con il dns
                Connection.close();

                return server_ip;
            }
            catch (UnknownHostException _) {
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "impossibile connettersi al dns legato al server: " + server_name
                ), null);
                Logger.log("impossibile comprendere l'indirizzo ip: " + info.get_dns_ip() + " del dns legato al server: " + server_name, true);
                return null;
            }
            catch (IOException _) {
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "impossibile connettersi al dns legato al server: " + server_name
                ), null);
                Logger.log("impossibile connettersi all'indirizzo: " + info.get_dns_ip() + " alla porta: " + DNS_PORT + " del dns legato al server: " + server_name, true);
                return null;
            }
        }
        else { //è memorizzato direttamente l'indirizzo ip del server
            return info_ip;
        }
    }

    //ricevuto certificato e informazioni dal server controlla che siano corrette e valide e invia il check code (16 byte random)
    private static byte[] init_secure_connection(Server_info info) {
        byte[] server_cInfo_bytes = Connection.wait_for_bytes(); //riceve le informazioni che devono essere contenute nel certificato del server
        byte[] server_certificate = Connection.wait_for_bytes(); //riceve il certificato del server, cioè "sha3-512(server_cInfo)" cifrato con la chiave privata del dns

        if (server_cInfo_bytes == null || server_certificate == null) {
            Logger.log("errore nella ricezione del certificato o delle informazioni al suo interno dal server: " + server_name, true);

            return null;
        }
        String server_cInfo = new String(server_cInfo_bytes);
        Logger.log("ricevute le informazioni dal server: " + server_name + ": " + server_cInfo);

        /*
         * controlla che le informazioni ricevute dal server siano formattate in modo corretto:
         * server_cInfo = {nome};{ip};{link};{base64(publicKey)};{mail}
         */
        Pattern cInfo_pattern = Pattern.compile("([^;])+;([^;])+;([^;])+;([^;])+;([^;])");
        Matcher cInfo_matcher = cInfo_pattern.matcher(server_cInfo);

        if (!cInfo_matcher.matches()) {
            Logger.log("impossibile comprendere la formattazione delle informazioni ricevute dal server: " + server_name);

            return null;
        }
        Logger.log("format delle informazioni ricevute valido");

        //controlla la validità delle informazioni in server_cInfo
        if (!validate_info(server_cInfo, server_certificate, cInfo_matcher, info)) {
            return null;
        }

        //decodifico la chiave pubblica del server contenuta nel suo certificato
        byte[] server_pKey = Base64.getDecoder().decode(cInfo_matcher.group(4));
        server_pKey_encoder = get_server_cipher(server_pKey, Cipher.ENCRYPT_MODE);
        server_pKey_decoder = get_server_cipher(server_pKey, Cipher.DECRYPT_MODE);

        //controlla il cipher sia stato generato correttamente
        if (server_pKey_encoder == null) {
            return null;
        }

        //genera 36 byte random e li invia al server cifrandoli con la sua chiave pubblica
        byte[] check = new byte[36];
        new SecureRandom().nextBytes(check);

        try {
            byte[] encoded_check = server_pKey_encoder.doFinal(check);

            if (!Connection.send_directly(encoded_check)) { //se non riesce a inviare il messaggio al server
                Logger.log("impossibile inviare al server: " + server_name + " il check code", true);
                return null;
            }
            Logger.log("inviato al server: " + server_name + " il check code");
        }
        catch (IllegalBlockSizeException | BadPaddingException _) {} //impossibile essendo la lunghezza di check sempre equivalente a 36, RSA può sempre cifrarlo

        return check;
    }

    /*
     * controlla che il certificato ricevuto dal server sia effettivamente equivalente all hash sha3-512 delle informazioni
     * ricevute cifrate con la chiave privata del dns, e che tutte queste informazioni si riferiscano effettivamente al
     * server a cui ci si vuole collegare
     */
    private static boolean validate_info(String cInfo, byte[] certificate, Matcher cInfo_matcher, Server_info info) {
        //controlla la validità delle informazioni in server_cInfo
        try {
            MessageDigest md = MessageDigest.getInstance("SHA3-512");

            byte[] server_hash = Database.dns_ca_key.get(info.get_dns_ip()).el1.doFinal(certificate);
            byte[] calculated_hash = md.digest(cInfo.getBytes());

            //se i due hash non sono uguali o se ip/link voluto non coincidono con quelli nel certificato
            if (!Arrays.equals(server_hash, calculated_hash) || ( !cInfo_matcher.group(2).equals(info.get_ip()) && !cInfo_matcher.group(3).equals(info.get_link()) )) {
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "errore nel controllo del certificato, impossibile assicurare una connessione sicura"
                ), null);
                Logger.log("le informazioni ricevute dal server: " + server_name + " non sono le stesse contenute nel certificato o non sono relative al server voluto", true);

                return false;
            }
        }
        catch (NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException _) {
            Logger.log("impossibile decifrare il certificato del server: " + server_name + " utilizzando la chiave pubblica legata al dns: " + info.get_dns_ip(), true);
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "impossibile comprendere il certificato ricevuto dal server"
            ), null);

            return false;
        }

        Logger.log("validità delle informazioni ricevute dal server: " + server_name + " verificata");
        return true;
    }

    //inizializza un cipher con la chiave pubblica del server a cui ci si sta collegando
    private static Cipher get_server_cipher(byte[] pKey, int mode) {
        try {
            KeyFactory key_f = KeyFactory.getInstance("RSA");
            PublicKey server_key = key_f.generatePublic(new X509EncodedKeySpec(pKey));

            Cipher server_pKey_encoder = Cipher.getInstance("RSA");
            server_pKey_encoder.init(mode, server_key);

            Logger.log("definizione del cipher per codificare utilizzando la chiave pubblica del server: " + server_name + " avvenuta con successo");
            return server_pKey_encoder;
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidKeySpecException _) {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "impossibile comprendere il certificato ricevuto dal server"
            ), null);
            Logger.log("errore nella definizione del cipher per codificare utilizzando la chiave pubblica del server: " + server_name, true);

            return null;
        }
    }

    //ritorna la lista di protocolli supportati dal server, o null per errori
    private static String[] get_supported_protocols(byte[] check_code) {
        byte[] encoded_list = Connection.wait_for_bytes();
        if (encoded_list == null) {
            return null;
        }

        try {
            byte[] decoded_list = server_pKey_decoder.doFinal(encoded_list); //controlla gli index!!!!!!!!!!!!!!!!!!!!!!

            //check if the first 36 received bytes are the same as the check code
            byte[] received_cc = Arrays.copyOfRange(decoded_list, 0, 36);
            if (!Arrays.equals(check_code, received_cc)) { //if they are not the same, the server haven't understood the check coode corectly
                Logger.log("il server non ha ritornato il check code corretto, non è possibile instaurare una connessione sicura");
                return null;
            }

            //rimuove il check code dalla lista di protocolli accettati
            String protoccols_list_string = new String(Arrays.copyOfRange(encoded_list, 36, encoded_list.length));

            Logger.log("ricevuta dal server: " + server_name + " la lista di protocolli supportati: " + protoccols_list_string);

            Pattern list_sep = Pattern.compile(";");
            return list_sep.split(protoccols_list_string);
        }
        catch (IllegalBlockSizeException | BadPaddingException _) {
            Logger.log("impossibile comprendere la lista di protocolli supportati dal server: " + server_name, true);
            return null;
        }
    }

    /*
     * 1) se preferred_protocol è compatibile con un protocollo del server, ritorna il nome del protocollo che il server dovrà utilizzare
     * 2) se preferred_protocol non è compatibile, ma il server supporta il protocollo standard ritorna ""
     * 3) se preferred_protocol non è compatibile e il server non supporta il protocollo standard ritorna null
     */
    private static String get_common_protocol(String[] server_protocols, Connection_encoder preferred_protocol) {
        String[] pp_compatible_list = preferred_protocol.compatible_with(); //lista di protocolli con cui preferred_protocol è compatibile
        List<String> sp_list = Arrays.asList(server_protocols);

        //cerca se uno dei protocolli supportati da preferred_protocol è supportato anche dal server
        for (String protocol : pp_compatible_list) {
            if (sp_list.contains(protocol)) {
                return protocol;
            }
        }

        //preferred_protocol non è compatibile, controlla se il protocollo standard lo è o meno
        if (sp_list.contains("standard_protocol")) {
            return "";
        }
        else {
            return null;
        }
    }

    //inizia a proteggere la connessione utilizzando un dato protocollo
    private static void use_protocol(Connection_encoder client_encoder, String server_protocol, byte[] check) {
        try {
            //invia al server il nome del protocollo scelto
            byte[] encoded_protocol_name = server_pKey_encoder.doFinal(server_protocol.getBytes());
            Connection.send_directly(encoded_protocol_name);

            //inizializza l encoder
            client_encoder.init(check);

            //inizia a utilizzare le conversazioni per la connessione, e per la sicurezza utilizza il protocollo scelto
            Connection.start_conv(client_encoder);
        }
        catch (IllegalBlockSizeException | BadPaddingException _) {}
    }

    //esegue il login o crea un nuovo utente in un server
    private static void manage_login() {
        Logger.log("inizio processo per eseguire il login o registrarsi ad un utente nel server");
        TempPanel.show(new TempPanel_info(
                TempPanel_info.SINGLE_MSG,
                true,
                "per eseguire il login premere 'ok', per registrarsi o disconnettersi premere 'annulla'"
        ), login_or_register);
    }

    //ha appena scelto se vuole eseguire il login o se vuole registrarsi / disconnettersi, primo stage
    private static final TempPanel_action login_or_register = new TempPanel_action() {
        @Override
        public void success() { //vuole eseguire il login
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    true,
                    "inserisci il nome utente:",
                    "inserisci la password:"
            ).set_psw_indices(1), login_action);
        }

        @Override
        public void fail() { //vuole registrarsi o disconnettersi
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    true,
                    "premere 'ok' per registrarsi, 'annulla' per disconnettersi dal server"
            ), register_or_disconnect);
        }
    };

    //ha inserito la password e nome utente per eseguire il login
    private static final TempPanel_action login_action = new TempPanel_action() {
        @Override
        public void success() { //inseriti nome utente e password
            String username = (String) input.elementAt(0);
            char[] password = (char[]) input.elementAt(1);

            Logger.log("tento di eseguire il login nell'utente: " + username);

            //copia l'ultimo byte di ogni char in password[] per formare l array byte[] utilizzato come password
            byte[] password_bytes = new byte[password.length];
            for (int i = 0; i < password_bytes.length; i++) {
                password_bytes[i] = (byte) password[i];
            }

            //crea l array con il messaggio da inviare al server "login:<usr>;<psw>"
            int first_segment_len = 7 + username.length(); //7 è la lunghezza di "login:;"
            byte[] server_msg = Arrays.copyOf(("login:" + username + ";").getBytes(), first_segment_len + password_bytes.length);
            System.arraycopy(password_bytes, 0, server_msg, first_segment_len, password_bytes.length);

            //invia la richiesta di login al server
            byte conv_code = Connection.send(server_msg);
            byte[] response = Connection.wait_for_reply(conv_code);

            if (response != null && Arrays.equals(response, "log".getBytes())) { //risposta valida e positiva, login riuscito
                Logger.log("il login è stato effettuato con successo, nome utente: " + username);

                //attiva il pannello ClientList_panel
                Godzilla_frame.enable_panel(Godzilla_frame.CLIENT_LIST);
                ClientList_panel.setEnabled(true);

                Server_manager.username = username;

                return;
            }
            else if (response != null) { //risposta valida ma negativa, login fallito
                Logger.log("nome o utente errati, il server non ha accettato il login");
            }
            else { //errore durante l'attesa
                Logger.log("errore durante l'attesa per la risposta dal server, login fallito", true);
            }

            //se arriva qui significa che qualcosa è andato storto nel login, mostra un errore e richiede dati per il login
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "nome utente o password errati, riprovare"
            ), null);

            //aggiunge alla coda in TempPanel la richiesta del nuovo login
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    true,
                    "inserisci il nome utente:",
                    "inserisci la password:"
            ).set_psw_indices(1), login_action);
        }

        @Override
        public void fail() { //non vuole più fare il login, torna alla "fase 0"
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    true,
                    "per eseguire il login premere 'ok', per registrarsi o disconnettersi premere 'annulla'"
            ), login_or_register);
        }
    };

    //ha appena scelto se vuole registrarsi o se vuole disconnettersi
    private static final TempPanel_action register_or_disconnect = new TempPanel_action() {
        @Override
        public void success() { //vuole registrare un nuovo account
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    true,
                    "inserisci il nome utente:",
                    "inserisci la password:",
                    "ripeti la password:"
            ).set_psw_indices(1, 2), register_action);
        }

        @Override
        public void fail() { //vuole scollegarsi dal server
            close(true);
        }
    };

    //ha inserito i dati per registrarsi al server
    private static final TempPanel_action register_action = new TempPanel_action() {
        @Override
        public void success() { //ha inserito tutti i dati, cerca di registrare un nuovo account
            String username = (String) input.elementAt(0);
            char[] psw1 = (char[]) input.elementAt(1);
            char[] psw2 = (char[]) input.elementAt(2);

            Logger.log("tento di registrare un nuovo utente nel server: " + username);

            if (!Arrays.equals(psw1, psw2)) { //se le due password inserite sono differenti
                Logger.log("sono state inserite due password differenti, tentativo di registrarsi fallito");

                //mostra il messaggio di errore
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "inserire la stessa password due volte"
                ), null);

                //aggiunge alla coda in TempPanel la richiesta per la nuova registrazione
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.INPUT_REQ,
                        true,
                        "inserisci il nome utente:",
                        "inserisci la password:",
                        "ripeti la password:"
                ).set_psw_indices(1, 2), register_action);
            }

            //copia in psw_bytes il secondo byte di ogni char in psw1 = psw2 per avere una password in byte[]
            byte[] psw_bytes = new byte[psw1.length];
            for (int i = 0; i < psw1.length; i++) {
                psw_bytes[i] = (byte) psw1[i];
            }

            //crea l array con il messaggio da inviare al server "register:<usr>;<psw>"
            int first_segment_len = 10 + username.length(); //10 è la lunghezza di "register:;"
            byte[] server_msg = Arrays.copyOf(("register:" + username + ";").getBytes(), first_segment_len + psw_bytes.length);
            System.arraycopy(psw_bytes, 0, server_msg, first_segment_len, psw_bytes.length);

            //invia al server la richiesta di registrarsi e attende una risposta
            byte conv_code = Connection.send(server_msg);
            byte[] response = Connection.wait_for_reply(conv_code);

            if (response != null && Arrays.equals(response, "reg".getBytes())) { //ricevuta una risposta valida e positiva, registrazione avvenuta con successo
                Logger.log("registrazione al server con l'utente: " + username + " avvenuta con successo");

                //attiva il pannello ClientList_panel
                Godzilla_frame.enable_panel(Godzilla_frame.CLIENT_LIST);
                ClientList_panel.setEnabled(true);

                Server_manager.username = username;

                return;
            }
            else if (response != null) { //ricevuta una risposta valida, ma negativa, registrazione fallita
                Logger.log("il server non ha accettato la registrazione all'account: " + username);
            }
            else { //errore durante l'attesa della risposta dal server
                Logger.log("errore durante l'attesa del server per l'esito della registrazione dell'account: " + username, true);
            }

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "il server non ha accettato la registrazione, riprovare"
            ), null);

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    true,
                    "inserisci il nome utente:",
                    "inserisci la password:",
                    "ripeti la password:"
            ).set_psw_indices(1, 2), register_action);
        }

        @Override
        public void fail() { //non vuole più registrarsi, torna alla "fase 0"
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    true,
                    "per eseguire il login premere 'ok', per registrarsi o disconnettersi premere 'annulla'"
            ), login_or_register);
        }
    };

    //registra nuovi encoders alla lista dei supportati
    public static boolean register_encoder(String name, Connection_encoder encoder) {
        Logger.log("registrato un nuovo encoder per connessioni in Server_manager: " + name);
        return encoders.putIfAbsent(name, encoder) == null;
    }

    public static String[] get_encoders_list() {
        return encoders.keySet().toArray(new String[0]);
    }

    //risponde alla richiesta di collegarsi da un altro utente
    public static void pair_with(String usr, byte conv_code) {
        if (paired_usr == null && pairing_usr == null) {
            pairing_usr = usr;

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    true,
                    "il client: " + usr + " ha richiesto di collegarsi"
            ), new Pair_request(conv_code));
        }
    }

    //invia la richiesta di collegarsi a un altro utente
    public static void pair_with(String pair_usr) {
        if (pairing_usr != null || paired_usr != null) { //se è in corso la connessione, o è già connesso a un altro client, fallisce subito
            return;
        }

        pairing_usr = pair_usr;

        //richiede all'utente di collegarsi e attende la risposta dal server
        byte conv_code = Connection.send(("pair:" + pair_usr).getBytes());
        byte[] reply = Connection.wait_for_reply(conv_code);

        if (reply != null && Arrays.equals(reply, "acc".getBytes())) { //risposta valida e positiva, connessione accettata
            Logger.log("collegamento con il client: " + pair_usr + " instaurato con successo");
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "il collegamento con il client: " + pair_usr + " è stato instaurato con successo"
            ), null);

            //risponde al server confermando il collegamento
            Connection.send(conv_code, "acc".getBytes());
            paired_usr = pair_usr;
            pairing_usr = null;

            return;
        }
        else if (reply != null) { //risposta valida negativa, connessione rifiutata
            Logger.log("il collegamento con il client: " + pair_usr + " è stato rifiutato");
        }
        else { //errore mentre si attende la risposta dal server
            Logger.log("errore durante l'attesa di una risposta per il collegamento con il client: " + pair_usr, true);
        }

        pairing_usr = null;
        TempPanel.show(new TempPanel_info(
                TempPanel_info.SINGLE_MSG,
                false,
                "il collegamento con il client: " + pair_usr + " è stato rifiutato"
        ), null);
    }

    public static boolean is_paired() {
        return paired_usr != null;
    }

    //azione da eseguire per accettare / rifiutare connessioni ad altri client
    private static class Pair_request implements TempPanel_action {
        private final byte CONV_CODE;

        public Pair_request(byte conv_code) {
            this.CONV_CODE = conv_code;
        }

        @Override
        public synchronized void success() { //hai accettato la connessione al client
            //controlla non sia collegato a nessun altro client
            if (paired_usr != null) {
                Logger.log("tentativo di accettare la connessione con il client fallito, si è già connessi al client: " + paired_usr, true);
                Connection.send(CONV_CODE, "den".getBytes());
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "impossibile accettare più di una connessione alla volta"
                ), null);

                return;
            }

            Logger.log("accettata la connessione con il client, attendo conferma dal server");

            //conferma al server che ha accettato la connessione e attende la sua conferma
            Connection.send(CONV_CODE, "acc".getBytes());
            byte[] byte_confirm = Connection.wait_for_reply(CONV_CODE); //attende la conferma dal server

            if (byte_confirm != null && Arrays.equals(byte_confirm, "acc".getBytes())) { //ha ricevuto una risposta valida ed è anche affermativa
                paired_usr = pairing_usr;

                Logger.log("connessione con il client: " + paired_usr + " avvenuta con successo");
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "connessione con il client: " + paired_usr + " avvenuta con successo"
                ), null);
            }
            else if (byte_confirm != null) { //ha ricevuto una risposta valida ma negativa
                Logger.log("tentativo di connessione con il client: " + pairing_usr + " fallito, il server non ha confermato il collegamento");
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "il server non ha confermato il collegamento con il client: " + pairing_usr
                ), null);
            }
            else { //errore nell'attesa della risposta dal server
                Logger.log("tentativo di connessione con il client: " + pairing_usr + " fallito, errore durante l'attesa della conferma dal server", true);
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "il server non ha confermato il collegamento con il client: " + pairing_usr
                ), null);
            }

            pairing_usr = null;
        }

        @Override
        public void fail() {
            Logger.log("rifiutata la connessione con il client: " + pairing_usr);
            Connection.send(CONV_CODE, "den".getBytes());

            pairing_usr = null;
        }
    }

    public static String get_paired_usr() {
        return paired_usr;
    }

    public static void unpair(boolean notify_server) {
        if (paired_usr == null) {
            Logger.log("tentativo di scollegarsi da un client mentre non si è collegati a nessuno");
            return;
        }

        paired_usr = null;
        if (notify_server) { //se deve avvisare l'altro client che si è scollegato
            Connection.send("EOP".getBytes());
        }
    }
}
