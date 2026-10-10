package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Every enemy has its own sprite (see SpriteFactory) and its own trick:
 *
 *  SLIME     plain chaser
 *  RUNNER    winds up, then lunges at you
 *  BRUTE     slams the ground (telegraphed blast + shockwave)
 *  SPLITTER  bursts into two minis
 *  MINI      fast little swarmer
 *  SPITTER   keeps its distance and fires 3-orb volleys
 *  BOMBER    lights a fuse next to you
 *  GOBLIN    treasure goblin, flees
 *  SHADE     fades out and re-appears next to you for a stab
 *  WARDEN    shields every ally around it (kill it first)
 *  SUMMONER  conjures minis
 *  SNIPER    locks on with a long laser
 *
 * Elites add a random affix: SWIFT, VOLATILE (explodes on death) or ARMORED.
 */
public class Enemy extends GameObject {

    public enum Type {
        SLIME(1f, 1.5f, 1, 1f, new Color(0.4f, 0.9f, 0.45f, 1f)),
        RUNNER(1f, 2.3f, 1, 0.85f, new Color(0.4f, 0.85f, 1f, 1f)),
        BRUTE(4f, 0.9f, 3, 1.6f, new Color(0.9f, 0.3f, 0.3f, 1f)),
        SPLITTER(2.5f, 1.2f, 2, 1.25f, new Color(0.65f, 1f, 0.4f, 1f)),
        MINI(0.5f, 2.2f, 1, 0.55f, new Color(0.7f, 1f, 0.6f, 1f)),
        SPITTER(2f, 0.8f, 3, 1f, new Color(0.85f, 0.5f, 1f, 1f)),
        BOMBER(1.5f, 1.9f, 2, 1.1f, new Color(1f, 0.7f, 0.2f, 1f)),
        GOBLIN(5f, 3.4f, 15, 0.9f, new Color(1f, 0.85f, 0.2f, 1f)),
        SHADE(2f, 1.8f, 4, 0.95f, new Color(0.65f, 0.4f, 0.95f, 1f)),
        WARDEN(8f, 0.7f, 6, 1.5f, new Color(0.4f, 0.65f, 1f, 1f)),
        SUMMONER(4f, 0.8f, 6, 1.1f, new Color(0.3f, 0.9f, 0.8f, 1f)),
        SNIPER(3f, 0.6f, 5, 1.2f, new Color(1f, 0.3f, 0.3f, 1f)),
        BOSS(1f, 1f, 0, 2.6f, Color.WHITE);

        final float maxHealth;
        final float speed;
        final int xpValue;
        final float sizeMultiplier;
        final Color tint; // also the colour of this enemy's particles / projectiles

        Type(float maxHealth, float speed, int xpValue, float sizeMultiplier, Color tint) {
            this.maxHealth = maxHealth;
            this.speed = speed;
            this.xpValue = xpValue;
            this.sizeMultiplier = sizeMultiplier;
            this.tint = tint;
        }
    }

    public enum Affix { NONE, SWIFT, VOLATILE, ARMORED }

    /** Kept up to date by GameScreen. */
    public static float arenaWidth = GameConfig.WORLD_WIDTH;
    public static float arenaHeight = GameConfig.WORLD_HEIGHT;
    /** World size of a 1.0x enemy — measured from your slime.png so everything keeps your original scale. */
    public static float baseSize = 1f;
    public static SpriteFactory sprites;

    // Visual clarity (set every frame by GameScreen)
    public static float focusX, focusY;        // where the player is
    public static float crowdFade = 1f;        // alpha for harmless fodder when the screen is busy
    public static boolean simpleOverlays;      // skip health bars / ice / shield bubbles in a crowd

    protected static final float SCALE = 1 / 32f;
    private static final float HIT_FLASH_DURATION = 0.1f;

    protected final Player player;
    protected final Type type;
    protected float health;
    protected float maxHealth;
    protected float hitFlashTimer;
    protected float periodicHitTimer;
    protected float age;
    protected boolean frozenVisual;
    protected boolean faceLeft;
    protected Array<EnemyProjectile> projectileSink;
    protected Array<Hazard> hazardSink;
    protected Array<Enemy> spawnSink;

