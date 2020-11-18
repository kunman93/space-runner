package ch.zhaw.it.pm3.spacerunner.model.spaceelement;

import ch.zhaw.it.pm3.spacerunner.model.spaceelement.velocity.VelocityManager;
import ch.zhaw.it.pm3.spacerunner.technicalservices.visual.manager.VisualManager;
import ch.zhaw.it.pm3.spacerunner.technicalservices.visual.VisualNotSetException;

import java.awt.geom.Point2D;

public class UFO extends Obstacle {

    //TODO discuss how to set the speed and movement, eventually use strategy-Patter for different movements?

    private final VisualManager visualManager = VisualManager.getManager();
    private final VelocityManager velocityManager = VelocityManager.getManager();
    private double waveOffset = 0;

    public UFO(Point2D.Double startPosition) {
        super(startPosition);
    }

    public UFO(Point2D.Double startPosition, double waveOffset) {
        super(startPosition);
        this.waveOffset = waveOffset;
    }

    @Override
    public void move(long timeInMillis) {
        double currentXPos = getRelativePosition().x;
        Point2D.Double currentPos = new Point2D.Double(currentXPos, sinWave(currentXPos));
        setRelativePosition(currentPos);
        double nextXPos = getNextPosition().x;
        Point2D.Double nextPos = new Point2D.Double(nextXPos, sinWave(nextXPos));
        Point2D.Double velocity = new Point2D.Double(nextPos.x - currentPos.x, nextPos.y - currentPos.y);

        //TODO: Velocity should be considered inside here and not updated
        velocityManager.setVelocity(UFO.class, velocity);

        super.move(timeInMillis);
    }

    private double sinWave(double currentXPos) {
        try {
            return 0.35 * Math.sin(currentXPos * 3 + 1 + 2*Math.PI*waveOffset) + 0.5 - 0.5 * visualManager.getElementRelativeHeight(UFO.class);
        } catch (VisualNotSetException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
