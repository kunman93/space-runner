package ch.zhaw.it.pm3.spacerunner.technicalservices.persistence.util;

import ch.zhaw.it.pm3.spacerunner.domain.ContentId;
import ch.zhaw.it.pm3.spacerunner.domain.PlayerProfile;
import ch.zhaw.it.pm3.spacerunner.domain.ShopContent;
import ch.zhaw.it.pm3.spacerunner.technicalservices.persistence.Persistence;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility tool to persist data (load / save) with Gson-library
 * Implemented with the singleton-pattern
 *
 * @author islermic, kunnuman
 */
public class JsonPersistenceUtil implements Persistence {

    private final Logger logger = Logger.getLogger(JsonPersistenceUtil.class.getName());

    private static final JsonPersistenceUtil JSON_PERSISTENCE_UTIL = new JsonPersistenceUtil();

    private static final Gson GSON = new Gson();

    /**
     * private constructor for the singleton-pattern
     */
    private JsonPersistenceUtil() {
    }

    public static JsonPersistenceUtil getUtil() {
        return JSON_PERSISTENCE_UTIL;
    }

    /**
     * Checks if the user has activated the double duration upgrade for the coin power up.
     *
     * @return if it is activated
     */
    @Override
    public boolean hasDoubleDurationForCoinPowerUp() {
        PlayerProfile profile = loadProfile();
        return profile.getActiveContentIds().stream().anyMatch((activeContent) -> activeContent.equals(ContentId.DOUBLE_DURATION_COIN_UPGRADE));
    }

    /**
     * Checks if the user has activated the power up chance multiplier upgrade.
     *
     * @return if it is activated
     */
    @Override
    public boolean hasPowerUpChanceMultiplierUpgrade() {
        PlayerProfile profile = loadProfile();
        return profile.getActiveContentIds().stream().anyMatch((activeContent) -> activeContent.equals(ContentId.POWER_UP_CHANCE_MULTIPLIER));
    }

    /**
     * Deactivated the content with the specified contentId in the profile of the user.
     *
     * @param contentId id to be deactivated
     */
    @Override
    public void deactivateContent(ContentId contentId) {
        PlayerProfile profile = loadProfile();
        profile.deactivateContent(contentId);
        saveProfile(profile);
    }

    /**
     * Activated the content with the specified contentId in the profile of the user.
     *
     * @param contentId id to be activated
     */
    @Override
    public void activateContent(ContentId contentId) {
        PlayerProfile profile = loadProfile();

        boolean ownsContent = profile.getPurchasedContentIds().contains(contentId);

        if (!ownsContent) {
            throw new IllegalArgumentException("The player does not own the content that should be activated");
        }

        profile.activateContent(contentId);
        saveProfile(profile);
    }

    /**
     * returns the amount of coins needed to buy the content with the given price. If the player has enough coins  {@literal =>} 0 is returned
     *
     * @param price price of the content. Has to be higher or equal zero
     * @return coins needed to be able to buy the content
     */
    @Override
    public int getAmountOfCoinsNeededToBuyContent(int price) {
        if (price < 0) {
            throw new IllegalArgumentException("price to get amount of coins needed to buy has to be 0 or greater");
        }

        PlayerProfile profile = loadProfile();
        if (profile.getCoins() >= price) {
            return 0;
        }
        return price - profile.getCoins();
    }

    /**
     * Checks if the player has enough coins to buy the content with the given price.
     *
     * @param price price of the content. Has to be higher or equal zero
     * @return user has enough coins
     */
    @Override
    public boolean playerHasEnoughCoinsToBuy(int price) {
        if (price < 0) {
            throw new IllegalArgumentException("price has to be 0 or greater");
        }

        PlayerProfile profile = loadProfile();
        return profile.getCoins() >= price;
    }

    /**
     * Buys the content with the specified content if. {@literal =>} Subtracts coins from profile and adds the content to the profile.
     *
     * @param contentId content to add to profile. Not Null
     * @param price     price of content. Has to be higher or equal zero
     */
    @Override
    public void buyContent(ContentId contentId, int price) {
        if (contentId == null) {
            throw new IllegalArgumentException("ContentId can not be null");
        } else if (price < 0) {
            throw new IllegalArgumentException("price has to be 0 or greater");
        } else if (!playerHasEnoughCoinsToBuy(price)) {
            throw new IllegalArgumentException("player does not have enough coins to buy the content: " + contentId.name());
        }

        PlayerProfile profile = loadProfile();
        profile.subtractCoins(price);
        profile.addContent(contentId);
        saveProfile(profile);
    }

