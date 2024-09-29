package gui.custom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class CascateMenu extends JPanel {
    private final JPanel LIST_PANEL = new JPanel();
    private CascateItem selected = null;

    public CascateMenu() {
        this.setLayout(new GridBagLayout());
        this.setOpaque(false);

        LIST_PANEL.setOpaque(false);
        LIST_PANEL.setLayout(new GridBagLayout());

        JPanel filler = new JPanel();
        filler.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1;
        c.weightx = 1;
        this.add(filler, c);

        c.weighty = 0;
        c.gridy = 0;
        this.add(LIST_PANEL, c);
    }

    public void updated_color() {
        for (Component comp : LIST_PANEL.getComponents()) {
            CascateItem list_item = (CascateItem) comp;
            list_item.update_color();
        }
    }

    public void add_item(String name) {
        add_item(name, false, null);
    }

    public void add_item(String name, ActionListener action) {
        add_item(name, false, action);
    }

    public void add_item(String name, boolean have_childs, ActionListener action) {
        if (name.contains("/")) {
            String parent_name = name.substring(0, name.indexOf('/'));
            name = name.substring(name.indexOf('/') + 1);

            for (Component item : LIST_PANEL.getComponents()) {
                CascateItem cItem = (CascateItem) item;

                if (cItem.getRelativeName().equals(parent_name) && cItem.have_child()) {
                    cItem.add_child(name, name, have_childs, action);
                    return;
                }
            }

            add_item(parent_name, true, null);
            add_item(parent_name + "/" + name, have_childs, action);
        }
        else {
            GridBagConstraints c = new GridBagConstraints();

            c.weighty = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = LIST_PANEL.getComponents().length;
            LIST_PANEL.add(new CascateItem(this, name, name, have_childs, action), c);
        }
    }

    public void remove_item(String name) {
        if (name.contains("/")) {
            String parent_name = name.substring(0, name.indexOf('/'));
            name = name.substring(name.indexOf('/') + 1);

            for (Component item : LIST_PANEL.getComponents()) {
                CascateItem cItem = (CascateItem) item;

                if (cItem.getRelativeName().equals(parent_name) && cItem.have_child()) {
                    cItem.remove_child(name);
                }
            }
        }
        else {
            Component[] components = LIST_PANEL.getComponents();
            GridBagLayout layout =  (GridBagLayout) LIST_PANEL.getLayout();

            boolean found = false;
            for (Component component : components) {
                CascateItem cItem = (CascateItem) component;

                if (!found) { //controlla se il nome di cItem equivale a quello che deve eliminare
                    if (cItem.getRelativeName().equals(name)) {

                        LIST_PANEL.remove(cItem);
                        found = true;
                    }
                } else { //una volta eliminato l'item sposta tutti quelli sotto di lui in su di 1
                    GridBagConstraints constraints = layout.getConstraints(cItem);
                    constraints.gridy--;

                    layout.setConstraints(component, constraints);
                }
            }
        }
    }

    protected void set_selected(CascateItem item) {
        if (selected != null && !selected.equals(item)) {
            selected.unselect();
        }

        selected = item;
    }

    //se ha un item selezionato lo deseleziona
    public void unselect() {
        if (selected != null) {
            selected.unselect();
            selected = null;
        }
    }

    public String get_selected_text() {
        if (selected != null) {
            return selected.getRelativeName();
        }
        else {
            return null;
        }
    }

    public void reset() {
        LIST_PANEL.removeAll();
    }
}