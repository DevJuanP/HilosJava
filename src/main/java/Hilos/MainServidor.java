package Hilos;

public class MainServidor {

    public static void main(String[] args) {
        ServidorArchivos serv= new ServidorArchivos(1250);
        serv.start();
    }
}
