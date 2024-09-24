package gui.settingsFrame;

import gui.custom.*;
import gui.graphicsSettings.GraphicsSettings;
import gui.settingsFrame.file_settings_panels.ColorSettings_panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public abstract class Settings_panel {
    private static final JPanel CONTENT_PANEL = new JPanel();
    private static final CascateMenu MENU_PANEL = new CascateMenu();
    private static final GScrollPane MENU_SCROLL_PANE = new GScrollPane(MENU_PANEL);
    private static final JPanel MAIN_PANEL = new JPanel();

    public static void init() {
        MAIN_PANEL.setLayout(new GridBagLayout());
        CONTENT_PANEL.setLayout(new GridBagLayout());

        MAIN_PANEL.setOpaque(false);
        CONTENT_PANEL.setOpaque(false);

        CONTENT_PANEL.setPreferredSize(new Dimension(550, 0));

        Color list_bg = (Color) GraphicsSettings.active_theme.get_value("list_background");
        MENU_SCROLL_PANE.getViewport().setBackground(list_bg);
        MENU_SCROLL_PANE.setBorder(BorderFactory.createLineBorder(list_bg.darker()));
        MENU_SCROLL_PANE.setPreferredSize(new Dimension(250, 0));
        MENU_SCROLL_PANE.set_scrollbar_thickness(10);

        //aggiorna i colori quando si cambia tema
        GraphicsSettings.run_at_theme_change(Settings_panel::update_color);

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        MAIN_PANEL.add(MENU_SCROLL_PANE, c);

        c.weightx = 1;
        c.gridx = 1;
        MAIN_PANEL.add(CONTENT_PANEL, c);

        add_std_settings();
    }

    public static void update_color() {
        MENU_SCROLL_PANE.update_colors();
        MENU_PANEL.updated_color();
    }

    public static JPanel load() {
        MENU_PANEL.unselect();

        return MAIN_PANEL;
    }

    public static void reset() {
        CONTENT_PANEL.removeAll();
    }

    public static JPanel get_content_panel() {
        CONTENT_PANEL.removeAll();
        return CONTENT_PANEL;
    }

    public static void remove_menu(String name) {
        MENU_PANEL.remove_item(name);
    }

    public static void add_menu(String name, ActionListener branch_action) {
        add_menu(name, false, branch_action);
    }

    public static void add_menu(String name, boolean have_submenu, ActionListener branch_action) {
        MENU_PANEL.add_item(name, have_submenu, branch_action);
    }

    private static void add_std_settings() {
        ColorSettings_panel.init();

        add_menu("color schemes", _ -> ColorSettings_panel.load());
    }
}