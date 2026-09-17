package Hilos;

public class MiHilo implements  Runnable{

    @Override
    public void run() {
        System.out.println("Otra forma de crear hilos");
    }
    public static void main(String[] args) {
      MiHilo  hilo = new MiHilo();
      Thread  interfaceHilo = new Thread(hilo);
      interfaceHilo.start();
    }
}
