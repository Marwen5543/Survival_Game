package io.github.SurvivalGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.EnumMap;
import java.util.Map;

public class GameScreen extends ScreenAdapter {
    private static final boolean DRAW_DEBUG = false;
    private static final int LEVEL_UP_CHOICES = 3;

    private static final AbilityPickup.Type[] HOTBAR_TYPES = {
        AbilityPickup.Type.SHIELD, AbilityPickup.Type.HASTE, AbilityPickup.Type.MAGNET, AbilityPickup.Type.STORM,
        AbilityPickup.Type.FREEZE, AbilityPickup.Type.NUKE, AbilityPickup.Type.OVERDRIVE
    };
    private static final int[] HOTBAR_KEYS = {
        Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4,
        Input.Keys.NUM_5, Input.Keys.NUM_6, Input.Keys.NUM_7
    };
    private static final String[] HOTBAR_LABELS = { "1", "2", "3", "4", "5", "6", "7" };

    // General
    private final GdxGame game;
    private final Batch batch;
    private final ShapeRenderer shapeRenderer;
    private final Viewport gameViewport = new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
    private final Viewport uiViewport = new ScreenViewport();
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final Array<XpGem> xpGems = new Array<>();
    private final Array<HealthPickup> healthPickups = new Array<>();
    private final Array<AbilityPickup> abilityPickups = new Array<>();
    private final Array<Particle> particles = new Array<>();
    private final Array<BuffEffect> buffEffects = new Array<>();
    private final Array<EnemyProjectile> enemyProjectiles = new Array<>();
    private final Array<Hazard> hazards = new Array<>();
    private final Array<Enemy> spawnQueue = new Array<>();
    private final AbilityFx fx = new AbilityFx(particles);

    // Field dressing
    private final Array<GroundDecoration> groundDecorations = new Array<>();
    private final Array<Vector2> ambient = new Array<>();
    private final Array<LightningBolt> lightningBolts = new Array<>();

    // Assets
    private final SpriteFactory sprites = new SpriteFactory();
    private final Texture bgdTexture = new Texture(Gdx.files.internal(GameConfig.BACKGROUND_TEXTURE));
    private final Texture playerTexture = new Texture(Gdx.files.internal(GameConfig.PLAYER_TEXTURE));
    private final Texture enemyTexture = new Texture(Gdx.files.internal(GameConfig.ENEMY_TEXTURE));
    private final Array<Texture> attackTextures = loadAttackTextures();
    private final Animation<Texture> attackAnimation = new Animation<>(1 / 12f, attackTextures);
    private final Map<AbilityPickup.Type, Texture> abilityIcons = loadAbilityIcons();
    private final Texture buffBurstTexture = new Texture(Gdx.files.internal(GameConfig.BUFF_BURST_TEXTURE));
    private final Animation<TextureRegion> buffAnimation = loadBuffAnimation();
    private final Music music;
    private final Sound slashSfx = Gdx.audio.newSound(Gdx.files.internal(GameConfig.SLASH_SFX));

    // Player
    private final Player player = new Player(
        GameConfig.WORLD_WIDTH / 2f, GameConfig.WORLD_HEIGHT / 2f,
        gameViewport,
        playerTexture,
        attackAnimation,
        slashSfx
    );
    private final Vector2 inputMovement = new Vector2();

    // Weapons + level-up
    private final WeaponSystem weapons = new WeaponSystem(player, particles);
    private final LevelUpMenu levelUpMenu = new LevelUpMenu();
    private int pendingLevelUps;

    // Enemies & bosses
    private final Array<Enemy> enemies = new Array<>();
    private float enemySpawnTimer;
    private float elapsedTime;
    private float enemyDt;
    private float crowdAmount;                       // 0 = calm, 1 = chaos (drives the clarity system)
    private final Array<Enemy> drawList = new Array<>();
    private Boss boss;
    private float bossTimer;
    private int bossesDefeated;
    private float waveTimer;

    // Abilities
    private float abilitySpawnTimer;
    private float stormStrikeTimer;
    private float nukeDelay;
    private final Vector2 nukeSpot = new Vector2();

    // Events
    private float eventTimer;
    private boolean bloodMoon;
    private float bloodMoonTimer;

    // Combo
    private int combo;
    private float comboTimer;
    private int bestCombo;
    private int kills;

    // Banner / edge flash / time effects
    private String bannerTitle = "";
    private String bannerSub = "";
    private final Color bannerColor = new Color(1, 1, 1, 1);
    private float bannerTimer;
    private float bannerDuration = 1f;
    private final Color edgeColor = new Color(1, 1, 1, 1);
    private float edgeTimer;
    private float edgeDuration = 1f;
    private float slowMoTimer;
    private float hitStopTimer;
    private final Color tmpA = new Color();
    private final Color tmpB = new Color();

    // Game State
    private int score;
    private boolean paused;
    private float shakeTimer;
    private boolean runRecorded;
    private boolean newRecord;

    public GameScreen(GdxGame game) {
        this.game = game;
        this.batch = game.getBatch();
        this.shapeRenderer = game.getShapeRenderer();
        this.font = game.getFont();
        this.music = game.getMusic();

        bgdTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        Enemy.sprites = sprites;
        Enemy.baseSize = enemyTexture.getWidth() * Enemy.SCALE;
    }

    private Array<Texture> loadAttackTextures() {
        Array<Texture> textures = new Array<>(14);
        for (int i = 0; i <= 13; i++) {
            textures.add(new Texture(Gdx.files.internal(String.format("slash_%02d.png", i))));
        }
        return textures;
    }

    private Map<AbilityPickup.Type, Texture> loadAbilityIcons() {
        Map<AbilityPickup.Type, Texture> icons = new EnumMap<>(AbilityPickup.Type.class);
        icons.put(AbilityPickup.Type.SHIELD, loadIcon(GameConfig.SHIELD_ICON_TEXTURE, AbilityPickup.Type.SHIELD));
        icons.put(AbilityPickup.Type.HASTE, loadIcon(GameConfig.HASTE_ICON_TEXTURE, AbilityPickup.Type.HASTE));
        icons.put(AbilityPickup.Type.MAGNET, loadIcon(GameConfig.MAGNET_ICON_TEXTURE, AbilityPickup.Type.MAGNET));
        icons.put(AbilityPickup.Type.STORM, loadIcon(GameConfig.STORM_ICON_TEXTURE, AbilityPickup.Type.STORM));
        icons.put(AbilityPickup.Type.FREEZE, loadIcon(GameConfig.FREEZE_ICON_TEXTURE, AbilityPickup.Type.FREEZE));
        icons.put(AbilityPickup.Type.NUKE, loadIcon(GameConfig.NUKE_ICON_TEXTURE, AbilityPickup.Type.NUKE));
        icons.put(AbilityPickup.Type.OVERDRIVE, loadIcon(GameConfig.OVERDRIVE_ICON_TEXTURE, AbilityPickup.Type.OVERDRIVE));
        return icons;
    }

    private Texture loadIcon(String path, AbilityPickup.Type type) {
        if (Gdx.files.internal(path).exists()) {
            return new Texture(Gdx.files.internal(path));
        }
        return IconFactory.create(type);
    }

