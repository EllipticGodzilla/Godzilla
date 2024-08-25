package gui.custom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GIntegerField extends JTextField {
    private final int MAX, MIN;
    private Runnable on_validation = null;

    private String prev_value = "";

    public GIntegerField(Color background, Color foreground, Color border, int min, int max) {
        super();
        this.MAX = max;
        this.MIN = min;

        setPreferredSize(new Dimension(35, 20));
        setForeground(foreground);
        setBackground(background);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(4, 4, 0, 0)
        ));

        addFocusListener(FOCUS_LISTENER);
        addKeyListener(KEY_LISTENER);
    }

    public void on_validation(Runnable action) {
        on_validation = action;
    }

    public void set_value(int value) {
        if (value < MIN)
            value = MIN;

        if (value > MAX)
            value = MAX;

        setText(Integer.toString(value));
    }

    public int get_value() {
        String value = validate(getText());
        return Integer.parseInt(value);
    }

    private String validate(String value) {
        StringBuilder validated_input = new StringBuilder();

        char[] input_chars = value.toCharArray();
        for (int i = 0; i < value.length(); i++) {
            if (i == 0 && input_chars[i] == '-') { //il primo carattere può essere un - oltre a numero
                validated_input.append("-");
            } else if ('0' <= input_chars[i] && input_chars[i] <= '9') {
                validated_input.append(input_chars[i]);
            }
        }

        String input_str = validated_input.toString();

        if (!input_str.isEmpty() && !input_str.equals("-")) { //se c'è almeno un numero controlla sia -1 <= x <= 255
            int input_num = Integer.parseInt(input_str);

            if (input_num > 255)
                input_num = 255;

            if (input_num < -1)
                input_num = -1;

            return Integer.toString(input_num);
        }

        return "0";
    }

    private final FocusListener FOCUS_LISTENER = new FocusListener() {
        @Override
        public void focusGained(FocusEvent focusEvent) {
            prev_value = getText();
        }
        @Override
        public void focusLost(FocusEvent focusEvent) {
            String validated_str = validate(getText());
            setText(validated_str);

            if (on_validation != null)
                on_validation.run();
        }
    };

    private final KeyListener KEY_LISTENER = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent keyEvent) {}
        @Override
        public void keyReleased(KeyEvent keyEvent) {}
        @Override
        public void keyPressed(KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                String validated_txt = validate(getText());
                setText(validated_txt);

                if (on_validation != null)
                    on_validation.run();

                transferFocus();
            }
            else if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                setText(prev_value);
                transferFocus();
            }
        }
    };
}
