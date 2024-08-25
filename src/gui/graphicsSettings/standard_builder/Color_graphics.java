package gui.graphicsSettings.standard_builder;

import files.Logger;
import gui.custom.ColorPanel;
import gui.custom.GIntegerField;
import gui.settingsFrame.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Color_graphics implements GraphicsOption_builder<Color> {
    private static final Pattern COLOR_PATTERN = Pattern.compile("color\\(([0-9]+),([0-9]+),([0-9]+)\\)");

    @Override
    public Color cast(String value_str) {
        Matcher matcher = COLOR_PATTERN.matcher(value_str);

        if (matcher.matches()) {
            Color color;

            try {
                color = new Color(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
            }
            catch (Exception _) {
                Logger.log("impossibile inizializzare il colore dalle info: " + value_str);
                return null;
            }

            return color;
        }

        Logger.log("impossibile comprendere la formattazione del colore: " + value_str);
        return null;
    }

    @Override
    public String revert_cast(Object value) {
        Color color = (Color) value;
        return "color(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    @Override
    public void display(JPanel panel, Object value) {
        Color color = (Color) value;
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        //definizione degli elementi
        GIntegerField red_spinner = new GIntegerField(SettingsFrame.background.brighter(), SettingsFrame.foreground, SettingsFrame.background.darker().darker(), -1, 255);
        GIntegerField blue_spinner = new GIntegerField(SettingsFrame.background.brighter(), SettingsFrame.foreground, SettingsFrame.background.darker().darker(), -1, 255);
        GIntegerField green_spinner = new GIntegerField(SettingsFrame.background.brighter(), SettingsFrame.foreground, SettingsFrame.background.darker().darker(), -1, 255);

        JLabel r_label = new JLabel("r:");
        JLabel g_label = new JLabel("g:");
        JLabel b_label = new JLabel("b:");

        ColorPanel ex_color = new ColorPanel();
        JPanel filler = new JPanel();

        //inizializzo
        red_spinner.set_value(r);
        green_spinner.set_value(g);
        blue_spinner.set_value(b);

        r_label.setForeground(SettingsFrame.foreground);
        g_label.setForeground(SettingsFrame.foreground);
        b_label.setForeground(SettingsFrame.foreground);

        ex_color.set_color(r, g, b);

        filler.setOpaque(false);

        red_spinner.on_validation(() -> {
            int val = Integer.parseInt(red_spinner.getText());
            Color c_bg = ex_color.getBackground();

            ex_color.set_color(val, c_bg.getGreen(), c_bg.getBlue());
            ex_color.repaint();
        });

        green_spinner.on_validation(() -> {
            int val = Integer.parseInt(green_spinner.getText());
            Color c_bg = ex_color.getBackground();

            ex_color.set_color(c_bg.getRed(), val, c_bg.getBlue());
            ex_color.repaint();
        });

        blue_spinner.on_validation(() -> {
            int val = Integer.parseInt(blue_spinner.getText());
            Color c_bg = ex_color.getBackground();

            ex_color.set_color(c_bg.getRed(), c_bg.getGreen(), val);
            ex_color.repaint();
        });

        //aggiungo al pannello
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 10);
        c.weightx = 0;
        c.weighty = 0;
        panel.add(r_label, c);

        c.gridx = 1;
        panel.add(red_spinner, c);

        c.gridx = 2;
        panel.add(g_label, c);

        c.gridx = 3;
        panel.add(green_spinner, c);

        c.gridx = 4;
        panel.add(b_label, c);

        c.gridx = 5;
        panel.add(blue_spinner, c);

        c.gridx = 6;
        panel.add(ex_color, c);

        c.gridx = 7;
        c.weightx = 1;
        panel.add(filler, c);
    }

    @Override
    public void update(JPanel panel, Object value) {
        Color color = (Color) value;
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        ((GIntegerField) panel.getComponent(1)).set_value(r);
        ((GIntegerField) panel.getComponent(3)).set_value(g);
        ((GIntegerField) panel.getComponent(5)).set_value(b);
        ((ColorPanel) panel.getComponent(6)).set_color(r, g, b);
    }

    @Override
    public Color new_value(JPanel panel) {
        int red, green, blue;

        red = ((GIntegerField) panel.getComponent(1)).get_value();
        green = ((GIntegerField) panel.getComponent(3)).get_value();
        blue = ((GIntegerField) panel.getComponent(5)).get_value();

        if (red < 0 || green < 0 || blue < 0) //colore non valido
            return null;

        return new Color(red, green, blue);
    }
}