    protected float statusTimer;
    protected Color statusColor;

    // pose used when drawing (squash, stretch, hop, rotation)
    protected float poseSx = 1f, poseSy = 1f, poseRot = 0f, poseY = 0f;

    // generic AI state
    protected int phase;
    protected float phaseTimer;
    protected float cooldown;
    protected final Vector2 lock = new Vector2();

    private final float seed = MathUtils.random();
    private boolean elite;
    private Affix affix = Affix.NONE;
    private float shieldTimer;
    private float ghostAlpha = 1f;
    private float shootTimer = MathUtils.random(1f, 2.5f);
    private float fuse = -1f;
    private boolean blastReady;
    private float lifeLeft = GameConfig.GOBLIN_LIFETIME;
    private boolean escaped;
    private final Vector2 blinkDest = new Vector2();
    private final Vector2 summonPos = new Vector2();
    private final Color drawColor = new Color(1f, 1f, 1f, 1f);

    public Enemy(float x, float y, Texture sprite, Player player, Type type) {
        super(x, y, baseSize * type.sizeMultiplier, baseSize * type.sizeMultiplier, sprite);
        this.player = player;
        this.type = type;
        this.health = type.maxHealth;
        this.maxHealth = type.maxHealth;
        this.cooldown = MathUtils.random(0.8f, 2.5f);
    }

    @Override
    void update(float deltaTime) {
        tickTimers(deltaTime);

        Vector2 dir = player.getCenter(TMP_VEC2).sub(rect.x + rect.width * 0.5f, rect.y + rect.height * 0.5f);
        float dist = dir.len();
        if (dist > 0.0001f) dir.scl(1f / dist);
        else dir.setZero();
        if (Math.abs(dir.x) > 0.15f) faceLeft = dir.x < 0f;

        float speedMul = Settings.enemySpeed * (affix == Affix.SWIFT ? 1.4f : 1f);
        float speed = type.speed * speedMul * deltaTime;

        switch (type) {
            case RUNNER -> updateRunner(dir, dist, speed, deltaTime);
            case BRUTE -> updateBrute(dir, dist, speed, deltaTime);
            case SPITTER -> updateSpitter(dir, dist, speed, deltaTime);
            case BOMBER -> updateBomber(dir, dist, speed, deltaTime);
            case GOBLIN -> updateGoblin(dir, speed, deltaTime);
            case SHADE -> updateShade(dir, dist, speed, deltaTime);
            case SUMMONER -> updateSummoner(dir, dist, speed, deltaTime);
            case SNIPER -> updateSniper(dir, dist, speed, deltaTime);
            default -> move(dir.x * speed, dir.y * speed);
        }
    }

    protected void tickTimers(float deltaTime) {
        age += deltaTime;
        if (hitFlashTimer > 0f) hitFlashTimer -= deltaTime;
        if (statusTimer > 0f) statusTimer -= deltaTime;
        if (periodicHitTimer > 0f) periodicHitTimer -= deltaTime;
        if (shieldTimer > 0f) shieldTimer -= deltaTime;
    }

    protected void move(float dx, float dy) {
        rect.setPosition(rect.x + dx, rect.y + dy);
    }

    protected void clampToArena() {
        rect.x = MathUtils.clamp(rect.x, 0f, Math.max(0f, arenaWidth - rect.width));
        rect.y = MathUtils.clamp(rect.y, 0f, Math.max(0f, arenaHeight - rect.height));
    }

    private Vector2 center() {
        return getCenter(new Vector2());
    }

    // ------------------------------------------------------------ behaviours