    /**
     * Checks if the specified content is active.
     *
     * @param contentId content to check for
     * @return is active
     */
    @Override
    public boolean isContentActive(ContentId contentId) {
        PlayerProfile profile = loadProfile();
        return profile.getActiveContentIds().contains(contentId);
    }

    /**
     * Checks if the specified content is already purchased.
     *
     * @param contentId content to check
     * @return is purchased
     */
    @Override
    public boolean isContentPurchased(ContentId contentId) {
        PlayerProfile profile = loadProfile();
        return profile.getPurchasedContentIds().contains(contentId);
    }

    /**
     * Set the specified sound volume in the profile.
     */
    @Override
    public void setSoundVolume(int soundVolume) {
        PlayerProfile profile = loadProfile();
        profile.setVolume(soundVolume);
        saveProfile(profile);
    }

    ;

    /**
     * Get the specified sound volume from the profile.
     *
     * @return sound volume
     */
    @Override
    public int getSoundVolume() {
        return loadProfile().getVolume();
    }

    /**
     * Returns if the audio is enabled.
     *
     * @return is audio enabled
     */
    @Override
    public boolean isAudioEnabled() {
        return loadProfile().isAudioEnabled();
    }

    /**
     * Load the profile of the player from the disk where it is saved in json.
     * If there is no profile.json found it will return a new default profile!
     *
     * @return the player's profile (or a default profile if it doesn't exist)
     */
    @Override
    public PlayerProfile loadProfile() {
        Path path = Path.of(GameFile.PROFILE.getFileName());
        PlayerProfile playerProfile = null;


        if (path != null && Files.exists(path)) {
            try {
                playerProfile = loadAndDeserializeData(GameFile.PROFILE.getFileName(), PlayerProfile.class);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to Load and / or Deserialize Data");
                playerProfile = new PlayerProfile();
            }
        } else {
            playerProfile = new PlayerProfile();
        }

        playerProfile.setActiveShopContent(loadActiveContent(playerProfile.getActiveContentIds()));

        return playerProfile;
    }

    /**
     * load and deserialize data from path to a data object of class T
     *
     * @param path      path to load data from
     * @param dataClass Class of data object
     * @param <T>       Type of the data object
     * @return loaded data as object of class T
     * @throws IOException if there is an error loading the file
     */
    public <T> T loadAndDeserializeData(String path, Type dataClass) throws IOException {
        T data = null;
        try (FileReader reader = new FileReader(path)) {
            data = GSON.fromJson(reader, dataClass);
        } catch (IOException e) {
            throw e;
        }
        return data;
    }

    /**
     * Save the player profile in json-format to the disk
     *
     * @param playerProfile player profile to save
     */
    @Override
    public void saveProfile(PlayerProfile playerProfile) {
        if (playerProfile == null) {
            throw new IllegalArgumentException("null is not a legal argument for a player profile!");
        }

        try {
            serializeAndSaveData(GameFile.PROFILE.getFileName(), playerProfile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to Serialize and / or load Data");
        }
    }


    /**
     * Serialize the data object of type T and save it as JSON to the path
     *
     * @param path path for the file
     * @param data data to serialize and save
     * @param <T>  type of the data to be saved
     * @throws IOException if there is an error loading the file
     */
    public <T> void serializeAndSaveData(String path, T data) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            throw e;
        }
    }

    private Set<ShopContent> loadActiveContent(Set<ContentId> activeContentIds) {
        List<ShopContent> shopContentList = loadShopContent();
        return shopContentList.stream().filter((shopContent) -> {
            return activeContentIds.stream().anyMatch((purchasedContentId) -> {
                return purchasedContentId == shopContent.getContentId();
            });
        }).collect(Collectors.toSet());
    }

    private Set<ShopContent> loadPurchasedContent(Set<ContentId> purchasedContentIds) {
        List<ShopContent> shopContentList = loadShopContent();
        return shopContentList.stream().filter((shopContent) -> {
            return purchasedContentIds.stream().anyMatch((purchasedContentId) -> {
                return purchasedContentId == shopContent.getContentId();
            });
        }).collect(Collectors.toSet());
    }

    /**
     * Loads the shop content list.
     *
     * @return shop content list
     */
    @Override
    public List<ShopContent> loadShopContent() {
        Type listOfShopContentType = new TypeToken<ArrayList<ShopContent>>() {
        }.getType();

        List<ShopContent> shopContentList = null;
        try {
            shopContentList = loadAndDeserializeData(GameFile.SHOP_CONTENT.getFileName(), listOfShopContentType);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error with Loading and / or Deserializing Data");
        }
        return shopContentList;
    }
}
