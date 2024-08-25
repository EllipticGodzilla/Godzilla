package network;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class AESCipher {
    private final Cipher ENCODER;
    private final Cipher DECODER;
    private final Random RANDOM;
    private final SecretKey KEY;

    public AESCipher(SecretKey key, byte[] rnd_seed) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        RANDOM = new Random(); //inizializza random
        RANDOM.setSeed(byte_array_to_long(rnd_seed));

        this.KEY = key;

        ENCODER = Cipher.getInstance("AES/CBC/PKCS5Padding"); //inizializza i cipher
        DECODER = Cipher.getInstance("AES/CBC/PKCS5Padding");

        IvParameterSpec iv = next_iv(); //genera un iv casuale
        ENCODER.init(Cipher.ENCRYPT_MODE, this.KEY, iv);
        DECODER.init(Cipher.DECRYPT_MODE, this.KEY, iv);
    }

    public byte[] encode(byte[] msg) throws IllegalBlockSizeException, BadPaddingException {
        byte[] encoded_msg = ENCODER.doFinal(msg); //decodifica il messaggio
        regen_iv(); //rigenera gli iv

        return encoded_msg;
    }

    public byte[] decode(byte[] msg) throws IllegalBlockSizeException, BadPaddingException {
        byte[] plain_msg = DECODER.doFinal(msg);
        regen_iv();

        return plain_msg;
    }

    private long byte_array_to_long(byte[] arr) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result |= arr[i]; //aggiunge un byte in fondo al long
            result <<= 8; //sposta tutti i bit a sinistra di 8 posizioni
        }

        return result;
    }

    private void regen_iv() { //rigenera gli iv, in modo che cifrando più volte uno stesso messaggio il risultato sia sempre differente
        try {
            IvParameterSpec iv = next_iv();
            ENCODER.init(Cipher.ENCRYPT_MODE, KEY, iv);
            DECODER.init(Cipher.DECRYPT_MODE, KEY, iv);
        }
        catch (InvalidKeyException | InvalidAlgorithmParameterException _) {} //impossibile rientrare
    }

    private IvParameterSpec next_iv() {
        byte[] iv_byte = new byte[ENCODER.getBlockSize()];
        RANDOM.nextBytes(iv_byte);

        return new IvParameterSpec(iv_byte);
    }
}
