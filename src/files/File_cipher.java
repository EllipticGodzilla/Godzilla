package files;

import gui.TempPanel_action;
import gui.TempPanel;
import gui.TempPanel_info;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class File_cipher {
    private static Cipher encoder = null;
    private static Cipher decoder = null;

    public static void init() throws InterruptedException, IOException {
        Database.FileCypherKey_test = File_interface.class.getClassLoader().getResourceAsStream("files/FileCipherKey.dat").readAllBytes();
        TempPanel.show(new TempPanel_info( //chiede la password
                TempPanel_info.INPUT_REQ,
                false,
                "inserisci la chiave per i database: "
        ).set_psw_indices(0), test_password);
    }

    //inserita la password per decifrare i file controlla sia corretta e se lo è inizia a decifrare i loro contenuti
    private static final TempPanel_action test_password = new TempPanel_action() {
        @Override
        public void success() { //ha ricevuto un password, controlla sia giusta e inizializza i cipher
            try {
                char[] password = (char[]) input.elementAt(0);

                //ricava un array di byte[] da password[] prendendo il secondo byte per ogni char in esso
                byte[] password_bytes = new byte[password.length];
                for (int i = 0; i < password.length; i++) {
                    password_bytes[i] = (byte) password[i];
                }

                //calcola l hash 512 della password inserita
                MessageDigest md = MessageDigest.getInstance("SHA3-512");
                byte[] hash = md.digest(password_bytes);

                //la seconda metà dell hash viene utilizzata per controllare che la password sia corretta, confrontandola con una copia che ha in un file dell'hash corretto
                byte[] psw_text = Arrays.copyOfRange(hash, 32, 64);
                if (Arrays.equals(psw_text, Database.FileCypherKey_test)) { //se i due pezzi di hash sono uguali, la password è corretta
                    Logger.log("inserita la password corretta per decifrare i file");
                    init_ciphers(hash);

                    //decifra tutte le informazioni contenute dei file cifrati e aggiorna tutte le variabili interne con i dati
                    File_interface.update_servers_info();
                }
                else { //se sono diversi è stata inserita una password sbagliata
                    Logger.log("è stata inserita una password per decifrare i file errata");

                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "password non corretta, riprovare"
                    ), null);

                    TempPanel.show(new TempPanel_info( //chiede la password
                            TempPanel_info.INPUT_REQ,
                            false,
                            "inserisci la chiave per i database: "
                    ).set_psw_indices(0), test_password);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fail() {}
    };

    private static void init_ciphers(byte[] key_hash) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        //utilizza i primi 32byte dell hash come key e iv per inizializzare AES
        byte[] key_bytes = Arrays.copyOf(key_hash, 16);
        byte[] iv_bytes = Arrays.copyOfRange(key_hash, 16, 32);

        SecretKey key = new SecretKeySpec(key_bytes, "AES");
        IvParameterSpec iv = new IvParameterSpec(iv_bytes);

        //inzializza encoder e decoder con key e iv appena calcolati
        encoder = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decoder = Cipher.getInstance("AES/CBC/PKCS5Padding");

        encoder.init(Cipher.ENCRYPT_MODE, key, iv);
        decoder.init(Cipher.DECRYPT_MODE, key, iv);
        Logger.log("definiti i cipher per decifrare e cifrare i file correttamente");
    }

    protected static byte[] decrypt(byte[] txt) throws IllegalBlockSizeException, BadPaddingException {
        if (decoder != null) {
            return decoder.doFinal(txt);
        }
        else {
            Logger.log("il decoder non è ancora stato definito, impossibile decifrare il testo", true);
            return null;
        }
    }

    protected static byte[] crypt(byte[] txt) throws IllegalBlockSizeException, BadPaddingException {
        if (encoder != null) {
            return encoder.doFinal(txt);
        }
        else {
            Logger.log("l'encoder non è ancora stato definito, impossibile cifrare il testo", true);
            return null;
        }
    }
}
