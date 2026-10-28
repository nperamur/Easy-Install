package neelesh.easy_install;

import java.io.File;
import java.nio.file.Path;

public interface Platform {
    String getLoader();

    String getGameDir();

    File getGameDirAsFile();

    String getModLoaderDisplayText();

    Path getConfigDir();

}
