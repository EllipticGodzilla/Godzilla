package gui.custom;

import file_database.Database;
import file_database.Pair;
import network.Server;
import javax.swing.*;
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
 * utilizzando un estensione di JList viene più semplice ma aggiungere e rimuovere elementi dalla lista in modo dinamico può provocare problemi grafici
 * dove la lista viene mostrata vuota finché non le si dà un nuovo update, di conseguenza ho creato la mia versione di JList utilizzando varie JTextArea
 * e partendo da un JPanel.
 * Non so bene da che cosa sia dovuto il problema con JList ma sembra essere risolto utilizzando la mia versione
 */
public class GList extends JPanel {
    private Map<String, ListCell> elements = new LinkedHashMap<>();
    private int selected_index = -1;

    private JPanel list_panel = new JPanel(); //pannello che contiene tutte le JTextArea della lista
    private JTextArea filler = new JTextArea(); //filler per rimepire lo spazio in basso

    private Constructor popupMenu = null;

    public GList() {
        super();
        this.setLayout(new GridBagLayout());

        this.setForeground(new Color(44, 46, 47));
        this.setBackground(new Color(98, 101, 103));
        this.setFont(new Font("custom_list", Font.BOLD, 11));

        filler.setBackground(this.getBackground());
        filler.setFocusable(false);
        filler.setEditable(false);

        list_panel.setLayout(new GridBagLayout());
        list_panel.setBackground(this.getBackground());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        c.weightx = 1;
        this.add(list_panel, c);

        c.gridy = 1;
        c.weighty = 1;
        this.add(filler, c);
    }

    public void set_popup(Class PopupMenu) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.popupMenu = PopupMenu.getDeclaredConstructor(String.class, GList.class);

        for (ListCell cell : elements.values()) {
            cell.setComponentPopupMenu((JPopupMenu) this.popupMenu.newInstance(cell.getText(), this));
        }
    }

    public void add(String name) throws InvocationTargetException, InstantiationException, IllegalAccessException { //aggiunge una nuova casella nell'ultima posizione
        ListCell cell = new ListCell(name, this, elements.size());
        elements.put(name, cell);
        if (this.popupMenu != null) {
            cell.setComponentPopupMenu((JPopupMenu) this.popupMenu.newInstance(name, this));
        }

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridy = elements.size() - 1;
        c.gridx = 0;

        list_panel.add(cell, c);

        this.updateUI(); //aggiorna la gui
    }

    public void remove(String name) {
        ListCell cell = elements.get(name);

        elements.remove(name); //rimuove la cella dalla lista
        list_panel.remove(cell); //rimuove la casella dalla lista

        if (cell.my_index == selected_index) { //se questa casella era selezionata
            selected_index = -1;
        }

        this.updateUI(); //aggiorna la gui
    }

    public void rename_element(String old_name, String new_name) {
        for (ListCell cell : elements.values()) {
            if (cell.getText().equals(old_name)) {
                cell.setText(new_name);
                break;
            }
        }
    }

    public String getSelectedValue() {
        if (selected_index == -1) { //se non è selezionata nessuna casella
            return "";
        }
        else {
            return ((ListCell) list_panel.getComponent(selected_index)).getText();
        }
    }

    public void reset_list() {
        elements = new LinkedHashMap<>();
        list_panel.removeAll();
        this.repaint();

        selected_index = -1;
    }

    class ListCell extends JTextArea {
        private static final Color STD_BACKGROUND = new Color(98, 101, 103);
        private static final Color SEL_BACKGROUND = new Color(116, 121, 125);
        private static final Color SEL_BORDER = new Color(72, 74, 75);

        private final GList PARENT_LIST;
        private int my_index;

        public ListCell(String text, GList list, int index) {
            super(text);
            this.PARENT_LIST = list;
            this.my_index = index;

            //imposta tutti i colori
            this.setForeground(new Color(44, 46, 47));
            this.setBackground(STD_BACKGROUND);
            this.setFont(new Font("custom_list", Font.BOLD, 11));
            this.setBorder(null);

            this.setEditable(false);
            this.setCaretColor(STD_BACKGROUND);
            this.setCursor(null);

            this.addKeyListener(key_l);
            this.addMouseListener(mouse_l);
        }

        private KeyListener key_l = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 40: //freccia in basso
                        try {
                            ListCell next_cell = (ListCell) PARENT_LIST.list_panel.getComponent(my_index + 1);

                            next_cell.set_selected();
                            next_cell.requestFocus();
                        } catch (Exception ex) {} //se non esiste un elemento ad index my_index + 1
                        break;

                    case 38: //freccia in alto
                        try {
                            ListCell prev_cell = (ListCell) PARENT_LIST.list_panel.getComponent(my_index - 1);

                            prev_cell.set_selected();
                            prev_cell.requestFocus();
                        } catch (Exception ex) {} //se non esiste un elemento ad index my_index - 1
                        break;

                    case 27: //esc
                        unselect();
                        break;

                    case 10: //invio, si collega a questo server
                        Pair<String, String> server_info = Database.serverList.get(getText());
                        Server.start_connection_with(server_info.el1, server_info.el2);
                        break;
                }
            }
        };

        private MouseListener mouse_l = new MouseListener() {
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
            if (PARENT_LIST.selected_index != my_index) {
                //deseleziona la casella selezionata in precedenza, se ne era selezionata una
                if (PARENT_LIST.selected_index != -1) {
                    ((ListCell) PARENT_LIST.list_panel.getComponent(PARENT_LIST.selected_index)).unselect();
                }

                //imposta questa JTextArea come selezionata
                setBackground(SEL_BACKGROUND);
                setBorder(BorderFactory.createLineBorder(SEL_BORDER));
                setCaretColor(SEL_BACKGROUND);
                setSelectionColor(SEL_BACKGROUND);

                PARENT_LIST.selected_index = my_index;
            }
        }

        public void unselect() {
            setBackground(STD_BACKGROUND);
            setBorder(null);
            setCaretColor(STD_BACKGROUND);
            setSelectionColor(STD_BACKGROUND);

            PARENT_LIST.selected_index = -1;
        }
    }
}