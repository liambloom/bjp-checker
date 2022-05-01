package dev.liambloom.checker.ui;

import java.nio.ByteBuffer;

public class IncompatibleSettingsVersionException extends Exception {
    public static IncompatibleSettingsVersionException cantDowngrade(ByteBuffer settingsVersion) {
        return new IncompatibleSettingsVersionException("Application previously opened in version " + settingsVersionToString(settingsVersion)
            + ", unable to open version " + IncompatibleSettingsVersionException.class.getPackage().getImplementationVersion());
    }

    public static IncompatibleSettingsVersionException cantUpgrade(ByteBuffer settingsVersion) {
        return new IncompatibleSettingsVersionException("Unable to upgrade settings from version " + settingsVersionToString(settingsVersion)
            + " to " + IncompatibleSettingsVersionException.class.getPackage().getImplementationVersion());
    }

    public IncompatibleSettingsVersionException(String msg) {
        super(msg);
    }

    private static String settingsVersionToString(ByteBuffer settingsVersion) {
        StringBuilder stringVersion = new StringBuilder();
        stringVersion.append(settingsVersion.getInt());
        while (settingsVersion.hasRemaining()) {
            stringVersion.append('.')
                .append(settingsVersion.getInt());
        }
        return stringVersion.toString();
    }
}
