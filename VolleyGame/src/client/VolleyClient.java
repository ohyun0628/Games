package client;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import game.VolleyPanel;
import game.WavPlayer;
import server.VolleyServer;

public class VolleyClient {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new VolleyClient().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void start() throws Exception {

        Socket socket = new Socket("localhost", VolleyServer.PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        VolleyPanel panel = new VolleyPanel(out);

        JFrame frame = new JFrame("Volley Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        WavPlayer bgm = new WavPlayer("sounds/VolleyBackground.wav");
        bgm.playLoop();

        WavPlayer spikeSound = new WavPlayer("sounds/Spike.wav");
        WavPlayer touchSound = new WavPlayer("sounds/Touch.wav");

        new Thread(() -> {
            try {
                String line;

                while ((line = in.readLine()) != null) {

                    if (line.startsWith("STATE")) {

                        String[] p = line.split(" ");
                        if (p.length == 7) {
                            int p1x = Integer.parseInt(p[1]);
                            int p1y = Integer.parseInt(p[2]);
                            int p2x = Integer.parseInt(p[3]);
                            int p2y = Integer.parseInt(p[4]);
                            int bx  = Integer.parseInt(p[5]);
                            int by  = Integer.parseInt(p[6]);

                            panel.updateState(p1x, p1y, p2x, p2y, bx, by);
                        }

                    } else if (line.startsWith("SCORE")) {

                        String[] p = line.split(" ");
                        if (p.length == 3) {
                            int s1 = Integer.parseInt(p[1]);
                            int s2 = Integer.parseInt(p[2]);
                            panel.updateScore(s1, s2);
                        }

                    } else if (line.startsWith("SET")) {

                        String[] p = line.split(" ");
                        if (p.length == 3) {
                            int s1 = Integer.parseInt(p[1]);
                            int s2 = Integer.parseInt(p[2]);
                            panel.updateSetScore(s1, s2);
                        }


                    } else if (line.equals("READY")) {

                        panel.showStatus("READY", 1500);

                    } else if (line.equals("GO")) {

                        panel.showStatus("GO!", 1000);

                    } else if (line.startsWith("GAMEOVER")) {

                        String[] p = line.split(" ");
                        int winner = (p.length == 2) ? Integer.parseInt(p[1]) : 0;

                        panel.showGameOver(winner);

                        bgm.stop();

                    } else if (line.equals("NEWGAME")) {

                        panel.onNewGame();

                        bgm.playLoop();
                    }

                    else if (line.equals("SFX_SPIKE")) {
                        spikeSound.stop();
                        spikeSound.playOnce();
                    }


                    else if (line.equals("SFX_TOUCH")) {
                        touchSound.stop();
                        touchSound.playOnce();
                    }

                }

            } catch (Exception e) {
                System.out.println("서버 연결 종료");
            }
        }).start();
    }
}
