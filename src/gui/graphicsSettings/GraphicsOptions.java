package gui.graphicsSettings;

import files.Logger;
import gui.graphicsSettings.standard_builder.*;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class GraphicsOptions {
    private static List<String> keys = List.of( //tutte le chiavi a cui si possono assegnare valori, viene utilizzata una lista poiche Arrays.binarySearch() non è affidabile con String
            "button_top_bar_background",
            "button_top_bar_left_shift",
            "button_top_bar_right_shift",
            "button_top_bar_stop_mod",
            "central_panel_background",
            "central_panel_icon",
            "central_panel_image_background",
            "client_panel_background",
            "client_panel_list_background",
            "client_panel_list_foreground",
            "client_panel_list_selected_background",
            "client_panel_list_selected_foreground",
            "client_panel_list_selected_border",
            "client_panel_connect",
            "client_panel_disconnect",
            "server_panel_background",
            "server_panel_list_background",
            "server_panel_list_foreground",
            "server_panel_list_selected_background",
            "server_panel_list_selected_foreground",
            "server_panel_list_selected_border",
            "server_panel_connect",
            "server_panel_disconnect",
            "server_panel_add_server",
            "title_bar_background",
            "title_bar_foreground",
            "title_bar_selected_background",
            "title_bar_selected_foreground",
            "title_bar_dropdown_background",
            "title_bar_dropdown_foreground",
            "title_bar_dropdown_border",
            "title_bar_dropdown_selected_background",
            "title_bar_dropdown_selected_foreground",
            "title_bar_close",
            "title_bar_maximize",
            "title_bar_iconize"
    );
    private static GraphicsOption_builder<?>[] builders = new GraphicsOption_builder[] {  //array con tutti i diversi builder registrati
            new Color_graphics(),
            new Icons_graphics(),
            new Image_graphics(),
            new Border_graphics()
    };
    private static int[] builders_index = new int[] {0, 1, 1, 1, 0, 2, 0, 0, 0, 0, 0, 0, 3, 1, 1, 0, 0, 0, 0, 0, 3, 1, 1, 1, 0, 0, 0, 0, 0, 0, 3, 0, 0, 1, 1, 1}; //per ogni chiave deve essere assegnato l'index del builder a lei assegnato
    private static Object[] std_values = new Object[] { //valori standard per ogni chiave
            new Color(58, 61, 63),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/left_arrow.png")),
                    new ImageIcon(get_resource("/images/left_arrow_sel.png")),
                    new ImageIcon(get_resource("/images/left_arrow_pres.png")),
                    new ImageIcon(get_resource("/images/left_arrow.png"))
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/right_arrow.png")),
                    new ImageIcon(get_resource("/images/right_arrow_sel.png")),
                    new ImageIcon(get_resource("/images/right_arrow_pres.png")),
                    new ImageIcon(get_resource("/images/right_arrow.png"))
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/power_off.png")),
                    new ImageIcon(get_resource("/images/power_off_sel.png")),
                    new ImageIcon(get_resource("/images/power_off_pres.png")),
                    new ImageIcon(get_resource("/images/power_off_dis.png"))
            ),
            new Color(58, 61, 63),
            new ImageIcon(get_resource("/images/godzilla.png")),
            new Color(0,0,0),
            new Color(58, 61, 63),
            new Color(98, 101, 103),
            new Color(44, 46, 47),
            new Color(138, 141, 143),
            new Color(44, 46, 47),
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(72, 74, 75)),
                    BorderFactory.createEmptyBorder(2, 2, 0, 0)
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/power_on.png")),
                    new ImageIcon(get_resource("/images/power_on_sel.png")),
                    new ImageIcon(get_resource("/images/power_on_pres.png")),
                    new ImageIcon(get_resource("/images/power_on_dis.png"))
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/power_off.png")),
                    new ImageIcon(get_resource("/images/power_off_sel.png")),
                    new ImageIcon(get_resource("/images/power_off_pres.png")),
                    new ImageIcon(get_resource("/images/power_off_dis.png"))
            ),
            new Color(58, 61, 63),
            new Color(98, 101, 103),
            new Color(44, 46, 47),
            new Color(138, 141, 143),
            new Color(44, 46, 47),
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(72, 74, 75)),
                    BorderFactory.createEmptyBorder(2, 2, 0, 0)
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/power_on.png")),
                    new ImageIcon(get_resource("/images/power_on_sel.png")),
                    new ImageIcon(get_resource("/images/power_on_pres.png")),
                    new ImageIcon(get_resource("/images/power_on_dis.png"))
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/power_off.png")),
                    new ImageIcon(get_resource("/images/power_off_sel.png")),
                    new ImageIcon(get_resource("/images/power_off_pres.png")),
                    new ImageIcon(get_resource("/images/power_off_dis.png"))
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/add_server.png")),
                    new ImageIcon(get_resource("/images/add_server_sel.png")),
                    new ImageIcon(get_resource("/images/add_server_pres.png")),
                    new ImageIcon(get_resource("/images/add_server_dis.png"))
            ),
            new Color(88, 91, 93),
            new Color(211, 211, 211),
            new Color(73, 76, 78),
            new Color(211, 211, 211),
            new Color(58, 61, 63),
            new Color(211, 211, 211),
            BorderFactory.createLineBorder(new Color(108, 111, 113), 2),
            new Color(73, 76, 78),
            new Color(211, 211, 211),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/exit.png")),
                    new ImageIcon(get_resource("/images/exit_pres.png")),
                    new ImageIcon(get_resource("/images/exit_pres.png")),
                    new ImageIcon(get_resource("/images/exit.png"))
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/fullScreen.png")),
                    new ImageIcon(get_resource("/images/fullScreen_pres.png")),
                    new ImageIcon(get_resource("/images/fullScreen_pres.png")),
                    new ImageIcon(get_resource("/images/fullScreen.png"))
            ),
            new ButtonIcons(
                    new ImageIcon(get_resource("/images/hide.png")),
                    new ImageIcon(get_resource("/images/hide_pres.png")),
                    new ImageIcon(get_resource("/images/hide_pres.png")),
                    new ImageIcon(get_resource("/images/hide.png"))
            )
    };

    //aggiunge un builder e ritorna l'index a cui è stato aggiunto
    public static int add_builder(GraphicsOption_builder<?> builder) {
        builders = Arrays.copyOf(builders, builders.length + 1);
        builders[builders.length - 1] = builder;

        return builders.length - 1;
    }

    //aggiunge una key con builder e valore standard
    public static boolean add_std(String key, int builder_index, Object value) { //prima che inizi GraphicsSetting è possibile aggiungere nuove chiavi ed assegnarle valori standard e builder
        if (!keys.contains(key)) { //se non è già stata registrata una chiave con questo nome
            keys.add(key);

            builders_index = Arrays.copyOf(builders_index, builders_index.length + 1);
            builders_index[builders_index.length - 1] = builder_index;

            std_values = Arrays.copyOf(std_values, std_values.length + 1);
            std_values[std_values.length - 1] = value;

            return true;
        }
        else {
            Logger.log("tentativo di registrare due volte la chiave: " + key + " in GraphicsOptions", true, '\n');
            return false;
        }
    }

    //ritorna la lista di keys
    public static String[] get_keys() {
        return keys.toArray(new String[0]);
    }

    private static String get_resource(String path) {
        return GraphicsOptions.class.getResource(path).getPath();
    }

    //ritorna il builder assegnato alla key
    public static GraphicsOption_builder<?> get_builder(String key) {
        int index = keys.indexOf(key);

        if (index != -1) {
            int builder_index = builders_index[index];

            return builders[builder_index];
        }
        else {
            Logger.log("impossibile trovare il builder assegnato alla key: " + key + ", la key non è stata registrata in GraphicsOptions", true, '\n');
            return null;
        }
    }

    private final Object[] VALUES; //valori assegnati ad ogni chiave
    private final String NAME; //nome di questa impostazione

    public GraphicsOptions(String name) {
        this.NAME = name;
        this.VALUES = std_values.clone();
    }

    public String get_name() {
        return NAME;
    }

    public void set_value(String key, String value_str) {
        int index = keys.indexOf(key);

        if (index != -1) { //se ha trovato la chiave fra le possibili
            int builder_index = builders_index[index];
            Object value = builders[builder_index].cast(value_str);

            VALUES[index] = value;
        }
        else {
            Logger.log("impossibile assegnare il valore: " + value_str + " alla key: " + key + ", la key non è stata registrata in GraphicsOptions", true, '\n');
        }
    }

    public void set_value(String key, Object value) {
        int index = keys.indexOf(key);

        if (index != -1) {
            VALUES[index] = value;
        }
        else {
            Logger.log("impossibile assegnare il valore: " + value + " alla key: " + key + ", la key non è stata registrata in GraphicsOptions", true, '\n');
        }
    }

    public Object get_value(String key) {
        int index = keys.indexOf(key);

        if (index != -1) {
            return VALUES[index];
        }
        else {
            Logger.log("impossibile trovare il valore della key: " + key + ", non è stata registrata in GraphicsOptions", true, '\n');
            return null;
        }
    }

    public boolean equals(GraphicsOptions other) {
        for (int i = 0; i < VALUES.length; i++) {
            int builder_index = builders_index[i];

            if (!builders[builder_index].equals(VALUES[i], other.VALUES[i]))
                return false;
        }

        return true;
    }
}