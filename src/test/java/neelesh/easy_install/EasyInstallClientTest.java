package neelesh.easy_install;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class EasyInstallClientTest {
    //private static final String GAME_VERSION = SharedConstants.getGameVersion().getName();

    /*@BeforeAll
    public static void setup() {
        SharedConstants.getGameVersion().getName();
    }*/


    @BeforeAll
    public static void setup() {

    }

    @Test
    public void test1() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("test.txt"); // file in src/test/resources/
        if (resource != null) {
            String hash = EasyInstallClient.createFileHash(Paths.get(resource.toURI()));
            System.out.println(hash);
            assertEquals(hash, "da39a3ee5e6b4b0d3255bfef95601890afd80709");
        } else {
            System.out.println("File not found.");
        }
    }

    @Test
    public void test2() {
        assertNotEquals(5, 2*2);
    }

    @Test
    public void test3() {
        assertTrue(10/2==5);
    }
}