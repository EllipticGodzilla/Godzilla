package gui.graphicsSettings.standard_builder;

import files.Logger;
import gui.custom.ColorPanel;
import gui.custom.GIntegerField;
import gui.settingsFrame.SettingsFrame;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Border_graphics implements GraphicsOption_builder<Border> {
    private static final Pattern BORDER_PATTERN = Pattern.compile("border\\((?:color\\(([0-9]+),([0-9]+),([0-9]+)\\))?,?([0-9]*),?(?:insets\\(([0-9]+),([0-9]+),([0-9]+),([0-9]+)\\))?\\)");
    private static Color_graphics color_graphics = new Color_graphics();

    @Override
    public boolean equals(Object obj1, Object obj2) {
        int[] info1 = get_border_info(obj1);
        int[] info2 = get_border_info(obj2);

        return Arrays.equals(info2, info1);
    }

    @Override
    public Border cast(String value_str) {
        Matcher matcher = BORDER_PATTERN.matcher(value_str);
        if (matcher.matches()) {
            Color color;
            int[] insets;
            int thickness;

            try {
                color = new Color(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
            }
            catch (NumberFormatException | IllegalStateException _) { color = null; }

            try {
                thickness = Integer.parseInt(matcher.group(4));
            }
            catch (IllegalStateException | NumberFormatException _) { thickness = 1; }

            try {
                insets = new int[] {
                        Integer.parseInt(matcher.group(5)),
                        Integer.parseInt(matcher.group(6)),
                        Integer.parseInt(matcher.group(7)),
                        Integer.parseInt(matcher.group(8)),
                };
            }
            catch (NumberFormatException | IllegalStateException _) { insets = null; }

            if (color == null && insets == null) {
                Logger.log("impossibile creare il bordo: " + value_str + ", almeno il colore o gli insets devono essere specificati", true, '\n');
                return null;
            }

            if (color == null)
                return BorderFactory.createEmptyBorder(insets[0], insets[1], insets[2], insets[3]);

            if (insets == null)
                return BorderFactory.createLineBorder(color, thickness);

            return BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color, thickness),
                    BorderFactory.createEmptyBorder(insets[0], insets[1], insets[2], insets[3])
            );
        }

        Logger.log("impossibile comprendere il valore del bordo: " + value_str, true, '\n');
        return null;
    }

    @Override
    public String revert_cast(Object value) {
        StringBuilder value_str = new StringBuilder("border(");
        int[] border_info = get_border_info(value);

        if (border_info[0] != -1) { //il colore è definito
            value_str.append("color(")
                    .append(border_info[0]).append(",")
                    .append(border_info[1]).append(",")
                    .append(border_info[2]).append(")");
        }

        if (border_info[3] != -1) { //insets è definito
            if (border_info[0] != -1) //anche il colore è definito
                value_str.append(",");

            value_str.append("insets(")
                    .append(border_info[3]).append(",")
                    .append(border_info[4]).append(",")
                    .append(border_info[5]).append(",")
                    .append(border_info[6]).append(")");
        }

        value_str.append(")");
        String result = value_str.toString();

        if (result.equals("border()")) { //se non è stato definito ne il colore ne gli insets
            return null;
        }
        else {
            return value_str.toString();
        }
    }

    @Override
    public void display(JPanel panel, Object value) {
        int[] border_info = get_border_info(value);

        add_color(panel, border_info);
        add_insets(panel, border_info);
    }

    @Override
    public void update(JPanel panel, Object value) {
        int[] border_info = get_border_info(value);

        set_color(panel, border_info);
        set_insets(panel, border_info);
    }

    @Override
    public Border new_value(JPanel panel) {
        Color color = color_graphics.new_value((JPanel) panel.getComponent(1));

        int top_insets = ((GIntegerField) panel.getComponent(4)).get_value();
        int left_insets = ((GIntegerField) panel.getComponent(6)).get_value();
        int bottom_insets = ((GIntegerField) panel.getComponent(8)).get_value();
        int right_insets = ((GIntegerField) panel.getComponent(10)).get_value();

        if (top_insets == -1 || left_insets == -1 || bottom_insets == -1 || right_insets == -1) {
            if (color == null) //bordo non valido
                return null;

            return BorderFactory.createLineBorder(color);
        }

        if (color == null)
            return BorderFactory.createEmptyBorder(top_insets, left_insets, bottom_insets, right_insets);

        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(top_insets, left_insets, bottom_insets, right_insets)
        );
    }

    private static int[] get_border_info(Object obj) {
        int[] info = new int[] {-1, -1, -1, -1, -1, -1, -1};

        Color color = null;
        Insets insets = null;
        if (obj instanceof CompoundBorder) {
            CompoundBorder border = (CompoundBorder) obj;

            color = ((LineBorder) border.getOutsideBorder()).getLineColor();
            insets = ((EmptyBorder) border.getInsideBorder()).getBorderInsets();
        }
        else if (obj instanceof LineBorder) {
            color = ((LineBorder) obj).getLineColor();
        }
        else if (obj instanceof EmptyBorder) {
            insets = ((EmptyBorder) obj).getBorderInsets();
        }

        if (color != null) {
            info[0] = color.getRed();
            info[1] = color.getGreen();
            info[2] = color.getBlue();
        }

        if (insets != null) {
            info[3] = insets.top;
            info[4] = insets.left;
            info[5] = insets.bottom;
            info[6] = insets.right;
        }

        return info;
    }

    private void add_color(JPanel panel, int[] border_info) {
        int r = border_info[0];
        int g = border_info[1];
        int b = border_info[2];

        JLabel color_label = new JLabel("color =");
        JPanel color_filler = new JPanel();

        color_label.setForeground(SettingsFrame.foreground);
        color_filler.setOpaque(false);
        color_filler.setLayout(new GridBagLayout());

        color_graphics.display(color_filler, Color.black);

        ((GIntegerField) color_filler.getComponent(1)).set_value(r);
        ((GIntegerField) color_filler.getComponent(3)).set_value(g);
        ((GIntegerField) color_filler.getComponent(5)).set_value(b);
        ((ColorPanel) color_filler.getComponent(6)).set_color(r, g, b);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 10, 10);
        c.fill = GridBagConstraints.BOTH;
        panel.add(color_label, c);

        c.gridx = 1;
        c.gridwidth = 9;
        c.weightx = 1;
        c.insets.right = 0;
        panel.add(color_filler, c);
    }

    private void set_color(JPanel panel, int[] border_info) {
        int r = border_info[0];
        int g = border_info[1];
        int b = border_info[2];

        JPanel color_panel = (JPanel) panel.getComponent(1);

        ((GIntegerField) color_panel.getComponent(1)).set_value(r);
        ((GIntegerField) color_panel.getComponent(3)).set_value(g);
        ((GIntegerField) color_panel.getComponent(5)).set_value(b);
        ((ColorPanel) color_panel.getComponent(6)).set_color(r, g, b);
    }

    private void add_insets(JPanel panel, int[] border_info) {
        int top = border_info[3];
        int left = border_info[4];
        int bottom = border_info[5];
        int right = border_info[6];

        JLabel insets_label = new JLabel("insets =");
        JLabel top_label = new JLabel("top:");
        JLabel left_label = new JLabel("left:");
        JLabel bottom_label = new JLabel("bottom:");
        JLabel right_label = new JLabel("right:");

        GIntegerField top_spinner = new GIntegerField(SettingsFrame.background.brighter(), SettingsFrame.foreground, SettingsFrame.background.darker().darker(), -1, 255);
        GIntegerField left_spinner = new GIntegerField(SettingsFrame.background.brighter(), SettingsFrame.foreground, SettingsFrame.background.darker().darker(), -1, 255);
        GIntegerField bottom_spinner = new GIntegerField(SettingsFrame.background.brighter(), SettingsFrame.foreground, SettingsFrame.background.darker().darker(), -1, 255);
        GIntegerField right_spinner = new GIntegerField(SettingsFrame.background.brighter(), SettingsFrame.foreground, SettingsFrame.background.darker().darker(), -1, 255);

        JPanel insets_filler = new JPanel();

        //inizializza i componenti
        top_spinner.set_value(top);
        left_spinner.set_value(left);
        bottom_spinner.set_value(bottom);
        right_spinner.set_value(right);

        insets_filler.setOpaque(false);

        insets_label.setForeground(SettingsFrame.foreground);
        top_label.setForeground(SettingsFrame.foreground);
        left_label.setForeground(SettingsFrame.foreground);
        bottom_label.setForeground(SettingsFrame.foreground);
        right_label.setForeground(SettingsFrame.foreground);

        //aggiunge tutti i componenti al pannello
        GridBagConstraints c = new GridBagConstraints();

        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets.right = 10;
        c.weightx = 0;
        panel.add(insets_label, c);

        c.gridx = 1;
        panel.add(top_label, c);

        c.gridx = 2;
        panel.add(top_spinner, c);

        c.gridx = 3;
        panel.add(left_label, c);

        c.gridx = 4;
        panel.add(left_spinner, c);

        c.gridx = 5;
        panel.add(bottom_label, c);

        c.gridx = 6;
        panel.add(bottom_spinner, c);

        c.gridx = 7;
        panel.add(right_label, c);

        c.gridx = 8;
        panel.add(right_spinner, c);

        c.gridx = 9;
        panel.add(insets_filler, c);
    }

    private void set_insets(JPanel panel, int[] border_info) {
        int top = border_info[3];
        int left = border_info[4];
        int bottom = border_info[5];
        int right = border_info[6];

        ((GIntegerField) panel.getComponent(4)).set_value(top);
        ((GIntegerField) panel.getComponent(6)).set_value(left);
        ((GIntegerField) panel.getComponent(8)).set_value(bottom);
        ((GIntegerField) panel.getComponent(10)).set_value(right);
    }
}
