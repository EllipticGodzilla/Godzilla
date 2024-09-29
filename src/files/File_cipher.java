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

public abstract class File_cipher {
    private static Cipher encoder = null;
    private static Cipher decoder = null;

    public static void init() throws InterruptedException, IOException {
        try {
            Database.FileCypherKey_test = File_interface.class.getClassLoader().getResourceAsStream("files/FileCipherKey.dat").readAllBytes();
        }
        catch (NullPointerException _) { //il file contenente la password è mancante
            System.out.println("impossibile trovare il file contenente la password per decifrare i file all'interno dell'eseguibile");
            System.exit(0);
        }
    }

    public static void init_ciphers(byte[] key_hash) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (encoder == null && decoder == null) {
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
        else {
            Logger.log("impossibile inizializzare File_cipher più di una volta", true);
        }
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
