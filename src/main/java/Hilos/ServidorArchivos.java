package Hilos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/* Servidor thread-por-cliente: acepta conexiones y crea un hilo por cada una. */
public class ServidorArchivos extends  Thread{
    /* Puerto y bandera de parada. */
    private int puerto;
    private boolean parar = false;
    /* Guarda el puerto de escucha. */
    public ServidorArchivos(int puerto){
        this.puerto= puerto;
    }
    /* Pide la parada del loop. */
    public void pararServidor(){
        this.parar=true;
    }

    /* Escucha, acepta y delega. */
    @Override
    public void run() {
        System.out.println("Servidor Funcionando");
        ServerSocket servidor = null;
        try{
            /* Abre el ServerSocket. */
            servidor = new ServerSocket(this.puerto);
            System.out.println("Esperando conexiones puerto:"+ this.puerto);
            /* Acepta clientes: un hilo nuevo por cada uno. */
            while(!parar){
                Socket nuevoCliente = servidor.accept();
                ClassThreadCliente tNuevoCliente = new ClassThreadCliente(nuevoCliente);
                tNuevoCliente.start();

            }
            servidor.close();
            System.out.println("Servidor Cerrado correctamente");

        }catch (IOException e){
            /* Cierre abrupto. */
            System.out.println("Servidor cerrado abruptamente");
        }finally {
            /* Libera el puerto. */
            if(servidor != null){
                try{
                    servidor.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
