package ch.zhaw.it.pm3.spacerunner.domain;

import ch.zhaw.it.pm3.spacerunner.domain.preset.Preset;
import ch.zhaw.it.pm3.spacerunner.domain.preset.RandomPresetGenerator;
import ch.zhaw.it.pm3.spacerunner.domain.spaceelement.*;
import ch.zhaw.it.pm3.spacerunner.domain.spaceelement.powerup.ActivatedPowerUpManager;
import ch.zhaw.it.pm3.spacerunner.domain.spaceelement.powerup.PowerUp;
import ch.zhaw.it.pm3.spacerunner.domain.spaceelement.velocity.VelocityManager;
import ch.zhaw.it.pm3.spacerunner.technicalservices.persistence.Persistence;
import ch.zhaw.it.pm3.spacerunner.technicalservices.persistence.util.JsonPersistenceUtil;
import ch.zhaw.it.pm3.spacerunner.technicalservices.sound.util.GameSound;
import ch.zhaw.it.pm3.spacerunner.technicalservices.sound.util.GameSoundUtil;
import ch.zhaw.it.pm3.spacerunner.technicalservices.sound.util.SoundClip;
import ch.zhaw.it.pm3.spacerunner.technicalservices.visual.manager.VisualManager;
import ch.zhaw.it.pm3.spacerunner.technicalservices.visual.manager.VisualNotSetException;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The GameController is responsible for the game logic of the Space-Runner application.
 *
 * @author islermic, hirsceva, blattpet, nachbric, freymar1, kunnuman
 */
public class GameController {

    private final Logger logger = Logger.getLogger(GameController.class.getName());

    private final Persistence persistenceUtil = JsonPersistenceUtil.getUtil();
    private final GameSoundUtil gameSoundUtil = GameSoundUtil.getUtil();
    private final VisualManager visualManager = VisualManager.getManager();
    private final VelocityManager velocityManager = VelocityManager.getManager();
    private final ActivatedPowerUpManager activatedPowerUpManager = new ActivatedPowerUpManager();


    private static final long GAME_SPEED_INCREASE_PERIOD_TIME = 1000L;
    private static final double RELATIVE_GAME_SPEED_INCREASE_PER_SECOND = 0.005;


    private double remainingDistanceUntilNextPreset = 0.1;
    private static final double BUFFER_DISTANCE_BETWEEN_PRESETS = 0.45;

    private static final int GENERAL_POWER_UP_COOLDOWN = 5000;


    private boolean isPaused = false;
    private int collectedCoins = 0;
    private int score = 0;
    private int fps = 60;
    private boolean gameOver = false;

    private boolean isInitialized = false;
    private boolean isTerminated = false;

    private Timer gameTimer;
    private SpaceWorld background = null;
    private SpaceShip spaceShip;

    private final Set<SpaceElement> elements = ConcurrentHashMap.newKeySet();
    private PlayerProfile playerProfile;
    private RandomPresetGenerator elementPreset;


    private long lastUpdate = 0;


    /**
     * Initializes the class variables.
     */
    public void initialize() {
        velocityManager.setupGameElementVelocity();
        visualManager.loadGameElementVisuals();

        gameTimer = new Timer("GameBackgroundTimer");
        gameTimer.scheduleAtFixedRate(getGameBackgroundTask(), 0, GAME_SPEED_INCREASE_PERIOD_TIME);
        gameTimer.schedule(getPowerUpGeneratorTask(), 0, GENERAL_POWER_UP_COOLDOWN);

        playerProfile = persistenceUtil.loadProfile();

        elementPreset = new RandomPresetGenerator();

        background = new SpaceWorld(new Point2D.Double(0, 0));
        spaceShip = new SpaceShip(new Point2D.Double(.05, 0.45));

        fps = playerProfile.getFps();

        isTerminated = false;
        isInitialized = true;
    }

