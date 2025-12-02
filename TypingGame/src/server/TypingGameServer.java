package server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TypingGameServer {

    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();

    private Map<Integer, Player> players = new HashMap<>();
    private int nextId = 1;

    private Timer gameTimer;
    private Timer timeTimer;

    private final int GAME_HEIGHT = 600;
    private final int SPEED = 2;
    private int remainingTime = 120;

    private List<String> wordList;

    class Player {
        int id;
        int score = 0;
        String word;
        String input = "";
        int y = 0;

        Player(int i, String w) {
            id = i;
            word = w;
        }
    }

    public TypingGameServer(int port) throws Exception {

        serverSocket = new ServerSocket(port);

        wordList = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader("words.txt"));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty())
                wordList.add(line.trim());
        }
        br.close();

        System.out.println("서버 실행됨");
    }

    public void start() {

        new Thread(() -> {
            while (clients.size() < 2) {
                try {
                    Socket s = serverSocket.accept();

                    int id = nextId++;
                    Player p = new Player(id, randomWord());
                    players.put(id, p);

                    ClientHandler h = new ClientHandler(s, id);
                    clients.add(h);
                    h.start();

                    if (clients.size() == 2) {
                        broadcast("READY");
                        startGame();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startGame() {

        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                update();
                sendState();
            }
        }, 0, 30);

        timeTimer = new Timer();
        timeTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                remainingTime--;
                broadcast("TIME " + remainingTime);

                if (remainingTime <= 0)
                    endGame();
            }
        }, 1000, 1000);
    }

    private void update() {
        for (Player p : players.values()) {
            p.y += SPEED;

            if (p.y > GAME_HEIGHT) {
                p.score = Math.max(0, p.score - 5);
                p.word = randomWord();
                p.input = "";
                p.y = 0;
            }
        }
    }

    private void endGame() {
        gameTimer.cancel();
        timeTimer.cancel();

        Player p1 = players.get(1);
        Player p2 = players.get(2);

        String result;
        if (p1.score > p2.score) result = "P1_WIN";
        else if (p2.score > p1.score) result = "P2_WIN";
        else result = "DRAW";

        broadcast("GAMEOVER " + result);
    }

    private void sendState() {

        Player p1 = players.get(1);
        Player p2 = players.get(2);

        String msg = "GAMESTATE "
                + "P1 score=" + p1.score + " word=" + p1.word + " input=" + p1.input + " y=" + p1.y + " "
                + "P2 score=" + p2.score + " word=" + p2.word + " input=" + p2.input + " y=" + p2.y;

        broadcast(msg);
    }

    private void broadcast(String m) {
        for (ClientHandler c : clients)
            c.send(m);
    }

    private String randomWord() {
        return wordList.get((int) (Math.random() * wordList.size()));
    }

    class ClientHandler extends Thread {

        Socket socket;
        int id;
        PrintWriter out;
        BufferedReader in;

        ClientHandler(Socket s, int i) {
            socket = s;
            id = i;

            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            } catch (Exception e) {
            }
        }

        public void send(String m) {
            out.println(m);
        }

        public void run() {
            try {
                String line;

                while ((line = in.readLine()) != null) {

                    if (line.startsWith("INPUT")) {
                        String text = line.substring(6).trim();

                        Player me = players.get(id);
                        Player other = players.get(id == 1 ? 2 : 1);

                        if (text.equals(me.word)) {
                            me.score += 10;
                            me.word = randomWord();
                            me.input = "";
                            me.y = 0;
                        }
                        
                        else if (text.equals(other.word)) {
                            me.score += 10;
                            other.word = randomWord();
                            other.input = "";
                            other.y = 0;
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("클라이언트 종료");
            }
        }
    }

    public static void main(String[] args) {
        try {
            new TypingGameServer(8000).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
