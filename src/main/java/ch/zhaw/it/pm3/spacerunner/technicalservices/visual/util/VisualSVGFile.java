package ch.zhaw.it.pm3.spacerunner.technicalservices.visual.util;

import ch.zhaw.it.pm3.spacerunner.FileResource;

/**
 * Path variables of all images which are SVG files.
 * @author blattpet
 */
public enum VisualSVGFile implements FileResource {
    SHINEY_COIN_1("coin/shiny-coin1.svg"),
    SHINEY_COIN_2("coin/shiny-coin2.svg"),
    SHINEY_COIN_3("coin/shiny-coin3.svg"),
    SHINEY_COIN_4("coin/shiny-coin4.svg"),
    SHINEY_COIN_5("coin/shiny-coin5.svg"),
    SHINEY_COIN_6("coin/shiny-coin6.svg"),
    SPACE_SHIP_1("space-ship.svg"),
    UFO_1("UFO.svg"),
    ASTEROID("comet-asteroid.svg"),
    POWER_UP_SPAWN_RATE_UPGRADE("1.svg"),
    DOUBLE_DURATION_COIN_UPGRADE("2.svg"),
    SPACE_SHIP_SKIN_1("3.svg"),
    SPACE_SHIP_SKIN_2("4.svg"),
    LOADING_SPINNER("loading-spinner.svg"),
    DOUBLE_COIN_POWER_UP("powerup/double-coin-powerup.svg"),
    SHIELD_POWER_UP("powerup/shield-powerup.svg"),
    ROCKET_1("rocket.svg");

    private String fileName;

    VisualSVGFile(String fileName) {
        this.fileName = "images/" + fileName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }
}
