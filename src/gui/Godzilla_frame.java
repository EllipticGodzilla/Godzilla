package gui;

import gui.custom.GLayeredPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class Godzilla_frame {
    //tutti i pannelli da aggiungere al frame
    private static JPanel server_list = null;
    private static JPanel client_list = null;
    private static JPanel button_topbar = null;
    private static JLayeredPane central_terminal = null;
    private static JPanel temp_panel = null;

    //ricorda quali pannelli erano attivi prima di aprire una temp window in modo da riattivarli una volta chiusa
    public static final int SERVER_LIST = 0;
    public static final int CLIENT_LIST = 1;
    public static final int BUTTON_TOPBAR = 2;

    private static boolean active = true;
    private static boolean[] active_panel = new boolean[] {true, false, false};

    private static JFrame godzilla_frame = null;

    public static JFrame init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException { //inizializza la schermata e ritorna il JFrame
        if (godzilla_frame == null) {
            godzilla_frame = new JFrame("Godzilla - Client");
            godzilla_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            godzilla_frame.setMinimumSize(new Dimension(900, 500));

            //inizializza tutti i pannelli che formeranno la gui principale
            server_list = ServerList_panel.init();
            client_list = ClientList_panel.init();
            button_topbar = ButtonTopBar_panel.init();
            central_terminal = CentralTerminal_panel.init();
            temp_panel = TempPanel.init();

            //inizializza la gui principale (tutti i pannelli tranne Temp Panels)
            JPanel content_panel = new JPanel();
            content_panel.setBackground(new Color(58, 61, 63));
            content_panel.setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.22; //i due pannelli sulla sinistra, devono essere più corti rispetto a quelli sulla destra

            c.gridx = 0;
            c.gridy = 2;
            c.gridheight = 1;
            c.weighty = 0.4; //deve compensare per selection panel
            c.insets = new Insets(5, 10, 10, 5);
            content_panel.add(server_list, c);

            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.weighty = 0.6; //selection panel deve essere un po' più alto rispetto a connection panel
            c.insets = new Insets(10, 10, 5, 5);
            content_panel.add(client_list, c);

            c.weightx = 0.78; //i due pannelli sulla destra, devono essere più lunghi e conpensare i due pannelli sulla sinistra

            c.gridx = 1;
            c.gridy = 0;
            c.gridheight = 1;
            c.weighty = 0; //la top bar non viene ridimensionata per le y
            c.insets = new Insets(10, 5, 5, 10);
            content_panel.add(button_topbar, c);

            c.gridx = 1;
            c.gridy = 1;
            c.weighty = 1; //per compensare la top bar
            c.insets = new Insets(5, 5, 10, 10);
            c.gridheight = 2;
            content_panel.add(central_terminal, c);

            GLayeredPane lp = new GLayeredPane();
            content_panel.setBounds(0, 0, 900, 663);
            lp.add_fullscreen(content_panel, JLayeredPane.DEFAULT_LAYER);
            lp.add(temp_panel, JLayeredPane.POPUP_LAYER);

            godzilla_frame.setLayeredPane(lp);
            godzilla_frame.setVisible(true);

            //mantiene TempPanel sempre al centro del frame
            godzilla_frame.addComponentListener(new ComponentListener() {
                @Override
                public void componentMoved(ComponentEvent e) {}
                @Override
                public void componentShown(ComponentEvent e) {}
                @Override
                public void componentHidden(ComponentEvent e) {}

                @Override
                public void componentResized(ComponentEvent e) {
                    recenter_temp_panel();
                }
            });
        }
        return godzilla_frame;
    }

    public static void set_title(String title) {
        Godzilla_frame.godzilla_frame.setTitle(title);
    }

    protected static boolean is_enabled(int panel) {
        return active_panel[panel];
    }

    protected static boolean enabled() {
        return active;
    }

    protected static void disable_panels() { //disabilita tutti i pannelli quando si apre TempPanel
        active = false;

        ServerList_panel.setEnabled(false);
        ClientList_panel.setEnabled(false);
        ButtonTopBar_panel.setEnabled(false);
    }

    public static void disable_panel(int panel) {
        active_panel[panel] = false;
    }

    public static void enable_panel(int panel) {
        active_panel[panel] = true;
    }

    protected static void enable_panels() { //riattiva i pannelli una volta chiusa TempPanel
        active = true;

        ServerList_panel.setEnabled(active_panel[SERVER_LIST]);
        ClientList_panel.setEnabled(active_panel[CLIENT_LIST]);
        ButtonTopBar_panel.setEnabled(active_panel[BUTTON_TOPBAR]);
    }

    protected static void recenter_temp_panel() {
        temp_panel.setLocation(
                godzilla_frame.getWidth() / 2 - temp_panel.getWidth() / 2,
                godzilla_frame.getHeight() / 2 - temp_panel.getHeight() / 2
        );
    }
}

