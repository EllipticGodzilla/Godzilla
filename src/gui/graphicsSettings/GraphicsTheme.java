package gui.graphicsSettings;

import files.Logger;
import gui.graphicsSettings.standard_builder.*;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class GraphicsTheme {
    private static List<String> keys = List.of( //tutte le chiavi a cui si possono assegnare valori, viene utilizzata una lista poiché Arrays.binarySearch() non è affidabile con String
            "frame_background",
            "text_color",
            "list_background",
            "list_text_color",
            "list_selected_background",
            "list_selected_text_color",
            "list_selected_border",
            "input_background",
            "input_text_color",
            "input_border",
            "temp_panel_show_password_button",
            "temp_panel_hide_password_button",
            "temp_panel_border",
            "temp_panel_ok",
            "temp_panel_annulla",
            "button_top_bar_left_shift",
            "button_top_bar_right_shift",
            "button_top_bar_stop_mod",
            "central_panel_icon",
            "central_panel_background",
            "client_panel_connect",
            "client_panel_disconnect",
            "server_panel_connect",
            "server_panel_disconnect",
            "server_panel_add_server",
            "title_bar_background",
            "title_bar_close",
            "title_bar_maximize",
            "title_bar_iconize",
            "dropdown_selected_background",
            "dropdown_selected_text_color",
            "dropdown_border",
            "dropdown_background",
            "dropdown_text_color",
            "settings_ok",
            "settings_apply",
            "settings_annulla",
            "settings_dropdown_list_opened_icon",
            "settings_dropdown_list_closed_icon",
            "scroll_bar_thumb_color",
            "scroll_bar_thumb_darkshadow_color",
            "scroll_bar_thumb_highlight_color",
            "scroll_bar_rail_color"
    );
    private static GraphicsOption_builder<?>[] builders = new GraphicsOption_builder[] {  //array con tutti i diversi builder registrati
            new Color_builder(),
            new ButtonIcons_builder(),
            new Image_builder(),
            new Border_builder()
    };
    private static int[] builders_index = new int[] {0, 0, 0, 0, 0, 0, 3, 0, 0, 3, 1, 1, 3, 1, 1, 1, 1, 1, 2, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 3, 0, 0, 1, 1, 1, 2, 2, 0, 0, 0, 0}; //per ogni chiave deve essere assegnato l index del builder a lei assegnato
    private static Object[] std_values = new Object[] { //valori standard per ogni chiave
            new Color(58, 61, 63), //frame_background
            Color.lightGray, //text_color
            new Color(98, 101, 103), //list_background
            new Color(44, 46, 47), //list_text_color
            new Color(138, 141, 143), //list_selected_background
            new Color(44, 46, 47), //list_selected_text_color
            BorderFactory.createCompoundBorder( //list_selected_border
                    BorderFactory.createLineBorder(new Color(72, 74, 75)),
                    BorderFactory.createEmptyBorder(2, 2, 0, 0)
            ),
            new Color(108, 111, 113), //input_background
            new Color(218, 221, 223), //input_text_color
            BorderFactory.createCompoundBorder( //input_border
                    BorderFactory.createLineBorder(new Color(68, 71, 73)),
                    BorderFactory.createEmptyBorder(2, 0, 0, 0)
            ),
            new ButtonIcons( //temp_panel_show_password_button
                    new ImageIcon(get_resource("/images/eye.png")),
                    new ImageIcon(get_resource("/images/eye_pres.png")),
                    new ImageIcon(get_resource("/images/eye_sel.png")),
                    new ImageIcon(get_resource("/images/eye.png"))
            ),
            new ButtonIcons(//temp_panel_hide_password_button
                    new ImageIcon(get_resource("/images/no_eye.png")),
                    new ImageIcon(get_resource("/images/no_eye_pres.png")),
                    new ImageIcon(get_resource("/images/no_eye_sel.png")),
                    new ImageIcon(get_resource("/images/no_eye.png"))
            ),
            BorderFactory.createLineBorder(new Color(38, 41, 43)), //temp_panel_border
            new ButtonIcons( //temp_panel_ok
                    new ImageIcon(get_resource("/images/ok.png")),
                    new ImageIcon(get_resource("/images/ok_pres.png")),
                    new ImageIcon(get_resource("/images/ok_sel.png")),
                    new ImageIcon(get_resource("/images/ok.png"))
            ),
            new ButtonIcons( //temp_panel_annulla
                    new ImageIcon(get_resource("/images/cancel.png")),
                    new ImageIcon(get_resource("/images/cancel_pres.png")),
                    new ImageIcon(get_resource("/images/cancel_sel.png")),
                    new ImageIcon(get_resource("/images/cancel.png"))
            ),
            new ButtonIcons( //button_top_bar_left_shift
                    new ImageIcon(get_resource("/images/left_arrow.png")),
                    new ImageIcon(get_resource("/images/left_arrow_sel.png")),
                    new ImageIcon(get_resource("/images/left_arrow_pres.png")),
                    new ImageIcon(get_resource("/images/left_arrow.png"))
            ),
            new ButtonIcons( //button_top_bar_right_shift
                    new ImageIcon(get_resource("/images/right_arrow.png")),
                    new ImageIcon(get_resource("/images/right_arrow_sel.png")),
                    new ImageIcon(get_resource("/images/right_arrow_pres.png")),
                    new ImageIcon(get_resource("/images/right_arrow.png"))
            ),
            new ButtonIcons( //button_top_bar_stop_mod
                    new ImageIcon(get_resource("/images/power_off.png")),
                    new ImageIcon(get_resource("/images/power_off_sel.png")),
                    new ImageIcon(get_resource("/images/power_off_pres.png")),
                    new ImageIcon(get_resource("/images/power_off_dis.png"))
            ),
            new ImageIcon(get_resource("/images/godzilla.png")), //central_panel_icon
            Color.black, //central_panel_background
            new ButtonIcons( //client_panel_connect
                    new ImageIcon(get_resource("/images/power_on.png")),
                    new ImageIcon(get_resource("/images/power_on_sel.png")),
                    new ImageIcon(get_resource("/images/power_on_pres.png")),
                    new ImageIcon(get_resource("/images/power_on_dis.png"))
            ),
            new ButtonIcons( //client_panel_disconnect
                    new ImageIcon(get_resource("/images/power_off.png")),
                    new ImageIcon(get_resource("/images/power_off_sel.png")),
                    new ImageIcon(get_resource("/images/power_off_pres.png")),
                    new ImageIcon(get_resource("/images/power_off_dis.png"))
            ),
            new ButtonIcons( //server_panel_connect
                    new ImageIcon(get_resource("/images/power_on.png")),
                    new ImageIcon(get_resource("/images/power_on_sel.png")),
                    new ImageIcon(get_resource("/images/power_on_pres.png")),
                    new ImageIcon(get_resource("/images/power_on_dis.png"))
            ),
            new ButtonIcons( //server_panel_disconnect
                    new ImageIcon(get_resource("/images/power_off.png")),
                    new ImageIcon(get_resource("/images/power_off_sel.png")),
                    new ImageIcon(get_resource("/images/power_off_pres.png")),
                    new ImageIcon(get_resource("/images/power_off_dis.png"))
            ),
            new ButtonIcons( //server_panel_add_server
                    new ImageIcon(get_resource("/images/add_server.png")),
                    new ImageIcon(get_resource("/images/add_server_sel.png")),
                    new ImageIcon(get_resource("/images/add_server_pres.png")),
                    new ImageIcon(get_resource("/images/add_server_dis.png"))
            ),
            new Color(88, 91, 93), //title_bar_background
            new ButtonIcons( //title_bar_close
                    new ImageIcon(get_resource("/images/exit.png")),
                    new ImageIcon(get_resource("/images/exit_pres.png")),
                    new ImageIcon(get_resource("/images/exit_pres.png")),
                    new ImageIcon(get_resource("/images/exit.png"))
            ),
            new ButtonIcons( //title_bar_maximize
                    new ImageIcon(get_resource("/images/fullScreen.png")),
                    new ImageIcon(get_resource("/images/fullScreen_pres.png")),
                    new ImageIcon(get_resource("/images/fullScreen_pres.png")),
                    new ImageIcon(get_resource("/images/fullScreen.png"))
            ),
            new ButtonIcons( //title_bar_iconize
                    new ImageIcon(get_resource("/images/hide.png")),
                    new ImageIcon(get_resource("/images/hide_pres.png")),
                    new ImageIcon(get_resource("/images/hide_pres.png")),
                    new ImageIcon(get_resource("/images/hide.png"))
            ),
            new Color(73, 76, 78), //dropdown_selected_background
            new Color(211, 211, 211), //dropdown_selected_text_color
            BorderFactory.createLineBorder(new Color(108, 111, 113), 2), //dropdown_border
            new Color(58, 61, 63), //dropdown_background
            Color.lightGray, //dropdown_text_color
            new ButtonIcons( //settings_ok
                    new ImageIcon(get_resource("/images/ok.png")),
                    new ImageIcon(get_resource("/images/ok_pres.png")),
                    new ImageIcon(get_resource("/images/ok_sel.png")),
                    new ImageIcon(get_resource("/images/ok.png"))
            ),
            new ButtonIcons( //settings_apply
                    new ImageIcon(get_resource("/images/apply.png")),
                    new ImageIcon(get_resource("/images/apply_pres.png")),
                    new ImageIcon(get_resource("/images/apply_sel.png")),
                    new ImageIcon(get_resource("/images/apply.png"))
            ),
            new ButtonIcons( //settings_annulla
                    new ImageIcon(get_resource("/images/cancel.png")),
                    new ImageIcon(get_resource("/images/cancel_pres.png")),
                    new ImageIcon(get_resource("/images/cancel_sel.png")),
                    new ImageIcon(get_resource("/images/cancel.png"))
            ),
            new ImageIcon(get_resource("/images/menu_open.png")), //settings_dropdown_list_opened_icon
            new ImageIcon(get_resource("/images/menu_closed.png")), //settings_dropdown_list_closed_icon
            new Color(78, 81, 83), //scroll_bar_thumb_color
            new Color(58, 61, 63), //scroll_bar_thumb_darkshadow_color
            new Color(108, 111, 113), //scroll_bar_thumb_highlight_color
            new Color(128, 131, 133) //scroll_bar_rail_color
    };
    private static String get_resource(String path) {
        return GraphicsTheme.class.getResource(path).getPath();
    }

    //aggiunge un builder e ritorna l index a cui è stato aggiunto
    public static synchronized int add_builder(GraphicsOption_builder<?> builder) {
        builders = Arrays.copyOf(builders, builders.length + 1);
        builders[builders.length - 1] = builder;

        return builders.length - 1;
    }

    //aggiunge una key e le assegna builder e valore standard
    public static synchronized boolean add_key(String key, int builder_index, Object value) { //prima che inizi GraphicsSetting è possibile aggiungere nuove chiavi assegnandole valori standard e builder
        if (!keys.contains(key)) { //se non è già stata registrata una chiave con questo nome
            keys.add(key);

            builders_index = Arrays.copyOf(builders_index, builders_index.length + 1);
            builders_index[builders_index.length - 1] = builder_index;

            std_values = Arrays.copyOf(std_values, std_values.length + 1);
            std_values[std_values.length - 1] = value;

            return true;
        }
        else {
            Logger.log("tentativo di registrare due volte la chiave: " + key + " in GraphicsTheme", true);
            return false;
        }
    }

    //ritorna la lista di keys
    public static String[] get_key_list() {
        return keys.toArray(new String[0]);
    }

    //ritorna il builder assegnato alla key
    public static GraphicsOption_builder<?> get_builder(String key) {
        int index = keys.indexOf(key);

        if (index != -1) {
            int builder_index = builders_index[index];

            return builders[builder_index];
        }
        else {
            Logger.log("impossibile trovare il builder assegnato alla key: " + key + ", la key non è stata registrata in GraphicsTheme", true);
            return null;
        }
    }

    private final Object[] VALUES; //valori assegnati a ogni chiave
    private final String NAME; //nome di questo tema

    public GraphicsTheme(String name) {
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
            Logger.log("impossibile assegnare il valore: " + value_str + " alla key: " + key + ", la key non è stata registrata in GraphicsTheme", true);
        }
    }

    public void set_value(String key, Object value) {
        int index = keys.indexOf(key);

        if (index != -1) {
            VALUES[index] = value;
        }
        else {
            Logger.log("impossibile assegnare il valore: " + value + " alla key: " + key + ", la key non è stata registrata in GraphicsTheme", true);
        }
    }

    public Object get_value(String key) {
        int index = keys.indexOf(key);

        if (index != -1) {
            return VALUES[index];
        }
        else {
            Logger.log("impossibile trovare il valore della key: " + key + ", non è stata registrata in GraphicsTheme", true);
            return null;
        }
    }

    //ritorna la lista di tutte le key i cui valori sono stati modificati dallo standard
    public String[] get_edited_key() {
        String[] edited_key = new String[0];

        for (String key : keys) {
            int key_index = keys.indexOf(key);
            int builder_index = builders_index[key_index];

            Object value = VALUES[key_index];
            Object std_value = std_values[key_index];

            //se i due valori sono diversi aggiunge la key alle modificate, altrimenti la ignora
            if (!builders[builder_index].equals(value, std_value)) {
                edited_key = Arrays.copyOf(edited_key, edited_key.length + 1);
                edited_key[edited_key.length - 1] = key;
            }
        }

        return edited_key;
    }

    public boolean equals(GraphicsTheme other) {
        for (int i = 0; i < VALUES.length; i++) {
            int builder_index = builders_index[i];

            if (!builders[builder_index].equals(VALUES[i], other.VALUES[i]))
                return false;
        }

        return true;
    }
}