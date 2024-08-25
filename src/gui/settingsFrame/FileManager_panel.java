package gui.settingsFrame;

import files.File_interface;
import files.Pair;
import gui.custom.CascateItem;
import gui.custom.CascateMenu;
import gui.custom.GScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public abstract class FileManager_panel {
    private static final JPanel MAIN_PANEL = new JPanel();
    private static final FileEditor_panel CONTENT_PANEL = new FileEditor_panel();
    private static final CascateMenu MENU = new CascateMenu();

    private static final Map<String, Pair<String, Boolean>> changed_files = new HashMap<>(); //ricorda tutte le modifiche ai vari file aperti

    public static void init() {
        MAIN_PANEL.setBackground(new Color(58, 61, 63));
        MAIN_PANEL.setBorder(null);
        MAIN_PANEL.setLayout(new GridBagLayout());

        GScrollPane scrollPane = new GScrollPane(MENU, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(250, 0));
        scrollPane.set_scrollbar_thickness(10);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        MAIN_PANEL.add(scrollPane, c);

        c.gridx = 1;
        c.weightx = 1;
        MAIN_PANEL.add(CONTENT_PANEL, c);
    }

    public static JPanel load() {
        MENU.add_item("root", true, MENU_BUTTONS_ACTION);

        String[] files_list = File_interface.get_file_list(); //lista di tutti i file caricati
        for (String file : files_list) {
            MENU.add_item("root/" + file);
        }

        boolean valid_action_listener = true;
        valid_action_listener &= SettingsFrame.set_action_listener(SettingsFrame.OK_BUTTON, OK_LISTENER);
        valid_action_listener &= SettingsFrame.set_action_listener(SettingsFrame.APPLY_BUTTON, APPLY_LISTENER);

        if (valid_action_listener) {
            return MAIN_PANEL;
        }
        else {
            return null;
        }
    }

    public static void reset() {
        MENU.reset();
        CONTENT_PANEL.reset();
        changed_files.clear(); //dimentica tutt le modifiche
    }

    protected static void save_changes(String file_name, String content, boolean encoded) {
        changed_files.put(file_name, new Pair<>(content, encoded));
    }

    private static void save_data() {
        for (String file_name : changed_files.keySet()) {
            Pair<String, Boolean> changes = changed_files.get(file_name);

            File_interface.set_encoded(file_name, changes.el2);
            File_interface.overwrite_file(file_name, changes.el1);
        }
    }

    private static final ActionListener OK_LISTENER = _ -> {
        CONTENT_PANEL.save_changes(); //salva le ultime modifiche

        save_data(); //scrive tutte le modifiche nei rispetivi file
        SettingsFrame.hide(); //chiude il frame
    };

    private static final ActionListener APPLY_LISTENER = _ -> {
        CONTENT_PANEL.save_changes(); //salva le ultime modifiche

        save_data(); //scrive le modifiche nei rispettivi files
    };

    private static final ActionListener MENU_BUTTONS_ACTION = e -> {
        CascateItem menu = (CascateItem) ((Component) e.getSource()).getParent();
        CONTENT_PANEL.read_file(menu.getFullName());
    };
}

class FileEditor_panel extends JPanel {
    private final JTextField INFO_FIELD = new JTextField();
    private final JTextArea TEXT_AREA = new JTextArea();
    private final JButton ENCODED_CHECKBOX = new JButton();

    private static final ImageIcon CHECKBOX_DIS = new ImageIcon(FileEditor_panel.class.getResource("/images/checkbox_dis.png"));
    private static final ImageIcon CHECKBOX_SEL = new ImageIcon(FileEditor_panel.class.getResource("/images/checkbox_sel.png"));

    private String original_text = null;
    private String original_name = null;
    private boolean original_encoded = false;

    public FileEditor_panel() {
        INFO_FIELD.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
        INFO_FIELD.setFocusable(false);
        INFO_FIELD.setBackground(new Color(58, 61, 63));
        INFO_FIELD.setForeground(Color.lightGray);
        INFO_FIELD.setText("file: , encoded:");

        TEXT_AREA.setBackground(new Color(138, 141, 143));
        TEXT_AREA.setForeground(Color.darkGray.darker());
        TEXT_AREA.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.darkGray, 2),
                BorderFactory.createEmptyBorder(2, 2, 0, 0)
        ));
        TEXT_AREA.setText("file content");

        ENCODED_CHECKBOX.setBorder(null);
        ENCODED_CHECKBOX.setIcon(CHECKBOX_DIS);
        ENCODED_CHECKBOX.addActionListener(CHECKBOX_LISTENER);
        ENCODED_CHECKBOX.setOpaque(false);
        ENCODED_CHECKBOX.setContentAreaFilled(false);

        GScrollPane scrollPane = new GScrollPane(TEXT_AREA);
        scrollPane.set_scrollbar_thickness(10);
        scrollPane.setPreferredSize(new Dimension(538, 315)); //dimensioni di TEXT_AREA quando il frame è alle dimensioni minime

        this.setBackground(new Color(58, 61, 63));
        this.setLayout(new GridBagLayout());

        JPanel filler = new JPanel();
        filler.setBackground(new Color(58, 61, 63));

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        this.add(INFO_FIELD, c);

        c.gridx = 1;
        this.add(ENCODED_CHECKBOX);

        c.gridx = 2;
        c.weightx = 1;
        this.add(filler);

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1;
        c.insets.top = 0;
        c.gridwidth = 3;
        this.add(scrollPane, c);
    }

    private ActionListener CHECKBOX_LISTENER = _ -> {
        if (ENCODED_CHECKBOX.getIcon().equals(CHECKBOX_SEL)) { //se il file ora è cifrato, si vuole impostare come in chiaro
            ENCODED_CHECKBOX.setIcon(CHECKBOX_DIS);
        }
        else { //il file ora non è cifrato, si vuole iniziare a cifrare
            ENCODED_CHECKBOX.setIcon(CHECKBOX_SEL);
        }
    };

    public void reset() {
        original_text = original_name = null;
        original_encoded = false;

        INFO_FIELD.setText("file: , encoded:");
        TEXT_AREA.setText("file content");
    }

    public void save_changes() {
        boolean is_encoded = ENCODED_CHECKBOX.getIcon().equals(CHECKBOX_SEL);

        if (!original_text.equals(TEXT_AREA.getText()) || original_encoded != is_encoded) { //se ci sono state modifiche
            FileManager_panel.save_changes(original_name, TEXT_AREA.getText(), is_encoded); //salva le modifiche apportate al file
        }
    }

    public void read_file(String name) {
        String file_cont = File_interface.read_file(name);

        if (file_cont == null) { //se non riesce a leggere il file
            INFO_FIELD.setText("file: " + name + ", encoded:");
            TEXT_AREA.setText("File Not Found, or content unreadable");
        }
        else { //se riesce a leggere il contenuto del file
            if (original_name != null) { //se prima era aperto un altro file, salva le eventuali modifiche
                save_changes();
            }

            boolean is_encoded = File_interface.is_encoded(name) == 1; // non può ritornare -1 poichè già con file_cont != null sappiamo che il file esiste

            INFO_FIELD.setText("file: " + name + ", encoded:");
            ENCODED_CHECKBOX.setIcon(is_encoded? CHECKBOX_SEL: CHECKBOX_DIS);
            TEXT_AREA.setText(file_cont);

            this.updateUI();

            original_name = name;
            original_encoded = is_encoded;
            original_text = file_cont;
        }
    }
}