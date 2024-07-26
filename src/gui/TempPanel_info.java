package gui;

import java.util.LinkedHashMap;

public class TempPanel_info {
    public static final int INPUT_REQ = 0,
                            SINGLE_MSG = 1,
                            DOUBLE_COL_MSG = 2,
                            NORMAL_REQUEST = 0,
                            PASSWORD_REQUEST = 1,
                            COMBO_BOX_REQUEST = 2;
    private final int TYPE;

    private boolean show_annulla; //imposta la visibilità del pulsante "annulla"
    private boolean request_psw = false; //false = non richiede nessuna password, true richiede delle password
    private String[] msg_text; //contiene tutti i messaggi da mostrare nel temp_panel
    private int[] req_type; //per ogni richiesta memorizza 0 = richiesta normale, 1 = password, 2 = JComboBox

    private LinkedHashMap<Integer, String[]> cbox_info = new LinkedHashMap<>();
//    private boolean[] password_indices; //per ogni richiesta di input memorizza se richiede una password o meno
//    private boolean[] cbox_indices; //per ogni richiesta memorizza true = combobox false non combobox

    public TempPanel_info(int type, boolean show_annulla, String... txts) {
        this.TYPE = type;
        this.msg_text = txts;
        this.show_annulla = show_annulla;

        req_type = new int[txts.length];
//        password_indices = new boolean[txts.length]; // di default inizializza l'array con tutti false
//        cbox_indices = new boolean[txts.length];
    }

    public TempPanel_info set_psw_indices(int... psw_indices) { //specifica quali fra le richieste che ha inserito richiedono delle password
        request_psw = true;

        for (int index : psw_indices) {
            req_type[index] = PASSWORD_REQUEST;
        }

        return this;
    }

    public TempPanel_info set_combo_box(int[] indices, String[]... cbox_list) {
        for (int i = 0; i < indices.length; i++) {
            req_type[indices[i]] = COMBO_BOX_REQUEST;
            cbox_info.put(indices[i], cbox_list[i]);
        }

        return this;
    }

    public String[] get_cbox_info(int index) {
        return cbox_info.get(index);
    }

    public int[] get_requests_info() { return req_type; }
    public boolean annulla_vis() { return show_annulla; }
    public int get_type() { return TYPE; }
    public boolean request_psw() { return request_psw; }
    public String[] get_txts() { return msg_text; }
}
