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
            Logger.log("impossibile aprire il file " + pathname + ", il file non esiste", true, '\n');
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
            Logger.log("impossibile creare il file: " + pathname, true, '\n');
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
        try {
            FileInputStream fis = new FileInputStream(FILE);
            if (!is_encoded) {
                fis.readNBytes(6);
            } //skippa i primi 6 byte poiche non sono contenuto del file

            byte[] txt = fis.readAllBytes();
            fis.close();

            if (is_encoded) {
                txt = File_cipher.decrypt(txt);
            }

            return txt;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Logger.log("impossibile decifrare il contenuto del file " + FILE.getAbsolutePath() + ", file corrotto", true, '\n');
            return null;
        } catch (IOException _) {
            Logger.log("impossibile aprire il file " + FILE.getAbsolutePath() + ", il file non esiste", true, '\n');
            return null;
        }
    }

    protected void append(String txt) {
        try {
            if (is_encoded) {
                String file_txt = new String(read()) + txt;
                replace(file_txt);
            } else {
                FOS.write(txt.getBytes());
            }
        } catch (IOException _) {
            Logger.log("impossibile aprire il file " + FILE.getAbsolutePath() + ", il file non esiste", true, '\n');
        }
    }

    protected void replace(String txt) {
        if (!is_encoded) {
            txt = "clear\n" + txt;
        }

        byte[] txt_b = txt.getBytes();
        replace(txt_b);
    }

    protected void replace(byte[] txt) {
        try {
            clear_file();

            if (is_encoded) {
                txt = File_cipher.crypt(txt);
            }

            FOS.write(txt);
        } catch (IllegalBlockSizeException | BadPaddingException _) {
            Logger.log("impossibile decifrare il contenuto del file " + FILE.getAbsolutePath() + ", file corrotto", true, '\n');
        } catch (IOException _) {
            Logger.log("impossibile aprire il file " + FILE.getAbsolutePath() + ", il file non esiste", true, '\n');
        }
    }

    private void clear_file() throws IOException {
        new FileOutputStream(FILE, false).close();
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
