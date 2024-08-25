package gui.graphicsSettings;

import files.File_interface;
import files.Logger;
import gui.*;
import gui.graphicsSettings.standard_builder.GraphicsOption_builder;
import gui.graphicsSettings.standard_builder.Image_graphics;

import java.util.*;
import java.util.regex.Pattern;

public abstract class GraphicsSettings {
    private static final Map<String, GraphicsOptions> graphics = new LinkedHashMap<>();
    private static final Vector<String> deleted_graphics = new Vector<>();
    public static GraphicsOptions active_option;

    // carica tutte le impostazioni dai file
    public static void load_files() {
        Image_graphics.init();

        //inizializza active_option come standard
        active_option = new GraphicsOptions("standard");
        graphics.put("standard", active_option);

        //carica tutte le optizioni dai file
        String[] loaded_files = File_interface.get_file_list();
        Pattern graphics_file_pattern = Pattern.compile("database/graphics/[a-zA-Z0-9_-]+\\.gps");

        Logger.log("inizio la ricerca di file contenenti temi grafici");
        for (String file : loaded_files) {
            if (graphics_file_pattern.matcher(file).matches()) { //se è un file con impostazioni grafiche
                String file_name = file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("."));
                file_name = file_name.replaceAll("_", " ");

                Logger.log("importo: " + file_name);

                String file_cont = File_interface.read_file(file);
                if (file_cont != null) {
                    file_cont = file_cont.replaceAll(" ", "");
                    Pattern options_pat = Pattern.compile("[\n:]");

                    String[] options = options_pat.split(file_cont);

                    GraphicsOptions graphics_options = get_settings_from(file_name, options);
                    graphics.put(file_name, graphics_options);
                }
            }
        }
        Logger.log("tutte le grafiche sono importate");

        File_interface.add_updater(file_updater); //aggiorna tutte le informazioni scritte nei file
    }

    public static void reload_files() {
        graphics.clear();

        load_files();
    }

    //aggiunge una nuova opzione alla lista
    public static boolean add_options(GraphicsOptions options) {
        String gps_name = options.get_name();

        if (graphics.containsKey(gps_name))
            return false;

        graphics.put(gps_name, options);
        return true;
    }

    //imposta le opzioni da visualizzare in questo momento
    public static void set_active_options(String name){
        active_option = graphics.get(name);

        Godzilla_frame.update_colors();
        ButtonTopBar_panel.update_colors();
        ClientList_panel.update_colors();
        ServerList_panel.update_colors();
        Central_panel.update_colors();
    }

    //aggiorna delle impostazioni
    public static void update_options(String options_name, GraphicsOptions new_options) {
        graphics.replace(options_name, new_options);
    }

    public static String[] get_options_list() {
        return graphics.keySet().toArray(new String[0]);
    }

    public static GraphicsOptions get_option(String name) {
        return graphics.get(name);
    }

    public static void remove_option(String name) {
        graphics.remove(name);
        deleted_graphics.add(name);
    }

    //dato un array di composto da key e valori alternati ritorna le GraphicsOptions
    private static GraphicsOptions get_settings_from(String opt_name, String[] options_array) {
        GraphicsOptions options = new GraphicsOptions(opt_name);

        for (int i = 0; i < options_array.length; i += 2) {
            try {
                //salta le linee vuote
                String option_name = options_array[i];

                try {
                    while (option_name.isEmpty()) {
                        i++;
                        option_name = options_array[i];
                    }
                }
                catch (ArrayIndexOutOfBoundsException _) {
                    return options;
                }

                //trovata una key, prende il valore assegnato
                String option_str_value = options_array[i + 1];

                //imposta il nuovo valore alla key trovata
                options.set_value(option_name, option_str_value);
            }
            catch (ArrayIndexOutOfBoundsException _) {
                Logger.log("errore nella lettura delle opzioni, risulta una key senza valore assegnato", true, '\n');
                return new GraphicsOptions(opt_name);
            }
        }

        return options;
    }

    private static final Runnable file_updater = () -> {
        for (String name : deleted_graphics) { //elimina tutti i file con grafiche eliminate
            String file_name = "database/graphics/" + name.replaceAll(" ", "_") + ".gps";

            File_interface.delete_file(file_name);
        }
        deleted_graphics.clear();

        for (GraphicsOptions options : graphics.values()) { //salva tutte le informazioni sulle grafiche registrate
            if (!options.get_name().equals("standard")) { //standard non va salvato in un file
                String file_name = "database/graphics/" + options.get_name().replaceAll(" ", "_") + ".gps";

                if (!File_interface.exist(file_name)) {
                    File_interface.create_file(file_name, false);
                }
                else {
                    File_interface.overwrite_file(file_name, "");
                }

                for (String key : GraphicsOptions.get_keys()) {
                    Object value = options.get_value(key);
                    GraphicsOption_builder<?> builder = GraphicsOptions.get_builder(key);

                    String value_str = builder.revert_cast(value);

                    File_interface.append_to(file_name, key + ":" + value_str + "\n");
                }
            }
        }
    };
}