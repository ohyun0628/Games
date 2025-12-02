package game;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class WavPlayer {

    private Clip clip;

    public WavPlayer(String path) {
        try {
            File audioFile = new File(path);  
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            clip = AudioSystem.getClip();
            clip.open(audioStream);

        } catch (Exception e) {
            System.out.println("WAV LOAD ERROR: " + path);
            e.printStackTrace();
        }
    }

    public void play() {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }


    public void playLoop() {
        if (clip == null) return;
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
    }

    public void stop() {
        if (clip != null) clip.stop();
    }
}
