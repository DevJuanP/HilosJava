package Hilos;

/* Hilo creado implementando Runnable (forma 2). */
public class MiHilo implements  Runnable{

    /* Lo que hace el hilo. */
    @Override
    public void run() {
        System.out.println("Otra forma de crear hilos");
    }
    /* Envuelve en un Thread y lo lanza. */
    public static void main(String[] args) {
      MiHilo  hilo = new MiHilo();
      Thread  interfaceHilo = new Thread(hilo);
      interfaceHilo.start();
    }
}