    private void updateRunner(Vector2 dir, float dist, float speed, float dt) {
        switch (phase) {
            case 0 -> {
                move(dir.x * speed, dir.y * speed);
                cooldown -= dt;
                if (cooldown <= 0f && dist < 3.8f && dist > 1.3f) {
                    phase = 1;
                    phaseTimer = 0.45f;
                    lock.set(dir);
                }
            }
            case 1 -> { // wind-up: quivers in place, tracks you, then locks
                phaseTimer -= dt;
                if (phaseTimer > 0.12f) lock.set(dir);
                if (phaseTimer <= 0f) {
                    phase = 2;
                    phaseTimer = 0.32f;
                }
            }
            case 2 -> { // the lunge
                move(lock.x * speed * 4.4f, lock.y * speed * 4.4f);
                phaseTimer -= dt;
                if (phaseTimer <= 0f) {
                    phase = 3;
                    phaseTimer = 0.45f;
                }
            }
            default -> {
                phaseTimer -= dt;
                if (phaseTimer <= 0f) {
                    phase = 0;
                    cooldown = MathUtils.random(1.8f, 3f);
                }
            }
        }
    }

    private void updateBrute(Vector2 dir, float dist, float speed, float dt) {
        switch (phase) {
            case 0 -> {
                move(dir.x * speed, dir.y * speed);
                cooldown -= dt;
                if (cooldown <= 0f && dist < 2.8f && hazardSink != null && hazardSink.size < 40) {
                    phase = 1;
                    phaseTimer = 0.8f;
                    Vector2 c = center();
                    hazardSink.add(Hazard.blast(c.x, c.y, 1.8f, 0.8f, 1.2f, type.tint));
                }
            }
            case 1 -> { // raised fists, shaking — the slam is coming
                phaseTimer -= dt;
                if (phaseTimer <= 0f) {
                    phase = 2;
                    phaseTimer = 0.55f;
                    Vector2 c = center();
                    hazardSink.add(Hazard.ring(c.x, c.y, 3.6f, 5.5f, 0.8f, type.tint));
                }
            }
            default -> {
                phaseTimer -= dt;
                if (phaseTimer <= 0f) {
                    phase = 0;
                    cooldown = 4.5f;
                }
            }
        }
    }

    private void updateSpitter(Vector2 dir, float dist, float speed, float dt) {
        if (dist > 4.8f) {
            move(dir.x * speed, dir.y * speed);
        } else if (dist < 3.2f) {
            move(-dir.x * speed, -dir.y * speed);
        } else {
            move(-dir.y * speed * 0.6f, dir.x * speed * 0.6f);
        }

        shootTimer -= dt;
        if (shootTimer <= 0f && projectileSink != null && dist < 9f && projectileSink.size < 120) {
            shootTimer = 2.6f;
            Vector2 me = center();
            float base = player.getCenter(new Vector2()).sub(me).angleRad();
            for (int i = -1; i <= 1; i++) {
                projectileSink.add(new EnemyProjectile(me.x, me.y, base + i * 0.28f, 3.4f,
                    GameConfig.PROJECTILE_DAMAGE, type.tint, 0.1f));
            }
        }
    }

    private void updateBomber(Vector2 dir, float dist, float speed, float dt) {
        if (fuse < 0f) {
            move(dir.x * speed, dir.y * speed);
            if (dist < 1.3f) {
                fuse = GameConfig.BOMBER_FUSE;
            }
        } else {
            move(dir.x * speed * 0.15f, dir.y * speed * 0.15f);
            fuse -= dt;
            if (fuse <= 0f) {
                blastReady = true;
                fuse = -1f;
                health = 0f;
            }
        }
    }

    private void updateGoblin(Vector2 dir, float speed, float dt) {
        lifeLeft -= dt;
        if (lifeLeft <= 0f) {
            escaped = true;
            return;
        }
        dir.scl(-1f);
        float cx = rect.x + rect.width * 0.5f;
        float cy = rect.y + rect.height * 0.5f;
        float margin = 1.2f;
        if (cx < margin) dir.x += 1.2f;
        if (cx > arenaWidth - margin) dir.x -= 1.2f;
        if (cy < margin) dir.y += 1.2f;
        if (cy > arenaHeight - margin) dir.y -= 1.2f;

        float wig = MathUtils.sin(age * 5f) * 0.6f;
        float ox = -dir.y * wig;
        float oy = dir.x * wig;
        dir.add(ox, oy).nor();
        move(dir.x * speed, dir.y * speed);
        clampToArena();
    }

