package gui;

import files.Database;
import files.Logger;
import gui.custom.GList;
import gui.custom.GScrollPane;
import gui.graphicsSettings.ButtonIcons;
import gui.graphicsSettings.GraphicsSettings;
import network.Server_info;
import network.Server_manager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

public abstract class ServerList_panel extends Database {

    private static JButton connect;
    private static JButton disconnect;
    private static JButton add_server;
    private static GList server_list; //rispetto a JList viene modificata la grafica ed inserito un popup per rinominare ed eliminare server dalla lista

    private static JPanel serverL_panel = null;
    protected static JPanel init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        if (serverL_panel == null) {
            serverL_panel = new JPanel();
            serverL_panel.setBackground((Color) GraphicsSettings.active_option.get_value("server_panel_background"));
            serverL_panel.setLayout(new GridBagLayout());
            serverL_panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 5));

            connect = new JButton();
            disconnect = new JButton();
            add_server = new JButton();
            server_list = new GList(
                    (Color) GraphicsSettings.active_option.get_value("server_panel_list_background"),
                    (Color) GraphicsSettings.active_option.get_value("server_panel_list_foreground"),
                    (Color) GraphicsSettings.active_option.get_value("server_panel_list_selected_background"),
                    (Color) GraphicsSettings.active_option.get_value("server_panel_list_selected_foreground"),
                    (Border) GraphicsSettings.active_option.get_value("server_panel_list_selected_border")
            );
            GScrollPane server_scroller = new GScrollPane(server_list); //rispetto a JScrollPane viene modificata la grafica

            disconnect.setEnabled(false);

            server_scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //aggiunge i popup menu ai componenti della lista
            server_list.set_popup(CellPopupMenu.class);

            //inizializza tutti i componenti della gui
            connect.setBorder(null);
            disconnect.setBorder(null);
            add_server.setBorder(null);

            connect.addActionListener(CONNECT_LISTENER);
            disconnect.addActionListener(disconnect_listener);
            add_server.addActionListener(ADDSERVER_LISTENER);

            connect.setOpaque(false);
            disconnect.setOpaque(false);
            add_server.setOpaque(false);
            connect.setContentAreaFilled(false);
            disconnect.setContentAreaFilled(false);
            add_server.setContentAreaFilled(false);

            JPanel sep = new JPanel();
            sep.setBorder(null);

            //aggiunge tutti i componenti al pannello organizzando la gui
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;

            c.weightx = 0; //i tre pulsanti per connettersi, disconnettersi, aggiungere un server non si allungano ne sulle x ne sulle y
            c.weighty = 0;

            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 5, 5);
            serverL_panel.add(disconnect, c);

            c.gridx = 2;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 5);
            serverL_panel.add(add_server, c);

            c.gridx = 3;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 0);
            serverL_panel.add(connect, c);

            c.weightx = 1; //spacing fra i bottoni
            c.gridx = 1;
            c.insets = new Insets(0, 0, 0, 0);
            serverL_panel.add(sep, c);

            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 4;
            c.insets = new Insets(5, 0, 0, 0);
            serverL_panel.add(server_scroller, c);

            serverL_panel.setPreferredSize(new Dimension(0, 0));
            update_colors();
        }
        return serverL_panel;
    }

    public static void update_colors() {
        serverL_panel.setBackground((Color) GraphicsSettings.active_option.get_value("server_panel_background"));
        serverL_panel.getComponents()[3].setBackground((Color) GraphicsSettings.active_option.get_value("server_panel_background")); //cambia il colore del separatore
        server_list.change_colors(
                (Color) GraphicsSettings.active_option.get_value("server_panel_list_background"),
                (Color) GraphicsSettings.active_option.get_value("server_panel_list_foreground"),
                (Color) GraphicsSettings.active_option.get_value("server_panel_list_selected_background"),
                (Color) GraphicsSettings.active_option.get_value("server_panel_list_selected_foreground"),
                (Border) GraphicsSettings.active_option.get_value("server_panel_list_selected_border")
        );

        ButtonIcons connect_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("server_panel_connect");
        ButtonIcons disconnect_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("server_panel_disconnect");
        ButtonIcons add_server_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("server_panel_add_server");

        connect.setIcon(connect_icons.getStandardIcon());
        connect.setRolloverIcon(connect_icons.getRolloverIcon());
        connect.setPressedIcon(connect_icons.getPressedIcon());
        connect.setDisabledIcon(connect_icons.getDisabledIcon());
        disconnect.setIcon(disconnect_icons.getStandardIcon());
        disconnect.setRolloverIcon(disconnect_icons.getRolloverIcon());
        disconnect.setPressedIcon(disconnect_icons.getPressedIcon());
        disconnect.setDisabledIcon(disconnect_icons.getDisabledIcon());
        add_server.setIcon(add_server_icons.getStandardIcon());
        add_server.setRolloverIcon(add_server_icons.getRolloverIcon());
        add_server.setPressedIcon(add_server_icons.getPressedIcon());
        add_server.setDisabledIcon(add_server_icons.getDisabledIcon());
    }

    public static void setEnabled(boolean enabled) {
        if (Server_manager.is_connected()) { //se non è connesso a nessun server il pulsante disconnect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            connect.setEnabled(enabled);
        }
        else { //se è connesso ad un server il pulsante connect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            disconnect.setEnabled(enabled);
        }
        serverL_panel.setEnabled(enabled);
        add_server.setEnabled(enabled);
        server_list.setEnabled(enabled);
    }

    public static void update_gui() {
        for (String name : Database.server_list.keySet()) { //aggiunge alla lista tutti i server caricati sul database
            server_list.add(name);
        }
    }

    public static void clear() {
        server_list.clear();
    }

    public static void update_button_enable() {
        if (Godzilla_frame.enabled()) { //se i pulsanti dovrebbero essere attivi
            connect.setEnabled(Server_manager.is_connected());
            disconnect.setEnabled(!Server_manager.is_connected());
        }
    }

    private static final ActionListener ADDSERVER_LISTENER = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    true,
                    "link:",
                    "porta:",
                    "nome:",
                    "encoder:",
                    "indirizzo ip del dns:"
            ).set_combo_box(
                    new int[] {3, 4},
                    Server_manager.get_encoders_list(),
                    Database.dns_ca_key.keySet().toArray(new String[0])
            ), ADD_SERVER_ACTION);
        }

        private final TempPanel_action ADD_SERVER_ACTION = new TempPanel_action() {
            @Override
            public void success()  {
                String link = (String) input.elementAt(0);
                String ip = null;
                String port_str = (String) input.elementAt(1);
                int port;
                String name = (String) input.elementAt(2);
                String encoder = (String) input.elementAt(3);
                String dns_ip = (String) input.elementAt(4);

                try {
                    port = Integer.parseInt(port_str);
                }
                catch (NumberFormatException _) { //non è stato inserito un numero per porta
                    Logger.log("tentativo di aggiungere un server con come porta una stringa: " + port_str, true);

                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "inserire un numero valido come valore per la porta"
                    ), null);

                    fail();

                    return;
                }

                //se è stato specificato un ip e non il link del server
                Pattern ip_pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
                if (ip_pattern.matcher(link).matches()) {
                    ip = link;
                    link = null;
                }

                //crea l'oggetto con le informazioni del server
                Server_info info = new Server_info(link, ip, port, dns_ip, encoder);

                if (valid_server_name(name) && (link == null || valid_server_link(link))) {
                    //aggiunge il nuovo server al database e alla lista in ServerList_panel
                    Database.server_list.put(name, info);
                    server_list.add(name); //aggiunge il nome del server alla JList rendendolo visibile
                }
                else {
                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "il nome o indirizzo inseriti non sono validi, inserire nome ed indirizzo validi"
                    ), null);

                    fail();
                }
            }

            @Override
            public void fail() {
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.INPUT_REQ,
                        true,
                        "inserisci il link al server:",
                        "inserisci il nome del server:",
                        "inserisci l ip del dns:"
                ).set_combo_box(
                        new int[] {2},
                        Database.dns_ca_key.keySet().toArray(new String[0])
                ), ADD_SERVER_ACTION);
            }
        };
    };

    private static final ActionListener CONNECT_LISTENER = _ -> {
        String server_name = server_list.getSelectedValue();
        if (!server_name.isEmpty()) { //se è effettivamente selezionato un server
            Logger.log("tento la connessione con il server: " + server_name);

            Server_manager.connect_to(server_name);
        }
    };

    private static final ActionListener disconnect_listener = _ -> {
        Server_manager.close(true);
    };

    private static boolean valid_server_name(String new_server_name) { //controlla che il nome assegnato al server sia unico
        for (String name : Database.server_list.keySet()) {
            if (name.equals(new_server_name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean valid_server_link(String link) {
        Pattern link_pat = Pattern.compile("[a-zA-Z_]+\\.gz");
        return link_pat.matcher(link).matches();
    }

    public static class CellPopupMenu extends JPopupMenu {
        private String cell_name;
        private final GList PARENT_LIST;

        public CellPopupMenu(String name, GList list) {
            super();
            this.cell_name = name;
            this.PARENT_LIST = list;

            UIManager.put("MenuItem.selectionBackground", new Color(108, 111, 113));
            UIManager.put("MenuItem.selectionForeground", Color.lightGray);

            JMenuItem rename = new JMenuItem("rename");
            JMenuItem remove = new JMenuItem("remove");
            JMenuItem info = new JMenuItem("info");

            Border item_border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(78, 81, 83)),
                    BorderFactory.createEmptyBorder(4, 2, 0, 0)
            );
            this.setBorder(BorderFactory.createLineBorder(new Color(28, 31, 33)));
            rename.setBorder(item_border);
            remove.setBorder(item_border);
            info.setBorder(item_border);

            rename.setBackground(new Color(88, 91, 93));
            remove.setBackground(new Color(88, 91, 93));
            info.setBackground(new Color(88, 91, 93));
            rename.setForeground(Color.lightGray);
            remove.setForeground(Color.lightGray);
            info.setForeground(Color.lightGray);

            rename.addActionListener(RENAME_LISTENER);
            remove.addActionListener(REMOVE_LISTENER);
            info.addActionListener(INFO_LISTENER);

            this.add(rename);
            this.add(remove);
            this.add(info);
        }

        private final TempPanel_action RENAME_ACTION = new TempPanel_action() {
            @Override
            public void success() {
                String new_name = (String) input.elementAt(0);

                if (valid_server_name(new_name) && !new_name.equals(cell_name)) {
                    Logger.log("rinomino il server: " + cell_name + " in: " + new_name);
                    PARENT_LIST.rename_element(cell_name, new_name); //modifica il nome nella lista visibile

                    //rinomina il server nella lista Database.server_list
                    Database.server_list.put(
                            new_name,
                            Database.server_list.get(cell_name)
                    );
                    Database.server_list.remove(cell_name);

                    cell_name = new_name; //modifica il nome per questo popup
                } else {
                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "inserisci un nome valido ed unico fra tutti i server"
                    ), null);

                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.INPUT_REQ,
                            true,
                            "inserisci il nuovo nome per il server: " + cell_name
                    ), RENAME_ACTION);
                }
            }

            @Override
            public void fail() {} //non si vuole più rinominare il server
        };

        private final TempPanel_action REMOVE_CONFIRM_ACTION = new TempPanel_action() {
            @Override
            public void success() {
                Logger.log("rimuovo il server: " + cell_name + " dalla lista dei server memorizzati");

                Database.server_list.remove(cell_name); //rimuove il server dal database
                PARENT_LIST.remove(cell_name); //rimuove il server dalla lista visibile
            }

            @Override
            public void fail() {} //non vuole più rimuovere il server
        };

        private final ActionListener RENAME_LISTENER = _ -> {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    true,
                    "inserisci il nuovo nome per il server: " + cell_name
            ), RENAME_ACTION);
        };

        private final ActionListener REMOVE_LISTENER = _ -> {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    true,
                    "il server " + cell_name + " verrà rimosso"
            ), REMOVE_CONFIRM_ACTION);
        };

        private final ActionListener INFO_LISTENER = _ -> {
            Server_info info = Database.server_list.get(cell_name);

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.DOUBLE_COL_MSG,
                    false,
                    "nome:", cell_name,
                    "ip:", (info.get_ip() == null)? "not defined" : info.get_ip(),
                    "link:", (info.get_link() == null)? "not defined" : info.get_link(),
                    "porta:", Integer.toString(info.get_port()),
                    "dns ip:", info.get_dns_ip(),
                    "encoder:", info.get_encoder_name()
            ), null);
        };
    }
}