    private Animation<TextureRegion> loadBuffAnimation() {
        int frameWidth = buffBurstTexture.getWidth() / GameConfig.BUFF_BURST_FRAME_COLS;
        int frameHeight = buffBurstTexture.getHeight();
        TextureRegion[][] grid = TextureRegion.split(buffBurstTexture, frameWidth, frameHeight);

        Array<TextureRegion> frames = new Array<>();
        for (TextureRegion region : grid[0]) {
            frames.add(region);
        }
        return new Animation<>(1 / 20f, frames);
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    @Override
    public void show() {
        game.applyAudioSettings();
        resetGame();
    }

    private void resetGame() {
        player.reset(GameConfig.WORLD_WIDTH / 2f, GameConfig.WORLD_HEIGHT / 2f);
        weapons.reset();
        levelUpMenu.close();
        pendingLevelUps = 0;

        enemies.clear();
        spawnQueue.clear();
        enemyProjectiles.clear();
        hazards.clear();
        xpGems.clear();
        healthPickups.clear();
        abilityPickups.clear();
        particles.clear();
        lightningBolts.clear();
        buffEffects.clear();
        fx.clear();
        boss = null;

        enemySpawnTimer = 0f;
        abilitySpawnTimer = 0f;
        stormStrikeTimer = 0f;
        nukeDelay = 0f;
        elapsedTime = 0f;
        enemyDt = 0f;
        crowdAmount = 0f;
        bossTimer = GameConfig.BOSS_FIRST_AT;
        bossesDefeated = 0;
        waveTimer = GameConfig.WAVE_FIRST_AT;
        eventTimer = GameConfig.EVENT_FIRST_AT;
        bloodMoon = false;
        bloodMoonTimer = 0f;
        combo = 0;
        comboTimer = 0f;
        bestCombo = 0;
        kills = 0;
        bannerTimer = 0f;
        edgeTimer = 0f;
        slowMoTimer = 0f;
        hitStopTimer = 0f;

        score = 0;
        paused = false;
        shakeTimer = 0f;
        runRecorded = false;
        newRecord = false;

        Enemy.arenaWidth = gameViewport.getWorldWidth();
        Enemy.arenaHeight = gameViewport.getWorldHeight();

        groundDecorations.clear();
        for (int i = 0; i < GameConfig.GROUND_DECORATION_COUNT; i++) {
            float x = MathUtils.random(0f, GameConfig.WORLD_WIDTH);
            float y = MathUtils.random(0f, GameConfig.WORLD_HEIGHT);
            groundDecorations.add(new GroundDecoration(x, y, GroundDecoration.randomType(), Settings.fieldTheme));
        }

        ambient.clear();
        for (int i = 0; i < GameConfig.AMBIENT_COUNT; i++) {
            ambient.add(new Vector2(
                MathUtils.random(0f, GameConfig.WORLD_WIDTH),
                MathUtils.random(0f, GameConfig.WORLD_HEIGHT)
            ));
        }

        if (!music.isPlaying()) {
            music.play();
        }
    }

    @Override
    public void render(float rawDelta) {
        float deltaTime = rawDelta;
        if (slowMoTimer > 0f) {
            slowMoTimer -= rawDelta;
            deltaTime = rawDelta * 0.35f;
        }
        if (hitStopTimer > 0f) {
            hitStopTimer -= rawDelta;
            deltaTime = rawDelta * 0.02f; // tiny freeze-frame on big impacts
        }

        boolean menuOpen = levelUpMenu.isOpen();

        if (player.isDead() && !runRecorded) {
            runRecorded = true;
            newRecord = Settings.recordRun(score, elapsedTime);
        }

        if (!menuOpen && !player.isDead() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }

        if ((paused || player.isDead()) && Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            if (!runRecorded) {
                runRecorded = true;
                Settings.recordRun(score, elapsedTime);
            }
            game.switchScreen(new MainMenuScreen(game));
            return;
        }

        if (menuOpen) {
            updateLevelUpMenu(deltaTime);
        } else if (!player.isDead() && !paused) {
            processInput();
            updateLogic(deltaTime);
            checkCollisions(deltaTime);
            updateEffects(deltaTime);
            openLevelUpMenuIfPending();
        } else if (player.isDead() && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }

        draw();
    }

    private void processInput() {
        inputMovement.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) inputMovement.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) inputMovement.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) inputMovement.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) inputMovement.x += 1;

        inputMovement.nor();
        player.changeDirection(inputMovement);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) {
            player.tryDash();
        }

        for (int i = 0; i < HOTBAR_KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(HOTBAR_KEYS[i])) {
                tryActivateAbility(HOTBAR_TYPES[i]);
            }
        }
    }

    private void tryActivateAbility(AbilityPickup.Type type) {
        if (!player.tryUseAbility(type)) {
            return;
        }

        fx.activate(type, player);
        Vector2 center = player.getCenter(new Vector2());
        Particle.burst(particles, center.x, center.y, type.color, GameConfig.PARTICLES_PER_DEATH);

        if (type == AbilityPickup.Type.NUKE) {
            // Supernova charges up for a beat (see AbilityFx.NUKE_CHARGE), then detonates where you stood.
            nukeSpot.set(center);
            nukeDelay = GameConfig.NUKE_CHARGE_TIME;
        }
    }

    /** Screen-wide blast shared by the Supernova and the Phoenix revive. */
    private void blastAll(Color color, float damage, float bossDamage, float x, float y) {
        fx.nukeBlast(x, y, color, GameConfig.NUKE_VISUAL_RADIUS);
        shakeTimer = 0.55f;
        hitStopTimer = 0.07f;
        edgeFlash(color, 0.35f);

        Vector2 c = new Vector2();
        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            enemy.takeDamage(enemy instanceof Boss ? bossDamage : damage);
            enemy.applyStatusFlash(color, 0.35f);
            enemy.getCenter(c);
            c.sub(x, y);
            if (!c.isZero()) {
                c.nor().scl(0.6f);
                enemy.knockback(c.x, c.y);
            }
        }
        enemyProjectiles.clear();
        hazards.clear();
    }

    // ------------------------------------------------------------ banner / edge flash

    private void announce(String title, String sub, Color color, float duration) {
        bannerTitle = title;
        bannerSub = sub;
        bannerColor.set(color);
        bannerTimer = duration;
        bannerDuration = duration;
    }

    private void edgeFlash(Color color, float duration) {
        edgeColor.set(color);
        edgeTimer = duration;
        edgeDuration = duration;
    }

    // ------------------------------------------------------------ level-up flow

    private void openLevelUpMenuIfPending() {
        if (pendingLevelUps > 0 && !player.isDead() && !levelUpMenu.isOpen()) {
            pendingLevelUps--;
            levelUpMenu.open(Upgrade.roll(player, LEVEL_UP_CHOICES), player);
        }
    }

    private void updateLevelUpMenu(float deltaTime) {
        Upgrade chosen = levelUpMenu.update(deltaTime, uiViewport);
        if (chosen == null) {
            return;
        }

        player.applyUpgrade(chosen);

        Vector2 center = player.getCenter(new Vector2());
        buffEffects.add(new BuffEffect(center.x, center.y, chosen.color, buffAnimation));
        Particle.burst(particles, center.x, center.y, chosen.color, GameConfig.PARTICLES_PER_DEATH * 2);
        fx.ringBurst(center.x, center.y, chosen.color, 1.8f, 0.6f);

        levelUpMenu.close();
        openLevelUpMenuIfPending();
    }

    // ------------------------------------------------------------ simulation

    private Enemy setupEnemy(Enemy enemy) {
        enemy.setProjectileSink(enemyProjectiles);
        enemy.setHazardSink(hazards);
        enemy.setSpawnSink(spawnQueue);
        enemy.scaleHealth(Settings.difficulty.healthMult * (1f + elapsedTime / 150f));
        return enemy;
    }

    private Enemy spawnAt(Enemy.Type type, float x, float y) {
        Enemy e = new Enemy(x, y, sprites.enemy(type), player, type);
        enemies.add(setupEnemy(e));
        return e;
    }

    private void updateLogic(float deltaTime) {
        elapsedTime += deltaTime;
        Enemy.arenaWidth = gameViewport.getWorldWidth();
        Enemy.arenaHeight = gameViewport.getWorldHeight();

        player.update(deltaTime);
        player.updateCombat(deltaTime, enemies);
        updateClarity();

        if (player.consumeRevive()) {
            Color phoenix = new Color(1f, 0.6f, 0.15f, 1f);
            Vector2 c = player.getCenter(new Vector2());
            blastAll(phoenix, 8f, 25f, c.x, c.y);
            announce("PHOENIX RISES!", "You cheated death.", phoenix, 3f);
        }

        if (nukeDelay > 0f) {
            nukeDelay -= deltaTime;
            if (nukeDelay <= 0f) {
                blastAll(AbilityPickup.Type.NUKE.color, GameConfig.NUKE_DAMAGE, GameConfig.NUKE_BOSS_DAMAGE, nukeSpot.x, nukeSpot.y);
            }
        }

        if (comboTimer > 0f) {
            comboTimer -= deltaTime;
            if (comboTimer <= 0f) combo = 0;
        }

        // Regular spawning — ramps up the longer you survive.
        float spawnInterval = Math.max(
            GameConfig.ENEMY_SPAWN_INTERVAL_MIN,
            GameConfig.ENEMY_SPAWN_INTERVAL_START - elapsedTime * GameConfig.DIFFICULTY_RAMP_PER_SECOND
        ) / Settings.spawnRate;
        if (bloodMoon) spawnInterval *= 0.7f;

        enemySpawnTimer += deltaTime;
        if (enemySpawnTimer >= spawnInterval && enemies.size < GameConfig.MAX_ENEMIES) {
            enemySpawnTimer = 0f;
            Enemy.Type type = Enemy.randomType(score);
            Enemy enemy = Enemy.spawn(gameViewport, sprites.enemy(type), player, type);
            setupEnemy(enemy);
            if (Settings.variants && elapsedTime > 60f && MathUtils.randomBoolean(0.07f)) {
                Enemy.Affix[] affixes = { Enemy.Affix.SWIFT, Enemy.Affix.VOLATILE, Enemy.Affix.ARMORED };
                enemy.makeElite(affixes[MathUtils.random(affixes.length - 1)]);
            }
            enemies.add(enemy);
        }

        if (Settings.events) {
            waveTimer -= deltaTime;
            if (waveTimer <= 0f && boss == null) {
                waveTimer = GameConfig.WAVE_INTERVAL;
                spawnWave();
            }
        }

        if (Settings.bosses && boss == null) {
            bossTimer -= deltaTime;
            if (bossTimer <= 0f) spawnBoss();
        }

        if (Settings.events) updateEvents(deltaTime);

        // Enemy simulation (frozen time and Blood Moon scale the delta)
        boolean frozen = player.isFreezeActive();
        enemyDt = deltaTime
            * (frozen ? GameConfig.FREEZE_TIME_SCALE : 1f)
            * (bloodMoon ? GameConfig.BLOOD_MOON_SPEED : 1f);

        applyWardenAuras();

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.setFrozenVisual(frozen);
            enemy.update(enemyDt);

            if (enemy.hasEscaped()) {
                enemies.removeIndex(i);
                announce("It got away...", "The Treasure Goblin vanished.", Color.LIGHT_GRAY, 2f);
                continue;
            }
            if (enemy.consumeBlast()) {
                explodeBomber(enemy);
            }
        }

        handleBossEvents();

        if (spawnQueue.size > 0) {
            for (int i = 0; i < spawnQueue.size; i++) {
                if (enemies.size < GameConfig.MAX_ENEMIES) {
                    Enemy e = spawnQueue.get(i);
                    setupEnemy(e);
                    enemies.add(e);
                }
            }
            spawnQueue.clear();
        }

        updateHazards(enemyDt);
        updateProjectiles(enemyDt);

        weapons.update(deltaTime, enemies);
        reapDeadEnemies();

        abilitySpawnTimer += deltaTime;
        if (abilitySpawnTimer >= GameConfig.ABILITY_SPAWN_INTERVAL) {
            abilitySpawnTimer = 0f;
            float x = MathUtils.random(1f, gameViewport.getWorldWidth() - 1f);
            float y = MathUtils.random(1f, gameViewport.getWorldHeight() - 1f);
            dropAbility(x, y);
        }
        for (AbilityPickup pickup : abilityPickups) {
            pickup.update(deltaTime);
        }

        updateStorm(deltaTime);
    }

    /**
     * Visual clarity: measures how chaotic the screen is and dials down the noise —
     * fodder enemies fade, telegraph fills thin out, particles are capped — while the
     * dangerous things (elites, bosses, telegraphing enemies, outlines, projectiles) stay sharp.
     */
    private void updateClarity() {
        float s = Settings.clarity.strength;

        float load = enemies.size + hazards.size * 3f + enemyProjectiles.size * 0.5f;
        crowdAmount = MathUtils.clamp((load - 25f) / 90f, 0f, 1f);

        // Only enemy ABILITY zones are made see-through (a bit more when the screen is busy).
        Hazard.fillScale = MathUtils.lerp(1f, 0.55f, s) * MathUtils.lerp(1f, 0.8f, crowdAmount * s);

        Enemy.simpleOverlays = false;
        Particle.density = s > 0f ? MathUtils.lerp(1f, 0.4f, crowdAmount * s) : 1f;
    }

    private void applyWardenAuras() {
        Vector2 a = new Vector2();
        Vector2 b = new Vector2();
        for (int i = 0; i < enemies.size; i++) {
            Enemy warden = enemies.get(i);
            if (warden.getType() != Enemy.Type.WARDEN || warden.isDead()) continue;
            warden.getCenter(a);
            for (int j = 0; j < enemies.size; j++) {
                Enemy other = enemies.get(j);
                if (other == warden || other.getType() == Enemy.Type.WARDEN) continue;
                other.getCenter(b);
                if (Vector2.dst(a.x, a.y, b.x, b.y) <= GameConfig.WARDEN_AURA_RADIUS) {
                    other.setShielded();
                }
            }
        }
    }

    // ------------------------------------------------------------ bosses, waves, events

    private void spawnBoss() {
        Boss.Kind[] kinds = Boss.Kind.values();
        Boss.Kind kind = kinds[bossesDefeated % kinds.length];
        float w = gameViewport.getWorldWidth();
        float h = gameViewport.getWorldHeight();

        boss = new Boss(w / 2f, h - 2.4f, sprites.boss(kind), player, kind, bossesDefeated,
            enemyProjectiles, hazards, spawnQueue);
        enemies.add(boss);

        announce("WARNING!  " + kind.title, kind.hint, kind.color, 3.8f);
        edgeFlash(kind.color, 0.5f);
        shakeTimer = 0.35f;
    }

    private void handleBossEvents() {
        if (boss == null) return;

        Vector2 c = boss.getCenter(new Vector2());
        if (boss.consumeLanding()) {
            shakeTimer = 0.7f;
            hitStopTimer = 0.06f;
            edgeFlash(boss.getKind().color, 0.35f);
            fx.ringBurst(c.x, c.y - 0.5f, boss.getKind().color, 7f, 0.7f);
            Particle.burst(particles, c.x, c.y - 0.8f, boss.getKind().color, 30);
        }
        if (boss.consumeImpact()) {
            shakeTimer = 0.5f;
            fx.ringBurst(c.x, c.y - 0.5f, boss.getKind().color, 8.5f, 0.8f);
            Particle.burst(particles, c.x, c.y - 0.8f, Color.GRAY, 24);
        }
        int stage = boss.consumeStageEvent();
        if (stage > 0) {
            announce(stage == 1 ? "ENRAGED!" : "DESPERATE!", boss.getKind().title + " unleashes its full power!", Color.RED, 2.8f);
            shakeTimer = 0.45f;
            edgeFlash(Color.RED, 0.45f);
            fx.ringBurst(c.x, c.y, Color.WHITE, 6f, 0.6f);
        }
    }

    private void onBossKilled(Boss defeated) {
        Vector2 c = defeated.getCenter(new Vector2());
        Color col = defeated.getKind().color;

        bossesDefeated++;
        boss = null;
        bossTimer = GameConfig.BOSS_INTERVAL;
        score += 10;
        kills++;

        int gemValue = GameConfig.BOSS_GEM_VALUE + 2 * defeated.getTier();
        for (int i = 0; i < GameConfig.BOSS_GEM_COUNT; i++) {
            float a = MathUtils.random(MathUtils.PI2);
            float r = MathUtils.random(0.3f, 1.8f);
            xpGems.add(new XpGem(c.x + MathUtils.cos(a) * r, c.y + MathUtils.sin(a) * r, gemValue));
        }
        for (int i = 0; i < 2; i++) {
            healthPickups.add(new HealthPickup(c.x + MathUtils.random(-1f, 1f), c.y + MathUtils.random(-1f, 1f)));
        }
        for (int i = 0; i < 3; i++) {
            dropAbility(c.x + MathUtils.random(-1.5f, 1.5f), c.y + MathUtils.random(-1f, 1f));
        }
        pendingLevelUps++;

        fx.explosionSeries(c.x, c.y, col, defeated.rect.width * 0.5f, 1.4f);
        fx.ringBurst(c.x, c.y, Color.WHITE, 10f, 0.9f);
        Particle.burst(particles, c.x, c.y, col, 60);
        Particle.burst(particles, c.x, c.y, Color.GOLD, 40);
        enemyProjectiles.clear();
        hazards.clear();

        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).takeDamage(9999f);
        }

        slowMoTimer = 1.4f;
        shakeTimer = 0.6f;
        edgeFlash(Color.WHITE, 0.5f);
        announce("BOSS DEFEATED!", "Treasure: huge XP haul, supplies and a FREE upgrade!", Color.GOLD, 4f);
    }

    private void spawnWave() {
        float w = gameViewport.getWorldWidth();
        float h = gameViewport.getWorldHeight();
        int edge = MathUtils.random(3);
        int n = Math.min(24, 6 + (int) (elapsedTime / 25f));
        Enemy.Type type = elapsedTime > 100f && MathUtils.randomBoolean(0.4f) ? Enemy.randomType(score + 20) : Enemy.Type.RUNNER;
        if (type == Enemy.Type.GOBLIN || type == Enemy.Type.BOSS) type = Enemy.Type.RUNNER;

        for (int i = 0; i < n; i++) {
            float t = (i + 0.5f) / n;
            float x, y;
            switch (edge) {
                case 0 -> { x = t * w; y = h + 0.3f; }
                case 1 -> { x = w + 0.3f; y = t * h; }
                case 2 -> { x = t * w; y = -1.4f; }
                default -> { x = -1.4f; y = t * h; }
            }
            spawnAt(type, x, y);
        }
        announce("WAVE INCOMING!", n + " " + type.name().toLowerCase() + "s rush in from "
            + (edge == 0 ? "above" : edge == 1 ? "the right" : edge == 2 ? "below" : "the left"), Color.ORANGE, 2.4f);
    }

    private void updateEvents(float deltaTime) {
        if (bloodMoon) {
            bloodMoonTimer -= deltaTime;
            if (bloodMoonTimer <= 0f) {
                bloodMoon = false;
                announce("The Blood Moon fades...", "", Color.LIGHT_GRAY, 2f);
            }
        }

        eventTimer -= deltaTime;
        if (eventTimer <= 0f) {
            if (boss != null) {
                eventTimer = 8f;
                return;
            }
            eventTimer = GameConfig.EVENT_INTERVAL + MathUtils.random(-8f, 8f);
            triggerRandomEvent();
        }
    }

    private void triggerRandomEvent() {
        switch (MathUtils.random(2)) {
            case 0 -> {
                if (bloodMoon) {
                    spawnGoblin();
                } else {
                    bloodMoon = true;
                    bloodMoonTimer = GameConfig.BLOOD_MOON_DURATION;
                    announce("BLOOD MOON", "Enemies are faster... but XP is doubled!", new Color(1f, 0.2f, 0.25f, 1f), 3.5f);
                    edgeFlash(Color.RED, 0.5f);
                }
            }
            case 1 -> spawnSwarm();
            default -> spawnGoblin();
        }
    }

    private void spawnSwarm() {
        Vector2 c = player.getCenter(new Vector2());
        float start = MathUtils.random(MathUtils.PI2);
        for (int i = 0; i < GameConfig.SWARM_SIZE; i++) {
            float a = start + i * MathUtils.PI2 / GameConfig.SWARM_SIZE;
            spawnAt(Enemy.Type.RUNNER, c.x + MathUtils.cos(a) * 6f, c.y + MathUtils.sin(a) * 6f);
        }
        announce("SWARM!", "They're closing in from every side!", Color.CYAN, 3f);
        shakeTimer = 0.3f;
    }

    private void spawnGoblin() {
        Enemy goblin = Enemy.spawn(gameViewport, sprites.enemy(Enemy.Type.GOBLIN), player, Enemy.Type.GOBLIN);
        enemies.add(setupEnemy(goblin));
        announce("TREASURE GOBLIN!", "Catch it before it escapes!", Color.GOLD, 3f);
    }

    private void explodeBomber(Enemy bomber) {
        Vector2 c = bomber.getCenter(new Vector2());
        Particle.burst(particles, c.x, c.y, Color.ORANGE, 24);
        Particle.burst(particles, c.x, c.y, Color.RED, 12);
        fx.ringBurst(c.x, c.y, Color.ORANGE, GameConfig.BOMBER_BLAST_RADIUS * 1.3f, 0.4f);
        shakeTimer = Math.max(shakeTimer, 0.25f);

        Vector2 p = player.getCenter(new Vector2());
        if (Vector2.dst(c.x, c.y, p.x, p.y) <= GameConfig.BOMBER_BLAST_RADIUS) {
            player.subLife(GameConfig.BOMBER_DAMAGE * Settings.difficulty.damageMult);
        }

        Vector2 o = new Vector2();
        for (int i = 0; i < enemies.size; i++) {
            Enemy other = enemies.get(i);
            if (other == bomber) continue;
            other.getCenter(o);
            if (Vector2.dst(c.x, c.y, o.x, o.y) <= GameConfig.BOMBER_BLAST_RADIUS) {
                other.takeDamage(2f);
            }
        }
    }

    private void updateHazards(float dt) {
        for (int i = hazards.size - 1; i >= 0; i--) {
            Hazard h = hazards.get(i);
            boolean hurt = h.update(dt, player);
            if (hurt) {
                shakeTimer = GameConfig.SCREEN_SHAKE_DURATION;
                edgeFlash(Color.RED, 0.3f);
            }
            if (h.isDone()) {
                hazards.removeIndex(i);
            }
        }
    }

    private void updateProjectiles(float dt) {
        Vector2 pc = player.getCenter(new Vector2());
        for (int i = enemyProjectiles.size - 1; i >= 0; i--) {
            EnemyProjectile p = enemyProjectiles.get(i);
            p.update(dt);
            boolean remove = p.isDone();

            if (!remove && p.hits(player)) {
                if (player.isShielded()) {
                    Particle.burst(particles, p.getX(), p.getY(), p.getColor(), 4);
                    fx.ripple(MathUtils.atan2(p.getY() - pc.y, p.getX() - pc.x));
                    remove = true;
                } else if (!player.isInvincible()) {
                    float before = player.getLife();
                    player.subLife(p.getDamage() * Settings.difficulty.damageMult);
                    if (player.getLife() < before) {
                        shakeTimer = GameConfig.SCREEN_SHAKE_DURATION;
                        edgeFlash(Color.RED, 0.25f);
                    }
                    Particle.burst(particles, p.getX(), p.getY(), p.getColor(), 5);
                    remove = true;
                }
            }

            if (remove) {
                enemyProjectiles.removeIndex(i);
            }
        }
    }

    private void reapDeadEnemies() {
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.isDead()) {
                enemies.removeIndex(i);
                onEnemyKilled(enemy);
            }
        }
    }

    private float comboMultiplier() {
        return 1f + Math.min(combo, 50) / 50f;
    }

    private void dropAbility(float x, float y) {
        float cx = MathUtils.clamp(x, 0.5f, gameViewport.getWorldWidth() - 0.5f);
        float cy = MathUtils.clamp(y, 0.5f, gameViewport.getWorldHeight() - 0.5f);
        AbilityPickup.Type type = AbilityPickup.randomType();
        abilityPickups.add(new AbilityPickup(cx, cy, type, abilityIcons.get(type)));
    }

    private void onEnemyKilled(Enemy enemy) {
        if (enemy instanceof Boss) {
            onBossKilled((Boss) enemy);
            return;
        }

        Vector2 center = enemy.getCenter(new Vector2());
        Color burst = enemy.getType().tint;

        score++;
        kills++;
        combo++;
        comboTimer = GameConfig.COMBO_WINDOW;
        bestCombo = Math.max(bestCombo, combo);

        float multiplier = comboMultiplier() * (bloodMoon ? 2f : 1f);
        int xp = Math.max(1, Math.round(enemy.getXpValue() * multiplier));

        if (enemy.getType() == Enemy.Type.GOBLIN) {
            for (int i = 0; i < 8; i++) {
                xpGems.add(new XpGem(center.x + MathUtils.random(-0.6f, 0.6f), center.y + MathUtils.random(-0.6f, 0.6f), 3));
            }
            dropAbility(center.x, center.y);
            announce("TREASURE!", "The goblin dropped its loot!", Color.GOLD, 2.5f);
        } else {
            xpGems.add(new XpGem(center.x, center.y, xp));
        }

        Particle.burst(particles, center.x, center.y, burst, GameConfig.PARTICLES_PER_DEATH + 2);

        if (enemy.isElite()) {
            dropAbility(center.x, center.y);
            for (int i = 0; i < 3; i++) {
                xpGems.add(new XpGem(center.x + MathUtils.random(-0.4f, 0.4f), center.y + MathUtils.random(-0.4f, 0.4f), 2));
            }
            if (MathUtils.randomBoolean(0.5f)) {
                healthPickups.add(new HealthPickup(center.x, center.y));
            }
            Particle.burst(particles, center.x, center.y, Color.GOLD, 14);
            if (enemy.getAffix() == Enemy.Affix.VOLATILE) {
                hazards.add(Hazard.blast(center.x, center.y, 1.5f, 0.7f, 0.9f, new Color(1f, 0.5f, 0.2f, 1f)));
            }
        }

        if (enemy.getType() == Enemy.Type.SPLITTER) {
            for (int i = 0; i < 2; i++) {
                spawnQueue.add(new Enemy(center.x - 0.3f + i * 0.4f, center.y - 0.2f,
                    sprites.enemy(Enemy.Type.MINI), player, Enemy.Type.MINI));
            }
        }

        if (MathUtils.randomBoolean(GameConfig.HEALTH_PICKUP_CHANCE)) {
            healthPickups.add(new HealthPickup(center.x, center.y));
        }

        int vampire = player.getUpgradeLevel(Upgrade.VAMPIRE);
        if (vampire > 0 && MathUtils.randomBoolean(0.04f * vampire)) {
            player.heal(0.4f);
            Particle.burst(particles, center.x, center.y, new Color(0.9f, 0.1f, 0.3f, 1f), 4);
        }
    }

    /** Storm: bolts fall from the thundercloud hovering over you, arcing across a cluster of enemies. */
    private void updateStorm(float deltaTime) {
        if (!player.isStormActive()) {
            stormStrikeTimer = 0f;
            return;
        }
        stormStrikeTimer -= deltaTime;
        if (stormStrikeTimer > 0f || enemies.size == 0) return;
        stormStrikeTimer = GameConfig.STORM_STRIKE_INTERVAL;

        Enemy target = enemies.get(MathUtils.random(enemies.size - 1));
        Vector2 strikeCenter = target.getCenter(new Vector2());
        Vector2 ec = new Vector2();
        Color stormColor = AbilityPickup.Type.STORM.color;
        fx.stormFlash();

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.getCenter(ec);

            if (ec.dst(strikeCenter) <= GameConfig.STORM_STRIKE_RADIUS) {
                boolean died = enemy.takeDamage(GameConfig.STORM_STRIKE_DAMAGE);
                enemy.applyStatusFlash(new Color(0.85f, 0.7f, 1f, 1f), 0.25f);
                float topY = Math.max(fx.getCloudY(), ec.y + 1.2f);
                lightningBolts.add(new LightningBolt(ec.x, ec.y, topY));
                fx.ringBurst(ec.x, ec.y, stormColor, 0.9f, 0.35f);
                Particle.burst(particles, ec.x, ec.y, stormColor, GameConfig.PARTICLES_PER_DEATH);

                if (died) {
                    enemies.removeIndex(i);
                    onEnemyKilled(enemy);
                }
            }
        }
    }

    private void updateAmbient(float deltaTime) {
        Settings.FieldTheme theme = Settings.fieldTheme;
        float w = gameViewport.getWorldWidth();
        float h = gameViewport.getWorldHeight();
        for (int i = 0; i < ambient.size; i++) {
            Vector2 a = ambient.get(i);
            a.y += theme.ambientVy * deltaTime * (0.6f + (i % 5) * 0.2f);
            a.x += MathUtils.sin(elapsedTime * 0.8f + i) * 0.25f * deltaTime;
            if (a.y > h + 0.2f) {
                a.y = -0.2f;
                a.x = MathUtils.random(0f, w);
            } else if (a.y < -0.2f) {
                a.y = h + 0.2f;
                a.x = MathUtils.random(0f, w);
            }
            if (a.x > w) a.x -= w;
            if (a.x < 0f) a.x += w;
        }
    }

    private void updateEffects(float deltaTime) {
        if (shakeTimer > 0f) shakeTimer -= deltaTime;
        if (bannerTimer > 0f) bannerTimer -= deltaTime;
        if (edgeTimer > 0f) edgeTimer -= deltaTime;

        updateAmbient(deltaTime);
        fx.update(deltaTime, player);

        var particleIterator = particles.iterator();
        while (particleIterator.hasNext()) {
            Particle particle = particleIterator.next();
            particle.update(deltaTime);
            if (particle.isDone()) {
                particleIterator.remove();
            }
        }

        var boltIterator = lightningBolts.iterator();
        while (boltIterator.hasNext()) {
            LightningBolt bolt = boltIterator.next();
            bolt.update(deltaTime);
            if (bolt.isDone()) {
                boltIterator.remove();
            }
        }

        var buffIterator = buffEffects.iterator();
        while (buffIterator.hasNext()) {
            BuffEffect effect = buffIterator.next();
            effect.update(deltaTime);
            if (effect.isDone()) {
                buffIterator.remove();
            }
        }
    }

    private void checkCollisions(float deltaTime) {
        for (Attack attack : player.getAttacks()) {
            for (int i = enemies.size - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);

                if (enemy.isDead() || attack.hasHit(enemy)) {
                    continue;
                }

                if (attack.overlaps(enemy)) {
                    attack.markHit(enemy);

                    boolean died = enemy.takeDamage(player.getDamage(1f));
                    if (died) {
                        enemies.removeIndex(i);
                        onEnemyKilled(enemy);
                    }
                }
            }
        }

        var xpIterator = xpGems.iterator();
        while (xpIterator.hasNext()) {
            XpGem gem = xpIterator.next();

            if (gem.collect(player, deltaTime, player.getPickupRadius())) {
                int gained = player.addExperience(gem.getAmount());
                xpIterator.remove();

                if (gained > 0) {
                    pendingLevelUps += gained;

                    Vector2 center = player.getCenter(new Vector2());
                    buffEffects.add(new BuffEffect(center.x, center.y, Color.GOLD, buffAnimation));
                    Particle.burst(particles, center.x, center.y, Color.GOLD, GameConfig.PARTICLES_PER_DEATH * 2);
                    fx.ringBurst(center.x, center.y, Color.GOLD, 1.6f, 0.5f);
                }
            }
        }

        var healthIterator = healthPickups.iterator();
        while (healthIterator.hasNext()) {
            HealthPickup pickup = healthIterator.next();
            if (pickup.overlaps(player)) {
                player.heal(GameConfig.HEALTH_PICKUP_AMOUNT);
                healthIterator.remove();
            }
        }

        var abilityIterator = abilityPickups.iterator();
        while (abilityIterator.hasNext()) {
            AbilityPickup pickup = abilityIterator.next();
            if (pickup.overlaps(player)) {
                player.addAbilityCharge(pickup.getType());
                Vector2 center = pickup.getCenter(new Vector2());
                Particle.burst(particles, center.x, center.y, pickup.getType().color, GameConfig.PARTICLES_PER_DEATH / 2);
                fx.ringBurst(center.x, center.y, pickup.getType().color, 0.8f, 0.35f);
                abilityIterator.remove();
            }
        }

        int numHits = 0;
        boolean bossHit = false;
        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            if (enemy instanceof Boss && ((Boss) enemy).isIntroducing()) continue;
            if (player.overlaps(enemy)) {
                ++numHits;
                if (enemy instanceof Boss) bossHit = true;
            }
        }

        if (numHits > 0) {
            float damageMult = Settings.difficulty.damageMult;
            float amount = GameConfig.DAMAGE_PER_SECOND * numHits * deltaTime * damageMult;
            if (bossHit) amount += GameConfig.BOSS_CONTACT_DAMAGE * damageMult;

            float lifeBefore = player.getLife();
            player.subLife(amount);
            if (player.getLife() < lifeBefore) {
                shakeTimer = GameConfig.SCREEN_SHAKE_DURATION;
            }
        }
    }

    // ------------------------------------------------------------ rendering

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        gameViewport.apply();
        float arenaW = gameViewport.getWorldWidth();
        float arenaH = gameViewport.getWorldHeight();

        Camera camera = gameViewport.getCamera();
        Vector3 originalCameraPos = new Vector3(camera.position);
        if (shakeTimer > 0f) {
            float magnitude = GameConfig.SCREEN_SHAKE_MAGNITUDE * Math.min(3f, shakeTimer / GameConfig.SCREEN_SHAKE_DURATION);
            camera.position.add(MathUtils.random(-magnitude, magnitude), MathUtils.random(-magnitude, magnitude), 0f);
            camera.update();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawBackground();
        batch.end();

        Settings.FieldTheme theme = Settings.fieldTheme;

        // --- ground layer: scenery, scorch marks, hazards, shadows, pickup glow
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (GroundDecoration decoration : groundDecorations) {
            decoration.draw(shapeRenderer, elapsedTime);
        }
        for (int i = 0; i < ambient.size; i++) {
            Vector2 a = ambient.get(i);
            shapeRenderer.setColor(theme.ambient.r, theme.ambient.g, theme.ambient.b, 0.55f);
            shapeRenderer.circle(a.x, a.y, 0.02f + 0.012f * (i % 3), 6);
        }
        fx.drawBelow(shapeRenderer, player);
        for (int i = 0; i < hazards.size; i++) {
            hazards.get(i).draw(shapeRenderer);
        }
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).drawGround(shapeRenderer);
        }
        for (AbilityPickup pickup : abilityPickups) {
            pickup.drawGlow(shapeRenderer);
        }
        shapeRenderer.end();

        // --- sprites (enemies depth-sorted so lower ones overlap higher ones; the player is drawn later, on top)
        drawList.clear();
        drawList.addAll(enemies);
        drawList.sort((a, b) -> Float.compare(b.rect.y, a.rect.y));

        batch.begin();
        for (int i = 0; i < drawList.size; i++) {
            drawList.get(i).draw(batch);
        }
        for (AbilityPickup pickup : abilityPickups) {
            pickup.draw(batch);
        }
        for (Attack attack : player.getAttacks()) {
            attack.draw(batch);
        }
        for (BuffEffect effect : buffEffects) {
            effect.draw(batch);
        }
        batch.end();

        // --- effects layer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (XpGem gem : xpGems) {
            gem.drawDebug(shapeRenderer);
        }
        for (HealthPickup pickup : healthPickups) {
            pickup.drawDebug(shapeRenderer);
        }
        weapons.drawWorld(shapeRenderer);
        for (int i = 0; i < enemyProjectiles.size; i++) {
            enemyProjectiles.get(i).draw(shapeRenderer);
        }
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).drawOverlay(shapeRenderer);
        }
        fx.drawAbove(shapeRenderer, player, arenaW, arenaH);
        for (Particle particle : particles) {
            particle.draw(shapeRenderer);
        }
        for (LightningBolt bolt : lightningBolts) {
            bolt.draw(shapeRenderer);
        }
        for (int i = 0; i < hazards.size; i++) {
            hazards.get(i).drawTop(shapeRenderer);   // crisp danger outlines above the crowd
        }
        shapeRenderer.end();

        drawPlayerLayer();

        if (shakeTimer > 0f) {
            camera.position.set(originalCameraPos);
            camera.update();
        }

        drawDebug();
        drawUi();
    }

    /** Soft coloured glow on the four screen edges — replaces the old flat full-screen tints. */
    private void vignette(Color c, float alpha, float size, float w, float h) {
        tmpA.set(c.r, c.g, c.b, MathUtils.clamp(alpha, 0f, 1f));
        tmpB.set(c.r, c.g, c.b, 0f);
        shapeRenderer.rect(0, 0, w, size, tmpA, tmpA, tmpB, tmpB);
        shapeRenderer.rect(0, h - size, w, size, tmpB, tmpB, tmpA, tmpA);
        shapeRenderer.rect(0, 0, size, h, tmpA, tmpB, tmpB, tmpA);
        shapeRenderer.rect(w - size, 0, size, h, tmpB, tmpA, tmpA, tmpB);
    }

    /** Ice crystals that grow in along the screen border while Freeze is active. */
    private void frostEdges(float pct, float w, float h) {
        float g = Math.min(1f, pct * 3f);
        g = g * g * (3f - 2f * g);
        int n = 24;
        float step = w / n;
        for (int i = 0; i < n; i++) {
            float height = (28f + 60f * ((i * 37) % 7) / 7f) * g;
            float x0 = i * step;
            shapeRenderer.setColor(0.7f, 0.92f, 1f, 0.34f);
            shapeRenderer.triangle(x0, 0, x0 + step, 0, x0 + step * 0.5f, height);
            shapeRenderer.triangle(x0, h, x0 + step, h, x0 + step * 0.5f, h - height);
            shapeRenderer.setColor(1f, 1f, 1f, 0.25f);
            shapeRenderer.triangle(x0 + step * 0.25f, 0, x0 + step * 0.6f, 0, x0 + step * 0.45f, height * 0.7f);
        }
        int m = 14;
        float stepY = h / m;
        for (int i = 0; i < m; i++) {
            float len = (24f + 50f * ((i * 53) % 5) / 5f) * g;
            float y0 = i * stepY;
            shapeRenderer.setColor(0.7f, 0.92f, 1f, 0.34f);
            shapeRenderer.triangle(0, y0, 0, y0 + stepY, len, y0 + stepY * 0.5f);
            shapeRenderer.triangle(w, y0, w, y0 + stepY, w - len, y0 + stepY * 0.5f);
        }
    }

    /** The player is simply drawn last, so effects and enemies never cover the character. */
    private void drawPlayerLayer() {
        batch.begin();
        player.draw(batch);
        batch.end();
    }

    private void drawCenteredText(String text, float y, float scale, Color color) {
        font.getData().setScale(scale);
        layout.setText(font, text, color, 0f, Align.left, false);
        font.draw(batch, layout, uiViewport.getWorldWidth() / 2f - layout.width / 2f, y);
        font.getData().setScale(1f);
    }

    private static String formatTime(float seconds) {
        int s = (int) seconds;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private static float easeOut(float p) {
        float q = 1f - MathUtils.clamp(p, 0f, 1f);
        return 1f - q * q * q;
    }

    private void drawUi() {
        uiViewport.apply();
        float width = uiViewport.getWorldWidth();
        float height = uiViewport.getWorldHeight();
        shapeRenderer.setProjectionMatrix(uiViewport.getCamera().combined);

        // --- edge vignettes & frost (animated, never a flat tint)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (bloodMoon) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(elapsedTime * 3f);
            vignette(new Color(0.8f, 0.05f, 0.1f, 1f), 0.30f + 0.15f * pulse, 170f, width, height);
        }
        float hpPct = player.getLife() / player.getMaxLife();
        if (hpPct < 0.3f && !player.isDead()) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(elapsedTime * 7f);
            vignette(Color.RED, 0.20f + 0.25f * pulse, 150f, width, height);
        }
        if (boss != null && boss.getStage() >= 1) {
            vignette(boss.getKind().color, 0.10f + 0.06f * MathUtils.sin(elapsedTime * 4f), 120f, width, height);
        }
        if (player.isSlowed()) {
            vignette(new Color(0.5f, 1f, 0.3f, 1f), 0.18f, 110f, width, height);
        }
        if (edgeTimer > 0f) {
            vignette(edgeColor, 0.55f * (edgeTimer / edgeDuration), 220f, width, height);
        }
        if (player.isFreezeActive()) {
            frostEdges(player.getFreezePercent(), width, height);
        }

        // --- banner bars
        if (bannerTimer > 0f) {
            float age = bannerDuration - bannerTimer;
            float in = Math.min(1f, age / 0.25f);
            float out = Math.min(1f, bannerTimer / 0.4f);
            float a = Math.min(in, out);
            float expand = easeOut(in) * width * 0.6f;
            float cy = height * 0.74f - 24f;
            shapeRenderer.setColor(bannerColor.r * 0.18f, bannerColor.g * 0.18f, bannerColor.b * 0.18f, 0.62f * a);
            shapeRenderer.rect(width / 2f - expand, cy - 52f, expand * 2f, 118f);
            shapeRenderer.setColor(bannerColor.r, bannerColor.g, bannerColor.b, 0.9f * a);
            shapeRenderer.rect(width / 2f - expand, cy + 64f, expand * 2f, 3f);
            shapeRenderer.rect(width / 2f - expand, cy - 52f, expand * 2f, 3f);
        }
        shapeRenderer.end();

        float barX = 20;
        float barWidth = 200;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float healthY = height - 40;
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, healthY, barWidth, 16);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, healthY, barWidth * Math.max(0f, hpPct), 16);

        float xpY = healthY - 20;
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, xpY, barWidth, 10);
        shapeRenderer.setColor(Color.CYAN);
        float xpPerc = (float) player.getExperience() / player.getExperienceToNextLevel();
        shapeRenderer.rect(barX, xpY, barWidth * xpPerc, 10);

        float dashY = xpY - 16;
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, dashY, 60, 6);
        shapeRenderer.setColor(player.getDashCooldownPercent() >= 1f ? Color.LIME : Color.GRAY);
        shapeRenderer.rect(barX, dashY, 60 * player.getDashCooldownPercent(), 6);

        float buffY = dashY - 14;
        if (player.isShielded()) {
            drawBuffBar(barX, buffY, 60, 6, AbilityPickup.Type.SHIELD.color, player.getShieldPercent());
            buffY -= 12;
        }
        if (player.isHasted()) {
            drawBuffBar(barX, buffY, 60, 6, AbilityPickup.Type.HASTE.color, player.getHastePercent());
            buffY -= 12;
        }
        if (player.isMagnetActive()) {
            drawBuffBar(barX, buffY, 60, 6, AbilityPickup.Type.MAGNET.color, player.getMagnetPercent());
            buffY -= 12;
        }
        if (player.isStormActive()) {
            drawBuffBar(barX, buffY, 60, 6, AbilityPickup.Type.STORM.color, player.getStormPercent());
            buffY -= 12;
        }
        if (player.isFreezeActive()) {
            drawBuffBar(barX, buffY, 60, 6, AbilityPickup.Type.FREEZE.color, player.getFreezePercent());
            buffY -= 12;
        }
        if (player.isOverdriven()) {
            drawBuffBar(barX, buffY, 60, 6, AbilityPickup.Type.OVERDRIVE.color, player.getOverdrivePercent());
        }

        if (boss != null && !boss.isIntroducing()) {
            float bw = Math.min(620f, width * 0.5f);
            float bx = width / 2f - bw / 2f;
            float by = height - 56f;
            Color kc = boss.getKind().color;
            shapeRenderer.setColor(0.08f, 0.08f, 0.08f, 0.9f);
            shapeRenderer.rect(bx - 3f, by - 3f, bw + 6f, 22f);
            shapeRenderer.setColor(kc.r * 0.3f, kc.g * 0.3f, kc.b * 0.3f, 1f);
            shapeRenderer.rect(bx, by, bw, 16f);
            shapeRenderer.setColor(boss.getStage() >= 1 ? Color.RED : kc);
            shapeRenderer.rect(bx, by, bw * boss.getHealthPercent(), 16f);
            shapeRenderer.setColor(1f, 1f, 1f, 0.22f);
            shapeRenderer.rect(bx, by + 8f, bw * boss.getHealthPercent(), 8f);
            shapeRenderer.setColor(0f, 0f, 0f, 0.9f);
            shapeRenderer.rect(bx + bw * 0.6f - 1f, by - 3f, 2f, 22f);
            shapeRenderer.rect(bx + bw * 0.3f - 1f, by - 3f, 2f, 22f);
        }

        shapeRenderer.end();

        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        font.draw(batch, "Score: " + score, barX, height - 60);
        font.draw(batch, "Level: " + player.getLevel(), barX, height - 96);

        StringBuilder activeBuffs = new StringBuilder();
        if (player.isShielded()) activeBuffs.append("Shield ");
        if (player.isHasted()) activeBuffs.append("Haste ");
        if (player.isMagnetActive()) activeBuffs.append("Magnet ");
        if (player.isStormActive()) activeBuffs.append("Storm ");
        if (player.isFreezeActive()) activeBuffs.append("Freeze ");
        if (player.isOverdriven()) activeBuffs.append("Overdrive ");
        if (activeBuffs.length() > 0) {
            font.getData().setScale(0.75f);
            font.draw(batch, activeBuffs.toString().trim(), barX, height - 130);
            font.getData().setScale(1f);
        }

        String timeText = "Time " + formatTime(elapsedTime);
        layout.setText(font, timeText);
        font.draw(batch, layout, width - 20 - layout.width, height - 30);

        if (combo >= 3) {
            String comboText = "COMBO x" + combo + "  (XP x" + String.format("%.1f", comboMultiplier() * (bloodMoon ? 2f : 1f)) + ")";
            font.getData().setScale(0.8f);
            layout.setText(font, comboText, Color.ORANGE, 0f, Align.left, false);
            font.draw(batch, layout, width - 20 - layout.width, height - 66);
            font.getData().setScale(1f);
        }
        if (bloodMoon) {
            font.getData().setScale(0.8f);
            layout.setText(font, "BLOOD MOON " + (int) Math.ceil(bloodMoonTimer) + "s", new Color(1f, 0.3f, 0.35f, 1f), 0f, Align.left, false);
            font.draw(batch, layout, width - 20 - layout.width, height - 96);
            font.getData().setScale(1f);
        }

        if (boss != null && !boss.isIntroducing()) {
            drawCenteredText(boss.getKind().title + (boss.getStage() >= 1 ? "  [PHASE " + (boss.getStage() + 1) + "]" : ""),
                height - 20f, 0.8f, boss.getKind().color);
            String cast = boss.getCastName();
            if (!cast.isEmpty()) {
                drawCenteredText(cast, height - 82f, 0.75f, boss.isStunned() ? Color.YELLOW : Color.WHITE);
            }
        }

        drawAbilityHotbar();
        drawOwnedUpgrades();

        if (bannerTimer > 0f) {
            float age = bannerDuration - bannerTimer;
            float in = Math.min(1f, age / 0.25f);
            float out = Math.min(1f, bannerTimer / 0.4f);
            float a = Math.min(in, out);
            float scale = 1.7f + 0.7f * (1f - easeOut(in));
            Color c = new Color(bannerColor.r, bannerColor.g, bannerColor.b, a);
            drawCenteredText(bannerTitle, height * 0.74f + 36f, scale, c);
            if (!bannerSub.isEmpty()) {
                drawCenteredText(bannerSub, height * 0.74f - 24f, 0.85f, new Color(1f, 1f, 1f, a));
            }
        }

        if (player.isDead()) {
            layout.setText(font, "GAME OVER");
            font.draw(batch, layout, width / 2 - layout.width / 2, height / 2 + 100);
            layout.setText(font, "Score: " + score + "   Time: " + formatTime(elapsedTime) + "   Bosses: " + bossesDefeated);
            font.draw(batch, layout, width / 2 - layout.width / 2, height / 2 + 60);
            layout.setText(font, "Best combo: " + bestCombo + "   Best run: " + Settings.highScore);
            font.draw(batch, layout, width / 2 - layout.width / 2, height / 2 + 25);
            if (newRecord) {
                layout.setText(font, "NEW RECORD!", Color.GOLD, 0f, Align.left, false);
                font.draw(batch, layout, width / 2 - layout.width / 2, height / 2 - 10);
            }
            layout.setText(font, "R - Restart     M - Main menu");
            font.draw(batch, layout, width / 2 - layout.width / 2, height / 2 - 50);
        } else if (paused) {
            layout.setText(font, "PAUSED");
            font.draw(batch, layout, width / 2 - layout.width / 2, height / 2 + 20);
            layout.setText(font, "ESC - Resume     M - Main menu");
            font.draw(batch, layout, width / 2 - layout.width / 2, height / 2 - 20);
        }
        batch.end();

        levelUpMenu.draw(shapeRenderer, batch, font, uiViewport);
    }

    private void drawOwnedUpgrades() {
        font.getData().setScale(0.7f);
        float y = 20f;

        for (Upgrade upgrade : Upgrade.values()) {
            int level = player.getUpgradeLevel(upgrade);
            if (level <= 0) continue;

            layout.setText(font, upgrade.title + "  Lv." + level, upgrade.color, 0f, Align.left, false);
            font.draw(batch, layout, 20f, y + layout.height);
            y += layout.height + 8f;
        }

        font.getData().setScale(1f);
    }

    private void drawAbilityHotbar() {
        float iconSize = 40f;
        float spacing = 54f;
        float startX = uiViewport.getWorldWidth() - 20 - spacing * HOTBAR_TYPES.length;
        float y = 30f;

        for (int i = 0; i < HOTBAR_TYPES.length; i++) {
            AbilityPickup.Type type = HOTBAR_TYPES[i];
            int charge = player.getAbilityCharge(type);
            float x = startX + i * spacing;

            Texture icon = abilityIcons.get(type);
            batch.setColor(1f, 1f, 1f, charge > 0 ? 1f : 0.35f);
            batch.draw(icon, x, y, iconSize, iconSize);
            batch.setColor(Color.WHITE);

            layout.setText(font, HOTBAR_LABELS[i]);
            font.draw(batch, layout, x + iconSize / 2f - layout.width / 2f, y - 4f);

            if (charge > 0) {
                layout.setText(font, "x" + charge);
                font.draw(batch, layout, x + iconSize - layout.width * 0.5f, y + iconSize + layout.height + 4f);
            }
        }
    }

    private void drawBuffBar(float x, float y, float width, float height, Color color, float percent) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.setColor(color);
        shapeRenderer.rect(x, y, width * Math.max(0f, Math.min(1f, percent)), height);
    }

    private void drawBackground() {
        float u2 = gameViewport.getWorldWidth() / GameConfig.WORLD_WIDTH;
        float v2 = gameViewport.getWorldHeight() / GameConfig.WORLD_HEIGHT;

        batch.setColor(Settings.fieldTheme.bgTint);
        batch.draw(bgdTexture,
            0, 0,
            gameViewport.getWorldWidth(),
            gameViewport.getWorldHeight(),
            0, 0,
            u2, v2
        );
        batch.setColor(Color.WHITE);
    }

    private void drawDebug() {
        if (!DRAW_DEBUG) {
            return;
        }

        shapeRenderer.setProjectionMatrix(gameViewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        player.drawDebug(shapeRenderer, Color.GREEN);
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).drawDebug(shapeRenderer, Color.RED);
        }
        for (Attack attack : player.getAttacks()) {
            attack.drawDebug(shapeRenderer, Color.YELLOW);
        }
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        bgdTexture.dispose();
        playerTexture.dispose();
        enemyTexture.dispose();
        attackTextures.forEach(Texture::dispose);
        for (Texture icon : abilityIcons.values()) {
            icon.dispose();
        }
        buffBurstTexture.dispose();
        slashSfx.dispose();
        sprites.dispose();
        if (Enemy.sprites == sprites) {
            Enemy.sprites = null;
        }
    }
}
