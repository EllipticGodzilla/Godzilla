package gui.custom;

import files.File_interface;
import gui.graphicsSettings.ButtonIcons;
import gui.graphicsSettings.GraphicsSettings;
import gui.settingsFrame.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GFrame extends JFrame {
    private final JMenuBar title_bar = new JMenuBar();
    private final GMenu FILE, CONNECTION, MOD;
    private final JButton ICONIZE, MAXIMIZE, EXIT;

    private final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> mouse_schedule;

    public GFrame(String name) {
        super(name);

        this.FILE = new GMenu("File", "title_bar");
        this.CONNECTION = new GMenu("Connection", "title_bar");
        this.MOD = new GMenu("Mod", "title_bar");
        this.ICONIZE = new JButton();
        this.MAXIMIZE = new JButton();
        this.EXIT = new JButton();

        EXIT.setOpaque(false);
        MAXIMIZE.setOpaque(false);
        ICONIZE.setOpaque(false);
        EXIT.setContentAreaFilled(false);
        MAXIMIZE.setContentAreaFilled(false);
        ICONIZE.setContentAreaFilled(false);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBackground(new Color(58, 61, 63));
        this.setSize(new Dimension(900, 500));
        this.setUndecorated(true);

        FrameResizer resizer = new FrameResizer();
        this.addMouseListener(resizer);
        this.addMouseMotionListener(resizer);

        update_colors();
    }

    public void update_colors() {
        title_bar.setBackground((Color) GraphicsSettings.active_option.get_value("title_bar_background"));
        FILE.update_colors();
        CONNECTION.update_colors();
        MOD.update_colors();

        set_buttons_icon();
    }

    public int init_title_bar() {
        //inizializza tutti i componenti
        init_file();
        init_mod();
        init_connections();
        set_buttons_icon();

        EXIT.setBorder(null);
        EXIT.addActionListener(_ -> System.exit(0)); //chiude tutto

        MAXIMIZE.setBorder(null);
        MAXIMIZE.addActionListener(_ -> toggle_max_dim());

        ICONIZE.setBorder(null);
        ICONIZE.addActionListener(_ -> this.setState(JFrame.ICONIFIED));

        //aggiunge tutti i componenti alla barra
        title_bar.add(FILE);
        title_bar.add(CONNECTION);
        title_bar.add(MOD);

        title_bar.add(Box.createHorizontalGlue());

        title_bar.add(ICONIZE);
        title_bar.add(MAXIMIZE);
        title_bar.add(EXIT);

        title_bar.addMouseListener(title_bar_MListener);

        this.setJMenuBar(title_bar);

        return title_bar.getPreferredSize().height;
    }

    MouseListener title_bar_MListener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {}
        @Override
        public void mouseEntered(MouseEvent mouseEvent) {}
        @Override
        public void mouseExited(MouseEvent mouseEvent) {}

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            //inizia  a muovere il frame per seguire il mouse
            follow = true;
            click_point = mouseEvent.getPoint();

            mouse_schedule = SCHEDULED_EXECUTOR.scheduleAtFixedRate(follow_cursor,0, 10, TimeUnit.MILLISECONDS);

            //controlla per il doppio click
            if (first_click) {
                double_click = true;
            }
            else {
                first_click = true;
                SCHEDULED_EXECUTOR.schedule(check_double_click, 200, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) { //smette di seguire il mouse
            follow = false;
        }
    };

    //    MOUSE FOLLOWING

    private boolean follow = false;
    private Point click_point = null;

    private final Runnable follow_cursor = () -> {
        if (follow) {
            Point mouse_position = MouseInfo.getPointerInfo().getLocation();

            setLocation(
                    mouse_position.x - click_point.x,
                    mouse_position.y - click_point.y
            );
        }
        else {
            mouse_schedule.cancel(true);
        }
    };

    //    DOUBLE CLICK MAXIMIZING

    boolean double_click = false;
    boolean first_click = false;

    Runnable check_double_click = () -> {
        if (double_click) { toggle_max_dim(); }

        first_click = double_click = false; //resetta tutto
    };

    //    INITIALIZING GUI PIECES

    private void init_file() {
        GMenuItem manage = new GMenuItem("Manage files", "title_bar"); //puoi modificare il contenuto dei file facilmente, non potendo farlo aprendo i file normalmente essendo cifrati
        GMenuItem reload = new GMenuItem("Reload all files from disk", "title_bar"); //ricarica tutti i file dal disco
        GMenuItem save  = new GMenuItem("Update all file", "title_bar"); //salva tutte le informazioni nel disco come se stesse chiudendo il programma in questo momento
        GMenuItem settings = new GMenuItem("Settings", "title_bar"); //puoi modificare i colori standard
        GMenuItem exit = new GMenuItem("Exit", "title_bar"); //chiude il programma

        manage.addActionListener(FILE_MANAGE_LISTENER);
        reload.addActionListener(FILE_RELOAD_LISTENER);
        save.addActionListener(FILE_SAVE_LISTENER);
        settings.addActionListener(FILE_SETTINGS_LISTENER);
        exit.addActionListener(FILE_EXIT_LISTENER);

        FILE.add(manage);
        FILE.add(reload);
        FILE.add(save);
        FILE.add(settings);
        FILE.add(exit);

        FILE.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    }

    private void init_mod() {
        JMenuItem manage = new GMenuItem("Manage mods", "title_bar"); //puoi importare o rimuovere mod
        JMenuItem reload_all = new GMenuItem("Reload all mods from disk", "title_bar"); //ricarica tutte le mod dal disco
        JMenuItem manage_startup = new GMenuItem("Manage startup mods", "title_bar"); //imposta o rimuove le mod da eseguire all'avvio

        manage.addActionListener(MOD_MANAGE_LISTENER);
        reload_all.addActionListener(MOD_RELOAD_LISTENER);
        manage_startup.addActionListener(MOD_STARTING_LISTENER);

        MOD.add(manage);
        MOD.add(reload_all);
        MOD.add(manage_startup);

        MOD.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    }

    private void init_connections() {
        JMenuItem manage_server = new GMenuItem("Manage servers", "title_bar"); //puoi modificare nome, link, dns di riferimento ed in futuro ci saranno anche varie impostazioni di sicurezza
        JMenuItem manage_dns = new GMenuItem("Manage dns", "title_bar"); //aggiungere / rimuovere dns, modificare la chiave pubblica associata
        JMenuItem add = new GMenuItem("Add server", "title_bar"); //aggiungi un nuovo server

        manage_server.addActionListener(CONN_SERVERMANAGE_LISTENER);
        manage_dns.addActionListener(CONN_DNSMANAGE_LISTENER);
        add.addActionListener(CONN_ADDSERVER_LISTENER);

        CONNECTION.add(manage_server);
        CONNECTION.add(manage_dns);
        CONNECTION.add(add);

        CONNECTION.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    }

    private void set_buttons_icon() {
        ButtonIcons max_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("title_bar_maximize");
        ButtonIcons min_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("title_bar_iconize");
        ButtonIcons close_icons = (ButtonIcons) GraphicsSettings.active_option.get_value("title_bar_close");

        MAXIMIZE.setIcon(max_icons.getStandardIcon());
        MAXIMIZE.setRolloverIcon(max_icons.getRolloverIcon());
        MAXIMIZE.setPressedIcon(max_icons.getPressedIcon());
        ICONIZE.setIcon(min_icons.getStandardIcon());
        ICONIZE.setRolloverIcon(min_icons.getRolloverIcon());
        ICONIZE.setPressedIcon(min_icons.getPressedIcon());
        EXIT.setIcon(close_icons.getStandardIcon());
        EXIT.setRolloverIcon(close_icons.getRolloverIcon());
        EXIT.setPressedIcon(close_icons.getPressedIcon());
    }

    private void toggle_max_dim() {
        this.setExtendedState(this.getExtendedState() ^ JFrame.MAXIMIZED_BOTH);
    }

    //    ACTIONS OF THE MENUITEM
    private final ActionListener FILE_MANAGE_LISTENER = _ -> {
        SettingsFrame.show(SettingsFrame.FILE_MANAGER);
    };

    private final ActionListener FILE_RELOAD_LISTENER = _ -> {
        File_interface.reload_from_disk();
    };

    private final ActionListener FILE_SAVE_LISTENER = _ -> {
        File_interface.update_files();
    };

    private final ActionListener FILE_SETTINGS_LISTENER = _ -> {
        SettingsFrame.show(SettingsFrame.SETTINGS);
    };

    private final ActionListener FILE_EXIT_LISTENER = _ -> System.exit(0);

    private final ActionListener MOD_MANAGE_LISTENER = _ -> {
        SettingsFrame.show(SettingsFrame.MOD_MANAGER);
    };

    private final ActionListener MOD_RELOAD_LISTENER = _ -> {

    };

    private final ActionListener MOD_STARTING_LISTENER = _ -> {
        SettingsFrame.show(SettingsFrame.STARTMOD_MANAGER);
    };

    private final ActionListener CONN_SERVERMANAGE_LISTENER = _ -> {
        SettingsFrame.show(SettingsFrame.SERVER_MANAGER);
    };

    private final ActionListener CONN_DNSMANAGE_LISTENER = _ -> {
        SettingsFrame.show(SettingsFrame.DNS_MANAGER);
    };

    private final ActionListener CONN_ADDSERVER_LISTENER = _ -> {

    };
}

