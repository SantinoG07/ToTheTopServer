package com.TfPSR.ToTheTop.network;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class ThreadServer extends Thread {

    private static final int MAX_CLIENTS = 4;
    private static final int MIN_CLIENTS = 2;

    private final DatagramSocket conection;
    private volatile boolean running = true;
    private final NetworkDir[] clients = new NetworkDir[MAX_CLIENTS];
    private volatile int currentClients = 0;
    private volatile boolean started = false;   // una vez en partida ya no se admiten cambios
    private int nextSession = 1;                // ids de 1 a 255, nunca 0

    /**
     * Four-digit code a client must send on CONNECT. It is generated once per
     * server so the room can be told apart from other servers on the network.
     */
    private final byte[] roomCode = createRoomCode();

    public ThreadServer() {
        try {
            conection = new DatagramSocket(6767);
        } catch (SocketException e) {
            e.printStackTrace();
            throw new IllegalStateException("No se pudo abrir el puerto 6767", e);
        }
    }

    private static byte[] createRoomCode() {
        int code = 1000 + new Random().nextInt(9000);
        return Integer.toString(code).getBytes(StandardCharsets.US_ASCII);
    }

    /** Four-digit code players must type to join this room, for the menu label. */
    public String getRoomCode() {
        return new String(roomCode, StandardCharsets.US_ASCII);
    }

    /** True once the match started, so the server can switch to the players' screen. */
    public boolean hasStarted() {
        return started;
    }

    /** Number of clients currently connected, one player each. */
    public int getPlayerCount() {
        return currentClients;
    }

    /** Highest slot index the server can assign; slots never move once given. */
    public int getMaxClients() {
        return MAX_CLIENTS;
    }

    /** True while that slot still belongs to a connected client. */
    public boolean isSlotOccupied(int slot) {
        return slot >= 0 && slot < MAX_CLIENTS && clients[slot] != null;
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
        if (slot < 0 || slot >= MAX_CLIENTS)
            return null;
        NetworkDir client = clients[slot];
        return client == null ? null : client.getLastInput();
    }

    /**
     * Name registered for the client in that slot, or {@code null} when the slot
     * is unknown or free. Meant for observation and tests.
     */
    public String getClientName(int slot) {
        if (slot < 0 || slot >= MAX_CLIENTS)
            return null;
        NetworkDir client = clients[slot];
        return client == null ? null : client.getName();
    }

    private void send(byte[] data, InetAddress ip, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
        conection.send(packet);
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
            case NetworkHeaders.DISCOVER -> handleDiscover(dp);
            case NetworkHeaders.READY -> handleReady(dp, true);
            case NetworkHeaders.UNREADY -> handleReady(dp, false);
            case NetworkHeaders.DISCONNECT -> handleDesconectar(dp);
            case NetworkHeaders.INPUT -> handleInput(dp);
        }
    }

    /**
     * Answers a discovery broadcast with the room code, so a client can ask which
     * room is on this network before sending a CONNECT.
     */
    private void handleDiscover(DatagramPacket dp) throws IOException {
        if (!hasCurrentVersion(dp)) {
            sendReject(dp, NetworkHeaders.REJECT_VERSION);
            return;
        }

        byte[] announce = new byte[]{
                NetworkHeaders.ANNOUNCE,
                ProtocolVersion.CURRENT,
                roomCode[0], roomCode[1], roomCode[2], roomCode[3],
                (byte) currentClients,
                (byte) (started ? 1 : 0),
                (byte) MAX_CLIENTS};

        send(announce, dp.getAddress(), dp.getPort());
    }

    /**
     * Stores the last input of a connected client. Nothing is simulated yet: the
     * packet is validated, decoded and handed to the slot.
     */
    private void handleInput(DatagramPacket dp) throws IOException {
        // Input only means something once the match is running.
        if (!started)
            return;

        int slot = findClient(dp);
        if (slot == -1)
            return;

        // A message whose session does not match the connection is dropped: a
        // stray or spoofed datagram must never reach the simulation.
        if (readSession(dp) != clients[slot].getSession())
            return;

        InputState input = InputPacket.decode(dp.getData(), dp.getOffset(), dp.getLength());
        if (input == null) {
            System.out.println("Input invalido del slot " + slot);
            return;
        }

        // Leaving is one more event of the input packet: the client stops
        // sending and its slot is freed for the rest of the match.
        if ((input.events() & InputPacket.EVENT_LEAVE) != 0) {
            System.out.println("Cliente abandono la partida en el slot " + slot);
            removeSlot(slot);
            broadcastRoster();
            return;
        }

        clients[slot].setLastInput(input);
    }

    private void handleConexion(DatagramPacket dp) throws IOException {
        if (!hasCurrentVersion(dp)) {
            sendReject(dp, NetworkHeaders.REJECT_VERSION);
            return;
        }

        // A malformed request is dropped instead of answered: the client retries
        // the handshake on timeout, and a valid request with another code is the
        // only case that deserves a room rejection.
        ConnectPacket request = ConnectPacket.decode(dp.getData(), dp.getOffset(), dp.getLength());
        if (request == null)
            return;

        if (!getRoomCode().equals(request.roomCode())) {
            sendReject(dp, NetworkHeaders.REJECT_ROOM);
            return;
        }

        int slot = findClient(dp);

        if (slot == -1) {
            if (started) {
                // la partida ya arranco: no se incorporan jugadores nuevos
                sendReject(dp, NetworkHeaders.REJECT_STARTED);
                return;
            }
            slot = firstFreeSlot();
            if (slot < 0) {
                // server lleno
                sendReject(dp, NetworkHeaders.REJECT_FULL);
                return;
            }
            int session = nextSession();
            clients[slot] = new NetworkDir(dp.getAddress(), dp.getPort(), session);
            currentClients++;
            System.out.println("Cliente conectado en slot " + slot + " sesion " + session
                    + " (" + dp.getAddress() + ":" + dp.getPort() + ")");
        }

        clients[slot].setName(playerName(request.name(), slot));

        send(new byte[]{NetworkHeaders.ACCEPT, ProtocolVersion.CURRENT,
                (byte) clients[slot].getSession(), (byte) slot,
                roomCode[0], roomCode[1], roomCode[2], roomCode[3]},
                dp.getAddress(), dp.getPort());

        broadcastRoster();

        checkStart();
    }

    /** A client that sends no name still gets a distinct roster entry. */
    private static String playerName(String name, int slot) {
        return name == null || name.isEmpty() ? "Player " + (slot + 1) : name;
    }

    private void handleReady(DatagramPacket dp, boolean ready) throws IOException {
        int slot = findClient(dp);
        if (slot == -1 || started)
            return;

        if (readSession(dp) != clients[slot].getSession())
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

        if (readSession(dp) != clients[slot].getSession())
            return;

        System.out.println("Cliente desconectado del slot " + slot + " (" + dp.getAddress() + ":" + dp.getPort() + ")");
        removeSlot(slot);
        broadcastRoster();
    }

    private void checkStart() throws IOException {
        if (started || currentClients < MIN_CLIENTS)
            return;
        for (NetworkDir client : clients) {
            if (client != null && !client.isReady())
                return;
        }
        started = true;
        System.out.println("Partida iniciada con " + currentClients + " jugadores");
        broadcastStart();
    }

    /** START carries the session of each recipient, so it is sent one by one. */
    private void broadcastStart() throws IOException {
        for (NetworkDir client : clients) {
            if (client == null)
                continue;
            send(new byte[]{NetworkHeaders.START, ProtocolVersion.CURRENT,
                    (byte) client.getSession()}, client.getIp(), client.getPort());
        }
    }

    private void broadcastRoster() throws IOException {
        int readyCount = 0;
        for (NetworkDir client : clients) {
            if (client != null && client.isReady())
                readyCount++;
        }
        broadcast(new byte[]{NetworkHeaders.ROSTER, ProtocolVersion.CURRENT,
                (byte) currentClients, (byte) readyCount});
    }

    private int findClient(DatagramPacket dp) {
        for (int i = 0; i < MAX_CLIENTS; i++) {
            if (clients[i] != null
                    && clients[i].getIp().equals(dp.getAddress())
                    && clients[i].getPort() == dp.getPort()) {
                return i;
            }
        }
        return -1;
    }

    /** True when the datagram carries the protocol version this server speaks. */
    private boolean hasCurrentVersion(DatagramPacket dp) {
        return dp.getLength() >= 2 && dp.getData()[dp.getOffset() + 1] == ProtocolVersion.CURRENT;
    }

    /**
     * Session byte of a client message, or -1 when it is too short or was
     * written by another protocol version. Callers drop the packet.
     */
    private int readSession(DatagramPacket dp) {
        if (!hasCurrentVersion(dp) || dp.getLength() < 3)
            return -1;
        return dp.getData()[dp.getOffset() + 2] & 0xFF;
    }

    /** Fresh session id, never zero, wrapping after 255. */
    private int nextSession() {
        int session = nextSession;
        nextSession = nextSession >= 255 ? 1 : nextSession + 1;
        return session;
    }

    private void sendReject(DatagramPacket dp, byte reason) throws IOException {
        send(new byte[]{NetworkHeaders.REJECT, ProtocolVersion.CURRENT, reason},
                dp.getAddress(), dp.getPort());
    }

    /** Lowest free slot, so a client that leaves frees its index for a new one. */
    private int firstFreeSlot() {
        for (int i = 0; i < MAX_CLIENTS; i++) {
            if (clients[i] == null)
                return i;
        }
        return -1;
    }

    /**
     * Frees one slot. The other clients keep the index they were given in
     * {@code ACCEPT}: slots are never reindexed, because every client caches its
     * own slot and the state packet is routed by it.
     */
    private void removeSlot(int slot) {
        if (slot < 0 || slot >= MAX_CLIENTS || clients[slot] == null)
            return;
        clients[slot] = null;
        currentClients--;
    }

    private void broadcast(byte[] data) throws IOException {
        for (NetworkDir client : clients) {
            if (client != null)
                send(data, client.getIp(), client.getPort());
        }
    }

    public void shutdown() {
        running = false;
        if (conection != null)
            conection.close();
    }
}
