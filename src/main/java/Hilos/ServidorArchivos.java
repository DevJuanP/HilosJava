package Hilos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorArchivos extends  Thread{
    private int puerto;
    private boolean parar = false;
    public ServidorArchivos(int puerto){
        this.puerto= puerto;
    }
    public void pararServidor(){
        this.parar=true;
    }

    @Override
    public void run() {
        System.out.println("Servidor Funcionando");
        ServerSocket servidor = null;
        try{
            servidor = new ServerSocket(this.puerto);
            System.out.println("Esperando conexiones puerto:"+ this.puerto);
            while(!parar){
                Socket nuevoCliente = servidor.accept();
                ClassThreadCliente tNuevoCliente = new ClassThreadCliente(nuevoCliente);
                tNuevoCliente.start();

            }
            servidor.close();
            System.out.println("Servidor Cerrado correctamente");

        }catch (IOException e){
            System.out.println("Servidor cerrado abruptamente");
        }finally {
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
