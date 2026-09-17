package Hilos;

import java.io.*;
import java.net.Socket;

/* Hilo que atiende a un cliente: recibe el nombre y envía el archivo. Muere al desconectarse. */
public class ClassThreadCliente extends  Thread{
    /* Socket, canales y tamaño de bloque. */
    private Socket cliente= null;
    private DataOutputStream dos = null;
    private DataInputStream dis= null;
    private boolean parar = false;
    private static final int BUFFER_SIZE = 8192;

    /* Guarda el socket del cliente. */
    public ClassThreadCliente(Socket cliente){this.cliente = cliente;}
    /* Lee el pedido y envía el archivo. */
    @Override
    public void run(){
        try{
            /* Canales sobre el socket. */
            dos =  new DataOutputStream(this.cliente.getOutputStream());
            dis = new DataInputStream(this.cliente.getInputStream());

            /* Buffer de 8192 bytes. */
            byte[] data = new byte[BUFFER_SIZE];
            /* Espera el nombre del archivo. */
            while(!parar){
                String strFichero = dis.readUTF();
                System.out.println("Client:"+ this.cliente.getLocalAddress()+"ha solicitado el archivo"+ strFichero);
                /* Busca el archivo en C:\cibertec\. */
                File fFichero = new File("C:\\cibertec\\"+ strFichero);
                if(fFichero.exists()){
                    /* Envía el tamaño y luego el contenido por bloques. */
                    long fileSize = fFichero.length();
                    int byteLeidos;
                    dos.writeLong(fileSize);
                    DataInputStream disFichero = new DataInputStream(new FileInputStream(fFichero));
                    while((byteLeidos = disFichero.read(data, 0,BUFFER_SIZE))>0){
                        dos.write(data, 0, byteLeidos);
                    }
                    System.out.println("archivo enviado correctamente");
                    disFichero.close();


                }else{
                    /* No existe: responde -1. */
                    dos.writeLong(-1);
                }

            }

        }catch (IOException e){
            /* Cliente desconectado. */
            System.out.println("Conexion con cliente:"+ cliente.getRemoteSocketAddress()+" cerrada");
        }finally{
            /* Cierra todo. */
            parar();
        }
        System.out.println("hio finalizado");
    }
    public  void parar(){
        parar= true;
        try{
            if(dos != null){
                dos.close();
            }
            if(dis != null){
                dis.close();
            }
            if(cliente != null){
                cliente.close();
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
