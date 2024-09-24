package gui.settingsFrame.file_settings_panels;

import gui.graphicsSettings.GraphicsSettings;
import gui.graphicsSettings.standard_builder.GraphicsOption_builder;
import gui.graphicsSettings.GraphicsTheme;
import gui.settingsFrame.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class GraphicsOptions_panel extends JPanel {
    private final Map<String, JPanel> VALUE_PANELS = new LinkedHashMap<>(); //mappa fra il nome dell'opzione e il pannello in cui viene mostrato il suo valore

    public GraphicsOptions_panel() {
        this.setLayout(new GridBagLayout());
        this.setOpaque(false);

        update_color();
        GraphicsSettings.run_at_theme_change(this::update_color);
    }

    public void update_color() {
        Color enc_bg = (Color) GraphicsSettings.active_theme.get_value("frame_background");
        enc_bg = enc_bg.darker();

        for (Component comp : getComponents()) { //cambia il background a tutti gli encapsulator
            JComponent jComp = (JComponent) comp;

            jComp.setBackground(enc_bg);
            jComp.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(enc_bg.darker()),
                    BorderFactory.createEmptyBorder(5, 5, 5, 0)
            ));

            JLabel label = (JLabel) jComp.getComponent(0);
            JPanel panel = (JPanel) jComp.getComponent(1);

            Color foreground = (Color) GraphicsSettings.active_theme.get_value("text_color");

            label.setForeground(foreground);
            for (Component element : panel.getComponents()) {
                if (element instanceof JLabel filed) {
                    filed.setForeground(foreground);
                }
            }
        }
    }

    public void display(GraphicsTheme options) {
        String[] options_list = GraphicsTheme.get_key_list(); //lista di tutte le opzioni grafiche modificate in questa opzione

        for (String opt_name : options_list) {
            Object value = options.get_value(opt_name);
            JPanel value_panel = VALUE_PANELS.get(opt_name);

            display_val(value_panel, opt_name, value); //aggiorna value_panel per mostrare value
        }

        this.updateUI();
    }

    public GraphicsTheme get_updated(String name) {
        GraphicsTheme new_options = new GraphicsTheme(name);

        for (String opt_name : VALUE_PANELS.keySet()) {
            JPanel value_panel = VALUE_PANELS.get(opt_name);

            GraphicsOption_builder<?> builder = GraphicsTheme.get_builder(opt_name);
            Object value = builder.new_value(value_panel);

            new_options.set_value(opt_name, value);
        }

        return new_options;
    }

    private void display_val(JPanel panel, String name, Object value) {
        if (panel == null) { //se non si è ancora mai visualizzato il valore di questa impostazione
            panel = new JPanel();

            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
            panel.setOpaque(false);

            VALUE_PANELS.put(name, panel);
            GraphicsTheme.get_builder(name).display(panel, value);

            add_panel(panel, name);
        }
        else {
            //aggiorna il valore nel pannello con quello nuovo
            GraphicsTheme.get_builder(name).update(panel, value);
        }
    }

    private void add_panel(JPanel panel, String name) {
        JPanel encapsulator = new JPanel();
        JLabel name_label = new JLabel(name + ":");
        Color enc_bg = ((Color) GraphicsSettings.active_theme.get_value("frame_background")).darker();

        encapsulator.setBackground(enc_bg);
        encapsulator.setLayout(new GridBagLayout());
        encapsulator.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(enc_bg.darker()),
                BorderFactory.createEmptyBorder(5, 5, 5, 0)
        ));

        name_label.setForeground((Color) GraphicsSettings.active_theme.get_value("text_color"));

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;
        c.weightx = 0;
        c.insets = new Insets(9, 0, 0, 5);
        encapsulator.add(name_label, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets.top = 0;
        encapsulator.add(panel, c);

        c.gridy = VALUE_PANELS.size() - 1;
        c.gridx = 0;
        c.insets = new Insets(0, 5, 5,5);
        this.add(encapsulator, c);
    }
}
