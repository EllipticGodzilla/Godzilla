package gui.custom;

import gui.graphicsSettings.GraphicsSettings;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Vector;

class GMenu extends JMenu {
    private final String COLOR_SETTINGS_BASE;
    private final Vector<GMenuItem> MENU_ITEMS = new Vector<>();

    public GMenu(String txt, String settings_base) {
        super(txt);
        this.COLOR_SETTINGS_BASE = settings_base;
        this.setBackground((Color) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_selected_background"));
        this.setForeground((Color) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_foreground"));
        this.getPopupMenu().setBorder((Border) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_dropdown_border"));
    }

    public void update_colors() {
        this.setBackground((Color) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_selected_background"));
        this.getPopupMenu().setBorder((Border) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_dropdown_border"));

        if (isSelected()) {
            this.setForeground((Color) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_selected_foreground"));
        } else {
            this.setForeground((Color) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_foreground"));
        }

        for (GMenuItem item : MENU_ITEMS) {
            item.update_colors();
        }
    }

    @Override
    public void setSelected(boolean b) {
        if (b) {
            this.setOpaque(true);
            this.setForeground((Color) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_selected_foreground"));
        } else {
            this.setOpaque(false);
            this.setForeground((Color) GraphicsSettings.active_option.get_value(COLOR_SETTINGS_BASE + "_foreground"));
        }
        this.repaint();
    }

    @Override
    public JMenuItem add(JMenuItem menuItem) {
        if (menuItem instanceof GMenuItem) {
            MENU_ITEMS.add((GMenuItem) menuItem);
        }

        return super.add(menuItem);
    }
}
