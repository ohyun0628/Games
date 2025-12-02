package intro;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import client.TypingGameClient;

public class IntroFrame extends JFrame {

    private Image bg;

    public IntroFrame() {

        setSize(600, 400);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
        bg = new ImageIcon("images/introbackground.png").getImage();
        ImageIcon startIcon = new ImageIcon("images/startbtn.png");

        JButton btnStart = new JButton(startIcon);
        btnStart.setBorderPainted(false);
        btnStart.setContentAreaFilled(false);
        btnStart.setFocusPainted(false);

        
        btnStart.setBounds(190, 150, startIcon.getIconWidth(), startIcon.getIconHeight());


        btnStart.addActionListener((ActionEvent e) -> {
            dispose();
            new TypingGameClient("127.0.0.1", 8000); 
        });

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                }

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 40));
            }
        };

        panel.setLayout(null);
        panel.add(btnStart);

        add(panel);
        setVisible(true);
    }

    public static void main(String[] args) {
        new IntroFrame();
    }
}
