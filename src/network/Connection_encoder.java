package network;

public interface Connection_encoder {
    String[] compatible_with(); //ritorna la lista di protocolli del server con cui questo protocollo è compatibile
    void init(byte[] check_code); //inizializza encoder e decoder utilizzando questo codice di 16 cifre condiviso con il server
    byte[] encode(byte[] msg); //codifica un messaggio da mandare al server
    byte[] decode(byte[] msg); //decodifica un messaggio ricevuto dal server
}