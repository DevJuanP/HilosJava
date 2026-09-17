package Hilos;

public class Hilo extends  Thread{
    @Override
    public void run(){
        System.out.println("Ejecutando el hilo de clase heredada");
    }
    public static void main(String[] args) {
        Hilo hilo = new Hilo();
        hilo.start();
    }
}
