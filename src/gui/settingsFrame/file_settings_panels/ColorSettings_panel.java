package gui.settingsFrame.file_settings_panels;

import gui.custom.GComboBox;
import gui.custom.GScrollPane;
import gui.graphicsSettings.GraphicsOptions;
import gui.graphicsSettings.GraphicsSettings;
import gui.settingsFrame.SettingsFrame;
import gui.settingsFrame.Settings_panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ColorSettings_panel {
    private static JPanel main_panel = new JPanel();
    private static final Map<String, GraphicsOptions> CHANGES = new LinkedHashMap<>();
    private static GraphicsOptions original_options;
    private static GraphicsOptions_panel options_panel;
    private static GComboBox graphics_combo_box;
    private static JTextField graphics_text_field;

    public static void init() {
        main_panel.setOpaque(false);
        main_panel.setLayout(new GridBagLayout());

        options_panel = new GraphicsOptions_panel();
        GScrollPane scrollPane = new GScrollPane(options_panel);

        //components
        JLabel txt1 = new JLabel("graphics settings:");
        graphics_combo_box = new GComboBox(GraphicsSettings.get_options_list());
        graphics_text_field = new JTextField();

        JButton add_graphics = new JButton();
        JButton remove_graphics = new JButton();
        JPanel filler = new JPanel();

        //components settings
        txt1.setForeground(SettingsFrame.foreground);
        txt1.setBorder(BorderFactory.createEmptyBorder(4, 4, 0,0));

        scrollPane.setBorder(BorderFactory.createLineBorder(SettingsFrame.background.darker()));
        options_panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        graphics_text_field.setVisible(false);
        graphics_text_field.setMinimumSize(graphics_combo_box.getPreferredSize());
        graphics_text_field.setBackground(graphics_combo_box.getBackground());
        graphics_text_field.setForeground(SettingsFrame.foreground);
        graphics_text_field.setBorder(graphics_combo_box.getBorder());
        graphics_text_field.setFont(graphics_combo_box.getFont());

        add_graphics.setIcon(new ImageIcon(ColorSettings_panel.class.getResource("/images/plus.png")));
        add_graphics.setRolloverIcon(new ImageIcon(ColorSettings_panel.class.getResource("/images/plus_sel.png")));
        add_graphics.setPressedIcon(new ImageIcon(ColorSettings_panel.class.getResource("/images/plus_pres.png")));
        remove_graphics.setIcon(new ImageIcon(ColorSettings_panel.class.getResource("/images/minus.png")));
        remove_graphics.setRolloverIcon(new ImageIcon(ColorSettings_panel.class.getResource("/images/minus_sel.png")));
        remove_graphics.setPressedIcon(new ImageIcon(ColorSettings_panel.class.getResource("/images/minus_pres.png")));

        add_graphics.setBorder(null);
        remove_graphics.setBorder(null);

        add_graphics.setOpaque(false);
        remove_graphics.setOpaque(false);

        add_graphics.setContentAreaFilled(false);
        remove_graphics.setContentAreaFilled(false);

        filler.setOpaque(false);

        //buttons actions
        graphics_combo_box.addActionListener(_ -> {
            String settings_name = (String) graphics_combo_box.getSelectedItem();

            change_visible_options(settings_name);
        });

        graphics_text_field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {}
            @Override
            public void focusLost(FocusEvent focusEvent) {
                graphics_text_field.setText("");
                graphics_text_field.setVisible(false);

                graphics_combo_box.setVisible(true);
            }
        });

        graphics_text_field.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {}
            @Override
            public void keyReleased(KeyEvent keyEvent) {}
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    graphics_combo_box.setVisible(true);
                    graphics_combo_box.requestFocus();
                }
                else if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    String new_settings_name = graphics_text_field.getText();
                    mk_new_options(new_settings_name);

                    graphics_combo_box.setVisible(true);
                    graphics_combo_box.requestFocus();
                }
            }
        });

        add_graphics.addActionListener(_ -> {
            if (graphics_combo_box.isVisible()) {
                graphics_combo_box.setVisible(false);
                graphics_text_field.setVisible(true);
                graphics_text_field.requestFocus();
            }
        });

        remove_graphics.addActionListener(_ -> {
            if (graphics_combo_box.getItemCount() != 1 && !Objects.equals(graphics_combo_box.getSelectedItem(), "standard")) { //lascia sempre almeno un elemento nella lista e non elimina mai standard
                int selected_index = graphics_combo_box.getSelectedIndex();

                //rimuove dalla lista
                String options_name = graphics_combo_box.getItemAt(selected_index);
                graphics_combo_box.removeItemAt(selected_index);

                CHANGES.put(options_name, null);
            }
        });

        //add to the panel
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        main_panel.add(txt1, c);

        c.gridx = 1;
        c.insets.left = 10;
        main_panel.add(graphics_combo_box, c);

        c.gridx = 2;
        main_panel.add(graphics_text_field, c);

        c.gridx = 4;
        c.insets.right = 5;
        c.insets.left = 0;
        main_panel.add(remove_graphics, c);

        c.gridx = 5;
        c.insets.right = 15;
        main_panel.add(add_graphics, c);

        c.gridx = 3;
        c.weightx = 1;
        c.insets.right = 0;
        main_panel.add(filler, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1;
        c.gridwidth = 6;
        main_panel.add(scrollPane, c);
    }

    public static void load() {
        CHANGES.clear();

        graphics_combo_box.set_list(GraphicsSettings.get_options_list());

        GraphicsOptions displayed_graphics = GraphicsSettings.active_option;
        original_options = displayed_graphics;
        graphics_combo_box.setSelectedItem(displayed_graphics.get_name());

        JPanel content_panel = Settings_panel.get_content_panel();
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        content_panel.add(main_panel, c);

        content_panel.updateUI();

        //imposta i listener di ok e cancella:
        SettingsFrame.set_action_listener(SettingsFrame.APPLY_BUTTON, APPLY_LISTENER);
        SettingsFrame.set_action_listener(SettingsFrame.OK_BUTTON, OK_LISTENER);
    }

    private static void change_visible_options(String new_options_name) {
        save_changes(original_options); //salva le modifiche avvenute nelle impostazioni di prima

        GraphicsOptions new_options;
        if ((new_options = CHANGES.get(new_options_name)) == null)
            new_options = GraphicsSettings.get_option(new_options_name);

        original_options = new_options;
        options_panel.display(new_options);
    }

    private static void mk_new_options(String name) {
        if (!graphics_combo_box.contains(name)) {
            graphics_combo_box.addItem(name);
            CHANGES.put(name, new GraphicsOptions(name));

            graphics_combo_box.setSelectedItem(name);
        }
    }

    private static void save_changes(GraphicsOptions original_options) {
        if (!original_options.get_name().equals("standard")) { //non salva le modifiche agli standard
            GraphicsOptions new_options = options_panel.get_updated(original_options.get_name());

            if (!new_options.equals(original_options)) { //se sono state modificate le opzioni
                CHANGES.put(original_options.get_name(), new_options);
            }
        }
    }

    private static final ActionListener APPLY_LISTENER = _ -> {
        save_changes(original_options);

        for (String name : CHANGES.keySet()) {
            GraphicsOptions options = CHANGES.get(name);

            if (GraphicsSettings.get_option(name) != null) { //se l'opzione non è appena stata creata, quindi è già registrata in GraphicsSettings
                if (options == null) { //si vuole rimuovere questa opzione
                    GraphicsSettings.remove_option(name);
                }
                else { //si vogliono modificare dei valori
                    GraphicsSettings.update_options(name, options);
                }
            }
            else if (options != null) { //se questa opzione è appena stata creata (e non è stata rimossa subito), deve registrarla in graphics settings
                GraphicsSettings.add_options(options);
            }
        }

        GraphicsSettings.set_active_options(original_options.get_name());
    };

    private static final ActionListener OK_LISTENER = _ -> {
        APPLY_LISTENER.actionPerformed(null);

        SettingsFrame.hide();
    };
}
