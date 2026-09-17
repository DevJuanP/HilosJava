package Hilos;

public class MainServidor {

    /* Arranca el servidor en el puerto 1250. */
    public static void main(String[] args) {
        ServidorArchivos serv= new ServidorArchivos(1250);
        serv.start();
    }
}
