package ch.zhaw.it.pm3.spacerunner.technicalservices.performance;

import ch.zhaw.it.pm3.spacerunner.model.spaceelement.UFO;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FPSTracker {

    private Logger logger = Logger.getLogger(FPSTracker.class.getName());

    private final long[] frameTimes = new long[100];
    private int frameTimeIndex = 0 ;
    private boolean arrayFilled = false ;

    public void track(long currentNanoTime){
        long oldFrameTime = frameTimes[frameTimeIndex];
        frameTimes[frameTimeIndex] = currentNanoTime;
        frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length;
        if (frameTimeIndex == 0) {
            arrayFilled = true ;
        }
        if (arrayFilled) {
            long elapsedNanos = currentNanoTime - oldFrameTime;
            long elapsedNanosPerFrame = elapsedNanos / frameTimes.length;
            double frameRate = 1_000_000_000.0 / elapsedNanosPerFrame;
            logger.log(Level.INFO, String.format("Current frame rate: %.3f", frameRate));
        }
    }
}
