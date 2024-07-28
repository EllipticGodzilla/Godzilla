package gui;

import file_database.Database;
import file_database.File_interface;
import file_database.Pair;
import gui.custom.GList;
import gui.custom.GScrollPane;
import network.Connection;
import network.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ServerList_panel extends Database {

    private static JButton connect;
    private static JButton disconnect;
    private static JButton add_server;
    private static JButton add_dns;
    private static GList server_list; //rispetto a JList viene modificata la grafica ed inserito un popup per rinominare ed eliminare server dalla lista

    private static JPanel serverL_panel = null;
    protected static JPanel init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        if (serverL_panel == null) {
            serverL_panel = new JPanel();
            serverL_panel.setBackground(new Color(58, 61, 63));
            serverL_panel.setLayout(new GridBagLayout());

            connect = new JButton();
            disconnect = new JButton();
            add_server = new JButton();
            add_dns = new JButton();
            server_list = new GList();
            GScrollPane server_scroller = new GScrollPane(server_list); //rispetto a JScrollPane viene modificata la grafica

            disconnect.setEnabled(false);

            server_scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //aggiunge i popup menu ai componenti della lista
            server_list.set_popup(CellPopupMenu.class);

            //inizializza tutti i componenti della gui

            connect.setBorder(null);
            disconnect.setBorder(null);
            add_server.setBorder(null);
            add_dns.setBorder(null);

            connect.setIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_on.png")));
            connect.setRolloverIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_on_sel.png")));
            connect.setPressedIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_on_pres.png")));
            connect.setDisabledIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_on_dis.png")));
            disconnect.setIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_off.png")));
            disconnect.setRolloverIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_off_sel.png")));
            disconnect.setPressedIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_off_pres.png")));
            disconnect.setDisabledIcon(new ImageIcon(ServerList_panel.class.getResource("/images/power_off_dis.png")));
            add_server.setIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_server.png")));
            add_server.setRolloverIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_server_sel.png")));
            add_server.setPressedIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_server_pres.png")));
            add_server.setDisabledIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_server_dis.png")));
            add_dns.setIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_dns.png")));
            add_dns.setRolloverIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_dns_sel.png")));
            add_dns.setPressedIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_dns_pres.png")));
            add_dns.setDisabledIcon(new ImageIcon(ServerList_panel.class.getResource("/images/add_dns_dis.png")));

            connect.setPreferredSize(new Dimension(30, 30));
            disconnect.setPreferredSize(new Dimension(30, 30));
            add_server.setPreferredSize(new Dimension(95, 30));
            add_dns.setPreferredSize(new Dimension(73, 30));

            connect.addActionListener(connect_listener);
            disconnect.addActionListener(disconnect_listener);
            add_server.addActionListener(add_server_listener);
            add_dns.addActionListener(add_dns_listener);

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

            c.weightx = 1; //spacing fra i bottoni per disconnettersi ed aggiungere un pulsante
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.VERTICAL;
            c.anchor = GridBagConstraints.FIRST_LINE_END;
            c.insets = new Insets(0, 5, 5, 5);
            serverL_panel.add(add_dns, c);

            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 4;
            c.insets = new Insets(5, 0, 0, 0);
            serverL_panel.add(server_scroller, c);

            serverL_panel.setPreferredSize(new Dimension(0, 0));

        }
        return serverL_panel;
    }

    public static void setEnabled(boolean enabled) {
        if (Connection.isClosed()) { //se non è connesso a nessun server il pulsante disconnect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            connect.setEnabled(enabled);
        }
        else { //se è connesso ad un server il pulsante connect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            disconnect.setEnabled(enabled);
        }
        serverL_panel.setEnabled(enabled);
        add_server.setEnabled(enabled);
        server_list.setEnabled(enabled);
        add_dns.setEnabled(enabled);
    }

    public static void update_gui() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        for (String name : Database.serverList.keySet()) { //aggiunge alla lista tutti i server caricati sul database
            server_list.add(name);
        }
    }

    public static void update_button() {
        if (Godzilla_frame.enabled()) { //se i pulsanti dovrebbero essere attivi
            connect.setEnabled(Connection.isClosed());
            disconnect.setEnabled(!Connection.isClosed());
        }
    }

    private static ActionListener add_server_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            error_name_ip.success();
        }

        private TempPanel_action name_and_ip = new TempPanel_action() {
            @Override
            public void success()  {
                try {
                    Pattern ip_pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");

                    if (valid_server_name(input.elementAt(1)) && (ip_pattern.matcher(input.elementAt(0)).matches() || valid_server_link(input.elementAt(0)))) {
                        Database.serverList.put(input.elementAt(1), new Pair<>(input.elementAt(0), input.elementAt(2))); //aggiunge indirizzo e nome alla mappa serverList
                        server_list.add(input.elementAt(1)); //aggiunge il nome del server alla JList rendendolo visibile
                    } else {
                        TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "il nome o indirizzo inseriti non sono validi, inserire nome ed indirizzo validi"), error_name_ip);
                    }
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void fail() {} //non si vuole più aggiungere un server
        };

        private TempPanel_action error_name_ip = new TempPanel_action() {
            @Override
            public void success() {
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.INPUT_REQ,
                        true,
                        "inserisci il link al server:",
                        "inserisci il nome del server:",
                        "inserisci l'ip del dns:"
                ).set_combo_box(
                        new int[] {2},
                        Database.DNS_CA_KEY.keySet().toArray(new String[0])
                ), name_and_ip);
            }

            @Override
            public void fail() {} //essendo un messaggio non può essere premuto il tasto "annulla"
        };

    };

    private static ActionListener add_dns_listener = e -> {
        File_interface.req_dns_ca.success(); //richiede le informazioni per il nuovo dns
    };

    private static ActionListener connect_listener = e -> {
        Pair<String, String> server_info = Database.serverList.get(server_list.getSelectedValue());
        if (server_info != null) { //se è effttivamente selezionato un server
            String link = server_info.el1;

            CentralTerminal_panel.terminal_write("connessione con il server: " + link + "\n", false);
            Server.start_connection_with(link, server_info.el2);
            Server.server_name = server_list.getSelectedValue();
        }
    };

    private static ActionListener disconnect_listener = e -> {
        Server.disconnect(true);
    };

    private static boolean valid_server_name(String new_server_name) { //controlla che il nome assegnato al server sia unico
        for (String name : Database.serverList.keySet()) {
            if (name.equals(new_server_name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean valid_server_link(String link) {
        Pattern link_pat = Pattern.compile("[a-zA-Z]+\\.gz");
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
            UIManager.put("MenuItem.selectionForeground", new Color(158, 161, 163));

            JMenuItem rename = new JMenuItem("rename");
            JMenuItem remove = new JMenuItem("remove");
            JMenuItem info = new JMenuItem("info");

            this.setBorder(BorderFactory.createLineBorder(new Color(28, 31, 33)));
            rename.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
            remove.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
            info.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));

            rename.setBackground(new Color(88, 91, 93));
            remove.setBackground(new Color(88, 91, 93));
            info.setBackground(new Color(88, 91, 93));
            rename.setForeground(new Color(158, 161, 163));
            remove.setForeground(new Color(158, 161, 163));
            info.setForeground(new Color(158, 161, 163));

            rename.addActionListener(rename_listener);
            remove.addActionListener(remove_listener);
            info.addActionListener(info_listener);

            this.add(rename);
            this.add(remove);
            this.add(info);
        }

        private TempPanel_action rename_action = new TempPanel_action() {
            @Override
            public void success() {
                if (valid_server_name(input.elementAt(0)) && !input.elementAt(0).equals(cell_name)) {
                    CentralTerminal_panel.terminal_write("rinomino il server \"" + cell_name + "\" in \"" + input.elementAt(0) + "\"\n", false);
                    PARENT_LIST.rename_element(cell_name, input.elementAt(0)); //modifica il nome nella lista visibile

                    Database.serverList.put( //modifica il nome nel database
                            input.elementAt(0),
                            Database.serverList.get(cell_name)
                    );
                    Database.serverList.remove(cell_name);

                    cell_name = input.elementAt(0); //modifica il nome per questo popup
                } else {
                    TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "inserisci un nome valido ed unico fra tutti i server"), rename_fail);
                }
            }

            @Override
            public void fail() {} //non si vuole più rinominare il server
        };

        private TempPanel_action rename_fail = new TempPanel_action() {
            @Override
            public void success() {
                TempPanel.show(new TempPanel_info(TempPanel_info.INPUT_REQ, true, "inserisci il nuovo nome per il server: " + cell_name), rename_action);
            }

            @Override
            public void fail() {} //essendo un messaggio non può "fallire"
        };

        private TempPanel_action remove_confirm = new TempPanel_action() {
            @Override
            public void success() {
                CentralTerminal_panel.terminal_write("rimuovo il server \"" + cell_name + "\"\n", false);

                Database.serverList.remove(cell_name); //rimuove il server dal database
                PARENT_LIST.remove(cell_name); //rimuove il server dalla lista visibile
            }

            @Override
            public void fail() {} //non vuole più rimuovere il server
        };

        private ActionListener rename_listener = (e) -> {
            TempPanel.show(new TempPanel_info(TempPanel_info.INPUT_REQ, true, "inserisci il nuovo nome per il server: " + cell_name), rename_action);
        };

        private ActionListener remove_listener = (e) -> {
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, true, "il server " + cell_name + " verrà rimosso"), remove_confirm);
        };

        private ActionListener info_listener = (e) -> {
            Pair<String, String> server_info = Database.serverList.get(cell_name);
            String ip_link = server_info.el1; //trova il link o l'ip del server a cui si riferisce questa casella
            String dns_ip = server_info.el2;

            if (Server.ip.equals(ip_link) || Server.link.equals(ip_link)) { //se è connesso a questo server
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.DOUBLE_COL_MSG,
                        false,
                        "dns registered name:", Server.registered_name,
                        "client name:", cell_name,
                        "ip:", Server.ip,
                        "link:", Server.link,
                        "mai:", Server.mail,
                        "dns link:", Server.dns_ip,
                        "pubblic key:", Base64.getEncoder().encodeToString(Server.pub_key),
                        "certificate:", Base64.getEncoder().encodeToString(Server.ce)
                ), null);
            }
            else { //se non è connesso a questo server le uniche informazioni che conosce sono nome e link/ip/dns_ip
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.DOUBLE_COL_MSG,
                        false,
                        "name:", cell_name,
                        "link:", ip_link,
                        "dns ip:", dns_ip
                ), null);
            }
        };
    }
}