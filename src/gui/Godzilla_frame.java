package gui;

import gui.custom.GFrame;
import gui.custom.GLayeredPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class Godzilla_frame {
    private static JPanel temp_panel = null;

    //ricorda quali pannelli erano attivi prima di aprire una temp window in modo da riattivarli una volta chiusa
    public static final int SERVER_LIST = 0;
    public static final int CLIENT_LIST = 1;
    public static final int BUTTON_TOPBAR = 2;

    private static boolean active = true;
    private static final boolean[] ACTIVE_PANELS = new boolean[] {true, false, false};

    private static GFrame godzilla_frame = null;

    public static JFrame init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException { //inizializza la schermata e ritorna il JFrame
        if (godzilla_frame == null) {
            godzilla_frame = new GFrame("Godzilla - Client");

            //inizializza tutti i pannelli che formeranno la gui principale
            //tutti i pannelli da aggiungere al frame
            JPanel server_list = ServerList_panel.init();
            JPanel client_list = ClientList_panel.init();
            JPanel button_topbar = ButtonTopBar_panel.init();
            JPanel central_terminal = Central_panel.init();
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
            content_panel.add(server_list, c);

            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.weighty = 0.6; //selection panel deve essere un po' più alto rispetto a connection panel
            content_panel.add(client_list, c);

            c.weightx = 0.78; //i due pannelli sulla destra, devono essere più lunghi e conpensare i due pannelli sulla sinistra

            c.gridx = 1;
            c.gridy = 0;
            c.gridheight = 1;
            c.weighty = 0; //la top bar non viene ridimensionata per le y
            content_panel.add(button_topbar, c);

            c.gridx = 1;
            c.gridy = 1;
            c.weighty = 1; //per compensare la top bar
            c.gridheight = 2;
            content_panel.add(central_terminal, c);

            GLayeredPane lp = new GLayeredPane();
            content_panel.setBounds(0, 0, 900, 663);
            lp.add_fullscreen(content_panel, JLayeredPane.DEFAULT_LAYER);
            lp.add(temp_panel, JLayeredPane.POPUP_LAYER);

            godzilla_frame.setLayeredPane(lp);
            int menu_height = godzilla_frame.init_title_bar();
            lp.set_menu_height(menu_height - 2);

            Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
            godzilla_frame.setLocation(
                    screen_size.width/2 - godzilla_frame.getWidth()/2,
                    screen_size.height/2 - godzilla_frame.getHeight()/2
            );

            godzilla_frame.setVisible(true);

            //mantiene gui.TempPanel sempre al centro del frame
            godzilla_frame.addComponentListener(new ComponentListener() {
                @Override
                public void componentMoved(ComponentEvent e) {}
                @Override
                public void componentShown(ComponentEvent e) {}
                @Override
                public void componentHidden(ComponentEvent e) {}

                @Override
                public void componentResized(ComponentEvent e) {
                    TempPanel.recenter_in_frame();
                }
            });
        }
        return godzilla_frame;
    }

    public static void update_colors() {
        godzilla_frame.update_colors();
    }

    public static void set_title(String title) {
        Godzilla_frame.godzilla_frame.setTitle(title);
    }

    protected static boolean enabled() {
        return active;
    }

    protected static void disable_panels() { //disabilita tutti i pannelli quando si apre gui.TempPanel
        active = false;

        ServerList_panel.setEnabled(false);
        ClientList_panel.setEnabled(false);
        ButtonTopBar_panel.setEnabled(false);
    }

    public static void disable_panel(int panel) {
        ACTIVE_PANELS[panel] = false;
    }

    public static void enable_panel(int panel) {
        ACTIVE_PANELS[panel] = true;
    }

    protected static void enable_panels() { //riattiva i pannelli una volta chiusa gui.TempPanel
        active = true;

        ServerList_panel.setEnabled(ACTIVE_PANELS[SERVER_LIST]);
        ClientList_panel.setEnabled(ACTIVE_PANELS[CLIENT_LIST]);
        ButtonTopBar_panel.setEnabled(ACTIVE_PANELS[BUTTON_TOPBAR]);
    }

    public static Rectangle get_bounds() {
        return godzilla_frame.getBounds();
    }
}

