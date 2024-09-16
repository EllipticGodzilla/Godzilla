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
import java.util.Vector;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Logger.log("================================== Client Started ==================================");
        Runtime.getRuntime().addShutdownHook(shut_down);

        File_interface.init();
        GraphicsSettings.load_files();
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
    }

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