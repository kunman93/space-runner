package ch.zhaw.it.pm3.spacerunner.technicalservices.visual.util;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Util to load visuals
 * Implemented with the singleton-pattern.
 * <p>
 * Uses the org.apache.batik libraries for svg processing.
 *
 * @author islermic
 */
public class VisualUtil {

    private final Logger logger = Logger.getLogger(VisualUtil.class.getName());

    private static final VisualUtil VISUAL_UTIL = new VisualUtil();

    /**
     * private constructor for the singleton-pattern
     */
    private VisualUtil() {
    }

    public static VisualUtil getUtil() {
        return VISUAL_UTIL;
    }

    /**
     * Loads the image from the URL provided
     *
     * @param imageURL URL of the image to load. not null
     * @return loaded image
     */
    public BufferedImage loadImage(URL imageURL) {
        if (imageURL == null) {
            throw new IllegalArgumentException("imageURL can not be null");
        }

        Image image = new ImageIcon(imageURL).getImage();
        return toBufferedImage(image);
    }

    /**
     * Generates the "infinite" background ("normal image" + "mirror image" + "normal image").
     *
     * @param inputImage   image for background. not null
     * @param scaledWidth  width used for the background (will be tripled in the output image). has to be higher than 0 (positive)
     * @param scaledHeight height used for background. has to be higher than 0 (positive)
     * @return background image with size (3*scaledWidth, scaledHeight) contains ("normal image" + "mirror image" + "normal image").
     */
    public BufferedImage generateBackground(BufferedImage inputImage, int scaledWidth, int scaledHeight) {
        if (inputImage == null) {
            throw new IllegalArgumentException("inputImage can not be null");
        } else if (scaledWidth <= 0 || scaledHeight <= 0) {
            throw new IllegalArgumentException("scaledWidth and scaledHeight have to be higher than 0");
        }

        BufferedImage outputImage = new BufferedImage(scaledWidth * 3, scaledHeight, inputImage.getType());

        // creates output image
        BufferedImage resizedImage = resizeImage(inputImage, scaledWidth, scaledHeight);
        BufferedImage mirrorImage = flipImage(inputImage, true);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resizedImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.drawImage(mirrorImage, scaledWidth, 0, scaledWidth, scaledHeight, null);
        g2d.drawImage(resizedImage, scaledWidth * 2, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return outputImage;
    }

    /**
     * This will resize the inputImage and return the resized image
     *
     * @param inputImage   image to resize. not null
     * @param scaledWidth  width for resized image. has to be higher than 0 (positive)
     * @param scaledHeight height for resized image. has to be higher than 0 (positive)
     * @return resized image
     */
    public BufferedImage resizeImage(BufferedImage inputImage, int scaledWidth, int scaledHeight) {
        if (inputImage == null) {
            throw new IllegalArgumentException("inputImage can not be null");
        } else if (scaledWidth <= 0 || scaledHeight <= 0) {
            throw new IllegalArgumentException("scaledWidth and scaledHeight have to be higher than 0");
        }

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return outputImage;
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param inputImage The Image to be converted. not null
     * @return The converted BufferedImage
     * @author Code is from stackoverflow https://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
     */
    private BufferedImage toBufferedImage(Image inputImage) {
        if (inputImage == null) {
            throw new IllegalArgumentException("inputImage can not be null");
        }

        if (inputImage instanceof BufferedImage) {
            return (BufferedImage) inputImage;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(inputImage.getWidth(null), inputImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D graphic = bimage.createGraphics();
        graphic.drawImage(inputImage, 0, 0, null);
        graphic.dispose();

        // Return the buffered image
        return bimage;
    }

    /**
     * Loads the SVG image from the URL provided
     *
     * @param imageURL URL of the image to load. not null
     * @param height   height for the image in px. higher than 0 (positive)
     * @return loaded image
     */
    public BufferedImage loadSVGImage(URL imageURL, float height) {
        if (imageURL == null) {
            throw new IllegalArgumentException("imageURL can not be null");
        } else if (height <= 0) {
            throw new IllegalArgumentException("height has to be higher than 0");
        }

        BufferedImage loadedImage = null;
        try {
            loadedImage = rasterize(new File(imageURL.getFile().replace("%20", " ")), height);
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Error Rasterizing File");
            return null;
        }
        return loadedImage;
    }


    /**
     * @author Code is from stackoverflow https://stackoverflow.com/questions/11435671/how-to-get-a-bufferedimage-from-a-svg and optimized by islermic
     */
    private BufferedImage rasterize(File svgFile, float height) throws IOException {

        final BufferedImage[] imagePointer = new BufferedImage[1];

        // Rendering hints can't be set programatically, so
        // we override defaults with a temporary stylesheet.
        // These defaults emphasize quality and precision, and
        // are more similar to the defaults of other SVG viewers.
        // SVG documents can still override these defaults.
        String css = "svg {"
                + "shape-rendering: geometricPrecision;"
                + "text-rendering:  geometricPrecision;"
                + "color-rendering: optimizeQuality;"
                + "image-rendering: optimizeQuality;"
                + "}";
        File cssFile = File.createTempFile("batik-default-override-", ".css");
        FileUtils.writeStringToFile(cssFile, css);

        TranscodingHints transcoderHints = new TranscodingHints();
        transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
        transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION,
                SVGDOMImplementation.getDOMImplementation());
        transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
                SVGConstants.SVG_NAMESPACE_URI);
        transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
        transcoderHints.put(ImageTranscoder.KEY_USER_STYLESHEET_URI, cssFile.toURI().toString());
        transcoderHints.put(ImageTranscoder.KEY_HEIGHT, height);

        try {

            TranscoderInput input = new TranscoderInput(new FileInputStream(svgFile));

            ImageTranscoder t = new ImageTranscoder() {

                @Override
                public BufferedImage createImage(int w, int h) {
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }

                @Override
                public void writeImage(BufferedImage image, TranscoderOutput out)
                        throws TranscoderException {
                    imagePointer[0] = image;
                }
            };
            t.setTranscodingHints(transcoderHints);
            t.transcode(input, null);
        } catch (TranscoderException ex) {
            logger.log(Level.SEVERE, "Couldn't convert {0}", svgFile);
            // Requires Java 6
            ex.printStackTrace();
            throw new IOException("Couldn't convert " + svgFile);
        } finally {
            cssFile.delete();
        }

        return imagePointer[0];
    }

    /**
     * Flips the image.
     *
     * @param image      image to flip. not null
     * @param horizontal should flip horizontal? if false it is flipped vertically
     * @return image flipped in the correct direction
     */
    public BufferedImage flipImage(BufferedImage image, boolean horizontal) {
        if (image == null) {
            throw new IllegalArgumentException("image can not be null");
        }

        // Flip the image horizontally
        AffineTransform tx;
        if (horizontal) {
            tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-image.getWidth(null), 0);
        } else {
            tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -image.getHeight(null));

        }
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        image = op.filter(image, null);
        return image;
    }

    /**
     * Rotate an image by the specified degree.
     *
     * @param bufferedImage image to rotate
     * @param deg           degrees for rotation
     * @return rotated image
     */
    public BufferedImage rotateImage(BufferedImage bufferedImage, int deg) {
        if (bufferedImage == null) {
            throw new IllegalArgumentException("bufferedImage can not be null");
        }

        BufferedImage image = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getType());
        AffineTransform trans = AffineTransform.getRotateInstance(deg, bufferedImage.getWidth() / 2, bufferedImage.getHeight() / 2);
        AffineTransformOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_BILINEAR);
        op.filter(bufferedImage, image);
        return image;
    }
}