    private void updateShade(Vector2 dir, float dist, float speed, float dt) {
        switch (phase) {
            case 0 -> {
                ghostAlpha = Math.min(1f, ghostAlpha + dt * 3f);
                move(dir.x * speed, dir.y * speed);
                cooldown -= dt;
                if (cooldown <= 0f && dist < 6.5f && hazardSink != null && hazardSink.size < 40) {
                    phase = 1;
                    phaseTimer = 0.55f;
                    Vector2 p = player.getCenter(new Vector2());
                    float ang = MathUtils.random(MathUtils.PI2);
                    blinkDest.set(
                        MathUtils.clamp(p.x + MathUtils.cos(ang) * 1.4f, 0.5f, arenaWidth - 0.5f),
                        MathUtils.clamp(p.y + MathUtils.sin(ang) * 1.4f, 0.5f, arenaHeight - 0.5f));
                    hazardSink.add(Hazard.blast(blinkDest.x, blinkDest.y, 1.0f, 0.9f, 1.1f, type.tint));
                }
            }
            case 1 -> { // dissolving into the dark
                ghostAlpha = Math.max(0.08f, ghostAlpha - dt * 3f);
                phaseTimer -= dt;
                if (phaseTimer <= 0f) {
                    rect.setCenter(blinkDest.x, blinkDest.y);
                    phase = 2;
                    phaseTimer = 0.35f;
                }
            }
            case 2 -> { // re-materialising at the marker
                ghostAlpha = Math.min(1f, ghostAlpha + dt * 4f);
                phaseTimer -= dt;
                if (phaseTimer <= 0f) {
                    phase = 3;
                    phaseTimer = 0.5f;
                }
            }
            default -> {
                phaseTimer -= dt;
                if (phaseTimer <= 0f) {
                    phase = 0;
                    cooldown = 3.6f;
                }
            }
        }
    }

    private void updateSummoner(Vector2 dir, float dist, float speed, float dt) {
        if (dist > 6.5f) {
            move(dir.x * speed, dir.y * speed);
        } else if (dist < 4.2f) {
            move(-dir.x * speed, -dir.y * speed);
        }
        if (phase == 0) {
            cooldown -= dt;
            if (cooldown <= 0f && spawnSink != null) {
                phase = 1;
                phaseTimer = 0.9f;
                Vector2 c = center();
                float a = MathUtils.random(MathUtils.PI2);
                summonPos.set(c.x + MathUtils.cos(a) * 1.3f, c.y + MathUtils.sin(a) * 1.3f);
            }
        } else {
            phaseTimer -= dt;
            if (phaseTimer <= 0f) {
                phase = 0;
                cooldown = 6.5f;
                for (int i = 0; i < 2; i++) {
                    spawnSink.add(new Enemy(summonPos.x - 0.2f + i * 0.4f, summonPos.y - 0.2f,
                        sprites.enemy(Type.MINI), player, Type.MINI));
                }
            }
        }
    }

    private void updateSniper(Vector2 dir, float dist, float speed, float dt) {
        if (phase == 0) {
            if (dist < 4.5f) {
                move(-dir.x * speed, -dir.y * speed);
            } else if (dist > 8f) {
                move(dir.x * speed, dir.y * speed);
            }
            cooldown -= dt;
            if (cooldown <= 0f && dist < 10f && hazardSink != null && hazardSink.size < 28) {
                phase = 1;
                phaseTimer = 1.7f;
                hazardSink.add(Hazard.trackingBeam(this, 16f, 0.35f, 1.7f, 0.45f, 0.16f, 1.2f, type.tint));
            }
        } else {
            phaseTimer -= dt;
            if (phaseTimer <= 0f) {
                phase = 0;
                cooldown = 4.2f;
            }
        }
    }

