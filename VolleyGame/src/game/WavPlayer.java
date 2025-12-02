package game;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class WavPlayer {
    private Clip clip;

    public WavPlayer(String path) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new java.io.File(path));
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playLoop() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        }
    }

    public void playOnce() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
        }
    }
}
