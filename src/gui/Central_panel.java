package gui;

import gui.custom.GLayeredPane;
import gui.graphicsSettings.GraphicsSettings;

import javax.swing.*;
import java.awt.*;

public abstract class Central_panel {
    private static final ImagePanel IMAGE_PANEL = new ImagePanel(new ImageIcon(Central_panel.class.getResource("/images/godzilla.png")));
    private static final JPanel PROGRAMMABLE_PANEL = new JPanel();
    private static GLayeredPane layeredPane = null;
    private static final JPanel MAIN_PANEL = new JPanel();

    protected static JPanel init() {
        if (layeredPane == null) {
            layeredPane = new GLayeredPane();
            MAIN_PANEL.setLayout(new GridLayout(1, 0));
            MAIN_PANEL.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 10));

            update_colors();

            PROGRAMMABLE_PANEL.setVisible(false);
            PROGRAMMABLE_PANEL.setLayout(null);

            layeredPane.add_fullscreen(PROGRAMMABLE_PANEL, JLayeredPane.DEFAULT_LAYER);
            layeredPane.add_fullscreen(IMAGE_PANEL, JLayeredPane.POPUP_LAYER);

            MAIN_PANEL.add(layeredPane);
        }
        return MAIN_PANEL;
    }

    public static void update_colors() {
        MAIN_PANEL.setBackground((Color) GraphicsSettings.active_option.get_value("central_panel_background"));
        IMAGE_PANEL.setBackground((Color) GraphicsSettings.active_option.get_value("central_panel_image_background"));
        IMAGE_PANEL.set_icon((ImageIcon) GraphicsSettings.active_option.get_value("central_panel_icon"));
    }

    public static JPanel get_programmable_panel() {
        return PROGRAMMABLE_PANEL;
    }
}

class ImagePanel extends JPanel {
    private ImageIcon image;

    public ImagePanel(ImageIcon image) {
        this.image = image;
        this.setBorder(null);
    }

    public void set_icon(ImageIcon image) {
        this.image = image;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        image.paintIcon(
                this,
                g,
                this.getWidth()/2 - image.getIconWidth()/2,
                this.getHeight()/2 - image.getIconHeight()/2
        );
    }
}