//    LET THE USER RESIZE THE JFRAME, revisit of https://github.com/tips4java/tips4java/blob/main/source/ComponentResizer.java
class FrameResizer extends MouseAdapter {
    private final Dimension MIN_SIZE = new Dimension(900, 500);

    private int direction;
    protected static final int NORTH = 1;
    protected static final int WEST = 2;
    protected static final int SOUTH = 4;
    protected static final int EAST = 8;

    private static final int RESIZE_EVENT_BORDER = 5;

    private Cursor sourceCursor;
    private boolean resizing;
    private Rectangle bounds;
    private Point pressed;

    private static final Map<Integer, Integer> CURSORS = new HashMap<>();
    static {
        CURSORS.put(1, Cursor.N_RESIZE_CURSOR);
        CURSORS.put(2, Cursor.W_RESIZE_CURSOR);
        CURSORS.put(4, Cursor.S_RESIZE_CURSOR);
        CURSORS.put(8, Cursor.E_RESIZE_CURSOR);
        CURSORS.put(3, Cursor.NW_RESIZE_CURSOR);
        CURSORS.put(9, Cursor.NE_RESIZE_CURSOR);
        CURSORS.put(6, Cursor.SW_RESIZE_CURSOR);
        CURSORS.put(12, Cursor.SE_RESIZE_CURSOR);
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        Component source = e.getComponent();
        Point location = e.getPoint();
        direction = 0;

        if (location.x < RESIZE_EVENT_BORDER)
            direction += WEST;

        if (location.x > source.getWidth() - RESIZE_EVENT_BORDER - 1)
            direction += EAST;

        if (location.y < RESIZE_EVENT_BORDER)
            direction += NORTH;

        if (location.y > source.getHeight() - RESIZE_EVENT_BORDER - 1)
            direction += SOUTH;

        //  Mouse is no longer over a resizable border

        if (direction == 0)
        {
            source.setCursor( sourceCursor );
        }
        else  // use the appropriate resizable cursor
        {
            int cursorType = CURSORS.get( direction );
            Cursor cursor = Cursor.getPredefinedCursor( cursorType );
            source.setCursor( cursor );
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        if (! resizing)
        {
            Component source = e.getComponent();
            sourceCursor = source.getCursor();
        }
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        if (! resizing)
        {
            Component source = e.getComponent();
            source.setCursor( sourceCursor );
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        //	The mouseMoved event continually updates this variable

        if (direction == 0) return;

        //  Setup for resizing. All future dragging calculations are done based
        //  on the original bounds of the component and mouse pressed location.

        resizing = true;

        Component source = e.getComponent();
        pressed = e.getPoint();
        SwingUtilities.convertPointToScreen(pressed, source);
        bounds = source.getBounds();
    }

    /**
     *  Restore the original state of the Component
     */
    @Override
    public void mouseReleased(MouseEvent e)
    {
        resizing = false;

        Component source = e.getComponent();
        source.setCursor( sourceCursor );

        Component parent = source.getParent();

        if (parent != null)
        {
            if (parent instanceof JComponent)
            {
                ((JComponent)parent).revalidate();
            }
            else
            {
                parent.validate();
            }
        }
    }

    /**
     *  Resize the component ensuring location and size is within the bounds
     *  of the parent container and that the size is within the minimum and
     *  maximum constraints.
     *
     *  All calculations are done using the bounds of the component when the
     *  resizing started.
     */
    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (resizing == false) return;

        Component source = e.getComponent();
        Point dragged = e.getPoint();
        SwingUtilities.convertPointToScreen(dragged, source);

        changeBounds(source, direction, bounds, pressed, dragged);
    }

    protected void changeBounds(Component source, int direction, Rectangle bounds, Point pressed, Point current)
    {
        //  Start with original locaton and size

        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;

        //  Resizing the West or North border affects the size and location

        if (WEST == (direction & WEST))
        {
            int drag = pressed.x - current.x;
            int maximum = Math.min(width + x - 10, Integer.MAX_VALUE);
            drag = getDragBounded(drag, width, MIN_SIZE.width, maximum);

            x -= drag;
            width += drag;
        }

        if (NORTH == (direction & NORTH))
        {
            int drag = pressed.y - current.y;
            int maximum = Math.min(height + y - 10, Integer.MAX_VALUE);
            drag = getDragBounded(drag, height, MIN_SIZE.height, maximum);

            y -= drag;
            height += drag;
        }

        //  Resizing the East or South border only affects the size

        if (EAST == (direction & EAST))
        {
            int drag = current.x - pressed.x;
            Dimension boundingSize = getBoundingSize( source );
            int maximum = Math.min(boundingSize.width - x, Integer.MAX_VALUE);
            drag = getDragBounded(drag, width, MIN_SIZE.width, maximum);
            width += drag;
        }

        if (SOUTH == (direction & SOUTH))
        {
            int drag = current.y - pressed.y;
            Dimension boundingSize = getBoundingSize( source );
            int maximum = Math.min(boundingSize.height - y, Integer.MAX_VALUE);
            drag = getDragBounded(drag, height, MIN_SIZE.height, maximum);
            height += drag;
        }

        source.setBounds(x, y, width, height);
        source.validate();
    }

    /*
     *  Adjust the drag value to be within the minimum and maximum range.
     */
    private int getDragBounded(int drag, int dimension, int minimum, int maximum)
    {
        if (dimension + drag < minimum)
            drag = minimum - dimension;

        if (dimension + drag > maximum)
            drag = maximum - dimension;

        return drag;
    }

    /*
     *  Keep the size of the component within the bounds of its parent.
     */
    private Dimension getBoundingSize(Component source)
    {
        if (source instanceof Window)
        {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = env.getMaximumWindowBounds();
            return new Dimension(bounds.width, bounds.height);
        }
        else
        {
            Dimension d = source.getParent().getSize();
            d.width += -10;
            d.height += -10;
            return d;
        }
    }
}