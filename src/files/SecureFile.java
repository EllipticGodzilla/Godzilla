package files;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.util.Arrays;

public class SecureFile {
    private boolean is_encoded;

    private final FileOutputStream FOS;
    private final File FILE;

    protected SecureFile(String pathname) {
        boolean temp_encoded = false;
        FileOutputStream temp_fos = null;
        File temp_file = null;

        try {
            temp_file = new File(pathname);
            temp_fos = new FileOutputStream(temp_file, true);

            FileInputStream fis = new FileInputStream(temp_file);
            temp_encoded = !Arrays.equals(fis.readNBytes(6), "clear\n".getBytes());
            fis.close();
        } catch (IOException _) {
            Logger.log("impossibile aprire il file " + pathname + ", il file non esiste", true);
        }

        is_encoded = temp_encoded;
        FOS = temp_fos;
        FILE = temp_file;
    }

    protected SecureFile(String pathname, boolean encoded) { //crea un nuovo SecureFile da un file non esistente
        this.is_encoded = encoded;
        this.FILE = new File(pathname);

        FileOutputStream temp_fos = null;

        try {
            if (!FILE.createNewFile())
                throw new IOException();

            temp_fos = new FileOutputStream(FILE);

            if (!encoded) {
                temp_fos.write("clear\n".getBytes());
            }
        }
        catch (IOException _) {
            Logger.log("impossibile creare il file: " + pathname, true);
        }

        FOS = temp_fos;
    }

    public void set_encoded(boolean encode) {
        this.is_encoded = encode;
    }

    public boolean is_protected() {
        return is_encoded;
    }

    protected byte[] read() {
        byte[] txt;
        try {
            FileInputStream fis = new FileInputStream(FILE);
            if (!is_encoded) {
                fis.readNBytes(6);
            } //salta i primi 6 byte poiché non sono contenuto del file ma indicano che il file non è cifrato

            txt = fis.readAllBytes();
            fis.close();
        }
        catch (IOException _) {
            Logger.log("impossibile leggere il contenuto del file: " + FILE.getAbsolutePath(), true);
            return null;
        }

        if (is_encoded) {
            try {
                txt = File_cipher.decrypt(txt);
            } catch (IllegalBlockSizeException | BadPaddingException _) {
                Logger.log("impossibile decifrare il contenuto del file: " + FILE.getAbsolutePath() + ", file corrotto", true);
                return null;
            }
        }

        return txt;
    }

    protected void append(String txt) {
        if (is_encoded) {
            String file_txt = new String(read()) + txt;
            replace(file_txt);
        } else {
            try {
                FOS.write(txt.getBytes());
            }
            catch (IOException _) {
                Logger.log("impossibile scrivere nel file: " + FILE.getAbsolutePath(), true);
            }
        }
    }

    protected void replace(String txt) {
        try { //elimina il contenuto del file
            new FileOutputStream(FILE, false).close();
        }
        catch (IOException _) {
            Logger.log("errore nell'eliminare il contenuto del file: " + FILE.getAbsolutePath(), true);
            return;
        }

        //calcola i bytes da scrivere nel file
        byte[] txt_bytes;
        if (is_encoded) { //se deve cifrare il testo
            try {
                txt_bytes = File_cipher.crypt(txt.getBytes());

                if (txt_bytes == null) //non è ancora stato inizializzato File_cipher
                    return;
            }
            catch (IllegalBlockSizeException | BadPaddingException _) {
                Logger.log("impossibile decifrare il contenuto del file: " + FILE.getAbsolutePath(), true);
                txt_bytes = new byte[0];
            }
        }
        else { //se non deve cifrare il testo, indica che il file non è cifrato
            txt_bytes = ("clear\n" + txt).getBytes();
        }

        //scrive il contenuto nel file
        try {
            FOS.write(txt_bytes);
        }
        catch (IOException _) {
            Logger.log("impossibile scrivere nel file: " + FILE.getAbsolutePath(), true);
        }
    }

    protected void delete() {
        close();
        FILE.delete();
    }

    protected void close() {
        try {
            FOS.close();
        } catch (IOException _) {}
    }
}
