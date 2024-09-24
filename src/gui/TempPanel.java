package gui;

import files.Pair;
import gui.custom.GComboBox;
import gui.custom.GPasswordField;
import gui.custom.GTextArea;
import gui.graphicsSettings.ButtonIcons;
import gui.graphicsSettings.GraphicsSettings;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Vector;

abstract public class TempPanel {
    private static final JButton OK_BUTTON = new JButton();
    private static final JButton ANNULLA_BUTTON = new JButton();
    private static final int MIN_WIDTH = 220; //ok_button.width + annulla_button.width + 30 (insects)
    private static final int MIN_HEIGHT = 40; //butto.height + 20 (insects)

    public static Vector<Component> input_array = new Vector<>();
    private static TempPanel_action action = null;
    private static Vector<Pair<TempPanel_info, TempPanel_action>> queue = new Vector<>();

    private static JPanel temp_panel = null; //temp panel
    private static final JPanel TXT_PANEL = new JPanel(); //pannello che contiene le txt area
    private static boolean visible = false;
    private static boolean accept_esc_or_enter = false; //se possono essere utilizzati i tasti esc o invio al posto di premere i bottoni annulla, ok

    public static JPanel init() throws IOException {
        if (temp_panel == null) {
            //imposta layout, background, border dei JPanel
            temp_panel = new JPanel();
            temp_panel.setLayout(new GridBagLayout());
            TXT_PANEL.setLayout(new GridBagLayout());
            TXT_PANEL.setOpaque(false);
            TXT_PANEL.setBorder(null);

            //imposta i colori e le icone dei pulsanti
            update_colors();
            GraphicsSettings.run_at_theme_change(TempPanel::update_colors);

            //inizializza i bottoni ok e annulla
            OK_BUTTON.addActionListener(OK_LISTENER);
            ANNULLA_BUTTON.addActionListener(annulla_listener);

            OK_BUTTON.setBorder(null);
            ANNULLA_BUTTON.setBorder(null);

            OK_BUTTON.setOpaque(false);
            ANNULLA_BUTTON.setOpaque(false);
            OK_BUTTON.setContentAreaFilled(false);
            ANNULLA_BUTTON.setContentAreaFilled(false);

            OK_BUTTON.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                @Override
                public void keyReleased(KeyEvent e) {}

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == 10 && accept_esc_or_enter) { //se viene premuto invio è come premere ok
                        OK_BUTTON.doClick();
                    }
                    else if (ANNULLA_BUTTON.isVisible() && e.getKeyCode() == 27 && accept_esc_or_enter) { //se viene premuto esc è come premere annulla
                        ANNULLA_BUTTON.doClick();
                    }
                }
            });

            //aggiunge gli elementi al tempPanel
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(10, 10, 0, 0);
            c.weighty = 1;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            temp_panel.add(TXT_PANEL, c);

            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.FIRST_LINE_START;
            c.insets = new Insets(0, 10, 10, 10);
            c.weighty = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            temp_panel.add(ANNULLA_BUTTON, c);

            c.anchor = GridBagConstraints.FIRST_LINE_END;
            c.insets.left = 0;
            c.gridx = 1;
            temp_panel.add(OK_BUTTON, c);
        }
        return temp_panel;
    }

    public static void update_colors() {
        temp_panel.setBackground((Color) GraphicsSettings.active_theme.get_value("frame_background"));
        temp_panel.setBorder((Border) GraphicsSettings.active_theme.get_value(("temp_panel_border")));

        ButtonIcons ok_icons = (ButtonIcons) GraphicsSettings.active_theme.get_value("temp_panel_ok");
        ButtonIcons annulla_icons = (ButtonIcons) GraphicsSettings.active_theme.get_value("temp_panel_annulla");

        OK_BUTTON.setIcon(ok_icons.getStandardIcon());
        OK_BUTTON.setPressedIcon(ok_icons.getPressedIcon());
        OK_BUTTON.setRolloverIcon(ok_icons.getRolloverIcon());
        ANNULLA_BUTTON.setIcon(annulla_icons.getStandardIcon());
        ANNULLA_BUTTON.setPressedIcon(annulla_icons.getPressedIcon());
        ANNULLA_BUTTON.setRolloverIcon(annulla_icons.getRolloverIcon());

        temp_panel.repaint();
    }

    public static void recenter_in_frame() {
        Rectangle frame_bounds = Godzilla_frame.get_bounds();

        temp_panel.setLocation(
                (int) (frame_bounds.getWidth()) / 2 - temp_panel.getWidth() / 2,
                (int) (frame_bounds.getHeight()) / 2 - temp_panel.getHeight() / 2
        );
    }

    private static final ActionListener OK_LISTENER = _ -> {
        accept_esc_or_enter = false; //non permette più di utilizzare i tasti invio ed esc

        //copia tutti i testi contenuti nelle input area in questo array, utilizza Object perchè per le password non sono String ma char[]
        Vector<Object> input_txt = new Vector<>();

        for (Component comp : input_array) {
            if (comp instanceof JPasswordField) {
                input_txt.add(((JPasswordField) comp).getPassword());
            }
            else if (comp instanceof JTextField) {
                input_txt.add(((JTextField) comp).getText());
            }
            else if (comp instanceof JComboBox) {
                input_txt.add(((JComboBox<?>) comp).getSelectedItem());
            }
        }
        input_array.clear();

        TempPanel_action action = TempPanel.action; //memorizza l'azione da eseguire per questo panel
        reset(); //resetta tutta la grafica e fa partire il prossimo in coda

        if (action != null) { //se è stata specificata un azione
            action.input.removeAllElements();
            action.input.addAll(input_txt); //fa partire il codice con tutti gli input ricavati

            new Thread(action::success).start();
        }
    };

    private static final ActionListener annulla_listener = _ -> {
        input_array.removeAllElements(); //rimuove tutti gli input precedenti

        reset(); //resetta tutta la grafica e fa partire il prossimo in coda

        if (action != null) {
            new Thread(() -> action.fail()).start();
        }
    };

    public static void show(TempPanel_info info, TempPanel_action action) {
        if (!visible) {
            try {
                //imposta questa azione come quella da eseguire una volta chiusa la finestra
                TempPanel.action = action;

                //imposta la visibilità del bottone annulla
                ANNULLA_BUTTON.setVisible(info.annulla_vis());

                //distingue nei vari tipi di finestra
                int panel_type = info.get_type();
                if (panel_type == TempPanel_info.INPUT_REQ) { //se richiede degli input
                    request_input(info.get_txts(), info.request_psw(), info.get_requests_info(), info);
                }
                else if (panel_type == TempPanel_info.SINGLE_MSG) { //se mostra un singolo messaggio
                    show_msg(info.get_txts());
                }
                else if (panel_type == TempPanel_info.DOUBLE_COL_MSG) {
                    show_dual_con_msg(info.get_txts());
                }

                //attende 0.2s e poi permette l'utilizzo dei tasti esc e invio
                new Thread(() -> {
                    try {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException _) {}

                    accept_esc_or_enter = true;
                }).start();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            queue.add(new Pair<>(info, action)); //aggiunge questa richiesta alla coda
        }
    }

    private static void show_msg(String[] txts) { //mostra un messaggio
        //aggiunge tutte le linee a txt_panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 10);

        for (int i = 0; i < txts.length; i++) {
            GTextArea line_area = new GTextArea(txts[i]);

            c.insets.bottom = (i == txts.length - 1)? 10 : 0;
            TXT_PANEL.add(line_area, c);

            c.gridy ++;
        }

        show_panel(TXT_PANEL.getPreferredSize().width + 20, TXT_PANEL.getPreferredSize().height + 10); //rende visibile il pannello
        OK_BUTTON.requestFocus(); //richiede il focus, in modo che se premuto invio appena il popup compare equivale a premere "ok"
    }

    private static void show_dual_con_msg(String[] txts) {
        //aggiunge tutte le linee a txt_panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 10);

        for (int i = 0; i < txts.length; i++) {
            GTextArea line_area1 = new GTextArea(txts[i]);
            GTextArea line_area2 = new GTextArea(txts[++i]);

            c.insets.bottom = (i == txts.length - 1)? 10 : 0;
            c.gridx = 0;
            TXT_PANEL.add(line_area1, c);

            c.gridx = 1;
            TXT_PANEL.add(line_area2, c);

            c.gridy ++;
        }

        show_panel(TXT_PANEL.getPreferredSize().width + 20, TXT_PANEL.getPreferredSize().height + 10); //rende visibile il pannello
        OK_BUTTON.requestFocus(); //richiede il focus, in modo che se premuto invio appena il popup compare equivale a premere "ok"
    }

    private static void request_input(String[] requests, boolean request_psw, int[] req_types, TempPanel_info info) throws IOException {
        int max_width = 0; //contiene la lunghezza della JTextArea che contiene il messaggio più lungo

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 10, 10);

        //genera e aggiunge al pannello txt_panel tutti le JTextArea
        for (c.gridy = 0; c.gridy < requests.length; c.gridy++) {
            //genera il JTextField che mostra il messaggio per richiedere l'input e lo aggiunge al pannello
            GTextArea msg_area = new GTextArea(requests[c.gridy]);
            if (msg_area.getPreferredSize().width > max_width) {
                max_width = msg_area.getPreferredSize().width;
            }

            c.weightx = 1;
            c.gridx = 0;
            TXT_PANEL.add(msg_area, c);

            Component input_comp = null;
            //aggiunge il TextField dove poter inserire l'input richiesto
            if (req_types[c.gridy] == TempPanel_info.NORMAL_REQUEST) { //se è una richiesta con JTextFiled normale
                JTextField input_field = new EditTextField(c.gridy);
                input_comp = input_field;

                //aggiunge al pannello input_field
                c.weightx = 0;
                c.gridx = 1;
                c.gridwidth = (request_psw)? 2 : 1; //se richiede delle password i campi di inserimento normali si estendono anche nella colonna del pulsante per mostrare il testo delle password
                TXT_PANEL.add(input_field, c);

                c.gridwidth = 1; //resetta gridwidth
            }
            else if (req_types[c.gridy] == TempPanel_info.PASSWORD_REQUEST) { //richiede una password
                GPasswordField input_field = new GPasswordField();
                input_field.addKeyListener(new Enter_listener(c.gridy));
                input_comp = input_field;

                //aggiunge al pannello PasswordField ed il pulsante per togglare la visibilità della scritta
                c.weightx = 0;
                c.gridx = 1;
                c.insets.right = 3;
                TXT_PANEL.add(input_field, c);

                c.gridx = 2;
                c.insets.right = 10;
                TXT_PANEL.add(input_field.get_toggle_button(), c);
            }
            else if (req_types[c.gridy] == TempPanel_info.COMBO_BOX_REQUEST) { //richiede una combo box
                JComboBox<String> combo_box = new GComboBox(info.get_cbox_info(c.gridy));
                combo_box.addKeyListener(new Enter_listener(c.gridy));

                input_comp = combo_box;

                //aggiunge il combo box al pannello
                c.weightx = 0;
                c.gridx = 1;
                c.gridwidth = (request_psw)? 2 : 1; //se richiede delle password ci si deve espandere nella colonna del pulsante per mostrare il testo delle password
                TXT_PANEL.add(combo_box, c);

                c.gridwidth = 1; //resetta gridwidth
            }

            input_array.add(input_comp); //aggiunge gli input_field in un vettore per poi ricavarne i testi inseriti
        }

        show_panel(max_width + EditTextField.WIDTH + 30, (EditTextField.HEIGHT + 10) * input_array.size());
        input_array.elementAt(0).requestFocusInWindow(); //richiede il focus nella prima input area
    }

    private static void show_panel(int comp_width, int comp_height) {
        //calcola le dimensioni
        temp_panel.setSize(
                Math.max(comp_width, MIN_WIDTH),
                comp_height + MIN_HEIGHT
        );

        //disattiva tutti i pannelli in Godzilla_frame e ricentra gui.TempPanel
        Godzilla_frame.disable_panels();
        recenter_in_frame();

        //mostra il pannello
        temp_panel.setVisible(true);
        temp_panel.updateUI();
        visible = true;
    }

    private static void reset() {
        //resetta il pannello e lo rende invisibile
        visible = false;
        ANNULLA_BUTTON.setVisible(true);
        temp_panel.setVisible(false);
        TXT_PANEL.removeAll();

        if (!queue.isEmpty()) { //se c'è qualche elemento nella coda
            Pair<TempPanel_info, TempPanel_action> next_in_queue = queue.elementAt(0); //mostra il prossimo elemento nella coda
            queue.removeElementAt(0);
            show(next_in_queue.el1, next_in_queue.el2);
        }
        else { //la coda è vuota
            Godzilla_frame.enable_panels(); //riattiva tutti i pannelli in Godzilla_frame
        }
    }

    private static class EditTextField extends JTextField {
        protected static final int WIDTH  = 150;
        protected static final int HEIGHT = 20;

        public EditTextField(int index) {
            super();

            this.setBackground((Color) GraphicsSettings.active_theme.get_value("input_background"));
            this.setBorder((Border) GraphicsSettings.active_theme.get_value("input_border"));
            this.setFont(new Font("Arial", Font.BOLD, 14));
            this.setForeground((Color) GraphicsSettings.active_theme.get_value("input_text_color"));

            this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            this.setMinimumSize(this.getPreferredSize());

            this.addKeyListener(new Enter_listener(index));
        }
    }

    private static class Enter_listener implements KeyListener {
        private final int INDEX;

        public Enter_listener(int index) {
            this.INDEX = index;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == 10) { //10 -> enter
                try {
                    Component input_cmp = input_array.elementAt(INDEX + 1);
                    if (input_cmp instanceof JTextField) { //passa il focus all'input successivo
                        ((JTextField) input_cmp).grabFocus();
                    }
                    else if (input_cmp instanceof JComboBox<?>) {
                        ((JComboBox<?>) input_cmp).grabFocus();
                    }
                }
                catch (Exception ex) { //se non esiste un input con index > di questo
                    if (accept_esc_or_enter) {
                        OK_BUTTON.doClick(); //simula il tasto "ok"
                    }
                }
            }
            else if (e.getKeyCode() == 27) { //27 -> esc
                if (ANNULLA_BUTTON.isVisible() && accept_esc_or_enter) {
                    ANNULLA_BUTTON.doClick();
                }
            }
        }
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {}
    }
}