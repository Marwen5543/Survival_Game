package io.github.SurvivalGame;

/**
 * Central place for every tunable number and asset path.
 * Want a different character or background? Change PLAYER_TEXTURE /
 * BACKGROUND_TEXTURE here — nothing else in the codebase needs to know.
 */
public final class GameConfig {
    private GameConfig() {
    }

    // World
    public static final float WORLD_WIDTH = 16f;
    public static final float WORLD_HEIGHT = 9f;

    // Assets
    public static final String BACKGROUND_TEXTURE = "bgd.png";
    public static final String PLAYER_TEXTURE = "player.png";
    public static final String ENEMY_TEXTURE = "slime.png";
    public static final String FONT_FILE = "CherryCreamSoda-Regular.ttf";
    public static final String MUSIC_FILE = "nightsplitter.mp3";
    public static final String SLASH_SFX = "slash.wav";

    // Pacing / difficulty
    public static final float ENEMY_SPAWN_INTERVAL_START = 1.5f;
    public static final float ENEMY_SPAWN_INTERVAL_MIN = 0.4f;
    public static final float DIFFICULTY_RAMP_PER_SECOND = 0.01f; // spawn interval shrinks over time
    public static final float DAMAGE_PER_SECOND = 1.0f;
    public static final float HEALTH_PICKUP_CHANCE = 0.12f; // 12% of enemy deaths drop healing
    public static final float HEALTH_PICKUP_AMOUNT = 1.5f;
    public static final float BASE_PICKUP_RADIUS = 1.2f;

    // Player feel
    public static final float PLAYER_INVINCIBILITY_DURATION = 0.6f;
    public static final float DASH_SPEED_MULTIPLIER = 3.2f;
    public static final float DASH_DURATION = 0.18f;
    public static final float DASH_COOLDOWN = 1.2f;

    // Sword targeting — swings toward whatever is closing in, from every side at once
    public static final float SWORD_DETECTION_RADIUS = 3f;
    public static final int SWORD_MAX_TARGETS = 3;

    // Abilities
    public static final float ABILITY_SPAWN_INTERVAL = 8f;
    public static final float SHIELD_DURATION = 4f;
    public static final float HASTE_DURATION = 5f;
    public static final float HASTE_MULTIPLIER = 1.6f;
    public static final float MAGNET_DURATION = 6f;
    public static final float MAGNET_RADIUS_BONUS = 2.6f;
    public static final float STORM_DURATION = 5f;
    public static final float STORM_STRIKE_INTERVAL = 0.5f;
    public static final float STORM_STRIKE_DAMAGE = 2f;
    public static final float STORM_STRIKE_RADIUS = 1.2f;

    // Juice
    public static final float SCREEN_SHAKE_DURATION = 0.2f;
    public static final float SCREEN_SHAKE_MAGNITUDE = 0.15f;
    public static final float PARTICLE_LIFETIME = 0.35f;
    public static final int PARTICLES_PER_DEATH = 6;

    // Field
    public static final int GROUND_DECORATION_COUNT = 90;
    public static final int RAIN_DROP_COUNT = 60;
    public static final float RAIN_FALL_SPEED = 6f;
}
