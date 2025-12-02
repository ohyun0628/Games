package server;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class VolleyServer {

    public static final int PORT = 9000;
    public static final int FLOOR_Y = 540;
    public static final int NET_X = 450;
    public static final int NET_WIDTH = 14;

    public static void main(String[] args) {
        new VolleyServer().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버 시작. 플레이어 2명을 기다립니다...");

            Socket p1Socket = serverSocket.accept();
            System.out.println("플레이어 1 연결됨");

            Socket p2Socket = serverSocket.accept();
            System.out.println("플레이어 2 연결됨");

            GameRoom room = new GameRoom(p1Socket, p2Socket);
            room.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class GameRoom extends Thread {

        private Socket p1Socket, p2Socket;
        PrintWriter out1, out2;

        private InputState input1 = new InputState();
        private InputState input2 = new InputState();

        ServerPlayer p1, p2;
        ServerBall ball;
        ServerNet net;

        private AtomicBoolean running = new AtomicBoolean(true);

        private int score1 = 0;
        private int score2 = 0;
        private int set1 = 0;
        private int set2 = 0;

        private static final int POINTS_TO_WIN_SET = 15;
        private static final int SETS_TO_WIN_MATCH = 2;

        private boolean matchOver = false;
        private boolean p1Restart = false;
        private boolean p2Restart = false;

        private boolean waitingForServe = false;
        private int readyTimer = 0;
        private int serveDir = 1;

        public GameRoom(Socket p1Socket, Socket p2Socket) {
            this.p1Socket = p1Socket;
            this.p2Socket = p2Socket;

            try {
                out1 = new PrintWriter(p1Socket.getOutputStream(), true);
                out2 = new PrintWriter(p2Socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            p1 = new ServerPlayer(150, 450, 1);
            p2 = new ServerPlayer(650, 450, 2);
            ball = new ServerBall(450, 200, this);
            net  = new ServerNet(NET_X, FLOOR_Y, NET_WIDTH, 160);
        }

        @Override
        public void run() {
            try {
                out1.println("ROLE 1");
                out2.println("ROLE 2");

                new Thread(new ClientReceiver(p1Socket, input1, this, 1)).start();
                new Thread(new ClientReceiver(p2Socket, input2, this, 2)).start();

                startSetRound(1);

                long prev = System.currentTimeMillis();
                final int FPS = 60;
                final int frameTime = 1000 / FPS;

                while (running.get()) {
                    long now = System.currentTimeMillis();
                    long elapsed = now - prev;

                    if (elapsed < frameTime)
                        Thread.sleep(frameTime - elapsed);

                    prev = System.currentTimeMillis();

                    updateGame();
                    broadcastState();
                }

            } catch (Exception e) {
                System.out.println("게임 종료: " + e.getMessage());
            }
        }

        private void startSetRound(int servePlayer) {

            ball.x = 450;
            ball.y = 200;
            ball.vx = 0;
            ball.vy = 0;

            p1.reset();
            p2.reset();

            serveDir = (servePlayer == 1) ? 1 : -1;

            waitingForServe = true;
            readyTimer = 120;

            out1.println("READY");
            out2.println("READY");
        }

        private void startPointRound(int servePlayer) {
            ball.x = 450;
            ball.y = 200;
            ball.vx = (servePlayer == 1) ? 4 : -4;
            ball.vy = 0;
            waitingForServe = false;
        }

        public synchronized void onRestartRequest(int playerId) {
            if (!matchOver) return;

            if (playerId == 1) p1Restart = true;
            if (playerId == 2) p2Restart = true;

            if (p1Restart && p2Restart) {
                resetMatch();
            }
        }

        private void resetMatch() {

            matchOver = false;
            p1Restart = false;
            p2Restart = false;

            score1 = score2 = 0;
            set1 = set2 = 0;

            out1.println("NEWGAME");
            out2.println("NEWGAME");

            out1.println("SCORE 0 0");
            out2.println("SCORE 0 0");
            out1.println("SET 0 0");
            out2.println("SET 0 0");

            startSetRound(1);
        }

        private void updateGame() {

            if (matchOver) return;

            if (waitingForServe) {

                p1.update(input1, net);
                p2.update(input2, net);

                readyTimer--;

                if (readyTimer == 60) {
                    out1.println("GO");
                    out2.println("GO");
                }

                if (readyTimer <= 0) {
                    waitingForServe = false;
                    ball.vx = 4 * serveDir;
                }
                return;
            }

            p1.update(input1, net);
            p2.update(input2, net);

            ball.update();
            ball.checkFloor(FLOOR_Y);
            ball.checkWall(0, 900);
            ball.checkNet(net);

            ball.checkPlayer(p1);
            ball.checkPlayer(p2);

            ball.checkSpike(p1);
            ball.checkSpike(p2);

            checkScore();
        }

        private void checkScore() {
            if (ball.y + ball.size >= FLOOR_Y) {
                if (ball.x < NET_X) { onPointScored(2); }
                else { onPointScored(1); }
            }
        }

        private void onPointScored(int scorer) {

            if (scorer == 1) score1++;
            else score2++;

            out1.println("SCORE " + score1 + " " + score2);
            out2.println("SCORE " + score1 + " " + score2);

            if (score1 >= POINTS_TO_WIN_SET || score2 >= POINTS_TO_WIN_SET) {

                if (score1 >= POINTS_TO_WIN_SET) set1++;
                else set2++;

                out1.println("SET " + set1 + " " + set2);
                out2.println("SET " + set1 + " " + set2);

                score1 = score2 = 0;

                if (set1 >= SETS_TO_WIN_MATCH || set2 >= SETS_TO_WIN_MATCH) {
                    int winner = (set1 > set2) ? 1 : 2;
                    out1.println("GAMEOVER " + winner);
                    out2.println("GAMEOVER " + winner);

                    matchOver = true;
                    return;
                }

                startSetRound(scorer);
                return;
            }

            startPointRound(scorer);
        }

        private void broadcastState() {
            String state = String.format(
                    "STATE %d %d %d %d %d %d",
                    p1.x, p1.y,
                    p2.x, p2.y,
                    ball.x, ball.y
            );
            out1.println(state);
            out2.println(state);
        }
    }

    static class InputState {
        volatile boolean left = false;
        volatile boolean right = false;
        volatile boolean jumpPressed = false;
        volatile boolean spikePressed = false;

        public synchronized void setKey(String key, boolean down) {
            switch (key) {
                case "LEFT":  left = down; break;
                case "RIGHT": right = down; break;
                case "JUMP":
                    if (down) jumpPressed = true;
                    break;
                case "SPIKE":
                    spikePressed = down;
                    break;
            }
        }

        public synchronized boolean consumeJump() {
            if (jumpPressed) {
                jumpPressed = false;
                return true;
            }
            return false;
        }
    }

    static class ClientReceiver implements Runnable {
        private Socket socket;
        private InputState input;
        private GameRoom room;
        private int playerId;

        public ClientReceiver(Socket socket, InputState input, GameRoom room, int playerId) {
            this.socket = socket;
            this.input = input;
            this.room = room;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {

                String line;
                while ((line = in.readLine()) != null) {
                    String[] p = line.split(" ");

                    if (p[0].equals("KEY") && p.length == 3) {
                        boolean down = p[1].equals("DOWN");
                        input.setKey(p[2], down);

                    } else if (line.equals("RESTART")) {
                        room.onRestartRequest(playerId);
                    }
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 종료");
            }
        }
    }

    static class ServerPlayer {
        int x, y;
        int width = 130, height = 130;
        double vx = 0, vy = 0;
        boolean onGround = true;
        int role;

        boolean isSpiking = false;
        int spikeTimer = 0;

        public ServerPlayer(int x, int y, int role) {
            this.x = x;
            this.y = y;
            this.role = role;
        }

        public void reset() {
            x = (role == 1) ? 150 : 650;
            y = 450;
            vx = vy = 0;
            onGround = true;
            isSpiking = false;
        }

        public void update(InputState input, ServerNet net) {

            if (input.left)      vx = -6;
            else if (input.right) vx = 6;
            else                 vx = 0;

            x += vx;

            int netLeft  = net.centerX - net.width / 2;
            int netRight = net.centerX + net.width / 2;

            if (role == 1 && x + width > netLeft)  x = netLeft - width;
            if (role == 2 && x < netRight)         x = netRight;

            vy += 0.8;
            y += vy;

            if (y + height >= FLOOR_Y) {
                y = FLOOR_Y - height;
                vy = 0;
                onGround = true;
            }

            if (input.consumeJump() && onGround) {
                vy = -15;
                onGround = false;
            }

            if (input.spikePressed && !isSpiking && !onGround) {
                isSpiking = true;
                spikeTimer = 8;
            }

            if (isSpiking) {
                spikeTimer--;
                if (spikeTimer <= 0)
                    isSpiking = false;
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public Rectangle getSpikeHitbox() {
            if (!isSpiking) return null;

            if (role == 1)
                return new Rectangle(x + width - 20, y - 20, 40, height + 40);
            else
                return new Rectangle(x - 20, y - 20, 40, height + 40);
        }
    }

    static class ServerBall {
        int x, y;
        int size = 70;
        double vx = 4, vy = 0;

        private GameRoom room;

        public ServerBall(int x, int y, GameRoom room) {
            this.x = x;
            this.y = y;
            this.room = room;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.5;
        }

        public void checkFloor(int floor) {
            if (y + size >= floor) {
                y = floor - size;
                vy *= -0.85;
            }
        }

        public void checkWall(int L, int R) {
            if (x <= L) {
                x = L;
                vx *= -1;
            }
            if (x + size >= R) {
                x = R - size;
                vx *= -1;
            }
        }

        public void checkNet(ServerNet net) {
            Rectangle br = new Rectangle(x, y, size, size);
            Rectangle nr = net.getBounds();

            if (!br.intersects(nr)) return;

            if (x + size / 2 < net.centerX) {
                x = nr.x - size - 1;
                vx = -Math.abs(vx);
            } else {
                x = nr.x + nr.width + 1;
                vx = Math.abs(vx);
            }
        }

        public void checkPlayer(ServerPlayer p) {
            Rectangle br = new Rectangle(x, y, size, size);
            Rectangle pr = p.getBounds();

            if (br.intersects(pr)) {

                room.out1.println("SFX_TOUCH");
                room.out2.println("SFX_TOUCH");

                vy = -10;
                vx = (x < p.x) ? -5 : 5;
            }
        }

        public void checkSpike(ServerPlayer p) {
            if (!p.isSpiking) return;

            Rectangle spikeBox = p.getSpikeHitbox();
            if (spikeBox == null) return;

            Rectangle br = new Rectangle(x, y, size, size);

            if (br.intersects(spikeBox)) {

                room.out1.println("SFX_SPIKE");
                room.out2.println("SFX_SPIKE");

                int dir = (p.role == 1) ? 1 : -1;
                vx = 12 * dir;
                vy = 14;
                y += 5;
            }
        }
    }

    static class ServerNet {
        int centerX;
        int floorY;
        int width;
        int height;

        public ServerNet(int centerX, int floorY, int width, int height) {
            this.centerX = centerX;
            this.floorY = floorY;
            this.width = width;
            this.height = height;
        }

        public Rectangle getBounds() {
            int leftX = centerX - width / 2;
            int topY  = floorY - height;
            return new Rectangle(leftX, topY, width, height);
        }
    }
}
