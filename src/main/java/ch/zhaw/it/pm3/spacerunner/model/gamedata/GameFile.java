package ch.zhaw.it.pm3.spacerunner.model.gamedata;

public enum GameFile {
    PROFILE("profile.json"),
    SHOP_CONTENT("shop_content.json");

    


    private String file;
    private GameFile(String file){
        this.file = file;
    }
}