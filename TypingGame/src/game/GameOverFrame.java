package game;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class GameOverFrame extends JFrame {

    public GameOverFrame(String result) {

        setTitle("GAME OVER");
        setSize(400, 250);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        JLabel title = new JLabel("GAME OVER", SwingConstants.CENTER);
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setBounds(0, 20, 400, 50);
        add(title);

        JLabel resText = new JLabel(result, SwingConstants.CENTER);
        resText.setFont(new Font("Malgun Gothic", Font.BOLD, 28));
        resText.setForeground(new Color(0, 200, 255));
        resText.setBounds(0, 100, 400, 50);
        add(resText);

        getContentPane().setBackground(new Color(20, 20, 40));

        setVisible(true);
    }
}

