package com.emailAI.security;

import java.util.prefs.Preferences;

public class ThemePreferences {

    private static final String KEY_THEME = "theme";
    private static final String KEY_PALETTE = "palette";
    private static final String KEY_MODE = "mode";

    private final Preferences prefs = Preferences.userNodeForPackage(ThemePreferences.class);

    public void saveTheme(String theme) {
        prefs.put(KEY_THEME, theme);
    }

    public String getTheme() {
        return prefs.get(KEY_THEME, "ocean-teal");
    }

    public void savePalette(String palette) {
        prefs.put(KEY_PALETTE, palette);
    }

    public String getPalette() {
        return prefs.get(KEY_PALETTE, "ocean-teal");
    }

    public void saveMode(String mode) {
        prefs.put(KEY_MODE, mode);
    }

    public String getMode() {
        return prefs.get(KEY_MODE, "dark");
    }
}