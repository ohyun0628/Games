package intro;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class IntroFrame extends JFrame {

    public IntroFrame() {

        setTitle("Volley Game Intro");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        setResizable(false);

        JPanel bg = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(140, 190, 255),   
                        0, getHeight(), new Color(30, 90, 180) 
                );

                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        bg.setBounds(0, 0, 400, 500);
        bg.setLayout(null);
        add(bg);

        JLabel title = new JLabel("Volley Game", SwingConstants.CENTER);
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 36));
        title.setForeground(Color.WHITE); 
        title.setBounds(50, 70, 300, 60);
        bg.add(title);

        JButton btnStart = new JButton("게임 시작");
        btnStart.setBounds(100, 200, 200, 55);
        btnStart.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        btnStart.setFocusPainted(false);
        btnStart.setBackground(new Color(255, 255, 255));
        btnStart.setForeground(new Color(20, 60, 140));
        btnStart.setBorder(BorderFactory.createLineBorder(new Color(90, 140, 230), 2));
        bg.add(btnStart);

        JButton btnExit = new JButton("종료");
        btnExit.setBounds(100, 280, 200, 55);
        btnExit.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        btnExit.setFocusPainted(false);
        btnExit.setBackground(new Color(255, 255, 255));
        btnExit.setForeground(new Color(20, 60, 140));
        btnExit.setBorder(BorderFactory.createLineBorder(new Color(90, 140, 230), 2));
        bg.add(btnExit);

        btnStart.addActionListener(e -> {
            new Thread(() -> {
                try {
                    client.VolleyClient.main(null);   
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

            dispose();
        });

        btnExit.addActionListener(e -> System.exit(0));

        setVisible(true);
    }

    public static void main(String[] args) {
        new IntroFrame();
    }
}
