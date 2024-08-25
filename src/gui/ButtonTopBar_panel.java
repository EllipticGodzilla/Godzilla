package gui;

import files.File_interface;
import files.Logger;
import gui.graphicsSettings.ButtonIcons;
import gui.graphicsSettings.GraphicsSettings;
import network.Connection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ButtonTopBar_panel {
    public static String active_mod = "";

    private static JPanel buttons_container;
    private static JScrollPane buttons_scroller;
    private static final Map<String, Runnable> STOP_ACTIVITIES = new LinkedHashMap<>();
    private static final Map<String, Method> METHOD_MAP = new LinkedHashMap<>();

    private static JButton left_shift;
    private static JButton right_shift;
    private static JButton stop_mod;

    private static JPanel buttons_panel = null;
    protected static JPanel init() throws IOException {
        if (buttons_container == null) {
            buttons_panel = new JPanel();
            buttons_panel.setBackground((Color) GraphicsSettings.active_option.get_value("button_top_bar_background"));
            buttons_panel.setLayout(new GridBagLayout());
            buttons_panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

            //inizializza tutti i componenti della gui
            right_shift = new JButton();
            left_shift = new JButton();
            stop_mod = new JButton();
            buttons_container = new JPanel();
            buttons_scroller = new JScrollPane(buttons_container, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            stop_mod.setEnabled(false);
            buttons_container.add(stop_mod);

            update_colors();

            right_shift.setBorder(null);
            left_shift.setBorder(null);
            stop_mod.setBorder(null);
            buttons_scroller.setBorder(null);

            right_shift.addActionListener(RIGHTSHIFT_LISTENER);
            left_shift.addActionListener(LEFTSHIFT_LISTENER);
            stop_mod.addActionListener(STOP_LISTENER);

            right_shift.setOpaque(false);
            left_shift.setOpaque(false);
            stop_mod.setOpaque(false);
            right_shift.setContentAreaFilled(false);
            left_shift.setContentAreaFilled(false);
            stop_mod.setContentAreaFilled(false);

            buttons_container.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

            //aggiunge tutti i componenti al pannello organizzandoli nella griglia
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.gridy = 0;
            c.weightx = 0; //i due pulsanti non vengono ridimensionati

            c.gridx = 0;
            buttons_panel.add(left_shift, c);

            c.gridx = 2;
            buttons_panel.add(right_shift, c);

            c.weightx = 1;

            c.gridx = 1;
            buttons_panel.add(buttons_scroller, c);
        }
        return buttons_panel;
    }

    public static void update_colors() {
        buttons_container.setBackground((Color) GraphicsSettings.active_option.get_value("button_top_bar_background"));
        buttons_panel.setBackground((Color) GraphicsSettings.active_option.get_value("button_top_bar_background"));

        ButtonIcons right_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("button_top_bar_right_shift");
        ButtonIcons left_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("button_top_bar_left_shift");
        ButtonIcons stop_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("button_top_bar_stop_mod");

        right_shift.setIcon(right_icons.getStandardIcon());
        right_shift.setRolloverIcon(right_icons.getRolloverIcon());
        right_shift.setPressedIcon(right_icons.getPressedIcon());
        right_shift.setDisabledIcon(right_icons.getDisabledIcon());
        left_shift.setIcon(left_icons.getStandardIcon());
        left_shift.setRolloverIcon(left_icons.getRolloverIcon());
        left_shift.setPressedIcon(left_icons.getPressedIcon());
        left_shift.setDisabledIcon(left_icons.getDisabledIcon());
        stop_mod.setIcon(stop_icons.getStandardIcon());
        stop_mod.setRolloverIcon(stop_icons.getRolloverIcon());
        stop_mod.setPressedIcon(stop_icons.getPressedIcon());
        stop_mod.setDisabledIcon(stop_icons.getDisabledIcon());
    }

    public static void setEnabled(boolean enabled) {
        buttons_container.setEnabled(enabled);
        for (Component c : buttons_container.getComponents()) { //disabilita tutti i bottoni registrati
            c.setEnabled(enabled);
        }
    }

    public static void init_buttons() {
        Logger.log("inizio a caricare tutti file con le classi delle mod");

        File buttons_folder = new File(File_interface.jar_path + "/mod");
        String[] button_class_files = buttons_folder.list();

        if (button_class_files != null) {
            class Button_class extends ClassLoader {
                public Class<?> find_class(String class_name) {
                    try {
                        byte[] class_data = new FileInputStream(File_interface.jar_path + "/mod/" + class_name + ".class").readAllBytes();
                        return defineClass(class_name, class_data, 0, class_data.length); //define class
                    }
                    catch (IOException _) {
                        Logger.log("impossibile inizializzare la mod: " + class_name + ", il file non esiste", true, '\n');
                        return null;
                    }
                }
            }
            Button_class class_gen = new Button_class();

            for (String file_name : button_class_files) {
                Logger.log("inizializzo la mod contenuta nel file: " + file_name);

                if (file_name.endsWith(".class")) { //se è un file .class
                    try {
                        String class_name = file_name.substring(0, file_name.length() - 6);
                        Class<?> button_class = class_gen.find_class(class_name);

                        //imposta la funzione "public static void on_press(String name)" all'interno della classe per essere invocata una volta premuto un pulsante
                        ButtonTopBar_panel.on_press = button_class.getDeclaredMethod("on_press", String.class);

                        //all'interno della classe dovrà essere definita una funzione "public static void register_button()" che viene invocata ora per far registrare tutti i bottoni
                        button_class.getDeclaredMethod("register_button").invoke(null);
                    }
                    catch (NoSuchMethodException _) {
                        Logger.log("impossibile caricare la mod nel file: " + file_name + ", non è presente il metodo 'register_button'", true, '\n');
                    } catch (InvocationTargetException | IllegalAccessException _) {
                        Logger.log("impossibile invocare il metodo 'register_button' per la mod in: " + file_name, true, '\n');
                    }
                }
            }
        }
        Logger.log("tutte le mod sono state inizializzate");
    }

    private static Method on_press;
    public static void register_button(ButtonInfo info, Runnable stop) {
        JButton button = new JButton();
        button.setToolTipText(info.name);

        Logger.log("aggiunto un nuovo pulsante al ButtonPanel: " + info.name);

        button.setIcon(info.default_icon);
        button.setRolloverIcon(info.rollover_icon);
        button.setPressedIcon(info.pressed_icon);
        button.setDisabledIcon(info.disabled_icon);

        button.addActionListener(new ActionListener() {
            private final Method on_press = ButtonTopBar_panel.on_press;
            private final String name = button.getToolTipText();
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ButtonTopBar_panel.active_mod.isEmpty()) { //se non c'è nessun'altra mod attiva al momento
                    Connection.write(
                            ("SOM:" + name).getBytes(),
                            (_, msg) -> {
                                try {
                                    if (new String(msg).equals("start")) { //se ha accettato di utilizzare questa mod
                                        TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, Connection.get_paired_usr() + " ha accettato la mod"), null);

                                        ButtonTopBar_panel.active_mod = name;
                                        on_press.invoke(null, name);
                                    } else { //se non è stato accettato di utilizzare questa mod
                                        TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, Connection.get_paired_usr() + " non ha accettato la mod"), null);
                                    }
                                } catch (Exception _) {}
                            });
                    }
            }
        });

        button.setPreferredSize(new Dimension(info.default_icon.getIconWidth(), info.default_icon.getIconHeight()));
        button.setBorder(null);
        button.setEnabled(false);

        STOP_ACTIVITIES.put(info.name, stop); //registra il metodo per stoppare l'azione di questo pulsante
        METHOD_MAP.put(info.name, ButtonTopBar_panel.on_press);

        buttons_container.add(button);
        buttons_scroller.updateUI();
        buttons_scroller.setBorder(null); //altrimenti con updateUI() si mostra il bordo
    }

    public static void start_mod(String name) {
        if (active_mod.isEmpty()) { //se nessuna mod è attiva
            try {
                active_mod = name;
                METHOD_MAP.get(name).invoke(null, name);
            }
            catch (IllegalAccessException | InvocationTargetException _) {
                Logger.log("impossibile invocare il metodo per attivare la mod: " + name, true, '\n');
            }
        }
    }

    public static void end_mod(boolean notify_pClient) {
        if (!ButtonTopBar_panel.active_mod.isEmpty()) { //se c'è effettivamente una mod attiva
            STOP_ACTIVITIES.get(ButtonTopBar_panel.active_mod).run();
            ButtonTopBar_panel.active_mod = "";

            Central_panel.get_programmable_panel().removeAll();
            Central_panel.get_programmable_panel().setVisible(false);

            if (notify_pClient) {
                Connection.write("EOM");
            }
        }
    }

    private static final ActionListener LEFTSHIFT_LISTENER = _ -> {
        buttons_scroller.getHorizontalScrollBar().setValue(
                buttons_scroller.getHorizontalScrollBar().getValue() - 30
        );
    };

    private static final ActionListener RIGHTSHIFT_LISTENER = _ -> {
        buttons_scroller.getHorizontalScrollBar().setValue(
                buttons_scroller.getHorizontalScrollBar().getValue() + 30
        );
    };

    private static final ActionListener STOP_LISTENER = _ -> {
        end_mod(true);
    };
}
