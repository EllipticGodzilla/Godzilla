package gui.custom;

import javax.swing.*;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class GScrollPane extends JScrollPane {
    public GScrollPane(Component c) {
        super(c);

        this.setAutoscrolls(true);
        this.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));
        this.setBackground(new Color(128, 131, 133));
    }

    public GScrollPane(Component c, int vertical_policy, int horizontal_policy) {
        super(c, vertical_policy, horizontal_policy);
        this.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));
        this.setBackground(new Color(128, 131, 133));
    }

    public void set_scrollbar_thickness(int thickness) {
        this.getVerticalScrollBar().setPreferredSize(new Dimension(
                thickness,
                this.getVerticalScrollBar().getPreferredSize().height
        ));
        this.getHorizontalScrollBar().setPreferredSize(new Dimension(
                this.getHorizontalScrollBar().getPreferredSize().width,
                thickness
        ));
    }

    @Override
    public JScrollBar createVerticalScrollBar() {
        JScrollBar scrollBar = super.createVerticalScrollBar();

        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(78, 81, 83);
                this.thumbDarkShadowColor = new Color(58, 61, 63);
                this.thumbHighlightColor = new Color(108, 111, 113);
            }

            static class null_button extends JButton {
                public null_button() {
                    super();
                    this.setPreferredSize(new Dimension(0, 0));
                }
            }

            @Override
            protected JButton createDecreaseButton(int orientation) { return new null_button(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return new null_button(); }
        });

        scrollBar.setBackground(new Color(128, 131, 133));
        scrollBar.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));

        return scrollBar;
    }

    @Override
    public JScrollBar createHorizontalScrollBar() {
        JScrollBar scrollBar = super.createHorizontalScrollBar();

        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(78, 81, 83);
                this.thumbDarkShadowColor = new Color(58, 61, 63);
                this.thumbHighlightColor = new Color(108, 111, 113);
            }

            static class null_button extends JButton {
                public null_button() {
                    super();
                    this.setPreferredSize(new Dimension(0, 0));
                }
            }

            @Override
            protected JButton createDecreaseButton(int orientation) { return new null_button(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return new null_button(); }
        });

        scrollBar.setBackground(new Color(128, 131, 133));
        scrollBar.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));

        return scrollBar;
    }
}