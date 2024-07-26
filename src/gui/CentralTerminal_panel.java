package gui;

import file_database.Database;
import gui.custom.GLayeredPane;
import gui.custom.GScrollPane;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class CentralTerminal_panel {
    private static TerminalJTextArea terminal = new TerminalJTextArea();

    private static JPanel terminal_panel = new JPanel();
    private static JPanel programmable_panel = new JPanel();
    private static GScrollPane terminal_scroller;
    private static GLayeredPane layeredPane = null;

    private static final Color ERROR_BACKGROUND = Color.lightGray;
    private static final Color ERROR_FOREGROUND = Color.red.darker();

    protected static JLayeredPane init() {
        if (layeredPane == null) {
            layeredPane = new GLayeredPane();
            terminal_scroller = new GScrollPane(terminal);

            terminal_scroller.setBackground(new Color(128, 131, 133));
            programmable_panel.setVisible(false);

            terminal_panel.setLayout(new GridLayout(1, 1));
            programmable_panel.setLayout(null);

            terminal_panel.add(terminal_scroller);

            layeredPane.add_fullscreen(programmable_panel, Integer.valueOf(0));
            layeredPane.add_fullscreen(terminal_panel, Integer.valueOf(1));

            if (Database.DEBUG) {
                terminal_write("!!! DEBUG MODE ON !!!\n", false);
            }
        }
        return layeredPane;
    }

    public synchronized static void terminal_write(String txt, boolean error) {
        EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();

        if (error) {
            eventQueue.postEvent(new TerminalEvent(terminal, txt, ERROR_BACKGROUND, ERROR_FOREGROUND, true));
        }
        else {
            eventQueue.postEvent(new TerminalEvent(terminal, txt));
        }
    }

    public static JPanel programming_mode() {
        terminal_panel.setVisible(false);
        programmable_panel.setVisible(true);

        return programmable_panel;
    }

    public static void reset_panel() { //ritorna a mostrare il terminale
        programmable_panel.removeAll();
        programmable_panel.setVisible(false);
        programmable_panel.setLayout(null);

        terminal_panel.setVisible(true);
    }

    public static String get_terminal_log() {
        return terminal.getText();
    }

    private static class TerminalJTextArea extends JTextPane {
        private Document doc;

        public TerminalJTextArea() {
            super();

            this.doc = this.getStyledDocument();
            this.enableEvents(TerminalEvent.ID); //accetta gli eventi del terminal

            this.setBackground(Color.BLACK);
            this.setForeground(Color.lightGray);
            this.setCaretColor(Color.WHITE);
            this.setSelectionColor(new Color(180, 180, 180));
            this.setSelectedTextColor(new Color(30, 30, 30));

            this.setEditable(false);
            this.setAutoscrolls(true);

            this.setText(" ================================== Client Starting " + get_data_time() + " ==================================\n");
        }

        private static String get_data_time() {
            String pattern = "dd.MM.yyyy - HH:mm.ss";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Calendar c = Calendar.getInstance();

            return sdf.format(c.getTime());
        }

        @Override
        protected void processEvent(AWTEvent e) {
            if (e instanceof TerminalEvent) {
                try {
                    TerminalEvent event = (TerminalEvent) e; //ricava l'oggetto TerminalEvent
                    SimpleAttributeSet attrib = new SimpleAttributeSet();

                    StyleConstants.setForeground(attrib, event.get_foreground()); //imposta la grafica del testo che vuole aggiungere al terminale
                    StyleConstants.setBackground(attrib, event.get_background());
                    StyleConstants.setBold(attrib, event.is_bold());

                    //aggiunge il testo al terminale e "va in giù" con la scrollbar
                    doc.insertString(doc.getLength(), event.get_txt(), attrib);
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else {
                super.processEvent(e);
            }
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getUI().getPreferredSize(this).width <= getParent().getSize().width;
        }
    }

}

class TerminalEvent extends AWTEvent {
    public static final int ID = AWTEvent.RESERVED_ID_MAX + 1;
    private static int counter = 0;

    private String txt;
    private Color background = Color.BLACK;
    private Color foreground = Color.lightGray.brighter();
    private boolean bold;

    public TerminalEvent(Object source, String txt) {
        super(source, ID);
        this.txt = txt;
    }

    public TerminalEvent(Object source, String txt, Color background, Color foreground, boolean bold) {
        super(source, ID);

        this.txt = txt;
        this.background = background;
        this.foreground = foreground;
        this.bold = bold;
    }

    public String get_txt() {
        return this.txt;
    }
    public Color get_background() { return background; }
    public Color get_foreground() { return foreground; }
    public boolean is_bold() { return bold; }
}