package vn.fgc.doorremote;

public class Constants {
    public enum DOOR_COMMANDS {
        UP,
        DOWN,
        STOP,
        LOCK
    }

    public interface ACTION {
        String MAIN_ACTION = "main_action";
        String STARTFOREGROUND_ACTION = "start_foreground";
        String STOPFOREGROUND_ACTION = "stop_foreground";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 111;
    }

    public interface BARCODE {
        String NAME = "BAR_NAME";
        String ADDRESS = "BAR_ADDRESS";
    }
}