    // ------------------------------------------------------------ rendering

    /** Squash / stretch / hop that gives every enemy a living feel. Boss overrides this. */
    protected void computePose() {
        float wob = MathUtils.sin(age * 8f + seed * 6.28f);
        poseSx = 1f + 0.05f * wob;
        poseSy = 1f - 0.05f * wob;
        poseRot = 0f;
        poseY = Math.abs(MathUtils.sin(age * 4f + seed * 6.28f)) * 0.04f * rect.height;

        switch (type) {
            case RUNNER -> {
                if (phase == 1) {
                    poseSx = 1.18f + 0.04f * MathUtils.sin(age * 60f);
                    poseSy = 0.82f;
                    poseY = 0f;
                } else if (phase == 2) {
                    poseSx = 1.45f;
                    poseSy = 0.7f;
                    poseRot = lock.angleDeg();
                    poseY = 0f;
                }
            }
            case BRUTE -> {
                if (phase == 1) {
                    float p = 1f - Math.max(0f, phaseTimer) / 0.8f;
                    poseSy = 1f + 0.25f * p;
                    poseSx = 1f - 0.1f * p;
                    poseY = 0.3f * p * rect.height * 0.5f;
                } else if (phase == 2) {
                    poseSy = 0.78f;
                    poseSx = 1.2f;
                    poseY = 0f;
                }
            }
            case BOMBER -> {
                if (fuse > 0f) {
                    float s = 1f + 0.12f * MathUtils.sin(age * 30f);
                    poseSx = s;
                    poseSy = s;
                }
            }
            case SNIPER, SUMMONER -> poseY = (0.5f + 0.5f * MathUtils.sin(age * 2.5f + seed * 6f)) * 0.12f * rect.height;
            case SHADE -> poseY = MathUtils.sin(age * 3f) * 0.05f * rect.height;
            default -> {
            }
        }
    }

    /** True for enemies the player must be able to read at all times (telegraphing, elite, dangerous). */
    public boolean isThreatening() {
        if (elite || type == Type.BOSS || type == Type.SNIPER || type == Type.WARDEN) return true;
        if (type == Type.BOMBER && fuse > 0f) return true;
        if (type == Type.RUNNER && (phase == 1 || phase == 2)) return true;
        return (type == Type.BRUTE || type == Type.SHADE || type == Type.SUMMONER) && phase == 1;
    }

    /** Enemies themselves stay fully opaque — only their ability telegraphs are see-through. */
    protected float visibility() {
        return 1f;
    }

    protected Color resolveTint() {
        if (statusTimer > 0f && statusColor != null) {
            boolean flicker = ((int) (statusTimer * 40f)) % 2 == 0;
            drawColor.set(flicker ? statusColor : Color.WHITE);
            drawColor.a = ghostAlpha * visibility();
            return drawColor;
        }
        if (hitFlashTimer > 0f) {
            drawColor.set(Color.WHITE);
            drawColor.a = ghostAlpha * visibility();
            return drawColor;
        }

        Color base = type.tint;
        switch (Settings.enemyStyle) {
            case CLASSIC -> drawColor.set(Color.WHITE);
            case NEON -> drawColor.fromHsv((type.ordinal() * 57f) % 360f, 0.7f, 1f);
            case SHADOW -> drawColor.set(base.r * 0.35f, base.g * 0.35f, base.b * 0.35f, 1f);
            case RAINBOW -> drawColor.fromHsv((age * 90f + rect.x * 30f) % 360f, 0.6f, 1f);
        }

        if (type == Type.BOMBER && fuse > 0f) {
            boolean on = ((int) (age * 14f)) % 2 == 0;
            drawColor.set(on ? Color.RED : Color.WHITE);
        }
        if (elite) {
            Color a = affixColor();
            drawColor.lerp(a.r, a.g, a.b, 1f, 0.30f + 0.2f * MathUtils.sin(age * 8f));
        }
        if (frozenVisual) {
            drawColor.lerp(0.5f, 0.8f, 1f, 1f, 0.55f);
        }
        drawColor.a = ghostAlpha * visibility();
        return drawColor;
    }

