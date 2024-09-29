import files.Database;
import files.File_cipher;
import files.File_interface;
import files.Logger;
import gui.*;
import gui.graphicsSettings.GraphicsSettings;
import gui.settingsFrame.SettingsFrame;
import network.Server_manager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Vector;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Logger.log("================================== Client Started ==================================");
        Runtime.getRuntime().addShutdownHook(shut_down);

        File_interface.init();
        GraphicsSettings.load_from_files();
        JFrame main_frame = Godzilla_frame.init();
        File_cipher.init();
        SettingsFrame.init();
        Server_manager.init_encoder();

        //imposta l'icona del main frame
        Vector<Image> icons = new Vector<>();

        icons.add(new ImageIcon(Main.class.getResource("/images/icon_16.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("/images/icon_32.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("/images/icon_64.png")).getImage());
        icons.add(new ImageIcon(Main.class.getResource("/images/icon_128.png")).getImage());

        main_frame.setIconImages(icons);

        //chiede la password per decifrare i file cifrati
        TempPanel.show(new TempPanel_info(
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
                    File_cipher.init_ciphers(hash); //inizializza File_cipher

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

    private static final Thread shut_down = new Thread(() -> {
        try {
            Server_manager.close(true); //se è ancora connesso a un server si disconnette

            if (File_interface.initialized) {
                File_interface.update_files();
                File_interface.close();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    });
}