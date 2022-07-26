package ch.zhaw.it.pm3.spacerunner.technicalservices.visual.util;

import ch.zhaw.it.pm3.spacerunner.FileResource;

/**
 * Path variables of all image files which are not SCG files.
 *
 * @author blattpet
 */
public enum VisualFile implements FileResource {

    BACKGROUND_STARS("background.jpg"),
    ROCKET_ICON("icon.png");

    private final String fileName;

    VisualFile(String fileName) {
        this.fileName = "images/" + fileName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }
}
