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
    private static Cipher encrypter = null;
    private static Cipher decrypter = null;

    public static void init() throws InterruptedException, IOException {
        Database.FileCypherKey_test = File_interface.class.getClassLoader().getResourceAsStream("files/FileCipherKey.dat").readAllBytes();
        TempPanel.show(new TempPanel_info(TempPanel_info.INPUT_REQ, false, "inserisci la chiave per i database: ").set_psw_indices(0), AES_init); //chiede la password
    }

    private static TempPanel_action AES_init = new TempPanel_action() {
        @Override
        public void success() { //ha ricevuto un password, controlla sia giusta ed inizializza i cipher
            try {
                MessageDigest md = MessageDigest.getInstance("SHA3-512");
                byte[] hash = md.digest(input.elementAt(0).getBytes());

                if (Arrays.compare(Arrays.copyOfRange(hash, 32, 64), Database.FileCypherKey_test) != 0) { //se la password inserita è sbagliata
                    fail();
                } else {
                    Logger.log("inserita la password corretta per decifrare i file");
                    init_ciphers(hash);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fail() {
            Logger.log("è stata inserita una password per decifrare i file errata");
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "password non corretta, riprovare"), ERROR_INIT);
        }
    };

    private static void init_ciphers(byte[] key_hash) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        //utilizza i primi 32byte dell'hash come key ed iv per inizializzare AES
        byte[] key_bytes = Arrays.copyOf(key_hash, 16);
        byte[] iv_bytes = Arrays.copyOfRange(key_hash, 16, 32);

        SecretKey key = new SecretKeySpec(key_bytes, "AES");
        IvParameterSpec iv = new IvParameterSpec(iv_bytes);

        //inzializza encrypter e decrypter con key ed iv appena calcolati
        encrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");

        encrypter.init(Cipher.ENCRYPT_MODE, key, iv);
        decrypter.init(Cipher.DECRYPT_MODE, key, iv);
        Logger.log("definiti i cipher per decifrare e cifrare i file correttamente");

        File_interface.update_servers_info();
    }

    private static final TempPanel_action ERROR_INIT = new TempPanel_action() {
        @Override
        public void success() {
            TempPanel.show(new TempPanel_info(TempPanel_info.INPUT_REQ, false, "inserisci la chiave per i database: ").set_psw_indices(0), AES_init); //richiede la password
        }

        @Override
        public void fail() {} //essendo un messaggio non può "fallire"
    };

    protected static byte[] decrypt(byte[] txt) throws IllegalBlockSizeException, BadPaddingException {
        if (decrypter != null) {
            return decrypter.doFinal(txt);
        }
        else {
            Logger.log("il decoder non è ancora stato definito, impossibile decifrare il testo", true, '\n');
            return null;
        }
    }

    protected static byte[] crypt(byte[] txt) throws IllegalBlockSizeException, BadPaddingException {
        if (encrypter != null) {
            return encrypter.doFinal(txt);
        }
        else {
            Logger.log("l'encoder non è ancora stato definito, impossibile cifrare il testo", true, '\n');
            return null;
        }
    }
}
