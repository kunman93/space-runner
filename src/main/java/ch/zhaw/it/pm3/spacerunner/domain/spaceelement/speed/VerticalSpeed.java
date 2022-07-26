package ch.zhaw.it.pm3.spacerunner.domain.spaceelement.speed;

/**
 * Vertical speed of elements per second relative to the view.
 *
 * @author blattpet
 */
public enum VerticalSpeed {
    ZERO(0),
    ASTEROID(0.3),
    SPACE_SHIP(1),
    UFO(3);

    private double speed;

    VerticalSpeed(double speed) {
        this.speed = speed;
    }

    public double getSpeed() {
        return speed;
    }
}
