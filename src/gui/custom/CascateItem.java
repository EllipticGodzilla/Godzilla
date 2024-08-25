package gui.custom;

import gui.settingsFrame.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class CascateItem extends JPanel {
    private final CascateMenu MENU;

    private MenuButton menu_button;
    private final boolean have_child;
    private final String FULL_NAME;
    private final ActionListener CHILDS_DEFAULT_ACTION;

    private final JPanel CHILD_PANEL = new JPanel();
    private final JPanel SEPARATOR = new JPanel();

    public CascateItem(CascateMenu menu, String name, ActionListener child_action) {
        this(menu, name, name, true, child_action);
    }

    public CascateItem(CascateMenu menu, String relative_name, String full_name, boolean have_child, ActionListener child_action) {
        super();
        super.setLayout(new GridBagLayout());
        this.MENU = menu;
        this.FULL_NAME = full_name;
        this.have_child = have_child;
        this.CHILDS_DEFAULT_ACTION = child_action;

        UIManager.put("Button.select", new Color(88, 91, 93));
        init_menu_button(relative_name);

        CHILD_PANEL.setLayout(new BoxLayout(CHILD_PANEL, BoxLayout.Y_AXIS));
        CHILD_PANEL.setVisible(false);

        SEPARATOR.setPreferredSize(new Dimension(
                have_child ? 15 : 25,
                0
        ));
        SEPARATOR.setBackground(new Color(58, 61, 63));
        SEPARATOR.setVisible(!have_child);

        GridBagConstraints c = new GridBagConstraints();

        if (have_child) {
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            this.add(menu_button, c);

            c.weightx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            this.add(SEPARATOR, c);

            c.gridx = 1;
            c.weightx = 1;
            this.add(CHILD_PANEL, c);
        }
        else {
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            c.gridx = 1;
            c.gridy = 0;
            this.add(menu_button, c);

            c.gridx = 0;
            c.weightx = 0;
            this.add(SEPARATOR, c);
        }
    }

    private void init_menu_button(String name) {
        menu_button = new MenuButton(MENU, name, have_child, SEPARATOR);

        if (have_child) {
            menu_button.addActionListener(menu_listener);
        } else {
            menu_button.addActionListener(CHILDS_DEFAULT_ACTION);
        }
    }

    public void add_child(String child_name, String full_name, boolean have_child, ActionListener action) {
        if (child_name.contains("/")) {
            String parent_name = child_name.substring(0, child_name.indexOf("/"));
            child_name = child_name.substring(child_name.indexOf("/") + 1);

            for (Component child_comp : CHILD_PANEL.getComponents()) { //controlla se ha già creato il submenu in cui aggiungere il nuovo item
                CascateItem parent = (CascateItem) child_comp; //in CHILD_PANALE sono presenti solo altri componenti CascateMenu

                if (parent.getRelativeName().equals(parent_name) && parent.have_child) {
                    parent.add_child(child_name, full_name, have_child, action);
                    return;
                }
            }

            //se non ha ancora il submenu per il nuovo item
            CascateItem subMenu = new CascateItem(MENU, parent_name, CHILDS_DEFAULT_ACTION); //crea il nuovo submenu
            subMenu.add_child(child_name, full_name, have_child, action); //aggiunge l'item al nuovo submenu

            CHILD_PANEL.add(subMenu); //agginge il submenu fra i suoi item
        } else {
            CHILD_PANEL.add(new CascateItem(
                    MENU,
                    child_name,
                    full_name,
                    have_child,
                    (action == null)? CHILDS_DEFAULT_ACTION : action
            ));
        }
    }

    public void remove_child(String name) {
        if (name.contains("/")) {
            String parent_name = name.substring(0, name.indexOf("/"));
            name = name.substring(name.indexOf("/") + 1);

            for (Component child_comp : CHILD_PANEL.getComponents()) {
                CascateItem child = (CascateItem) child_comp;

                if (child.getRelativeName().equals(parent_name) && child.have_child) {
                    child.remove_child(name);
                    return;
                }
            }
        }
        else {
            for (Component child_comp : CHILD_PANEL.getComponents()) {
                CascateItem child = (CascateItem) child_comp;

                if (child.getRelativeName().equals(name)) {
                    CHILD_PANEL.remove(child);
                    CHILD_PANEL.updateUI();
                }
            }
        }
    }

    public void unselect() {
        menu_button.unselect();
    }

    public String getRelativeName() {
        return FULL_NAME.substring(FULL_NAME.lastIndexOf("/") + 1);
    }

    public String getFullName() {
        return FULL_NAME;
    }

    public boolean have_child() {
        return have_child;
    }

    public void hide_submenu() {
        if (have_child) {
            for (Component subMenu_comp : CHILD_PANEL.getComponents()) {
                if (subMenu_comp instanceof CascateItem) {
                    ((CascateItem) subMenu_comp).hide_submenu(); //nasconde i submenu del submenu
                }
            }
            CHILD_PANEL.setVisible(false);
            SEPARATOR.setVisible(false);

            menu_button.close();
        }
    }

    private final ActionListener menu_listener = _ -> {
        if (CHILD_PANEL.isVisible()) {
            hide_submenu();
        } else {
            CHILD_PANEL.setVisible(true);
            SEPARATOR.setVisible(true);

            menu_button.open();
        }
    };
}

class MenuButton extends JButton {
    private static final ImageIcon CLOSE_ICON = new ImageIcon(SettingsFrame.class.getResource("/images/menu_closed.png"));
    private static final ImageIcon OPEN_ICON = new ImageIcon(SettingsFrame.class.getResource("/images/menu_open.png"));
    protected static final Color SELECTED_BACKGROUND = new Color(88, 91, 93);
    protected static final Color UNSELECTED_BACKGROUND = new Color(58, 61, 63);

    private CascateMenu menu;
    private JLabel text, icon;
    private JPanel separator;
    private boolean paint_sep;

    public MenuButton(CascateMenu menu, String name, boolean have_child, JPanel sep) {
        super();
        this.menu = menu;
        this.separator = sep;
        this.paint_sep = !have_child;

        this.setLayout(new BorderLayout());
        this.addMouseListener(listener);
        this.setBackground(new Color(58, 61, 63));
        this.setForeground(Color.lightGray);
        this.setHorizontalAlignment(SwingConstants.LEFT);
        this.setFocusPainted(false);
        this.setBorder(null);

        icon = new JLabel(CLOSE_ICON);
        text = new JLabel(name, SwingConstants.LEFT);

        text.setForeground(Color.lightGray);
        text.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        icon.setBorder(null);
        icon.setOpaque(false);

        if (have_child) {
            this.add(icon, BorderLayout.WEST);
        }
        this.add(text, BorderLayout.CENTER);
    }

    public void open() {
        icon.setIcon(OPEN_ICON);
    }

    public void close() {
        icon.setIcon(CLOSE_ICON);
    }

    public void select() {
        this.setBackground(SELECTED_BACKGROUND);
        if (paint_sep) separator.setBackground(SELECTED_BACKGROUND);
    }

    public void unselect() {
        this.setBackground(UNSELECTED_BACKGROUND);
        if (paint_sep) separator.setBackground(UNSELECTED_BACKGROUND);
    }

    private final MouseListener listener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {}
        @Override
        public void mouseReleased(MouseEvent mouseEvent) {}
        @Override
        public void mouseEntered(MouseEvent mouseEvent) {}
        @Override
        public void mouseExited(MouseEvent mouseEvent) {}

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            menu.set_selected((CascateItem) getParent());
            select();
        }
    };
}
