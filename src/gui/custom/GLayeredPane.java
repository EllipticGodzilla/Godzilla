package gui.custom;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class GLayeredPane extends JLayeredPane { //permette di ridimensionare componenti in modo che abbiamo sempre la sua stessa dimensione
    Vector<Component> full_screen = new Vector<>();
    private int menu_height = 0;

    public GLayeredPane() {
        super();
        this.setOpaque(false);
    }

    public void set_menu_height(int height) {
        this.menu_height = height;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) { //in questo modo si elimina il delay che si avrebbe utilizzando un component listener
        super.setBounds(x, y, width, height);

        for (Component cmp : full_screen) {
            cmp.setBounds(0, menu_height, width, height - menu_height);
        }
    }

    public void add_fullscreen(Component comp, int index) {
        super.add(comp, index);
        full_screen.add(comp);
    }
}