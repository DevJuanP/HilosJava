package Hilos;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/* Cliente: pide un archivo al servidor y lo descarga a C:\cibertec\. */
public class Cliente {
    /* Bloque de 8192 bytes. */
    private static final int BUFFER_SIZE = 8192;
    public static void main(String[] args){
        /* Buffer, canales y socket. */
        byte [] data = new byte[BUFFER_SIZE];
        DataInputStream dis= null;
        DataOutputStream dos = null;
        Socket socket = null;
        long fileSize= -1;
        try{
            /* Conecta a localhost:1250. */
            socket = new Socket("localhost", 1250);
            System.out.println("conectado con"+ socket.getRemoteSocketAddress());
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            /* Envía el nombre y lee el tamaño. */
            Scanner sc = new Scanner (System.in);
            System.out.println("Subir el archivo que deseas recibir:");
            String strFichero = sc.nextLine();
            strFichero = new String(strFichero.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            dos.writeUTF(strFichero);
            dos.flush();
            fileSize = dis.readLong();

            if(fileSize != -1){
                /* Guarda los bloques hasta completar. */
                DataOutputStream dosFile = new DataOutputStream(new FileOutputStream(new File("C:\\cibertec\\"+ strFichero)));
                int bytesReceived = 0;
                while(fileSize>0 && (bytesReceived = dis.read(data, 0,(int)Math.min(data.length,fileSize)))>0){
                    fileSize -= bytesReceived;
                    dosFile.write(data, 0, bytesReceived);
                    System.out.println(bytesReceived);
                }
                System.out.println("Archivo recibido correctamente");
                dosFile.close();
            }else{
                /* El servidor respondió -1. */
                System.out.println("El archivo solictado no existe");
            }



        }catch (UnknownHostException e){
            /* No se resolvió el host. */
            e.printStackTrace();
        }catch(IOException e){
            /* Se cayó la conexión. */
            System.out.println("conexion con el servidor cerrado");
            if(fileSize != 0){
                System.out.println("No se realizado la recepcion del archivo correctamente");
            }
        }finally{
            try{
                if(dis !=null){
                    dis.close();
                }
                if(dos!= null){
                    dos.close();
                }
                if(socket != null){
                    socket.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }
}
