package gui.graphicsSettings.standard_builder;

import files.Logger;
import gui.custom.GFileChooser;
import gui.graphicsSettings.ButtonIcons;
import gui.settingsFrame.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Icons_graphics implements GraphicsOption_builder<ButtonIcons> {
    private static final Pattern ICONS_PATTERN = Pattern.compile("icons\\(([^,]+),([^,]+),([^,]+),([^,]+)\\)");
    private static final Image_graphics image_graphics = new Image_graphics();

    @Override
    public boolean equals(Object obj1, Object obj2) {
        ButtonIcons icons1 = (ButtonIcons) obj1;
        ButtonIcons icons2 = (ButtonIcons) obj2;

        return  image_graphics.equals(icons1.getStandardIcon(), icons2.getStandardIcon()) &&
                image_graphics.equals(icons1.getRolloverIcon(), icons2.getRolloverIcon()) &&
                image_graphics.equals(icons1.getPressedIcon(), icons2.getPressedIcon()) &&
                image_graphics.equals(icons1.getDisabledIcon(), icons2.getDisabledIcon());
    }

    @Override
    public ButtonIcons cast(String value_str) {
        Matcher matcher = ICONS_PATTERN.matcher(value_str);

        if (matcher.matches()) {
            ImageIcon std_icon, roll_icon, press_icon, dis_icon;

            std_icon = image_graphics.cast(matcher.group(1));

            if (std_icon == null) {
                Logger.log("impossibile inizializzare l'icona standard dalle info: " + value_str, true, '\n');
                return null;
            }

            roll_icon = image_graphics.cast(matcher.group(2));

            if (roll_icon == null) {
                Logger.log("impossibile inizializzare l'icona rollover dalle info: " + value_str, true, '\n');
                return null;
            }

            press_icon = image_graphics.cast(matcher.group(3));

            if (press_icon == null) {
                Logger.log("impossibile inizializzare l'icona pressed dalle info: " + value_str, true, '\n');
                return null;
            }


            dis_icon = image_graphics.cast(matcher.group(4));

            if (dis_icon == null) {
                Logger.log("impossibile inizializzare l'icona disabled dalle info: " + value_str, true, '\n');
                return null;
            }

            return new ButtonIcons(std_icon, roll_icon, press_icon, dis_icon);
        }

        Logger.log("impossibile comprendere la formattazione delle icone: " + value_str);
        return null;
    }

    @Override
    public String revert_cast(Object value) {
        ButtonIcons icons = (ButtonIcons) value;

        return "icons(" +
                image_graphics.revert_cast( icons.getStandardIcon() ) + "," +
                image_graphics.revert_cast( icons.getRolloverIcon() ) + "," +
                image_graphics.revert_cast( icons.getPressedIcon() ) + "," +
                image_graphics.revert_cast( icons.getDisabledIcon() ) + ")";
    }

    @Override
    public void display(JPanel panel, Object value) {
        ButtonIcons icons = (ButtonIcons) value;

        String std_path = icons.getStandardIcon().getDescription();
        String rol_path = icons.getRolloverIcon().getDescription();
        String pres_path = icons.getPressedIcon().getDescription();
        String dis_path = icons.getDisabledIcon().getDescription();

        JTextField std_field = mk_field(std_path);
        JTextField rol_field = mk_field(rol_path);
        JTextField pres_field = mk_field(pres_path);
        JTextField dis_field = mk_field(dis_path);

        GFileChooser std_chooser = new GFileChooser(std_field, "png or jpg image files", "jpg", "png");
        GFileChooser rol_chooser = new GFileChooser(rol_field, "png or jpg image files","jpg", "png");
        GFileChooser pres_chooser = new GFileChooser(pres_field, "png or jpg image files","jpg", "png");
        GFileChooser dis_chooser = new GFileChooser(dis_field, "png or jpg image files","jpg", "png");

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        panel.add(std_chooser, c);

        c.gridy = 1;
        panel.add(rol_chooser, c);

        c.gridy = 2;
        panel.add(pres_chooser, c);

        c.gridy = 3;
        panel.add(dis_chooser, c);

        c.gridy = 0;
        c.gridx = 1;
        c.weightx = 1;
        panel.add(std_field, c);

        c.gridy = 1;
        panel.add(rol_field, c);

        c.gridy = 2;
        panel.add(pres_field, c);

        c.gridy = 3;
        panel.add(dis_field, c);
    }

    @Override
    public void update(JPanel panel, Object value) {
        ButtonIcons icons = (ButtonIcons) value;

        String std_path = icons.getStandardIcon().getDescription();
        String rol_path = icons.getRolloverIcon().getDescription();
        String pres_path = icons.getPressedIcon().getDescription();
        String dis_path = icons.getDisabledIcon().getDescription();

        ((JTextField) panel.getComponent(4)).setText(std_path);
        ((JTextField) panel.getComponent(5)).setText(rol_path);
        ((JTextField) panel.getComponent(6)).setText(pres_path);
        ((JTextField) panel.getComponent(7)).setText(dis_path);
    }

    @Override
    public ButtonIcons new_value(JPanel panel) {
        Component[] cps = panel.getComponents();

        String std_path = ((JTextField) cps[4]).getText();
        String rol_path = ((JTextField) cps[5]).getText();
        String pres_path = ((JTextField) cps[6]).getText();
        String dis_path = ((JTextField) cps[7]).getText();

        return new ButtonIcons(
                new ImageIcon(std_path),
                new ImageIcon(rol_path),
                new ImageIcon(pres_path),
                new ImageIcon(dis_path)
        );
    }

    public static JTextField mk_field(String txt) {
        JTextField field = new JTextField(txt);

        field.setEditable(false);
        field.setFocusable(false);
        field.setOpaque(false);
        field.setForeground(SettingsFrame.foreground);
        field.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));

        return field;
    }
}
