package gui.custom;

import gui.graphicsSettings.GraphicsSettings;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Vector;

class GMenu extends JMenu {
    private final Vector<GMenuItem> MENU_ITEMS = new Vector<>();

    public GMenu(String txt) {
        super(txt);

        this.setOpaque(false);
        this.setBackground((Color) GraphicsSettings.active_theme.get_value("dropdown_selected_background"));
        this.setForeground((Color) GraphicsSettings.active_theme.get_value("dropdown_text_color"));
        this.getPopupMenu().setBorder((Border) GraphicsSettings.active_theme.get_value("dropdown_border"));
        this.getPopupMenu().setBackground((Color) GraphicsSettings.active_theme.get_value("dropdown_background"));
    }

    public void update_colors() {
        this.getPopupMenu().setBorder((Border) GraphicsSettings.active_theme.get_value("dropdown_border"));
        this.getPopupMenu().setBackground((Color) GraphicsSettings.active_theme.get_value("dropdown_background"));
        setSelected(isSelected()); //aggiorna tutti i colori

        for (GMenuItem item : MENU_ITEMS) {
            item.update_colors();
        }
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected) {
            this.setOpaque(true);
            this.setBackground((Color) GraphicsSettings.active_theme.get_value("dropdown_selected_background"));
            this.setForeground((Color) GraphicsSettings.active_theme.get_value("dropdown_selected_text_color"));
        } else {
            this.setOpaque(false);
            this.setForeground((Color) GraphicsSettings.active_theme.get_value("dropdown_text_color"));
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
