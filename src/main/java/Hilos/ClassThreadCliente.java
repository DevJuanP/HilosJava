package Hilos;

import java.io.*;
import java.net.Socket;

public class ClassThreadCliente extends  Thread{
    private Socket cliente= null;
    private DataOutputStream dos = null;
    private DataInputStream dis= null;
    private boolean parar = false;
    private static final int BUFFER_SIZE = 8192;

    public ClassThreadCliente(Socket cliente){this.cliente = cliente;}
    @Override
    public void run(){
        try{
            dos =  new DataOutputStream(this.cliente.getOutputStream());
            dis = new DataInputStream(this.cliente.getInputStream());

            byte[] data = new byte[BUFFER_SIZE];
            while(!parar){
                String strFichero = dis.readUTF();
                System.out.println("Client:"+ this.cliente.getLocalAddress()+"ha solicitado el archivo"+ strFichero);
                File fFichero = new File("C:\\cibertec\\"+ strFichero);
                if(fFichero.exists()){
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
                    dos.writeLong(-1);
                }

            }

        }catch (IOException e){
            System.out.println("Conexion con cliente:"+ cliente.getRemoteSocketAddress()+" cerrada");
        }finally{
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
