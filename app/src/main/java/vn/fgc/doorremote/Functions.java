package vn.fgc.doorremote;

public class Functions {
    private static final Functions ourInstance = new Functions();

    private Functions() {
    }

    static Functions getInstance() {
        return ourInstance;
    }

    private String encode(String input) {
        //TODO implement encode stuff
        return input + '\n';
    }

    private String encode(Integer input) {
        //TODO implement encode stuff
        return input.toString() + '\n';
    }

    public String buildCommand(Constants.DOOR_COMMANDS command) {
        if (command == Constants.DOOR_COMMANDS.UP) {
            return encode("du");
        } else if (command == Constants.DOOR_COMMANDS.DOWN) {
            return encode("dd");
        } else if (command == Constants.DOOR_COMMANDS.STOP) {
            return encode("ds");
        } else if (command == Constants.DOOR_COMMANDS.LOCK) {
            return encode("dl");
        }
        return "";
    }
}
