package gui.settingsFrame;

import gui.Godzilla_frame;
import gui.graphicsSettings.ButtonIcons;
import gui.graphicsSettings.GraphicsSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class SettingsFrame {
    private static final JFrame frame = new JFrame();
    private static int current_settings = -1;

    private static final JButton ok_button = new JButton();
    private static final JButton cancel_button = new JButton();
    private static final JButton apply_button = new JButton();

    public static final int FILE_MANAGER = 0;
    public static final int SETTINGS = 1;
    public static final int MOD_MANAGER = 2;
    public static final int STARTMOD_MANAGER = 3;
    public static final int SERVER_MANAGER = 4;
    public static final int DNS_MANAGER = 5;

    public static void init() {
        FileManager_panel.init();
        Settings_panel.init();

        //inizializza i pulsanti ok, cancel, apply e lo spacer che divide il pannello con il contenuto dai pulsanti
        update_color();
        GraphicsSettings.run_at_theme_change(SettingsFrame::update_color);

        ok_button.setPreferredSize(new Dimension(95, 20));
        cancel_button.setPreferredSize(new Dimension(95, 20));
        apply_button.setPreferredSize(new Dimension(95, 20));

        ok_button.setBorder(null);
        cancel_button.setBorder(null);
        apply_button.setBorder(null);

        ok_button.setOpaque(false);
        cancel_button.setOpaque(false);
        apply_button.setOpaque(false);
        ok_button.setContentAreaFilled(false);
        cancel_button.setContentAreaFilled(false);
        apply_button.setContentAreaFilled(false);

        cancel_button.addActionListener(_ -> close.run());

        JPanel sep = new JPanel();
        sep.setBackground(Color.BLACK);
        sep.setBorder(null);

        sep.setPreferredSize(new Dimension(0, 2));

        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setBorder(null);

        //inizializza il frame ed aggiunge tutti i componenti
        frame.getContentPane().setLayout(new GridBagLayout());
        frame.setMinimumSize(new Dimension(800, 420));

        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent windowEvent) {}
            @Override
            public void windowIconified(WindowEvent windowEvent) {}
            @Override
            public void windowDeiconified(WindowEvent windowEvent) {}
            @Override
            public void windowActivated(WindowEvent windowEvent) {}
            @Override
            public void windowDeactivated(WindowEvent windowEvent) {}
            @Override
            public void windowClosed(WindowEvent windowEvent) {}

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                close.run();
            }
        });

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 4;
        c.insets = new Insets(0, 0, 5, 0);
        c.weightx = 1;
        c.weighty = 0;
        frame.getContentPane().add(sep, c);

        c.gridy = 2;
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 0, 0);
        frame.getContentPane().add(spacer, c);

        c.gridx = 1;
        c.insets = new Insets(0, 0, 5, 5);
        c.weightx = 0;
        frame.getContentPane().add(ok_button, c);

        c.gridx = 2;
        frame.getContentPane().add(cancel_button, c);

        c.gridx = 3;
        frame.getContentPane().add(apply_button, c);
    }

    public static void update_color() {
        frame.getContentPane().setBackground((Color) GraphicsSettings.active_theme.get_value("frame_background"));

        ButtonIcons ok_icons = (ButtonIcons) GraphicsSettings.active_theme.get_value("settings_ok");
        ButtonIcons annulla_icons = (ButtonIcons) GraphicsSettings.active_theme.get_value("settings_annulla");
        ButtonIcons apply_icons = (ButtonIcons) GraphicsSettings.active_theme.get_value("settings_apply");

        ok_button.setIcon(ok_icons.getStandardIcon());
        ok_button.setRolloverIcon(ok_icons.getRolloverIcon());
        ok_button.setPressedIcon(ok_icons.getPressedIcon());
        cancel_button.setIcon(annulla_icons.getStandardIcon());
        cancel_button.setRolloverIcon(annulla_icons.getRolloverIcon());
        cancel_button.setPressedIcon(annulla_icons.getPressedIcon());
        apply_button.setIcon(apply_icons.getStandardIcon());
        apply_button.setRolloverIcon(apply_icons.getRolloverIcon());
        apply_button.setPressedIcon(apply_icons.getPressedIcon());
    }

    public static void show(int type) {
        if (current_settings == -1) { //se non sta mostrando nulla al momento
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 4;
            c.weightx = 1;
            c.weighty = 1;

            JPanel panel = switch (type) {
                case FILE_MANAGER -> FileManager_panel.load();
                case SETTINGS -> Settings_panel.load();

                default -> null;
            };

            if (panel != null) { //se è stato inserito un tipo di pannello valido
                frame.getContentPane().add(panel, c);

                Rectangle frame_bounds = Godzilla_frame.get_bounds();
                frame.setLocation(
                        (int) (frame_bounds.getX() + (frame_bounds.getWidth() - frame.getSize().width) / 2),
                        (int) (frame_bounds.getY() + (frame_bounds.getHeight() - frame.getSize().height) / 2)
                );

                current_settings = type;
                frame.setVisible(true);
            }
            else {} //viene ignorato
        }
        else { //se sta già visualizzando un pannello
            frame.requestFocus(); //porta il frame in primo piano
            frame.setAlwaysOnTop(true);
            frame.setAlwaysOnTop(false);
        }
    }

    public static void hide() {
        close.run();
    }

    public static final int OK_BUTTON = 0;
    public static final int APPLY_BUTTON = 1;
    public static boolean set_action_listener(int button, ActionListener listener) {
        if (button == OK_BUTTON) {
            if (ok_button.getActionListeners().length == 0) { //se non ne ha già registrati
                ok_button.addActionListener(listener);
                return true;
            }
            return false;
        }
        else if (button == APPLY_BUTTON){
            if (apply_button.getActionListeners().length == 0) {
                apply_button.addActionListener(listener);
                return true;
            }
            return false;
        }
        else {
            return false;
        }
    }

    private static final Runnable close = () -> { //resetta il pannello visualizzato al momento
        //ha sempre solo massimo un action listener
        if (ok_button.getActionListeners().length != 0) {
            ok_button.removeActionListener(ok_button.getActionListeners()[0]);
        }
        if (apply_button.getActionListeners().length != 0) {
            apply_button.removeActionListener(apply_button.getActionListeners()[0]);
        }

        switch (current_settings) {
            case FILE_MANAGER -> FileManager_panel.reset();
            case SETTINGS ->  Settings_panel.reset();

            //default impossibile raggiungerlo
        }

        frame.getContentPane().remove(frame.getContentPane().getComponents().length - 1); //il pannello è sempre l'ultimo oggetto aggiunto al frame

        current_settings = -1;
        if (frame.isVisible()) { frame.setVisible(false); }
    };
}

