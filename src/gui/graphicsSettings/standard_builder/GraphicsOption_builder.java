package gui.graphicsSettings.standard_builder;

import javax.swing.*;

public interface GraphicsOption_builder<T> {
    T cast(String value_str);
    String revert_cast(Object value);
    void display(JPanel panel, Object value);
    void update(JPanel panel, Object value);
    T new_value(JPanel panel);
    default boolean equals(Object obj1, Object obj2) {
        return obj1.equals(obj2);
    }
}
