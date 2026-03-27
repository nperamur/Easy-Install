package neelesh.easy_install;

import java.io.File;

public interface Platform {
    String getLoader();

    String getGameDir();

    File getGameDirAsFile();

    String getModLoaderDisplayText();

}
