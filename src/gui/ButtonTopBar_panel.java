package gui;

import file_database.File_interface;
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
    private static Map<String, Runnable> stop_activities = new LinkedHashMap<>();
    private static Map<String, Method> method_map = new LinkedHashMap<>();

    private static JPanel buttons_panel = null;
    protected static JPanel init() throws IOException {
        if (buttons_container == null) {
            buttons_panel = new JPanel();
            buttons_panel.setLayout(new GridBagLayout());

            //inizializza tutti i componenti della gui
            JButton right_shift = new JButton();
            JButton left_shift = new JButton();
            JButton stop = new JButton();
            buttons_container = new JPanel();
            buttons_scroller = new JScrollPane(buttons_container, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            stop.setEnabled(false);
            stop.setPreferredSize(new Dimension(30, 30));
            buttons_container.add(stop);

            right_shift.setIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/right_arrow.png")));
            right_shift.setRolloverIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/right_arrow_sel.png")));
            right_shift.setPressedIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/right_arrow_pres.png")));
            left_shift.setIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/left_arrow.png")));
            left_shift.setRolloverIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/left_arrow_sel.png")));
            left_shift.setPressedIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/left_arrow_pres.png")));
            stop.setIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/power_off.png")));
            stop.setRolloverIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/power_off_sel.png")));
            stop.setPressedIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/power_off_pres.png")));
            stop.setDisabledIcon(new ImageIcon(ButtonTopBar_panel.class.getResource("/images/power_off_dis.png")));

            right_shift.setBorder(null);
            left_shift.setBorder(null);
            stop.setBorder(null);
            buttons_scroller.setBorder(null);

            right_shift.addActionListener(right_shift_listener);
            left_shift.addActionListener(left_shift_listener);
            stop.addActionListener(stop_listener);

            buttons_container.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
            buttons_container.setBackground(new Color(58, 61, 63));

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

            buttons_panel.setPreferredSize(new Dimension(0, 30));
        }
        return buttons_panel;
    }

    public static void setEnabled(boolean enabled) {
        buttons_container.setEnabled(enabled);
        for (Component c : buttons_container.getComponents()) { //disabilita tutti i bottoni registrati
            c.setEnabled(enabled);
        }
    }

    public static void init_buttons() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CentralTerminal_panel.terminal_write("inizializzo i bottoni nella gui:", false);
        File buttons_folder = new File(File_interface.jar_path + "/mod");
        String[] button_class_files = buttons_folder.list();

        class Button_class extends ClassLoader {
            public Class find_class(String class_name) throws IOException {
                byte[] class_data = new FileInputStream(File_interface.jar_path + "/mod/" + class_name + ".class").readAllBytes();
                return defineClass(class_name, class_data, 0, class_data.length); //define class
            }
        }
        Button_class class_gen = new Button_class();

        for (String file_name : button_class_files) {
            CentralTerminal_panel.terminal_write("\n   inizializzo - " + file_name + ": ", false);

            if (file_name.substring(file_name.length() - 6, file_name.length()).equals(".class")) { //se è un file .class
                String class_name = file_name.substring(0, file_name.length() - 6);
                Class button_class = class_gen.find_class(class_name);

                //imposta la funzione "public static void on_press(String name)" all'interno della classe per essere invocata una volta premuto un pulsante
                ButtonTopBar_panel.on_press = button_class.getDeclaredMethod("on_press", String.class);

                //all'interno della classe dovrà essere definita una funzione "public static void register_button()" che viene invocata ora per far registrare tutti i bottoni
                button_class.getDeclaredMethod("register_button").invoke(null);
            }
        }
        CentralTerminal_panel.terminal_write(" - finito\n", false);
    }

    private static Method on_press;
    public static void register_button(ButtonInfo info, Runnable stop) {
        CentralTerminal_panel.terminal_write("pulsante registrato!\n", false);

        JButton button = new JButton();
        button.setToolTipText(info.name);

        button.setIcon(info.default_icon);
        button.setRolloverIcon(info.rollover_icon);
        button.setPressedIcon(info.pressed_icon);
        button.setDisabledIcon(info.disabled_icon);

        button.addActionListener(new ActionListener() {
            private final Method on_press = ButtonTopBar_panel.on_press;
            private final String name = button.getToolTipText();
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ButtonTopBar_panel.active_mod.equals("")) { //se non c'è nessun'altra mod attiva al momento
                    Connection.write(
                            ("start_mod:" + name).getBytes(),
                            (conv_code, msg) -> {
                                try {
                                    if (new String(msg).equals("start")) { //se ha accettato di utilizzare questa mod
                                        TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, Connection.get_paired_usr() + " ha accettato la mod"), null);

                                        ButtonTopBar_panel.active_mod = name;
                                        on_press.invoke(null, name);
                                    } else { //se non è stato accettato di utilizzare questa mod
                                        TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, Connection.get_paired_usr() + " non ha accettato la mod"), null);
                                    }
                                } catch (Exception ex) {}
                            });
                    }
            }
        });

        button.setPreferredSize(new Dimension(info.default_icon.getIconWidth(), info.default_icon.getIconHeight()));
        button.setBorder(null);
        button.setEnabled(false);

        stop_activities.put(info.name, stop); //registra il metodo per stoppare l'azione di questo pulsante
        method_map.put(info.name, ButtonTopBar_panel.on_press);

        buttons_container.add(button);
        buttons_scroller.updateUI();
        buttons_scroller.setBorder(null); //altrimenti con updateUI() si mostra il bordo
    }

    public static void start_mod(String name) throws InvocationTargetException, IllegalAccessException {
        if (active_mod.equals("")) { //se nessuna mod è attiva
            active_mod = name;
            method_map.get(name).invoke(null, name);
        }
    }

    public static void end_mod(boolean notify_pClient) {
        if (!ButtonTopBar_panel.active_mod.equals("")) { //se c'è effettivamente una mod attiva
            stop_activities.get(ButtonTopBar_panel.active_mod).run();
            ButtonTopBar_panel.active_mod = "";
            CentralTerminal_panel.reset_panel();

            if (notify_pClient) {
                Connection.write("EOM");
            }
        }
    }

    private static ActionListener left_shift_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            buttons_scroller.getHorizontalScrollBar().setValue(
                    buttons_scroller.getHorizontalScrollBar().getValue() - 30
            );
        }
    };

    private static ActionListener right_shift_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            buttons_scroller.getHorizontalScrollBar().setValue(
                    buttons_scroller.getHorizontalScrollBar().getValue() + 30
            );
        }
    };

    private static ActionListener stop_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            end_mod(true);
        }
    };
}
