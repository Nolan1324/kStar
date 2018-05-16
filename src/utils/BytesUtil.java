package utils;

import structure.Settings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BytesUtil {
    /*
    public static byte[] SettingsToBytes(Settings o) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Field[] fields = o.getClass().getFields();
        for(Field field : fields) {
            try {
                if (!field.isAnnotationPresent(Settings.Ignore.class)) {
                    if (field.getType() == boolean.class) {
                        buffer.write(field.getBoolean(o) ? 1 : 0);
                    } else if (field.getType() == Settings.Theme.class) {
                        buffer.write(((Settings.Theme) field.get(o)).code);
                    } else if (field.getType() == String.class) {
                        String value = (String) field.get(o);
                        buffer.write(value.length());
                        try {
                            buffer.write(value.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IllegalAccessException e) {

            }
        }

        return buffer.toByteArray();
    }

    public static Settings BytesToSettings(byte[] bytes) {
        Settings output = new Settings();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        Field[] fields = Settings.class.getDeclaredFields();
        for(Field field : fields) {
            try {
                if (!field.isAnnotationPresent(Settings.Ignore.class)) {
                    if (field.getType() == boolean.class) {
                        field.set(output, buffer.get() != 0);
                    } else if (field.getType() == Settings.Theme.class) {
                        field.set(output, Settings.Theme.getTheme(buffer.get()));
                    } else if (field.getType() == String.class) {
                        int length = buffer.get();
                        if (length != 0) {
                            byte[] strBuffer = new byte[length];
                            buffer.get(strBuffer, 0, length);
                            field.set(output, new String(strBuffer));
                        } else {
                            field.set(output, "");
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return output;
    }
    */
}
