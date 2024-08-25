package network;

import files.Database;
import files.Logger;
import gui.*;
import javax.crypto.*;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

public abstract class Server {
    public static final int E_INVCE = -4; //certificato invalido
    public static final int E_GEN = -3; //viene usato come errore generale
    public static final int E_INVIP = -2; //ip non valido, non è nella forma xxx.xxx.xxx.xxx
    public static final int E_CONR = -1; //connessione rifiutata, il server è spento

    public static String server_name = ""; //nome con cui è chiamato il server da questo client
    public static String registered_name = ""; //nome con cui il server si è registrato al dns
    public static String ip = "";
    public static String link = "";
    public static String mail = "";
    public static byte[] pub_key;
    public static byte[] ce;

    public static String dns_ip;

    private static String username = "";

    private static final int CA_DNS_PORT = 9696;
    private static final int SERVER_PORT = 31415;

    private static MessageDigest sha3_hash = null;

    public static int start_connection_with(String link, String dns_ip) {
        Server.dns_ip = dns_ip;

        if (Connection.isClosed()) {
            boolean dns_alive = false; //distingue fra connessione rifiutata dal DNS e dal server

            try {
                String ip;
                if (is_an_ip(link)) { //se viene dato l'indirizzo ip del server a cui collegarsi
                    ip = link;

                    Logger.log("indirizzo ip del server: " + ip);
                    link = get_from_dns('r', ip);
                    Logger.log("link del server: " + link);
                }
                else { //se viene dato il link, si collega al DNS per ricevere l'indirizzo ip
                    Logger.log("link del server: " + link);
                    ip = get_from_dns('d', link);
                    Logger.log("indirizzo ip del server: " + ip);

                    if (ip.equals("error, the ip is not registered")) {
                        Logger.log("impossibile trovare l'indirizzo ip legato a questo link: " + link, true, '\n');
                        TempPanel.show(new TempPanel_info(
                                TempPanel_info.SINGLE_MSG,
                                false,
                                "il dns legato a questo server non conosce il suo indirizzo ip"
                        ), null);
                    }
                }

                dns_alive = true;
                Connection.init(ip, SERVER_PORT);

                //riceve il certificato del server e controlla sia tutto in regola
                boolean check_ce = check_certificate(link, ip);
                if (check_ce) { //se il certificato è valido, genera una chiave sint size = (bis.read() & 0xff) | ((bis.read() & 0xff) int size = (bis.read() & 0xff) | ((bis.read() & 0xff) << 8);<< 8);immetrica e la invia al server
                    Logger.log("il certificato ricevuto dal server è valido");

                    Cipher pubKey_cipher = get_pub_cipher();
                    secure_with_aes(pubKey_cipher);
                } else { //se è stato trovato un errore nel verificare il certificato
                    Logger.log("il certificato ricevuto dal server non è valido", true, '\n');
                    TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "il certificato ricevuto dal server non è valido, chiudo la connessione"), null);

                    Connection.close();

                    return E_INVCE;
                }

                return 0;
            } catch (UnknownHostException e) {
                Logger.log("l'indirizzo ip inserito non è valido", true, '\n');
                TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "l'indirizzo ip è stato inserito male, non ha senso"), null);
                return E_INVIP;
            } catch (ConnectException e) {
                if (dns_alive) { //se la connessione è stata rifiutata dal server
                    Logger.log("il server non è raggiungibile", true, '\n');
                    TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "connessione rifiutata, il server non è raggiungibile"), null);
                } else { //se la connessione è stata rifiutata dal DNS
                    Logger.log("il server dns a cui è registrato il server non è raggiungibile", true, '\n');
                    TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "errore nella configurazione del DNS, server non raggiungibile"), null);
                }
                return E_CONR;
            } catch (IOException e) {
                Logger.log("la connessione è stata interrotta inaspettatamente", true, '\n');
                TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "impossibile connettersi al server"), null);
                return E_GEN;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else {
            Logger.log("impossibile connettersi al server: " + link + ", si è già connessi ad un server", true, '\n');
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "già connessi ad un server!"), null);

            return E_GEN;
        }
    }

    public static String get_from_dns(char prefix, String msg) throws IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        Connection.init(dns_ip, CA_DNS_PORT);
        Connection.direct_write(prefix + msg); //aggiungendo d all'inizio specifichi al dns che vuoi conoscere l'indirizzo ip partendo dal link

        String response = Connection.wait_for_string();
        Connection.close();

        return response;
    }

    public static boolean check_certificate(String link, String ip) throws IllegalBlockSizeException, NoSuchAlgorithmException {
        try {
            //riceve le informazioni per il controllo del certificato dal server
            String server_info = Connection.wait_for_string();
            byte[] server_certificate = Connection.wait_for_bytes();

            //controlla che le informazioni coincidano con il certificato
            byte[] dec_ce = Database.DNS_CA_KEY.get(dns_ip).el1.doFinal(server_certificate); //decifra il certificato trovando l'hash delle informazioni del server
            byte[] server_info_hash = sha3_hash(server_info.getBytes());

            Pattern patt = Pattern.compile("[;]");
            String[] info_array = patt.split(server_info);

            /*
             * controlla che:
             * 1) l'hash calcolato da server_info e quello decifrato dal certificato coincidano => il certificato è stato effettivamente rilasciato dalla CA
             * 2) l'indirizzo scritto nelle server_info (e quindi nel certificato) coincide con quello voluto => il server è effettivamente quello a cui ci si vuole collegare
             * 3) l'indirizzo ip scritto nelle server_info (e quindi nel certificato) coincida con quello voluto, uguale a (2) non mi ricordo perché lo faccio, è redundant poiché già l'indirizzo è unico
             */
            if (Arrays.compare(dec_ce, server_info_hash) == 0 && info_array[1].equals(link) && info_array[2].equals(ip)) {
                //salva le informazioni con cui il server si è registrato al dns
                registered_name = info_array[0];
                Server.link = info_array[1];
                Server.ip = info_array[2];
                pub_key = Base64.getDecoder().decode(info_array[3].getBytes());
                mail = info_array[4];
                ce = server_certificate;

                return true;
            } else {
                return false;
            }
        } catch (BadPaddingException e) {
            return false;
        }
    }

    public static void secure_with_aes(Cipher pubKey_cipher) throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, IOException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        KeyGenerator kg = KeyGenerator.getInstance("AES"); //genera una chiave AES di sessione
        SecretKey key = kg.generateKey();

        byte check[] = new byte[8]; //genera 8 byte random che il server dovrà inviare cifrati con key per confermare di aver ricevuto correttamente la chiave di sessione
        new SecureRandom().nextBytes(check);

        byte[] aes_key_encoded = pubKey_cipher.doFinal(merge_array(key.getEncoded(), check)); //cifra la chiave AES assieme al check code con la chiave pubblica del server
        Connection.direct_write(aes_key_encoded);

        AESCipher cipher = new AESCipher(key, check);
        Connection.set_cipher(cipher); //da ora la connessione verrà cifrata con la chiave AES appena generata

        byte[] received_check = Connection.wait_for_bytes(); //attende che il server invii i check bytes cifrati con la chiave di sessione
        if (Arrays.compare(received_check, check) != 0) { //se i due array non coincidono, i byte di check sono stati cifrati in modo errato da parte del server
            Logger.log("il server non ha cifrato correttamente il check code, chiudo la connessione", true, '\n');
            Connection.close(); //chiude la connessione
        }
        else { //se il server cifra correttamente i byte di check prosegue con il protocollo:
            Logger.log("check code cifrato correttamente dal server, connessione instaurata con successo");

            ServerList_panel.update_button_enable(); //disattiva il tasto per collegarsi ad un server ed attiva quello per scollegarsi

            Connection.start_dinamic(); //inizia a ricevere messaggi dal server in modo dinamico
            login_register(); //si registra o fa il login
        }

    }

    public static void disconnect(boolean notify_server) {
        try {
            if (!Connection.isClosed()) {
                Logger.log("disconnessione dal server");
                Godzilla_frame.set_title("Godzilla - Client");

                //se è appaiato con un altro client si scollega
                if (Connection.is_paired()) {
                    Connection.unpair(notify_server);
                }

                //si scollega dal server
                if (notify_server) { //se deve avvisare il server che si sta scollegando
                    Connection.write("EOC"); //avvisa che sta chiudendo la connessione
                }
                Connection.close();

                //disattiva il pannello client list e resetta la lista dei client
                ClientList_panel.update_client_list("");
                ClientList_panel.setEnabled(false);
                Godzilla_frame.disable_panel(Godzilla_frame.CLIENT_LIST);

                //aggiorna quali pulsati sono attivi nella server_list gui
                ServerList_panel.update_button_enable();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] merge_array(byte[] arr1, byte[] arr2) { //unisce i due array
        byte[] merged_array = Arrays.copyOf(arr1, arr1.length + arr2.length);
        System.arraycopy(arr2, 0, merged_array, arr1.length, arr2.length);

        return merged_array;
    }

    private static void login_register() {
        TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, true, "se vuoi fare il login premi \"ok\", altrimenti cancella"), login_or_register);
    }

    private static TempPanel_action login_or_register = new TempPanel_action() {
        @Override
        public void success() { //se vuole fare il login
            TempPanel.show(new TempPanel_info(TempPanel_info.INPUT_REQ, true, "inserisci nome utente: ", "inserisci password: ").set_psw_indices(1), login);
        }

        @Override
        public void fail() { //se vuole registrarsi o se vuole uscire
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, true, "se vuoi registrarti premi \"ok\", altrimenti \"cancella\" e verrai disconnesso"), register_or_exit);
        }
    };

    private static TempPanel_action login = new TempPanel_action() {
        @Override
        public void success() { //sono stati inseriti nome utente e password
            try {
                Logger.log("tento di eseguire il login all'utente: " + input.elementAt(0));
                byte[] psw_hash = psw_array(input.elementAt(0).getBytes(), input.elementAt(1).getBytes()); //calcola l'hash da utilizzare come password per effettuare il login

                //forma il messaggio da inviare al server "login:<usr>;sha3-256(<psw>+0xff.ff^<usr>)" e lo invia
                byte[] server_msg = Arrays.copyOf(("login:" + input.elementAt(0) + ";").getBytes(), 7 + input.elementAt(0).length() + psw_hash.length);
                System.arraycopy(psw_hash, 0, server_msg, 7 + input.elementAt(0).length(), psw_hash.length);

                Connection.write(server_msg, login_result); //attende una risposta dal server
            } catch (NoSuchAlgorithmException _) {}
        }

        private On_arrival login_result = (_, msg) -> { //una volta ricevuta la risposta dal server
            if (new String(msg).equals("log")) { //se il login è andato a buon fine
                Logger.log("login effettuato con successo");

                username = input.elementAt(0);
                Godzilla_frame.set_title(server_name + " (" + registered_name + ")" + " - " + username);

                //attiva il pannello con la lista dei client
                Godzilla_frame.enable_panel(Godzilla_frame.CLIENT_LIST);
                ClientList_panel.setEnabled(true);
            }
            else { //se nome utente o password sono sbagliati
                Logger.log("login fallito, nome utente o password errati");
                TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, true, "nome utente o password errati, premere \"ok\" per ritentare"), login_or_register);
            }
        };

        @Override
        public void fail() { //è stato premuto "cancella" viene chiesto nuovamente se vuole fare il login o se vuole registrarsi/scollegarsi
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, true, "se vuoi fare il login premi \"ok\", altrimenti cancella"), login_or_register);
        }
    };

    private static final TempPanel_action register_or_exit = new TempPanel_action() {
        @Override
        public void success() { //vuole registrarsi
            TempPanel.show(new TempPanel_info(TempPanel_info.INPUT_REQ, true, "inserisci nome utente: ", "inserisci password: ").set_psw_indices(1), register);
        }

        @Override
        public void fail() { //si disconnette dal server
            disconnect(true);
        }
    };

    private static TempPanel_action register = new TempPanel_action() {
        @Override
        public void success() {
            try {
                byte[] psw_hash = psw_array(input.elementAt(0).getBytes(), input.elementAt(1).getBytes()); //calcola l'hash da utilizzare come password per registrarsi

                //forma il messaggio da inviare al server "register:<usr>;sha3-256(<psw>+0xff..ff^<usr>)" e lo invia
                byte[] server_msg = Arrays.copyOf(("register:" + input.elementAt(0) + ";").getBytes(), 10 + input.elementAt(0).length() + psw_hash.length);
                System.arraycopy(psw_hash, 0, server_msg, 10 + input.elementAt(0).length(), psw_hash.length);

                Connection.write(server_msg, register_result);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private On_arrival register_result = (conv_code, msg) -> {
            if (Arrays.compare(msg, "reg".getBytes()) == 0) { //se la registrazione è andata a buon fine
                Logger.log("è stato creato il nuovo utente con successo, nome utente: " + input.elementAt(0));

                username = input.elementAt(0);
                Godzilla_frame.set_title(server_name + " (" + registered_name + ")" + " - " + username);

                //attiva il pannello con la lista dei client
                Godzilla_frame.enable_panel(Godzilla_frame.CLIENT_LIST);
                ClientList_panel.setEnabled(true);
            }
            else { //se nome utente o password sono sbagliati
                TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "esiste già un utente con questo nome, scegline uno nuovo"), register_or_exit);
            }
        };

        @Override
        public void fail() { //se viene premuto cancella, chiede nuovamente se vuole fare il login o registrarsi/scollegarsi
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, true, "se vuoi fare il login premi \"ok\", altrimenti cancella"), login_or_register);
        }
    };

    private static byte[] psw_array(byte[] usr, byte[] psw) throws NoSuchAlgorithmException { //ritorna sha3-256(<psw>+0xff..ff^<usr>)
        byte[] usr_inverse = new byte[usr.length];

        for (int i = 0; i < usr.length; i++) {
            usr_inverse[i] = (byte) (usr[i] ^ 0xff);
        }

        int psw_len = psw.length;
        psw = Arrays.copyOf(psw, psw_len + usr_inverse.length); //aumenta la lunghezza di psw[]
        System.arraycopy(usr_inverse, 0, psw, psw_len, usr_inverse.length); //copia usr_inverse[] in psw[]

        return sha3_hash(psw); //ritorna l'hash di psw[]
    }

    private static boolean is_an_ip(String txt) {
        String[] segm = txt.split("\\.");

        if (segm.length != 4) { return false; } //un indirizzo ip ha 4 segmenti separati da "."

        try {
            for (int i = 0; i < segm.length; i++) {
                if (Integer.valueOf(segm[i]) > 255) { //in un ip ogni segmento può arrivare massimo a 255
                    return false;
                }
            }
        }
        catch (NumberFormatException e) { //un segmento non rappresenta un numero
            return false;
        }

        return true; //sono 4 segmenti dove ogni segmento è un numero minore o uguale a 255
    }

    //genera un Cipher RSA con la chiave pubblica del server a cui si sta collegando
    private static Cipher get_pub_cipher() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {
        KeyFactory key_f = KeyFactory.getInstance("RSA");
        PublicKey server_key = key_f.generatePublic(new X509EncodedKeySpec(pub_key));

        Cipher server_cipher = Cipher.getInstance("RSA");
        server_cipher.init(Cipher.ENCRYPT_MODE, server_key);

        return server_cipher;
    }

    private static byte[] sha3_hash(byte[] txt) throws NoSuchAlgorithmException { //calcola l'hash di txt secondo l'algoritmo sha3
        if (sha3_hash == null) {
            sha3_hash = MessageDigest.getInstance("SHA3-256");
        }

        return sha3_hash.digest(txt);
    }
}