    @Override
    public void draw(Batch batch) {
        if (texture == null) return;
        computePose();
        batch.setColor(resolveTint());
        batch.draw(texture,
            rect.x, rect.y + poseY,
            rect.width * 0.5f, rect.height * 0.5f,
            rect.width, rect.height,
            poseSx, poseSy, poseRot,
            0, 0, texture.getWidth(), texture.getHeight(),
            poseRot == 0f && faceLeft, false);
        batch.setColor(Color.WHITE);
    }

    private Color affixColor() {
        return switch (affix) {
            case SWIFT -> new Color(0.4f, 1f, 1f, 1f);
            case VOLATILE -> new Color(1f, 0.45f, 0.15f, 1f);
            case ARMORED -> new Color(0.75f, 0.8f, 0.95f, 1f);
            default -> new Color(1f, 0.85f, 0.2f, 1f);
        };
    }

    /** Beneath sprites (Filled shape pass): shadow, elite glow, auras, telegraphs. */
    public void drawGround(ShapeRenderer sr) {
        float cx = rect.x + rect.width * 0.5f;
        float cy = rect.y + rect.height * 0.5f;
        float pulse = 0.5f + 0.5f * MathUtils.sin(age * 6f);

        sr.setColor(0f, 0f, 0f, 0.25f * ghostAlpha * (simpleOverlays ? 0.5f : 1f));
        sr.ellipse(rect.x + rect.width * 0.12f, rect.y - rect.height * 0.04f, rect.width * 0.76f, rect.height * 0.22f);

        if (elite) {
            Color a = affixColor();
            sr.setColor(a.r, a.g, a.b, 0.20f + 0.12f * pulse);
            sr.circle(cx, cy, rect.width * 0.85f, 20);
        }
        if (type == Type.GOBLIN) {
            sr.setColor(1f, 0.9f, 0.3f, 0.18f + 0.12f * pulse);
            sr.circle(cx, cy, rect.width * (0.9f + 0.15f * pulse), 18);
        }
        if (type == Type.BOMBER && fuse > 0f) {
            boolean on = ((int) (age * 14f)) % 2 == 0;
            sr.setColor(1f, 0.2f, 0.1f, on ? 0.28f : 0.12f);
            sr.circle(cx, cy, GameConfig.BOMBER_BLAST_RADIUS, 28);
        }
        if (type == Type.WARDEN) {
            sr.setColor(type.tint.r, type.tint.g, type.tint.b, 0.07f + 0.04f * pulse);
            sr.circle(cx, cy, GameConfig.WARDEN_AURA_RADIUS, 40);
            sr.setColor(type.tint.r, type.tint.g, type.tint.b, 0.45f);
            for (int k = 0; k < 18; k++) {
                float a0 = age * 0.6f + k * MathUtils.PI2 / 18f;
                float a1 = a0 + 0.17f;
                sr.rectLine(cx + MathUtils.cos(a0) * GameConfig.WARDEN_AURA_RADIUS, cy + MathUtils.sin(a0) * GameConfig.WARDEN_AURA_RADIUS,
                    cx + MathUtils.cos(a1) * GameConfig.WARDEN_AURA_RADIUS, cy + MathUtils.sin(a1) * GameConfig.WARDEN_AURA_RADIUS, 0.04f);
            }
        }
        if (type == Type.SUMMONER && phase == 1) {
            float p = 1f - Math.max(0f, phaseTimer) / 0.9f;
            sr.setColor(type.tint.r, type.tint.g, type.tint.b, 0.25f + 0.3f * p);
            sr.circle(summonPos.x, summonPos.y, 0.5f * p + 0.1f, 20);
            sr.setColor(type.tint.r, type.tint.g, type.tint.b, 0.9f);
            float rot = age * 5f;
            for (int k = 0; k < 6; k++) {
                float a0 = rot + k * MathUtils.PI2 / 6f;
                float a1 = rot + (k + 1) * MathUtils.PI2 / 6f;
                sr.rectLine(summonPos.x + MathUtils.cos(a0) * 0.55f, summonPos.y + MathUtils.sin(a0) * 0.55f,
                    summonPos.x + MathUtils.cos(a1) * 0.55f, summonPos.y + MathUtils.sin(a1) * 0.55f, 0.04f);
            }
        }
        if (type == Type.SHADE && phase == 1 || type == Type.SHADE && phase == 2) {
            sr.setColor(type.tint.r, type.tint.g, type.tint.b, 0.35f);
            float r = 0.18f + 0.1f * MathUtils.sin(age * 20f);
            sr.circle(blinkDest.x, blinkDest.y, r, 12);
        }
    }

