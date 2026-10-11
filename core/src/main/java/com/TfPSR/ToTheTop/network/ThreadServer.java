package com.TfPSR.ToTheTop.network;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ThreadServer extends Thread{

    private DatagramSocket conection;
    private boolean end = false;
    private NetworkDir[] clients = new NetworkDir[4];
    private int currentClients = 0;

    public ThreadServer(){
        try {
             conection = new DatagramSocket(6767);
        }catch (SocketException e){
            e.printStackTrace();
        }
    }

    public void sendMessage(String string, InetAddress ip, int port) throws IOException {
        byte[] buf = string.getBytes();
        DatagramPacket data = new DatagramPacket(buf, buf.length, ip, port);
        conection.send(data);
    }

    @Override
    public void run(){
        do{
            byte[] buf = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            try {
                conection.receive(dp);
                processMessage(dp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }while(!end);
    }

    private void processMessage(DatagramPacket dp) throws IOException {
        String message = new String(dp.getData(), dp.getOffset(), dp.getLength(), StandardCharsets.UTF_8);
        if(message.equals("Conexion")){
            if(currentClients<4){
                clients[currentClients] = new NetworkDir(dp.getAddress(), dp.getPort());
                sendMessage("OK", clients[currentClients].getIp(), clients[currentClients].getPort());
                currentClients++;
                if(currentClients==4){
                    for(int i=0; i<currentClients;i++){
                        sendMessage("Empieza", clients[i].getIp(), clients[i].getPort());
                    }
                }
            }
        }
    }
}
