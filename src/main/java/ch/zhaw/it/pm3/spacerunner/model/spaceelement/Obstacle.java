package ch.zhaw.it.pm3.spacerunner.model.spaceelement;

import java.awt.*;

public abstract class Obstacle extends SpaceElement {

    public Obstacle(Point startPosition, int width, int length, Image visual) throws Exception {
        super(startPosition, width, length, visual);
    }
}
