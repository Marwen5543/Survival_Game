package io.github.SurvivalGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;

/** Global, persistent options edited from the Options screen. */
public final class Settings {
    private Settings() {
    }

    private static final String PREFS = "survival-game-settings";

    private static Color c(float r, float g, float b, float a) {
        return new Color(r, g, b, a);
    }

    /** Arena look: background tint, decoration palette and ambient particles. */
    public enum FieldTheme {
        MEADOW("Meadow", c(1f, 1f, 1f, 1f), c(0.15f, 0.35f, 0.08f, 1f), c(0.28f, 0.27f, 0.25f, 1f),
            c(0.05f, 0.08f, 0.05f, 0.5f), c(0.45f, 0.65f, 0.95f, 1f), c(1f, 0.95f, 0.6f, 1f), 0.12f),
        EMBER("Ember Wastes", c(1f, 0.55f, 0.4f, 1f), c(0.5f, 0.15f, 0.05f, 1f), c(0.2f, 0.12f, 0.1f, 1f),
            c(1f, 0.4f, 0.1f, 0.6f), c(1f, 0.5f, 0.15f, 1f), c(1f, 0.55f, 0.15f, 1f), 0.9f),
        FROST("Frozen Tundra", c(0.6f, 0.85f, 1f, 1f), c(0.7f, 0.9f, 1f, 1f), c(0.7f, 0.8f, 0.9f, 1f),
            c(0.8f, 0.95f, 1f, 0.5f), c(0.6f, 0.9f, 1f, 1f), c(1f, 1f, 1f, 1f), -0.7f),
        VOID("The Void", c(0.45f, 0.35f, 0.75f, 1f), c(0.3f, 0.15f, 0.5f, 1f), c(0.2f, 0.15f, 0.3f, 1f),
            c(0.6f, 0.2f, 1f, 0.6f), c(0.8f, 0.4f, 1f, 1f), c(0.8f, 0.5f, 1f, 1f), 0.3f);

        final String label;
        final Color bgTint, grass, rock, crack, rune, ambient;
        final float ambientVy; // >0 rises, <0 falls

        FieldTheme(String label, Color bgTint, Color grass, Color rock, Color crack, Color rune, Color ambient, float vy) {
            this.label = label;
            this.bgTint = bgTint;
            this.grass = grass;
            this.rock = rock;
            this.crack = crack;
            this.rune = rune;
            this.ambient = ambient;
            this.ambientVy = vy;
        }
    }

    public enum Difficulty {
        EASY("Easy", 0.75f, 0.7f),
        NORMAL("Normal", 1f, 1f),
        HARD("Hard", 1.4f, 1.35f),
        NIGHTMARE("Nightmare", 2f, 1.8f);

        final String label;
        final float healthMult;
        final float damageMult;

        Difficulty(String label, float healthMult, float damageMult) {
            this.label = label;
            this.healthMult = healthMult;
            this.damageMult = damageMult;
        }
    }

    public enum EnemyStyle {
        CLASSIC("Classic"), NEON("Neon"), SHADOW("Shadow"), RAINBOW("Rainbow");

        final String label;

        EnemyStyle(String label) {
            this.label = label;
        }
    }

    /** How aggressively the game keeps the player readable in late-game chaos. */
    public enum Clarity {
        OFF("Off", 0f), BALANCED("Balanced", 0.5f), MAX("Max Clarity", 1f);

        final String label;
        final float strength;

        Clarity(String label, float strength) {
            this.label = label;
            this.strength = strength;
        }
    }

    public static final int[][] RESOLUTIONS = {
        {1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}
    };

    public static float musicVolume = 0.7f;
    public static float sfxVolume = 1f;
    public static FieldTheme fieldTheme = FieldTheme.MEADOW;
    public static int resolutionIndex = 0;
    public static boolean fullscreen = false;
    public static Difficulty difficulty = Difficulty.NORMAL;
    public static float enemySpeed = 1f;
    public static float spawnRate = 1f;
    public static EnemyStyle enemyStyle = EnemyStyle.CLASSIC;
    public static Clarity clarity = Clarity.BALANCED;
    public static boolean variants = true;
    public static boolean bosses = true;
    public static boolean events = true;
    public static int highScore = 0;
    public static int bestTimeSeconds = 0;

    public static <T extends Enum<T>> T cycle(T[] values, T current, int dir) {
        return values[(current.ordinal() + dir + values.length) % values.length];
    }

    private static <T extends Enum<T>> T readEnum(Preferences p, String key, T[] values, T def) {
        int i = p.getInteger(key, def.ordinal());
        return (i >= 0 && i < values.length) ? values[i] : def;
    }

    public static void load() {
        Preferences p = Gdx.app.getPreferences(PREFS);
        musicVolume = p.getFloat("music", 0.7f);
        sfxVolume = p.getFloat("sfx", 1f);
        fieldTheme = readEnum(p, "field", FieldTheme.values(), FieldTheme.MEADOW);
        resolutionIndex = Math.max(0, Math.min(RESOLUTIONS.length - 1, p.getInteger("res", 0)));
        fullscreen = p.getBoolean("fullscreen", false);
        difficulty = readEnum(p, "difficulty", Difficulty.values(), Difficulty.NORMAL);
        enemySpeed = p.getFloat("enemySpeed", 1f);
        spawnRate = p.getFloat("spawnRate", 1f);
        enemyStyle = readEnum(p, "style", EnemyStyle.values(), EnemyStyle.CLASSIC);
        clarity = readEnum(p, "clarity", Clarity.values(), Clarity.BALANCED);
        variants = p.getBoolean("variants", true);
        bosses = p.getBoolean("bosses", true);
        events = p.getBoolean("events", true);
        highScore = p.getInteger("highScore", 0);
        bestTimeSeconds = p.getInteger("bestTime", 0);
    }

    public static void save() {
        Preferences p = Gdx.app.getPreferences(PREFS);
        p.putFloat("music", musicVolume);
        p.putFloat("sfx", sfxVolume);
        p.putInteger("field", fieldTheme.ordinal());
        p.putInteger("res", resolutionIndex);
        p.putBoolean("fullscreen", fullscreen);
        p.putInteger("difficulty", difficulty.ordinal());
        p.putFloat("enemySpeed", enemySpeed);
        p.putFloat("spawnRate", spawnRate);
        p.putInteger("style", enemyStyle.ordinal());
        p.putInteger("clarity", clarity.ordinal());
        p.putBoolean("variants", variants);
        p.putBoolean("bosses", bosses);
        p.putBoolean("events", events);
        p.putInteger("highScore", highScore);
        p.putInteger("bestTime", bestTimeSeconds);
        p.flush();
    }

    public static void applyVideo() {
        try {
            if (fullscreen) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else {
                int[] r = RESOLUTIONS[resolutionIndex];
                Gdx.graphics.setWindowedMode(r[0], r[1]);
            }
        } catch (Exception ignored) {
            // Some backends can't change mode at runtime — just keep the current one.
        }
    }

    /** @return true if this run set a new kill record. */
    public static boolean recordRun(int score, float seconds) {
        boolean record = false;
        if (score > highScore) {
            highScore = score;
            record = true;
        }
        if ((int) seconds > bestTimeSeconds) {
            bestTimeSeconds = (int) seconds;
        }
        save();
        return record;
    }
}