    public void terminate() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        isTerminated = true;
    }

    private TimerTask getGameBackgroundTask() {
        return new TimerTask() {
            public void run() {
                if (!isPaused) {
                    updateElementsSpeed();
                }

                removePastDrawables();
            }
        };
    }

    private TimerTask getPowerUpGeneratorTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if (!isPaused) {
                    PowerUp powerUp = activatedPowerUpManager.generatePowerUps();
                    if (powerUp != null) {
                        elements.add(powerUp);
                    }
                }
            }
        };
    }

    private void updateElementsSpeed() {
        velocityManager.accelerateAll(new Point2D.Double(-RELATIVE_GAME_SPEED_INCREASE_PER_SECOND, RELATIVE_GAME_SPEED_INCREASE_PER_SECOND));
    }

    /**
     * Removes drawable SpaceElements that have moved past the left side of the screen, so that their no longer visible on the UI.
     */
    private void removePastDrawables() {
        AtomicInteger count = new AtomicInteger();
        elements.removeIf((SpaceElement element) -> {
            try {
                boolean outOfScreen = element.getRelativePosition().x + visualManager.getElementRelativeWidth(element.getClass()) < 0;
                if (outOfScreen) {
                    count.getAndIncrement();
                }
                return outOfScreen;
            } catch (VisualNotSetException e) {
                logger.log(Level.SEVERE, "Visual of {0} wasn't set", element.getClass());
                return true;
            }
        });
        logger.log(Level.INFO, "removed " + count + " past drawables");

    }

    /**
     * Process each frame.
     * IMPORTANT: Initialize has to be called before processing frames!
     * IMPORTANT: If the game is terminated, initialize has to be called again to process frames or it will not process any more frames.
     *
     * @param upPressed   Is true when the Up-Key was pressed, else false.
     * @param downPressed Is true when the Down-Key was pressed, else false.
     */
    public void processFrame(boolean upPressed, boolean downPressed) {
        if (isTerminated) {
            throw new IllegalStateException("The game was already terminated when process frame was called! Initialize it again for re-use!");
        } else if (!isInitialized) {
            throw new IllegalStateException("The game is not initialized! Method initialized has to be called before processing frames!");
        }

        long timeSinceLastUpdate = millisSinceLastProcessing();

        if (!isPaused) {
            moveSpaceShip(upPressed, downPressed, timeSinceLastUpdate);
            updateHighScore(timeSinceLastUpdate);
            processCollision(detectCollision());
            generatePreset();
            moveElements(timeSinceLastUpdate);
        }

        lastUpdate = System.currentTimeMillis();
    }

    private long millisSinceLastProcessing() {
        if (lastUpdate == 0) {
            return 0;
        } else {
            return System.currentTimeMillis() - lastUpdate;
        }
    }

    /**
     * Checks if movement keys are pressed & moves the spaceship accordingly.
     */
    private void moveSpaceShip(boolean upPressed, boolean downPressed, long timeInMillis) {
        if (upPressed && !downPressed) {
            spaceShip.moveSpaceShip(SpaceShipDirection.UP, timeInMillis);
        } else if (downPressed && !upPressed) {
            spaceShip.moveSpaceShip(SpaceShipDirection.DOWN, timeInMillis);
        }
    }

    private void updateHighScore(long timeSinceLastUpdate) {
        score = score + (int) (timeSinceLastUpdate / 10);
    }

    /**
     * Checks if Spaceship has collided with any other SpaceElement and performs the corresponding actions
     *
     * @return Returns the SpaceElement-Object with which the Spaceship collided.
     */
    private SpaceElement detectCollision() {
        for (SpaceElement spaceElement : elements) {
            if (spaceShip.doesCollide(spaceElement)) {
                return spaceElement;
            }
        }
        return null;
    }

    /**
     * executes effects depending on type of spaceElement
     *
     * @param spaceElement The SpaceElements for example UFO, Asteroid, COIN, etc.
     */
    private void processCollision(SpaceElement spaceElement) {
        if (spaceElement == null) {
            return;
        }

        if (spaceElement instanceof Obstacle) {
            collisionWithObstacle((Obstacle) spaceElement);
        } else if (spaceElement instanceof Coin) {
            collisionWithCoin((Coin) spaceElement);
        } else if (spaceElement instanceof PowerUp) {
            collisionWithPowerUp((PowerUp) spaceElement);
        } else {
            logger.log(Level.INFO, "Collision with unknown spaceElement");
        }
    }

    /**
     * Executes the logic when the spaceship collided with an obstacle.
     *
     * @param o Obstacle with which the spaceship collided.
     */
    private void collisionWithObstacle(Obstacle o) {
        if (activatedPowerUpManager.hasShield()) {
            elements.remove(o);
            activatedPowerUpManager.removeShield();
        } else {
            endRun();
        }
    }

    /**
     * Ends the game if the spaceship collided with any obstacle and saves the game.
     */
    private void endRun() {
        gameTimer.cancel();
        gameOver = true;
        if (playerProfile.isAudioEnabled()) {
            new Thread(() -> {
                try {
                    SoundClip explosion = gameSoundUtil.loadClip(GameSound.EXPLOSION);

                    //Play next sound after this one has stopped playing!
                    explosion.addListener(() -> {
                        try {
                            SoundClip gameOverVoice = gameSoundUtil.loadClip(GameSound.GAME_OVER_VOICE);

                            //Play next sound after this one has stopped playing!
                            gameOverVoice.addListener(() -> {
                                try {
                                    gameSoundUtil.loadClip(GameSound.GAME_OVER_2).play();
                                } catch (Exception e) {
                                    //IGNORE ON PURPOSE
                                    logger.log(Level.WARNING, "Sound GAME_OVER_2 couldn't be loaded");
                                }
                            });

                            gameOverVoice.play();
                            gameSoundUtil.loadClip(GameSound.GAME_OVER_1).play();
                        } catch (Exception e) {
                            //IGNORE ON PURPOSE
                            logger.log(Level.WARNING, "Sound GAME_OVER_1 couldn't be loaded");
                        }
                    });
                    explosion.play();

                } catch (Exception e) {
                    //IGNORE ON PURPOSE
                    logger.log(Level.WARNING, "Sound EXPLOSION couldn't be loaded");
                }
            }).start();
        }

        try {
            Thread.sleep(500);
        } catch (Exception e) {
            //IGNORE ON PURPOSE
            logger.log(Level.WARNING, "Thread wasn't able to sleep");
        }
        saveGame();
    }

    private void saveGame() {
        updatePlayerProfile();
        persistenceUtil.saveProfile(playerProfile);
    }

    /**
     * Updates the playerProfile with collected coins and the new high score.
     */
    private void updatePlayerProfile() {
        playerProfile.addCoins(collectedCoins);
        if (score > playerProfile.getHighScore()) {
            playerProfile.setHighScore(score);
        }
    }

    /**
     * Executes the logic when the spaceship collides with Coin-Object.
     *
     * @param c A Coin-Object which the spaceship collects.
     */
    private void collisionWithCoin(Coin c) {
        collectedCoins += 1 * Math.pow(2, activatedPowerUpManager.getCoinMultiplier());
        score += 25 * Math.pow(2, activatedPowerUpManager.getCoinMultiplier());
        elements.remove(c);
        if (playerProfile.isAudioEnabled()) {
            new Thread(() -> {
                try {
                    gameSoundUtil.loadClip(GameSound.COIN_PICKUP).play();
                } catch (Exception e) {
                    //IGNORE ON PURPOSE
                    logger.log(Level.WARNING, "Sound COIN_PICKUP couldn't be loaded");
                }
            }).start();
        }
    }

    private void collisionWithPowerUp(PowerUp p) {
        activatedPowerUpManager.activatePowerUp(p);
        elements.remove(p);
        score += 50;
        if (playerProfile.isAudioEnabled()) {
            new Thread(() -> {
                try {
                    gameSoundUtil.loadClip(GameSound.POWER_UP_PICKUP).play();
                } catch (Exception e) {
                    //IGNORE ON PURPOSE
                    logger.log(Level.WARNING, "Sound POWER_UP_PICKUP couldn't be loaded");
                }
            }).start();
        }
    }

    /**
     * Generates SpaceElements offscreen, which are meant to move left towards the spaceship
     */
    private void generatePreset() {
        if (remainingDistanceUntilNextPreset < -BUFFER_DISTANCE_BETWEEN_PRESETS) {
            Preset generatedPreset = elementPreset.getRandomPreset();
            Collections.addAll(elements, generatedPreset.getElementsInPreset());

            remainingDistanceUntilNextPreset = generatedPreset.getPresetTimeUntilOnScreen();
        }
    }

    /**
     * Moves all SpaceElements
     */
    private void moveElements(long timeSinceLastUpdate) {
        for (SpaceElement element : elements) {
            element.move(timeSinceLastUpdate);
        }
        background.move(timeSinceLastUpdate);
        remainingDistanceUntilNextPreset -= timeSinceLastUpdate / 1000.0;
    }

    public ArrayList<SpaceElement> getGameElements() {
        ArrayList<SpaceElement> dataToDisplay = new ArrayList<SpaceElement>(elements);
        dataToDisplay.add(0, background);
        dataToDisplay.add(spaceShip);
        return dataToDisplay;
    }

    public int getCollectedCoins() {
        return collectedCoins;
    }

    public int getScore() {
        return score;
    }

    public int getFps() {
        return fps;
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Continues or stops game logic according to clicking pause/resume button
     */
    public void togglePause() {
        isPaused = !isPaused;
    }

    /**
     * Checks if the game is over.
     *
     * @return true if the game is over, else false.
     */
    public boolean isGameOver() {
        return gameOver;
    }


    public void setViewport(int width, int height) {
        this.visualManager.setViewport(width, height);
    }


    public Map<Class<? extends PowerUp>, PowerUp> getActivePowerUps() {
        return Collections.unmodifiableMap(activatedPowerUpManager.getActivePowerUps());
    }

    protected SpaceShip getSpaceShip() {
        return spaceShip;
    }
}
