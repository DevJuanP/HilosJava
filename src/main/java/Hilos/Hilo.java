package Hilos;

/* Hilo creado heredando de Thread (forma 1). */
public class Hilo extends  Thread{
    /* Lo que hace el hilo. */
    @Override
    public void run(){
        System.out.println("Ejecutando el hilo de clase heredada");
    }
    /* Crea y lanza el hilo. */
    public static void main(String[] args) {
        Hilo hilo = new Hilo();
        hilo.start();
    }
}
