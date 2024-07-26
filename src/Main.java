import file_database.Database;
import file_database.File_cipher;
import file_database.File_interface;
import gui.*;
import network.Server;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Vector;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        JFrame main_frame = Godzilla_frame.init();
        File_cipher.init();

        //imposta l'icona del main frame
        Vector<Image> icons = new Vector<>();

        icons.add(new ImageIcon(Main.class.getResource("/images/icon_16.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("/images/icon_32.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("/images/icon_64.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("/images/icon_128.png")).getImage());

        main_frame.setIconImages(icons);

        //aggiunge un shutdown hook
        Runtime.getRuntime().addShutdownHook(shut_down);
    }

    private static Thread shut_down = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Server.disconnect(true); //se è ancora connesso ad un server si disconnette

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("done\n salvo la server list - ", false); }
                save_server_list();

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("done\n salvo la cronologia del terminale", false); }
                File_interface.overwrite_file(File_interface.TERMINAL_LOG, CentralTerminal_panel.get_terminal_log());

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("done\n salvo la lista di dns e ca", false); }
                save_dns_ca();

                if (Database.DEBUG) { CentralTerminal_panel.terminal_write("done\n", false); }
                File_interface.close();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void save_server_list() throws IllegalBlockSizeException, IOException, BadPaddingException {
            String new_file_txt = "";
            for (String key : Database.serverList.keySet()) {
                new_file_txt += key + ";" + Database.serverList.get(key).el1 + ";" + Database.serverList.get(key).el2 + "\n";
            }

            if (!new_file_txt.equals("")) {
                File_interface.overwrite_file(File_interface.SERVER_LIST, new_file_txt);
            }
        }

        private void save_dns_ca() throws IllegalBlockSizeException, IOException, BadPaddingException {
            String new_file_txt = "";
            for (String ip : Database.DNS_CA_KEY.keySet()) { //per ogni dns riconosciuto
                new_file_txt += ip + ";" + Database.DNS_CA_KEY.get(ip).el2 + "\n"; //aggiunge al file la linea ip;Base64(pub_key)
            }

            if (!new_file_txt.equals("")) { //se è stato aggiunto qualcosa, rimuove \n final
                new_file_txt = new_file_txt.substring(0, new_file_txt.length() - 1);
            }

            File_interface.overwrite_file(File_interface.DNS_CA_LIST, new_file_txt); //salva i dati aggiornati sul file
        }
    });
}