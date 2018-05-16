package structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Settings {
    public Settings() {

    }

    public Theme theme = Theme.DEFAULT;
    public boolean highlightChanges = true;
    public String pin = "";
    public String password = "";
    public String sessionId = "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Ignore {

    }

    public enum Theme {
        //Theme defs
        DEFAULT(0, "default_theme"),
        DARK(1, "dark_theme");

        public byte code;
        public String file;

        Theme(int code, String file) {
            this.code = (byte) code;
            this.file = file;
        }

        public static Theme getTheme(int code) {
            for (Theme t : Theme.values()) {
                if (t.code == code) return t;
            }
            throw new IllegalArgumentException("No theme with code " + code + " found.");
        }
    }
}
