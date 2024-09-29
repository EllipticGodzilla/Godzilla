package gui.custom;

import gui.graphicsSettings.GraphicsSettings;
import network.Server_manager;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Utilizzando un estensione di JList viene più semplice ma aggiungere e rimuovere elementi dalla lista in modo dinamico può provocare problemi grafici
 * dove la lista viene mostrata vuota finché non le si dà un nuovo update, di conseguenza ho creato la mia versione di JList utilizzando varie JTextArea
 * e partendo da un JPanel.
 * Non so bene da che cosa sia dovuto il problema con JList ma sembra essere risolto utilizzando la mia versione
 */
public class GList extends JPanel {
    private Map<String, ListCell> elements = new LinkedHashMap<>();
    private int selected_index = -1;

    private final JPanel LIST_PANEL = new JPanel(); //pannello che contiene tutte le JTextArea della lista

    private Constructor<?> popupMenu = null;

    public GList() {
        super();
        this.setLayout(new GridBagLayout());
        this.setBackground((Color) GraphicsSettings.active_theme.get_value("list_background"));
        this.setFont(new Font("custom_list", Font.BOLD, 11));

        JPanel filler = new JPanel();
        filler.setOpaque(false);
        filler.setFocusable(false);
        filler.setBorder(null);

        LIST_PANEL.setLayout(new GridBagLayout());
        LIST_PANEL.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        c.weightx = 1;
        this.add(LIST_PANEL, c);

        c.gridy = 1;
        c.weighty = 1;
        this.add(filler, c);
    }

    public void update_colors() {
        this.setBackground((Color) GraphicsSettings.active_theme.get_value("list_background"));

        for (Component list_item : LIST_PANEL.getComponents()) {
            ((ListCell) list_item).update_colors();
        }
    }

    public void set_popup(Class<?> PopupMenu) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.popupMenu = PopupMenu.getDeclaredConstructor(String.class, GList.class);

        for (ListCell cell : elements.values()) {
            cell.setComponentPopupMenu((JPopupMenu) this.popupMenu.newInstance(cell.getText(), this));
        }
    }

    public void add(String name) { //aggiunge una nuova casella nell'ultima posizione
        ListCell cell = new ListCell(name, this, elements.size());
        elements.put(name, cell);
        if (this.popupMenu != null) {
            try {
                cell.setComponentPopupMenu((JPopupMenu) this.popupMenu.newInstance(name, this));
            }
            catch (InvocationTargetException | InstantiationException | IllegalAccessException _) {} //exception ignorate
        }

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridy = elements.size() - 1;
        c.gridx = 0;

        LIST_PANEL.add(cell, c);

        this.updateUI(); //aggiorna la gui
    }

    public void remove(String name) {
        ListCell cell = elements.get(name);

        elements.remove(name); //rimuove la cella dalla lista
        LIST_PANEL.remove(cell); //rimuove la casella dal pannello

        adjust_gridy(cell.MY_INDEX); //aggiusta il valore gridy per tutte le caselle sotto questa eliminata

        if (cell.MY_INDEX == selected_index) { //se questa casella era selezionata
            selected_index = -1;
        }

        this.updateUI(); //aggiorna la gui
    }

    public void clear() { //rimuove tutti gli oggetti
        LIST_PANEL.removeAll(); //rimuove tutti gli oggetti dal pannello
        elements.clear(); //rimuove tutte le caselle dalla mappa

        selected_index = -1;
    }

    public void rename_element(String old_name, String new_name) {
        for (ListCell cell : elements.values()) {
            if (cell.getText().equals(old_name)) {
                cell.setText(new_name);
                break;
            }
        }
    }

    public String get_selected_value() {
        if (selected_index == -1) { //se non è selezionata nessuna casella
            return "";
        }
        else {
            return ((ListCell) LIST_PANEL.getComponent(selected_index)).getText();
        }
    }

    private void adjust_gridy(int from) {
        GridBagLayout layout = (GridBagLayout) LIST_PANEL.getLayout();
        Component[] components = LIST_PANEL.getComponents();

        for (int i = from; i < LIST_PANEL.getComponents().length; i++) {
            Component component = components[i];

            GridBagConstraints constraints = layout.getConstraints(component);
            constraints.gridy --;

            layout.setConstraints(component, constraints);
        }
    }

    static class ListCell extends JTextField {
        private final GList PARENT_LIST;
        private final int MY_INDEX;

        public ListCell(String text, GList list, int index) {
            super(text);
            this.PARENT_LIST = list;
            this.MY_INDEX = index;

            //imposta tutti i colori standard
            this.setForeground((Color) GraphicsSettings.active_theme.get_value("list_text_color"));
            this.setOpaque(false);
            this.setBackground((Color) GraphicsSettings.active_theme.get_value("list_selected_background"));
            this.setCaretColor((Color) GraphicsSettings.active_theme.get_value("list_background"));
            this.setSelectionColor((Color) GraphicsSettings.active_theme.get_value("list_background"));
            this.setFont(new Font("custom_list", Font.BOLD, 11));
            this.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));

            this.setEditable(false);
            this.setCursor(null);

            this.addKeyListener(KEY_LISTENER);
            this.addMouseListener(MOUSE_LISTENER);
        }

        private final KeyListener KEY_LISTENER = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 40: //freccia in basso
                        try {
                            ListCell next_cell = (ListCell) PARENT_LIST.LIST_PANEL.getComponent(MY_INDEX + 1);

                            next_cell.set_selected();
                            next_cell.requestFocus();
                        } catch (Exception _) {} //se non esiste un elemento ad index my_index + 1
                        break;

                    case 38: //freccia in alto
                        try {
                            ListCell prev_cell = (ListCell) PARENT_LIST.LIST_PANEL.getComponent(MY_INDEX - 1);

                            prev_cell.set_selected();
                            prev_cell.requestFocus();
                        } catch (Exception _) {} //se non esiste un elemento ad index my_index - 1
                        break;

                    case 27: //esc
                        unselect();
                        break;

                    case 10: //invio, si collega a questo server
                        String server_name = getText();
                        Server_manager.connect_to(server_name);
                        break;
                }
            }
        };

        private final MouseListener MOUSE_LISTENER = new MouseListener() {
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {
                set_selected();
            }
        };

        public void set_selected() {
            if (PARENT_LIST.selected_index != MY_INDEX) {
                //deseleziona la casella selezionata in precedenza, se ne era selezionata una
                if (PARENT_LIST.selected_index != -1) {
                    ((ListCell) PARENT_LIST.LIST_PANEL.getComponent(PARENT_LIST.selected_index)).unselect();
                }

                PARENT_LIST.selected_index = MY_INDEX;
                update_colors();
            }
        }

        public void unselect() {
            PARENT_LIST.selected_index = -1;
            update_colors();
        }

        public void update_colors() {
            if (PARENT_LIST.selected_index == MY_INDEX) {
                setOpaque(true); //mostra il background
                setForeground((Color) GraphicsSettings.active_theme.get_value("list_selected_text_color"));
                setBorder((Border) GraphicsSettings.active_theme.get_value("list_selected_border"));
                setBackground((Color) GraphicsSettings.active_theme.get_value("list_selected_background"));
                setSelectionColor(getBackground());
                setCaretColor(getBackground());
            }
            else {
                setOpaque(false); //non mostra il background
                setForeground((Color) GraphicsSettings.active_theme.get_value("list_text_color"));
                setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
                setSelectionColor((Color) GraphicsSettings.active_theme.get_value("list_background"));
                setCaretColor((Color) GraphicsSettings.active_theme.get_value("list_background"));
            }
        }
    }
}