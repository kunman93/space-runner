package ch.zhaw.it.pm3.spacerunner.technicalservices.sound.util;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * SoundUtil to load sound / set volume
 *
 * @author islermic (copied from old project and extended)
 */
public class SoundUtil {
    // Singleton pattern
    private static final SoundUtil SOUND_UTIL = new SoundUtil();

    /**
     * private constructor for the singleton-pattern
     */
    private SoundUtil() {
    }

    public static SoundUtil getUtil() {
        return SOUND_UTIL;
    }


    private int volume = 100;

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    /**
     * Loads a SoundClip from a File
     *
     * @param audioFile File to be loaded from
     * @return SoundClip that was loaded
     * @throws IOException if there is an issue with loading the file
     * @throws UnsupportedAudioFileException if the audio format of the file is not supported
     * @throws LineUnavailableException if there is no audio line available
     */
    public SoundClip loadClip(File audioFile) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        if (audioFile == null) {
            throw new IllegalArgumentException();
        }

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile.getAbsoluteFile());

        SoundClip soundClip = getSoundClip(audioInputStream);
        soundClip.setVolume(volume);
        return soundClip;
    }

    private SoundClip getSoundClip(AudioInputStream audioInputStream) throws LineUnavailableException, IOException {
        Clip loadedClip;

        loadedClip = AudioSystem.getClip();
        loadedClip.open(audioInputStream);

        return new SoundClip(loadedClip);
    }


}