    /** Above sprites: shield bubble, ice crystals, health bar. */
    public void drawOverlay(ShapeRenderer sr) {
        float cx = rect.x + rect.width * 0.5f;
        float cy = rect.y + rect.height * 0.5f;

        if (shieldTimer > 0f && type != Type.WARDEN && !simpleOverlays) {
            float R = rect.width * 0.72f;
            sr.setColor(0.4f, 0.7f, 1f, 0.12f);
            sr.circle(cx, cy, R, 18);
            sr.setColor(0.5f, 0.8f, 1f, 0.7f);
            float rot = age * 1.2f;
            for (int i = 0; i < 6; i++) {
                float a0 = rot + i * MathUtils.PI2 / 6f;
                float a1 = rot + (i + 1) * MathUtils.PI2 / 6f;
                sr.rectLine(cx + MathUtils.cos(a0) * R, cy + MathUtils.sin(a0) * R,
                    cx + MathUtils.cos(a1) * R, cy + MathUtils.sin(a1) * R, 0.035f);
            }
        }

        if (frozenVisual && !simpleOverlays) {
            float R = rect.width * 0.55f;
            for (int i = 0; i < 6; i++) {
                float a = i * MathUtils.PI / 3f + 0.3f;
                float bx = cx + MathUtils.cos(a) * R * 0.8f;
                float by = cy + MathUtils.sin(a) * R * 0.8f;
                float tx = cx + MathUtils.cos(a) * (R + 0.16f + 0.04f * (i % 2));
                float ty = cy + MathUtils.sin(a) * (R + 0.16f + 0.04f * (i % 2));
                float nx = -MathUtils.sin(a) * 0.06f;
                float ny = MathUtils.cos(a) * 0.06f;
                sr.setColor(0.8f, 0.95f, 1f, 0.85f);
                sr.triangle(bx + nx, by + ny, bx - nx, by - ny, tx, ty);
            }
            sr.setColor(0.7f, 0.9f, 1f, 0.18f);
            sr.circle(cx, cy, R, 16);
        }

        if (type != Type.MINI && type != Type.BOSS && (health < maxHealth - 0.01f || elite) && (!simpleOverlays || elite)) {
            float w = rect.width * 0.8f;
            float bx = cx - w / 2f;
            float by = rect.y + rect.height + 0.08f + poseY;
            float pct = Math.max(0f, health / maxHealth);
            sr.setColor(0f, 0f, 0f, 0.7f);
            sr.rect(bx - 0.02f, by - 0.02f, w + 0.04f, 0.1f);
            sr.setColor(1f - pct, 0.2f + 0.8f * pct, 0.2f, 1f);
            sr.rect(bx, by, w * pct, 0.06f);
        }
    }

    // ------------------------------------------------------------ state

    public boolean isDead() {
        return health <= 0f;
    }

    public void knockback(float dx, float dy) {
        rect.setPosition(rect.x + dx, rect.y + dy);
    }

    public boolean tryPeriodicHit(float interval) {
        if (periodicHitTimer > 0f) return false;
        periodicHitTimer = interval;
        return true;
    }

