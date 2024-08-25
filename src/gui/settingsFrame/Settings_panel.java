package gui.settingsFrame;

import gui.custom.*;
import gui.settingsFrame.file_settings_panels.ColorSettings_panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public abstract class Settings_panel {
    private static final JPanel CONTENT_PANEL = new JPanel();
    private static final CascateMenu MENU_PANEL = new CascateMenu();
    private static final JPanel MAIN_PANEL = new JPanel();

    public static void init() {
        MAIN_PANEL.setLayout(new GridBagLayout());
        CONTENT_PANEL.setLayout(new GridBagLayout());

        MAIN_PANEL.setBackground(SettingsFrame.background);
        CONTENT_PANEL.setBackground(SettingsFrame.background);

        CONTENT_PANEL.setPreferredSize(new Dimension(550, 0));

        GScrollPane menu_scroller = new GScrollPane(MENU_PANEL);
        menu_scroller.setPreferredSize(new Dimension(250, 0));
        menu_scroller.set_scrollbar_thickness(10);

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        MAIN_PANEL.add(menu_scroller, c);

        c.weightx = 1;
        c.gridx = 1;
        MAIN_PANEL.add(CONTENT_PANEL, c);

        add_std_settings();
    }

    public static JPanel load() {
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