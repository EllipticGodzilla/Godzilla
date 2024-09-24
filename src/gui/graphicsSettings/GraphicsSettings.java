package gui.graphicsSettings;

import files.File_interface;
import files.Logger;
import gui.*;
import gui.graphicsSettings.standard_builder.GraphicsOption_builder;
import gui.graphicsSettings.standard_builder.Image_builder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GraphicsSettings {
    private static final Map<String, GraphicsTheme> themes = new LinkedHashMap<>();
    private static final Vector<String> deleted_themes = new Vector<>();
    private static final Vector<Runnable> color_updater = new Vector<>();
    public static GraphicsTheme active_theme;

    // carica tutti i temi dai file in database/graphics
    public static void load_from_files() {
        Image_builder.init();

        //inizializza active_option come tema standard
        active_theme = new GraphicsTheme("standard theme");
        themes.put("standard theme", active_theme);

        //carica tutti i temi salvati nei file
        String[] loaded_files = File_interface.get_file_list();
        Pattern graphics_file_pattern = Pattern.compile("database/graphics/([a-zA-Z0-9_-]+)\\.gps");

        Logger.log("inizio la ricerca di file contenenti temi grafici");
        for (String file : loaded_files) {
            Matcher file_name_matcher = graphics_file_pattern.matcher(file);

            if (file_name_matcher.matches()) { //se è un file con un tema grafico
                String file_name = file_name_matcher.group(1);
                file_name = file_name.replaceAll("_", " ");

                GraphicsTheme theme = load_theme_from_file(file, file_name);

                themes.put(file_name, theme);
                Logger.log("importato il tema in: " + file_name);
            }
        }
        Logger.log("tutte le grafiche sono importate");

        File_interface.add_updater(file_updater); //aggiorna tutte le informazioni scritte nei file
    }

    //dal nome del file e il nome del tema ritorna l'oggetto GraphicsTheme
    private static GraphicsTheme load_theme_from_file(String file_name, String theme_name) {
        String file_cont = File_interface.read_file(file_name);
        String[] lines = file_cont.split("\n");

        GraphicsTheme theme = new GraphicsTheme(theme_name);

        Pattern line_pattern = Pattern.compile("([^:]+):(.+)");
        for (int i = 0; i < lines.length; i++) {
            Matcher line_matcher = line_pattern.matcher(lines[i]);

            if (line_matcher.matches()) {
                String key = line_matcher.group(1);
                String val = line_matcher.group(2);

                theme.set_value(key, val);
            }
            else if (!lines[i].isEmpty()) { //se non è una linea vuota da errore, altrimenti la ignora
                Logger.log("impossibile comprendere la linea (" + i + "): " + lines[i] + " nel file contenente tema grafico: " + file_name, true);
            }
        }

        return theme;
    }


    //ricarica tutti i temi dai file
    public static void reload_from_files() {
        themes.clear();

        load_from_files();
    }

    //aggiunge una nuova opzione alla lista
    public static void add_theme(GraphicsTheme theme) {
        String theme_name = theme.get_name();
        themes.putIfAbsent(theme_name, theme);
    }

    //aggiunge un metodo da eseguire una volta modificato il tema
    public static void run_at_theme_change(Runnable runnable) {
        color_updater.add(runnable);
    }

    //imposta il tema da visualizzare
    public static void set_active_theme(String name){
        active_theme = themes.get(name);

        for (Runnable runnable : color_updater) {
            runnable.run();
        }
    }

    //aggiorna delle impostazioni
    public static void update_theme(String theme_name, GraphicsTheme new_theme) {
        themes.replace(theme_name, new_theme);
    }

    public static String[] get_theme_list() {
        return themes.keySet().toArray(new String[0]);
    }

    public static GraphicsTheme get_theme(String name) {
        return themes.get(name);
    }

    public static void remove_theme(String name) {
        themes.remove(name);
        deleted_themes.add(name);
    }

    //aggiorna il contenuto dei file con tutti i temi che sono contenuti in themes
    private static final Runnable file_updater = () -> {
        for (String name : deleted_themes) { //elimina tutti i file con temi eliminati
            String file_name = "database/graphics/" + name.replaceAll(" ", "_") + ".gps";

            File_interface.delete_file(file_name);
        }
        deleted_themes.clear();

        for (GraphicsTheme theme : themes.values()) { //salva tutte le informazioni sui temi memorizzati
            if (!theme.get_name().equals("standard theme")) { //il tema standard non va salvato in un file
                String file_name = "database/graphics/" + theme.get_name().replaceAll(" ", "_") + ".gps";

                if (!File_interface.exist(file_name)) { //è stato creato un nuovo tema che non ha ancora il file
                    File_interface.create_file(file_name, false);
                }
                else { //resetta il contenuto del file per aggiornarlo
                    File_interface.overwrite_file(file_name, "");
                }

                //salva nel file tutti i valori delle key che sono state modificate
                for (String key : theme.get_edited_key()) {
                    Object value = theme.get_value(key);
                    GraphicsOption_builder<?> builder = GraphicsTheme.get_builder(key);

                    String value_str = builder.revert_cast(value); //non può essere null poiché la key è scelta esplicitamente fra quelle già registrate

                    File_interface.append_to(file_name, key + ":" + value_str + "\n");
                }
            }
        }
    };
}