    /** @return true if this hit killed the enemy. Shielded, armoured and frozen state change the damage. */
    public boolean takeDamage(float amount) {
        if (shieldTimer > 0f) amount *= 1f - GameConfig.WARDEN_DAMAGE_REDUCTION;
        if (affix == Affix.ARMORED) amount *= 0.6f;
        if (frozenVisual) amount *= GameConfig.FREEZE_SHATTER_MULT;
        health -= amount;
        hitFlashTimer = HIT_FLASH_DURATION;
        return health <= 0f;
    }

    public void applyStatusFlash(Color color, float duration) {
        this.statusColor = color;
        this.statusTimer = duration;
    }

    /** Called every frame by GameScreen for enemies standing inside a Warden's aura. */
    public void setShielded() {
        shieldTimer = 0.15f;
    }

    public void scaleHealth(float multiplier) {
        health *= multiplier;
        maxHealth *= multiplier;
    }

    public void makeElite(Affix affix) {
        if (elite) return;
        elite = true;
        this.affix = affix;
        scaleHealth(3f);
        float cx = rect.x + rect.width * 0.5f;
        float cy = rect.y + rect.height * 0.5f;
        rect.setSize(rect.width * 1.25f, rect.height * 1.25f);
        rect.setCenter(cx, cy);
    }

    public boolean isElite() {
        return elite;
    }

    public Affix getAffix() {
        return affix;
    }

    public boolean consumeBlast() {
        if (blastReady) {
            blastReady = false;
            return true;
        }
        return false;
    }

    public boolean hasEscaped() {
        return escaped;
    }

    public void setProjectileSink(Array<EnemyProjectile> sink) {
        this.projectileSink = sink;
    }

    public void setHazardSink(Array<Hazard> sink) {
        this.hazardSink = sink;
    }

    public void setSpawnSink(Array<Enemy> sink) {
        this.spawnSink = sink;
    }

    public void setFrozenVisual(boolean frozen) {
        this.frozenVisual = frozen;
    }

    public float getHealthPercent() {
        return Math.max(0f, health / maxHealth);
    }

    public int getXpValue() {
        return type.xpValue * (elite ? 3 : 1);
    }

    public Type getType() {
        return type;
    }

    // ------------------------------------------------------------ spawning

    public static Enemy spawn(Viewport gameViewport, Texture sprite, Player player, Type type) {
        float w = gameViewport.getWorldWidth();
        float h = gameViewport.getWorldHeight();
        int edge = MathUtils.random(3); // 0: top, 1: right, 2: bottom, 3: left
        float size = baseSize * type.sizeMultiplier;
        float x, y;

        switch (edge) {
            case 0 -> {
                x = MathUtils.random(0f, 1f) * w;
                y = h;
            }
            case 1 -> {
                x = w;
                y = MathUtils.random(0f, 1f) * h;
            }
            case 2 -> {
                x = MathUtils.random(0f, 1f) * w;
                y = -size;
            }
            default -> {
                x = -size;
                y = MathUtils.random(0f, 1f) * h;
            }
        }

        return new Enemy(x, y, sprite, player, type);
    }

    /** Tougher / stranger enemies unlock as the score grows. */
    public static Type randomType(int score) {
        boolean v = Settings.variants;
        if (score > 40 && MathUtils.randomBoolean(0.12f)) return Type.BRUTE;
        if (v && score > 45 && MathUtils.randomBoolean(0.07f)) return Type.WARDEN;
        if (v && score > 50 && MathUtils.randomBoolean(0.08f)) return Type.SNIPER;
        if (v && score > 60 && MathUtils.randomBoolean(0.06f)) return Type.SUMMONER;
        if (v && score > 35 && MathUtils.randomBoolean(0.10f)) return Type.SHADE;
        if (v && score > 30 && MathUtils.randomBoolean(0.10f)) return Type.BOMBER;
        if (v && score > 25 && MathUtils.randomBoolean(0.10f)) return Type.SPITTER;
        if (v && score > 10 && MathUtils.randomBoolean(0.14f)) return Type.SPLITTER;
        if (score > 15 && MathUtils.randomBoolean(0.30f)) return Type.RUNNER;
        return Type.SLIME;
    }
}
