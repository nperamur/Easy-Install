package neelesh.easy_install;

import org.jspecify.annotations.NonNull;

public enum Environment {
    CLIENT_SIDE("client_side"),
    SERVER_SIDE("server_side"),
    CLIENT_AND_SERVER("client_and_server");

    String type;

    Environment(String type) {
        this.type = type;
    }


    public String getType() {
        return type;
    }
}
