package com.TfPSR.ToTheTop.network;

import java.io.IOException;
import java.net.*;

public class ThreadServer extends Thread {

    private static final int MAX_CLIENTS = 4;
    private static final int MIN_CLIENTS = 2;

    private final DatagramSocket conection;
    private volatile boolean running = true;
    private final NetworkDir[] clients = new NetworkDir[MAX_CLIENTS];
    private volatile int currentClients = 0;
    private volatile boolean started = false;   // una vez en partida ya no se admiten cambios

    public ThreadServer() {
        try {
            conection = new DatagramSocket(6767);
        } catch (SocketException e) {
            e.printStackTrace();
            throw new IllegalStateException("No se pudo abrir el puerto 6767", e);
        }
    }

    /** True once the match started, so the server can switch to the players' screen. */
    public boolean hasStarted() {
        return started;
    }

    /** Number of clients currently connected, one player each. */
    public int getPlayerCount() {
        return currentClients;
    }

    /**
     * Sends one encoded match state to every connected client. A client that
     * cannot be reached must never interrupt the simulation, so failures are
     * only logged.
     */
    public void broadcastState(byte[] state) {
        if (!started || state == null)
            return;
        try {
            broadcast(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Last input decoded for the client in that slot, or {@code null} if the slot
     * is unknown or never sent one. Meant for observation and tests.
     */
    public InputState getClientInput(int slot) {
        if (slot < 0 || slot >= currentClients)
            return null;
        NetworkDir client = clients[slot];
        return client == null ? null : client.getLastInput();
    }

    private void send(byte[] data, InetAddress ip, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
        conection.send(packet);
    }

    private void sendHeader(byte header, InetAddress ip, int port) throws IOException {
        send(new byte[]{header}, ip, port);
    }

    @Override
    public void run() {
        while (running) {
            byte[] buf = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            try {
                conection.receive(dp);
                processMessage(dp);
            } catch (IOException e) {
                if (running)
                    e.printStackTrace();
                return;
            }
        }
    }

    private void processMessage(DatagramPacket dp) throws IOException {
        if (dp.getLength() < 1)
            return;

        byte header = dp.getData()[dp.getOffset()];

        switch (header) {
            case NetworkHeaders.CONNECT -> handleConexion(dp);
            case NetworkHeaders.READY -> handleReady(dp, true);
            case NetworkHeaders.UNREADY -> handleReady(dp, false);
            case NetworkHeaders.DISCONNECT -> handleDesconectar(dp);
            case NetworkHeaders.INPUT -> handleInput(dp);
        }
    }

    /**
     * Stores the last input of a connected client. Nothing is simulated yet: the
     * packet is validated, decoded and handed to the slot.
     */
    private void handleInput(DatagramPacket dp) {
        // Input only means something once the match is running.
        if (!started)
            return;

        int slot = findClient(dp);
        if (slot == -1)
            return;

        InputState input = InputPacket.decode(dp.getData(), dp.getOffset(), dp.getLength());
        if (input == null) {
            System.out.println("Input invalido del slot " + slot);
            return;
        }

        clients[slot].setLastInput(input);
    }

    private void handleConexion(DatagramPacket dp) throws IOException {
        int slot = findClient(dp);

        if (slot == -1) {
            if (started) {
                // la partida ya arranco: no se incorporan jugadores nuevos
                sendHeader(NetworkHeaders.REJECT, dp.getAddress(), dp.getPort());
                return;
            }
            if (currentClients >= MAX_CLIENTS) {
                // server lleno
                sendHeader(NetworkHeaders.REJECT, dp.getAddress(), dp.getPort());
                return;
            }
            slot = currentClients++;
            clients[slot] = new NetworkDir(dp.getAddress(), dp.getPort());
            System.out.println("Cliente conectado en slot " + slot + " (" + dp.getAddress() + ":" + dp.getPort() + ")");
        }

        send(new byte[]{NetworkHeaders.ACCEPT, (byte) slot}, dp.getAddress(), dp.getPort());

        broadcastRoster();

        checkStart();
    }

    private void handleReady(DatagramPacket dp, boolean ready) throws IOException {
        int slot = findClient(dp);
        if (slot == -1 || started)
            return;

        clients[slot].setReady(ready);
        System.out.println("Slot " + slot + (ready ? " esta listo" : " dejo de estar listo"));

        broadcastRoster();
        checkStart();
    }

    private void handleDesconectar(DatagramPacket dp) throws IOException {
        int slot = findClient(dp);
        if (slot == -1)
            return;

        System.out.println("Cliente desconectado del slot " + slot + " (" + dp.getAddress() + ":" + dp.getPort() + ")");
        removeSlot(slot);
        broadcastRoster();
    }

    private void checkStart() throws IOException {
        if (started || currentClients < MIN_CLIENTS)
            return;
        for (int i = 0; i < currentClients; i++) {
            if (!clients[i].isReady())
                return;
        }
        started = true;
        System.out.println("Partida iniciada con " + currentClients + " jugadores");
        broadcast(NetworkHeaders.START);
    }

    private void broadcastRoster() throws IOException {
        int readyCount = 0;
        for (int i = 0; i < currentClients; i++) {
            if (clients[i].isReady())
                readyCount++;
        }
        broadcast(new byte[]{NetworkHeaders.ROSTER, (byte) currentClients, (byte) readyCount});
    }

    private int findClient(DatagramPacket dp) {
        for (int i = 0; i < currentClients; i++) {
            if (clients[i] != null
                    && clients[i].getIp().equals(dp.getAddress())
                    && clients[i].getPort() == dp.getPort()) {
                return i;
            }
        }
        return -1;
    }

    private void removeSlot(int slot) {
        for (int i = slot; i < currentClients - 1; i++) {
            clients[i] = clients[i + 1];
        }
        clients[currentClients - 1] = null;
        currentClients--;
    }

    private void broadcast(byte header) throws IOException {
        for (int i = 0; i < currentClients; i++) {
            sendHeader(header, clients[i].getIp(), clients[i].getPort());
        }
    }

    private void broadcast(byte[] data) throws IOException {
        for (int i = 0; i < currentClients; i++) {
            send(data, clients[i].getIp(), clients[i].getPort());
        }
    }

    public void shutdown() {
        running = false;
        if (conection != null)
            conection.close();
    }
}
