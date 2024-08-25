package gui.custom;

import gui.graphicsSettings.GraphicsSettings;

import javax.swing.*;
import java.awt.*;

//    MENU ITEM GRAPHICS
class GMenuItem extends JMenuItem {
    private final String GRAPHICS_SETTING_BASE;

    public GMenuItem(String txt, String settings_base) {
        super(txt);
        this.GRAPHICS_SETTING_BASE = settings_base;
        this.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        this.setBackground((Color) GraphicsSettings.active_option.get_value(GRAPHICS_SETTING_BASE  + "_dropdown_background"));
        this.setForeground((Color) GraphicsSettings.active_option.get_value(GRAPHICS_SETTING_BASE + "_dropdown_foreground"));
    }

    public void update_colors() {
        update_colors(this.isSelected());
    }

    @Override
    public void menuSelectionChanged(boolean isIncluded) {
        update_colors(isIncluded);
    }

    private void update_colors(boolean active) {
        if (active) {
            this.setBackground((Color) GraphicsSettings.active_option.get_value(GRAPHICS_SETTING_BASE + "_dropdown_selected_background"));
            this.setForeground((Color) GraphicsSettings.active_option.get_value(GRAPHICS_SETTING_BASE + "_dropdown_selected_foreground"));
        } else {
            this.setBackground((Color) GraphicsSettings.active_option.get_value(GRAPHICS_SETTING_BASE + "_dropdown_background"));
            this.setForeground((Color) GraphicsSettings.active_option.get_value(GRAPHICS_SETTING_BASE + "_dropdown_foreground"));
        }
    }
}
