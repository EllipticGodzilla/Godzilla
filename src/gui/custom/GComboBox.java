package gui.custom;

import javax.swing.*;
import javax.swing.plaf.metal.MetalComboBoxButton;
import java.awt.*;

public class GComboBox extends JComboBox<String> {
    public GComboBox(String[] list) {
        super(list);

        this.setBackground(new Color(108, 111, 113));
        this.setForeground(new Color(218, 221, 223));
        this.setBorder(BorderFactory.createLineBorder(new Color(68, 71, 73)));

        ComboBoxRenderer renderer = new ComboBoxRenderer();
        renderer.setPreferredSize(new Dimension(100, 16));
        renderer.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        this.setRenderer(renderer);
        ((MetalComboBoxButton) this.getComponents()[0]).setBorder(null);
    }

    public void set_list(String[] new_list) {
        DefaultComboBoxModel<String> new_model = new DefaultComboBoxModel<>(new_list);
        this.setModel(new_model);
    }

    public boolean contains(String item) {
        return ((DefaultComboBoxModel<String>) getModel()).getIndexOf(item) != -1;
    }

    static class ComboBoxRenderer extends JLabel implements ListCellRenderer<String> {
        boolean list_init = true;

        public ComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            if (list_init) { //setta
                list.setSelectionBackground(new Color(108, 111, 113));
                list.setSelectionForeground(new Color(218, 221, 223));
                list.setBorder(null);

                list_init = false;
            }

            if (isSelected) {
                setBackground(new Color(158, 161, 163));
            }
            else {
                setBackground(new Color(108, 111, 113));
            }

            setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            setText(value);

            return this;
        }
    }
}