package gui;

import files.Database;
import files.Logger;
import gui.custom.GList;
import gui.custom.GScrollPane;
import gui.graphicsSettings.ButtonIcons;
import gui.graphicsSettings.GraphicsSettings;
import network.Connection;
import network.On_arrival;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

public abstract class ClientList_panel extends Database {
    private static JButton connect = null;
    private static JButton disconnect = null;
    private static GList clients_list = null;
    private static JPanel client_panel = null;

    protected static JPanel init() throws IOException {
        if (client_panel == null) {
            client_panel = new JPanel();
            client_panel.setLayout(new GridBagLayout());
            client_panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

            //inizializza tutti i componenti della gui
            connect = new JButton();
            disconnect = new JButton();
            clients_list = new GList(
                    (Color) GraphicsSettings.active_option.get_value("client_panel_list_background"),
                    (Color) GraphicsSettings.active_option.get_value("client_panel_list_foreground"),
                    (Color) GraphicsSettings.active_option.get_value("client_panel_list_selected_background"),
                    (Color) GraphicsSettings.active_option.get_value("client_panel_list_selected_foreground"),
                    (Border) GraphicsSettings.active_option.get_value("client_panel_list_selected_border")
            );
            GScrollPane clients_scroller = new GScrollPane(clients_list);
            JPanel spacer = new JPanel();

            disconnect.setEnabled(false);

            spacer.setFocusable(false);

            connect.setBorder(null);
            disconnect.setBorder(null);
            spacer.setBorder(null);

            connect.addActionListener(try_pair);
            disconnect.addActionListener(disconnect_list);

            connect.setOpaque(false);
            disconnect.setOpaque(false);
            connect.setContentAreaFilled(false);
            disconnect.setContentAreaFilled(false);

            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0; //i due pulsanti non si ridimensionano
            c.weightx = 0;

            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 5, 5);
            client_panel.add(disconnect, c);

            c.gridx = 2;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 0);
            client_panel.add(connect, c);

            c.weightx = 1; //lo spacer dovrà allungarsi sulle x per permettere ai pulsanti di rimanere delle stesse dimensioni

            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 5, 5);
            client_panel.add(spacer, c);

            c.weighty = 1; //la lista di client dovrà allungarsi sulle y per compensare i pulsanti

            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 3;
            c.insets = new Insets(5, 0, 0, 0);
            client_panel.add(clients_scroller, c);

            connect.setEnabled(false);
            disconnect.setEnabled(false);
            clients_scroller.setPreferredSize(new Dimension(0, 0));
            update_colors();
        }

        return client_panel;
    }

    public static void update_colors() {
        client_panel.setBackground((Color) GraphicsSettings.active_option.get_value("client_panel_background"));
        client_panel.getComponents()[2].setBackground((Color) GraphicsSettings.active_option.get_value("client_panel_background"));
        clients_list.change_colors(
                (Color) GraphicsSettings.active_option.get_value("client_panel_list_background"),
                (Color) GraphicsSettings.active_option.get_value("client_panel_list_foreground"),
                (Color) GraphicsSettings.active_option.get_value("client_panel_list_selected_background"),
                (Color) GraphicsSettings.active_option.get_value("client_panel_list_selected_foreground"),
                (Border) GraphicsSettings.active_option.get_value("client_panel_list_selected_border")
        );

        ButtonIcons connect_icon = (ButtonIcons) GraphicsSettings.active_option.get_value("client_panel_connect");
        ButtonIcons disconnect_icon = (ButtonIcons) GraphicsSettings.active_option.get_value("client_panel_disconnect");

        connect.setIcon(connect_icon.getStandardIcon());
        connect.setRolloverIcon(connect_icon.getRolloverIcon());
        connect.setPressedIcon(connect_icon.getPressedIcon());
        connect.setDisabledIcon(connect_icon.getDisabledIcon());
        disconnect.setIcon(disconnect_icon.getStandardIcon());
        disconnect.setRolloverIcon(disconnect_icon.getRolloverIcon());
        disconnect.setPressedIcon(disconnect_icon.getPressedIcon());
        disconnect.setDisabledIcon(disconnect_icon.getDisabledIcon());
    }

    public static ActionListener try_pair = _ -> {
        On_arrival server_rep = (conv_code, msg) -> {
            if (new String(msg).equals("acc")) { //appaiamento accettato
                Connection.pair(clients_list.getSelectedValue());
                Logger.log("collegamento con il client: " + clients_list.getSelectedValue() + " instaurato con successo!");
                TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "collegamento con " + clients_list.getSelectedValue() + " instaurato con successo!"), null);

                Connection.write(conv_code, "acc".getBytes()); //appaiamento accettato
            }
            else { //appaiamento rifiutato
                Logger.log("il collegamento con " + clients_list.getSelectedValue() + " è sato rifiutato");
                TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "collegamento con " + clients_list.getSelectedValue() + " rifiutato"), null);
            }
        };

        String pair_usr = clients_list.getSelectedValue();
        if (!pair_usr.isEmpty() && !Connection.is_paired()) { //se è selezionato un client e non è appaiato con nessun altro client
            Connection.write(("pair:" + pair_usr).getBytes(), server_rep);
        }
        else if (Connection.is_paired()) {
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "impossibile collegarsi a più di un client"), null);
            Logger.log("tentativo di collegarsi ad un client mentre si è già collegati con: " + Connection.get_paired_usr(), true, '\n');
        }
        else if (pair_usr.isEmpty()) {
            TempPanel.show(new TempPanel_info(TempPanel_info.SINGLE_MSG, false, "selezionale il client a cui collegarsi"), null);
            Logger.log("non è stato selezionato nessun client a cui collegarsi dalla lista", true, '\n');
        }
    };

    public static ActionListener disconnect_list = e -> {
        Connection.unpair(true);
    };

    public static void setEnabled(boolean enabled) {
        if (Connection.is_paired()) { //se è appaiato il pulsante connect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            disconnect.setEnabled(enabled);
        }
        else { //se non è appaiato a nessun client il pulsante disconnect è disattivato, quindi non lo modifica qualsiasi sia il valore di enabled
            connect.setEnabled(enabled);
        }
        clients_list.setEnabled(enabled);
    }

    public static void update_buttons() {
        if (Godzilla_frame.enabled()) { //se i bottoni dovrebbero essere attivi (se non lo sono verranno attivati correttamente una volta chiuso il gui.TempPanel)
            disconnect.setEnabled(Connection.is_paired());
            connect.setEnabled(!Connection.is_paired());
        }
    }

    public static void update_client_list(String list) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        //elimina la lista precedente
        clients_list.reset_list();

        //imposta la nuova lista di client
        Pattern p = Pattern.compile(";");
        String[] names = p.split(list);

        for (String name : names) {
            if (!name.isEmpty()) {
                clients_list.add(name);
            }
        }
    }
}