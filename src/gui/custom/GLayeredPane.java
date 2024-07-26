package gui.custom;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class GLayeredPane extends JLayeredPane { //permette di ridimensionare componenti in modo che abbiamo sempre la sua stessa dimensione
    Vector<Component> resizable = new Vector<>();
    public GLayeredPane() {
        super();
        this.setBackground(new Color(58, 61, 63));
    }

    @Override
    public void setBounds(int x, int y, int width, int height) { //in questo modo si elimina il delay che si avrebbe utilizzando un component listener
        super.setBounds(x, y, width, height);

        for (Component cmp : resizable) {
            cmp.setBounds(0, 0, width, height);
        }
    }

    public void add_fullscreen(Component comp, int index) {
        super.add(comp, index);
        resizable.add(comp);
    }
}