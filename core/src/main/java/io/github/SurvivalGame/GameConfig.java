package io.github.SurvivalGame;

/**
 * Central place for every tunable number and asset path.
 * Want a different character or background? Change PLAYER_TEXTURE /
 * BACKGROUND_TEXTURE here — nothing else in the codebase needs to know.
 *
 * ENEMY / BOSS ART: every enemy and boss gets a procedurally painted sprite (see SpriteFactory).
 * To use your own art instead, drop PNGs into assets named  enemy_slime.png, enemy_runner.png, ...
 * and  boss_juggernaut.png, boss_warlock.png, boss_hive_queen.png, boss_storm_wraith.png
 * (lower-case enum names). They are picked up automatically.
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
    public static final String ENEMY_TEXTURE = "slime.png"; // only used now to measure the base enemy size
    public static final String FONT_FILE = "CherryCreamSoda-Regular.ttf";
    public static final String MUSIC_FILE = "nightsplitter.mp3";
    public static final String SLASH_SFX = "slash.wav";

    // Ability icons — optional PNGs, otherwise generated in code.
    public static final String SHIELD_ICON_TEXTURE = "shield_icon.png";
    public static final String HASTE_ICON_TEXTURE = "haste_icon.png";
    public static final String MAGNET_ICON_TEXTURE = "magnet_icon.png";
    public static final String STORM_ICON_TEXTURE = "storm_icon.png";
    public static final String FREEZE_ICON_TEXTURE = "freeze_icon.png";
    public static final String NUKE_ICON_TEXTURE = "nuke_icon.png";
    public static final String OVERDRIVE_ICON_TEXTURE = "overdrive_icon.png";

    // Tintable burst animation played on level-up.
    public static final String BUFF_BURST_TEXTURE = "buff_burst.png";
    public static final int BUFF_BURST_FRAME_COLS = 8;

    // Pacing / difficulty (tougher than v2)
    public static final float ENEMY_SPAWN_INTERVAL_START = 1.2f;
    public static final float ENEMY_SPAWN_INTERVAL_MIN = 0.28f;
    public static final float DIFFICULTY_RAMP_PER_SECOND = 0.012f;
    public static final float DAMAGE_PER_SECOND = 1.0f;
    public static final float HEALTH_PICKUP_CHANCE = 0.12f;
    public static final float HEALTH_PICKUP_AMOUNT = 1.5f;
    public static final float BASE_PICKUP_RADIUS = 1.2f;
    public static final int MAX_ENEMIES = 200;

    // Enemy waves (packs that rush in from one edge)
    public static final float WAVE_FIRST_AT = 20f;
    public static final float WAVE_INTERVAL = 30f;

    // Player feel
    public static final float PLAYER_INVINCIBILITY_DURATION = 0.6f;
    public static final float DASH_SPEED_MULTIPLIER = 3.2f;
    public static final float DASH_DURATION = 0.18f;
    public static final float DASH_COOLDOWN = 1.2f;
    public static final float PLAYER_SLOW_FACTOR = 0.55f; // while standing in acid / ice

    // Sword targeting
    public static final float SWORD_DETECTION_RADIUS = 3f;
    public static final int SWORD_MAX_TARGETS = 3;

    // Abilities
    public static final float ABILITY_SPAWN_INTERVAL = 8f;
    public static final int MAX_ABILITY_STACK = 5;
    public static final float ABILITY_STACK_DURATION_CAP_MULTIPLIER = 3f;

    public static final float SHIELD_DURATION = 4f;
    public static final float HASTE_DURATION = 5f;
    public static final float HASTE_MULTIPLIER = 1.6f;
    public static final float MAGNET_DURATION = 6f;
    public static final float MAGNET_RADIUS_BONUS = 2.6f;
    public static final float STORM_DURATION = 5f;
    public static final float STORM_STRIKE_INTERVAL = 0.5f;
    public static final float STORM_STRIKE_DAMAGE = 2f;
    public static final float STORM_STRIKE_RADIUS = 1.2f;

    public static final float FREEZE_DURATION = 4f;
    public static final float FREEZE_TIME_SCALE = 0.12f;
    public static final float FREEZE_SHATTER_MULT = 1.5f;  // frozen enemies take +50% damage
    public static final float OVERDRIVE_DURATION = 6f;
    public static final float NUKE_DAMAGE = 7f;
    public static final float NUKE_BOSS_DAMAGE = 22f;
    public static final float NUKE_CHARGE_TIME = 0.45f;
    public static final float NUKE_VISUAL_RADIUS = 16f;

    // Bosses
    public static final float BOSS_FIRST_AT = 55f;
    public static final float BOSS_INTERVAL = 75f;
    public static final float BOSS_BASE_HEALTH = 110f;
    public static final float BOSS_HEALTH_PER_TIER = 0.55f;
    public static final float BOSS_INTRO_TIME = 1.6f;
    public static final int BOSS_GEM_COUNT = 18;
    public static final int BOSS_GEM_VALUE = 6;
    public static final float BOSS_CONTACT_DAMAGE = 0.9f;
    public static final float PROJECTILE_DAMAGE = 0.6f;

    // Enemy specials
    public static final float BOMBER_BLAST_RADIUS = 1.5f;
    public static final float BOMBER_FUSE = 0.9f;
    public static final float BOMBER_DAMAGE = 1.0f;
    public static final float GOBLIN_LIFETIME = 9f;
    public static final float WARDEN_AURA_RADIUS = 2.6f;
    public static final float WARDEN_DAMAGE_REDUCTION = 0.6f;

    // Random events
    public static final float EVENT_FIRST_AT = 35f;
    public static final float EVENT_INTERVAL = 45f;
    public static final float BLOOD_MOON_DURATION = 20f;
    public static final float BLOOD_MOON_SPEED = 1.3f;
    public static final int SWARM_SIZE = 14;

    // Combo
    public static final float COMBO_WINDOW = 2.5f;

    // Juice
    public static final float SCREEN_SHAKE_DURATION = 0.2f;
    public static final float SCREEN_SHAKE_MAGNITUDE = 0.15f;
    public static final float PARTICLE_LIFETIME = 0.35f;
    public static final int PARTICLES_PER_DEATH = 6;

    // Field
    public static final int GROUND_DECORATION_COUNT = 90;
    public static final int AMBIENT_COUNT = 70;
}
