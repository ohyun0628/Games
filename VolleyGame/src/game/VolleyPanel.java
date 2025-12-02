package game;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class VolleyPanel extends JPanel implements KeyListener {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 600;
    public static final int FLOOR_Y = 540;

    private PrintWriter out;

    private Image backgroundImg;
    private Image player1Img;
    private Image player2Img;
    private Image ballImg;

    private volatile int p1x = 150, p1y = 450;
    private volatile int p2x = 650, p2y = 450;
    private volatile int ballx = 450, bally = 200;

    private volatile int score1 = 0, score2 = 0;
    private volatile int set1 = 0, set2 = 0;

    private volatile String statusText = "";
    private volatile long statusUntil = 0L;

    private volatile boolean gameOver = false;
    private volatile int winner = 0;
    private volatile boolean waitingRestart = false;

    private boolean hoverRestart = false;
    private boolean hoverExit = false;

    public VolleyPanel(PrintWriter out) {
        this.out = out;

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

        backgroundImg = new ImageIcon("images/background.png").getImage();
        player1Img    = new ImageIcon("images/player1.png").getImage();
        player2Img    = new ImageIcon("images/player2.png").getImage();
        ballImg       = new ImageIcon("images/ball.png").getImage();

        new Timer(16, e -> repaint()).start();
    }
    public void updateState(int p1x, int p1y, int p2x, int p2y, int bx, int by) {
        if (!gameOver) {
            this.p1x = p1x;
            this.p1y = p1y;
            this.p2x = p2x;
            this.p2y = p2y;
            this.ballx = bx;
            this.bally = by;
        }
    }

    public void updateScore(int s1, int s2) {
        this.score1 = s1;
        this.score2 = s2;
    }

    public void updateSetScore(int s1, int s2) {
        this.set1 = s1;
        this.set2 = s2;
    }

    public void showStatus(String text, int millis) {
        this.statusText = text;
        this.statusUntil = System.currentTimeMillis() + millis;
    }

    public void showGameOver(int winner) {
        this.winner = winner;
        this.gameOver = true;
        this.waitingRestart = false;
        this.hoverRestart = false;
        this.hoverExit = false;
    }

    public void onNewGame() {
        this.gameOver = false;
        this.waitingRestart = false;
        this.statusText = "";
        this.statusUntil = 0L;
        this.score1 = this.score2 = 0;
        this.set1 = this.set2 = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(backgroundImg, 0, 0, WIDTH, HEIGHT, null);

        int netCenterX = 450;
        int netWidth = 14;
        int netHeight = 180;
        g.setColor(new Color(30,30,30));
        g.fillRoundRect(netCenterX - netWidth/2, FLOOR_Y - netHeight, netWidth, netHeight, 8, 8);

        g.drawImage(player1Img, p1x, p1y, 130, 130, null);
        g.drawImage(player2Img, p2x, p2y, 130, 130, null);
        g.drawImage(ballImg, ballx, bally, 70, 70, null);

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(WIDTH/2 - 120, 10, 240, 60, 20, 20);

        g.setColor(Color.WHITE);
        g.setFont(new Font("NanumSquare", Font.BOLD, 26));
        String scoreText = score1 + "  :  " + score2;
        int strWidth = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, WIDTH/2 - strWidth/2, 48);

        g.setFont(new Font("NanumSquare", Font.PLAIN, 14));
        String setText = "SET  " + set1 + "  -  " + set2;
        int setWidth = g.getFontMetrics().stringWidth(setText);
        g.drawString(setText, WIDTH/2 - setWidth/2, 24);

        if (!statusText.isEmpty() && System.currentTimeMillis() < statusUntil && !gameOver) {
            g.setFont(new Font("NanumSquare", Font.BOLD, 60));
            int w = g.getFontMetrics().stringWidth(statusText);
            g.setColor(new Color(0,0,0,150));
            g.fillRoundRect(WIDTH/2 - w/2 - 20, HEIGHT/2 - 50, w + 40, 80, 30, 30);

            g.setColor(Color.YELLOW);
            g.drawString(statusText, WIDTH/2 - w/2, HEIGHT/2 + 15);
        }

        if (gameOver) {
            drawGameOverUI(g);
        }
    }

    private void drawGameOverUI(Graphics g) {

        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int boxW = 460, boxH = 300;
        int boxX = WIDTH/2 - boxW/2;
        int boxY = HEIGHT/2 - boxH/2;

        g.setColor(new Color(255, 255, 255, 235));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 35, 35);

        g.setColor(new Color(30, 30, 30));
        g.setFont(new Font("NanumSquare", Font.BOLD, 38));
        String winnerText = "PLAYER " + winner + " WIN!";
        int w = g.getFontMetrics().stringWidth(winnerText);
        g.drawString(winnerText, WIDTH/2 - w/2, boxY + 90);

        g.setFont(new Font("NanumSquare", Font.PLAIN, 18));
        String msg = waitingRestart
                ? "상대방이 기다리는 중..."
                : "다시하기 또는 종료를 선택하세요";
        int w2 = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, WIDTH/2 - w2/2, boxY + 135);

        drawButtons(g, boxX, boxY, boxW, boxH);
    }

    private void drawButtons(Graphics g, int boxX, int boxY, int boxW, int boxH) {
        int btnW = 150;
        int btnH = 45;

        int restartX = boxX + 55;
        int exitX    = boxX + boxW - btnW - 55;
        int btnY     = boxY + 190;

        Color restartColor;
        Color exitColor;

        if (waitingRestart) {
            restartColor = new Color(150, 170, 200);
            exitColor    = new Color(200, 120, 120);
        } else {
            restartColor = hoverRestart ? new Color(90,150,255) : new Color(70,130,255);
            exitColor    = hoverExit    ? new Color(255,100,100) : new Color(255,80,80);
        }

        g.setColor(new Color(0,0,0,70));
        g.fillRoundRect(restartX+3, btnY+3, btnW, btnH, 20, 20);
        g.fillRoundRect(exitX+3, btnY+3, btnW, btnH, 20, 20);

        g.setColor(restartColor);
        g.fillRoundRect(restartX, btnY, btnW, btnH, 20, 20);

        g.setColor(exitColor);
        g.fillRoundRect(exitX, btnY, btnW, btnH, 20, 20);

        g.setColor(Color.WHITE);
        g.setFont(new Font("NanumSquare", Font.BOLD, 20));
        g.drawString("다시하기", restartX + 30, btnY + 30);
        g.drawString("종료", exitX + 50, btnY + 30);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (!gameOver) return;

        int mx = e.getX();
        int my = e.getY();

        int boxW = 460;
        int boxH = 300;
        int boxX = WIDTH/2 - boxW/2;
        int boxY = HEIGHT/2 - boxH/2;

        int btnW = 150;
        int btnH = 45;

        int restartX = boxX + 55;
        int exitX    = boxX + boxW - btnW - 55;
        int btnY     = boxY + 190;

        if (e.getID() == MouseEvent.MOUSE_CLICKED) {

            if (waitingRestart) return;

            if (mx >= restartX && mx <= restartX + btnW &&
                my >= btnY     && my <= btnY + btnH) {
                restartGame();
            }

            if (mx >= exitX && mx <= exitX + btnW &&
                my >= btnY     && my <= btnY + btnH) {
                System.exit(0);
            }
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (!gameOver || waitingRestart) return;

        int mx = e.getX();
        int my = e.getY();

        int boxW = 460;
        int boxH = 300;
        int boxX = WIDTH/2 - boxW/2;
        int boxY = HEIGHT/2 - boxH/2;

        int btnW = 150;
        int btnH = 45;

        int restartX = boxX + 55;
        int exitX    = boxX + boxW - btnW - 55;
        int btnY     = boxY + 190;

        hoverRestart = (mx >= restartX && mx <= restartX+btnW &&
                        my >= btnY && my <= btnY+btnH);

        hoverExit = (mx >= exitX && mx <= exitX+btnW &&
                     my >= btnY && my <= btnY+btnH);

        repaint();
    }

    public void restartGame() {
        waitingRestart = true;
        hoverRestart = false;
        hoverExit = false;

        out.println("RESTART");
    }

    private void sendKey(String action, String key) {
        out.println("KEY " + action + " " + key);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;

        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT)  sendKey("DOWN", "LEFT");
        if (key == KeyEvent.VK_RIGHT) sendKey("DOWN", "RIGHT");
        if (key == KeyEvent.VK_UP)    sendKey("DOWN", "JUMP");
        if (key == KeyEvent.VK_SPACE) sendKey("DOWN", "SPIKE");
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) return;

        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT)  sendKey("UP", "LEFT");
        if (key == KeyEvent.VK_RIGHT) sendKey("UP", "RIGHT");
        if (key == KeyEvent.VK_UP)    sendKey("UP", "JUMP");
        if (key == KeyEvent.VK_SPACE) sendKey("UP", "SPIKE");
    }

    @Override public void keyTyped(KeyEvent e) {}
}
