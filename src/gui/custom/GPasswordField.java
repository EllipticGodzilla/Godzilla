package gui.custom;

import gui.TempPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class GPasswordField extends JPasswordField {
    private static final int WIDTH  = 127;
    private static final int HEIGHT = 20;

    private JButton toggle_button = null;
    private static final ImageIcon[] EYE_ICONS = new ImageIcon[] {
            new ImageIcon(TempPanel.class.getResource("/images/eye.png")),
            new ImageIcon(TempPanel.class.getResource("/images/eye_pres.png")),
            new ImageIcon(TempPanel.class.getResource("/images/eye_sel.png"))

    };
    private static final ImageIcon[] NO_EYE_ICONS = new ImageIcon[] {
            new ImageIcon(TempPanel.class.getResource("/images/no_eye.png")),
            new ImageIcon(TempPanel.class.getResource("/images/no_eye_pres.png")),
            new ImageIcon(TempPanel.class.getResource("/images/no_eye_sel.png"))
    };

    private boolean clear_text = false;

    public GPasswordField() {
        super();

        this.setBackground(new Color(108, 111, 113));
        this.setBorder(BorderFactory.createLineBorder(new Color(68, 71, 73)));
        this.setFont(new Font("Arial", Font.BOLD, 14));
        this.setForeground(new Color(218, 221, 223));

        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setMinimumSize(this.getPreferredSize());

        this.setEchoChar('*'); //nasconde il testo
        gen_toggle_button(); //genera il pulsante per togglare la visibilità del testo
    }

    public JButton get_toggle_button() {
        return toggle_button;
    }

    private void gen_toggle_button() { //genera un pulsante che premuto toggla la visibilità del testo
        toggle_button = new JButton();

        //inizializza la grafica del pulsante con le icone dell'occhio senza la barra
        toggle_button.setIcon(EYE_ICONS[0]);
        toggle_button.setPressedIcon(EYE_ICONS[1]);
        toggle_button.setRolloverIcon(EYE_ICONS[2]);

        toggle_button.setBorder(null);
        toggle_button.setOpaque(false);
        toggle_button.setContentAreaFilled(false);

        //aggiunge action listener e ritorna il pulsante
        toggle_button.addActionListener(TOGGLE_LISTENER);
    }

    private final ActionListener TOGGLE_LISTENER = _ -> {
        if (clear_text) //se in questo momento il testo si vede in chiaro
        {
            setEchoChar('*'); //nasconde il testo

            //modifica le icone del pulsante
            toggle_button.setIcon(EYE_ICONS[0]);
            toggle_button.setPressedIcon(EYE_ICONS[1]);
            toggle_button.setRolloverIcon(EYE_ICONS[2]);
        }
        else //se in questo momeno il testo è nascosto
        {
            setEchoChar((char) 0); //mostra il testo

            //modifica le icone del pulsante
            toggle_button.setIcon(NO_EYE_ICONS[0]);
            toggle_button.setPressedIcon(NO_EYE_ICONS[1]);
            toggle_button.setRolloverIcon(NO_EYE_ICONS[2]);
        }

        clear_text = !clear_text;
    };
}
