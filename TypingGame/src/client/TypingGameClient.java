package client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import game.GameOverFrame;
import game.WavPlayer;   

public class TypingGameClient extends JPanel implements KeyListener, Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame;

    private boolean ready = false;

    private int playerId;
    private int p1Score = 0, p2Score = 0;
    private int remainingTime = 120;

    private String word1 = "";
    private String word2 = "";

    private int wordY1 = 0;
    private int wordY2 = 0;

    private String currentInput = "";

    private Image backgroundImage;

    private WavPlayer bgm;
    private WavPlayer successSound;


    public TypingGameClient(String ip, int port) {

        setFocusable(true);
        addKeyListener(this);

        backgroundImage = new ImageIcon("images/background.png").getImage();

        frame = new JFrame("Typing Game");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

       
        bgm = new WavPlayer("sounds/TypingBackground.wav");
        successSound = new WavPlayer("sounds/TypingSuccess.wav");
        bgm.playLoop();

        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(this).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "서버 연결 실패");
            System.exit(0);
        }
    }

    @Override
    public void run() {
        try {
            String msg;

            while ((msg = in.readLine()) != null) {

                if (msg.equals("READY")) {
                    ready = true;
                }

                else if (msg.startsWith("YOUR_ID")) {
                    playerId = Integer.parseInt(msg.split(" ")[1]);
                }

                else if (ready && msg.startsWith("GAMESTATE")) {
                    parseGameState(msg);
                }

                else if (ready && msg.startsWith("TIME")) {
                    remainingTime = Integer.parseInt(msg.split(" ")[1]);
                }

                else if (msg.startsWith("GAMEOVER")) {

                    String result = msg.split(" ")[1];

                    String text = "";
                    if (result.equals("P1_WIN")) text = "Player 1 승리!";
                    else if (result.equals("P2_WIN")) text = "Player 2 승리!";
                    else text = "무승부!";

                    frame.dispose();

                    if (bgm != null) bgm.stop();

                    new GameOverFrame(text);
                    break;
                }

                repaint();
            }
        } catch (Exception e) {
            System.out.println("서버와 연결 종료");
        }
    }

    private void parseGameState(String msg) {

        String[] arr = msg.split(" ");

        int oldP1Score = p1Score;
        int oldP2Score = p2Score;

        String oldWord1 = word1;
        String oldWord2 = word2;

        p1Score = Integer.parseInt(arr[2].split("=")[1]);
        word1   = arr[3].split("=")[1];
        wordY1  = Integer.parseInt(arr[5].split("=")[1]);

        p2Score = Integer.parseInt(arr[7].split("=")[1]);
        word2   = arr[8].split("=")[1];
        wordY2  = Integer.parseInt(arr[10].split("=")[1]);

        if (p1Score > oldP1Score || p2Score > oldP2Score) {
            successSound.play();
        }

        if (!oldWord1.equals(word1) || !oldWord2.equals(word2)) {
            currentInput = "";
        }
    }


    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {

        if (!ready) return;

        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        }
        else if (Character.isLetterOrDigit(e.getKeyChar())) {
            currentInput += e.getKeyChar();
        }

        out.println("INPUT " + currentInput);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (backgroundImage != null)
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);

        if (!ready) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            g2.drawString("WAITING...", getWidth() / 2 - 120, getHeight() / 2);
            return;
        }

        g2.setFont(new Font("Arial", Font.BOLD, 32));
        g2.setColor(Color.WHITE);
        g2.drawString("TIME : " + remainingTime + "s", getWidth() / 2 - 80, 60);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("P1 SCORE : " + p1Score, 50, 100);

        g2.drawString("P2 SCORE : " + p2Score, getWidth() - 250, 100);

        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(new Color(255, 170, 170));
        g2.drawString(word1, getWidth() / 4 - word1.length() * 7, wordY1);
        g2.drawString(word2, getWidth() * 3 / 4 - word2.length() * 7, wordY2);

        g2.setColor(new Color(0, 200, 255));
        g2.drawRoundRect(getWidth() / 2 - 150, getHeight() - 80, 300, 40, 15, 15);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        g2.drawString(currentInput, getWidth() / 2 - 140, getHeight() - 52);
    }

    public static void main(String[] args) {
        new TypingGameClient("127.0.0.1", 8000);
    }
}
