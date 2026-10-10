package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Four bosses, each with its own sprite and a full moveset of telegraphed attacks:
 *
 *  JUGGERNAUT    Iron Charge (chains up to 3x), Seismic Slam (shockwave + falling rocks), Rockfall.
 *                After a charge or slam it is STUNNED and takes +50% damage — punish it!
 *  WARLOCK       Void Spiral bullet-hell, Gravity Wells that drag you in, rotating Rift Lasers, Phase Shift.
 *  HIVE QUEEN    Brood Call (minis), Acid Rain (slowing pools), Spore Bloom (bullet rings), Royal Guard (elites).
 *  STORM WRAITH  Lightning Rain, a sweeping Thunder Beam, Arc Volley, and Storm Step teleports.
 *
 * Bosses drop in from above (invulnerable during the intro), and escalate at 60% and 30% health.
 */
public class Boss extends Enemy {

    public enum Kind {
        JUGGERNAUT("THE JUGGERNAUT", "Ancient iron golem. Dash through its shockwaves!", new Color(1f, 0.35f, 0.25f, 1f), 3.0f),
        WARLOCK("THE VOID WARLOCK", "Reality bends around him. Never stand still!", new Color(0.75f, 0.4f, 1f, 1f), 2.6f),
        HIVE_QUEEN("THE HIVE QUEEN", "Acid, eggs and an endless brood.", new Color(0.55f, 1f, 0.4f, 1f), 3.2f),
        STORM_WRAITH("THE STORM WRAITH", "Lightning never strikes twice... it strikes ten times.", new Color(0.55f, 0.85f, 1f, 1f), 2.8f);

        final String title;
        final String hint;
        final Color color;
        final float scale;

        Kind(String title, String hint, Color color, float scale) {
            this.title = title;
            this.hint = hint;
            this.color = color;
            this.scale = scale;
        }
    }

    private enum Atk {
        NONE("", 0),
        CHARGE("IRON CHARGE", 0), SLAM("SEISMIC SLAM", 0), ROCKFALL("ROCKFALL", 1),
        SPIRAL("VOID SPIRAL", 0), WELL("GRAVITY WELL", 0), RIFT("RIFT LASERS", 1), BLINK_RING("PHASE SHIFT", 0),
        BROOD("BROOD CALL", 0), ACID("ACID RAIN", 0), SPORES("SPORE BLOOM", 0), GUARD("ROYAL GUARD", 1),
        LIGHTNING_RAIN("LIGHTNING RAIN", 0), BEAM_SWEEP("THUNDER BEAM", 0), ORB_VOLLEY("ARC VOLLEY", 0), STORM_STEP("STORM STEP", 1);

        final String label;
        final int minStage;

        Atk(String label, int minStage) {
            this.label = label;
            this.minStage = minStage;
        }
    }

    private static final Atk[] JUG = { Atk.CHARGE, Atk.SLAM, Atk.ROCKFALL };
    private static final Atk[] WAR = { Atk.SPIRAL, Atk.WELL, Atk.RIFT, Atk.BLINK_RING };
    private static final Atk[] QUE = { Atk.BROOD, Atk.ACID, Atk.SPORES, Atk.GUARD };
    private static final Atk[] STO = { Atk.LIGHTNING_RAIN, Atk.BEAM_SWEEP, Atk.ORB_VOLLEY, Atk.STORM_STEP };

    private final Kind kind;
    private final int tier;
    private final Color tint = new Color(1f, 1f, 1f, 1f);
    private final Vector2 chargeDir = new Vector2(1f, 0f);

    private final float targetY;
    private final float introFromY;
    private float introTimer;
    private boolean landed;

    private int stage;            // 0, 1, 2 (escalates at 60% / 30% health)
    private int stageEvent = -1;  // consumed by GameScreen to announce the escalation
    private Atk atk = Atk.NONE;
    private Atk lastAtk = Atk.NONE;
    private String castName = "";
    private float idleTimer = 1.6f;
    private float atkTimer;
    private float subTimer;
    private int atkStep;
    private int counter;
    private float spiralAngle;
    private float stunTimer;
    private boolean impact;

    public Boss(float cx, float cy, Texture sprite, Player player, Kind kind, int tier,
                Array<EnemyProjectile> projectiles, Array<Hazard> hazards, Array<Enemy> spawns) {
        super(0f, 0f, sprite, player, Type.BOSS);
        this.kind = kind;
        this.tier = tier;

        float size = baseSize * kind.scale;
        rect.setSize(size, size);

        maxHealth = GameConfig.BOSS_BASE_HEALTH * (1f + GameConfig.BOSS_HEALTH_PER_TIER * tier)
            * Settings.difficulty.healthMult;
        health = maxHealth;

        setProjectileSink(projectiles);
        setHazardSink(hazards);
        setSpawnSink(spawns);

        this.targetY = cy;
        this.introFromY = arenaHeight + size;
        this.introTimer = GameConfig.BOSS_INTRO_TIME;
        rect.setCenter(cx, introFromY);
    }

    // ------------------------------------------------------------ main loop

    @Override
    void update(float dt) {
        tickTimers(dt);

        if (introTimer > 0f) {
            updateIntro(dt);
            return;
        }

        Vector2 me = center();
        Vector2 toPlayer = player.getCenter(new Vector2()).sub(me);
        float dist = toPlayer.len();
        Vector2 dir = dist > 0.001f ? toPlayer.scl(1f / dist) : new Vector2(1f, 0f);
        if (Math.abs(dir.x) > 0.15f) faceLeft = dir.x < 0f;

        // escalation
        float hp = health / maxHealth;
        int newStage = hp < 0.3f ? 2 : hp < 0.6f ? 1 : 0;
        if (newStage > stage) {
            stage = newStage;
            stageEvent = stage;
            applyStatusFlash(Color.WHITE, 0.6f);
            hz(Hazard.ring(me.x, me.y, 7f, 6f, 0.8f, kind.color));
            ringShots(14 + 4 * stage, 3.2f, 0f);
            if (atk != Atk.NONE) {
                atk = Atk.NONE;
                castName = "";
            }
            idleTimer = 1.2f;
            stunTimer = 0f;
        }

        if (stunTimer > 0f) {
            stunTimer -= dt;
            clampToArena();
            return;
        }

        if (atk == Atk.NONE) {
            idleMove(dt, dir, dist);
            idleTimer -= dt;
            if (idleTimer <= 0f) {
                startAttack(pickAttack(), dir);
            }
        } else {
            runAttack(dt, dir);
        }
        clampToArena();
    }

    private void updateIntro(float dt) {
        introTimer -= dt;
        float p = 1f - Math.max(0f, introTimer) / GameConfig.BOSS_INTRO_TIME;
        float y = MathUtils.lerp(introFromY, targetY, p * p);
        rect.setCenter(rect.x + rect.width * 0.5f, y);
        if (introTimer <= 0f && !landed) {
            landed = true;
            Vector2 c = center();
            hz(Hazard.ring(c.x, c.y, 7f, 6f, 0.9f, kind.color));
            idleTimer = 1.2f;
        }
    }

    private Atk pickAttack() {
        Atk[] pool = switch (kind) {
            case JUGGERNAUT -> JUG;
            case WARLOCK -> WAR;
            case HIVE_QUEEN -> QUE;
            case STORM_WRAITH -> STO;
        };
        Array<Atk> options = new Array<>();
        for (Atk a : pool) {
            if (a.minStage <= stage && a != lastAtk) options.add(a);
        }
        if (options.size == 0) options.add(pool[0]);
        return options.get(MathUtils.random(options.size - 1));
    }

    private float aggression() {
        return 1f + 0.12f * stage + 0.05f * tier;
    }

    private void endAttack() {
        lastAtk = atk;
        atk = Atk.NONE;
        castName = "";
        idleTimer = (kind == Kind.WARLOCK ? 1.5f : 2.0f) * (1f - 0.18f * stage) / (1f + 0.06f * tier);
    }

    private void stun(float seconds) {
        stunTimer = seconds;
        applyStatusFlash(new Color(1f, 0.95f, 0.4f, 1f), 0.3f);
    }

    // ------------------------------------------------------------ idle movement

    private void idleMove(float dt, Vector2 dir, float dist) {
        float a = aggression();
        switch (kind) {
            case JUGGERNAUT -> moveDir(dir, 1.0f * a, dt);
            case WARLOCK -> {
                if (dist > 5.5f) moveDir(dir, 1.2f, dt);
                else if (dist < 3.8f) moveDir(dir, -1.2f, dt);
                else moveDir(new Vector2(-dir.y, dir.x), 0.9f * a * (MathUtils.sin(age * 0.8f) >= 0f ? 1f : -1f), dt);
            }
            case HIVE_QUEEN -> moveDir(dir, 0.55f * a, dt);
            case STORM_WRAITH -> {
                Vector2 tangent = new Vector2(-dir.y, dir.x);
                moveDir(tangent, 1.4f * a, dt);
                if (dist > 5.2f) moveDir(dir, 1.1f, dt);
                else if (dist < 3.8f) moveDir(dir, -1.1f, dt);
            }
        }
    }

    private void moveDir(Vector2 dir, float speed, float dt) {
        move(dir.x * speed * Settings.enemySpeed * dt, dir.y * speed * Settings.enemySpeed * dt);
    }

    // ------------------------------------------------------------ attacks

    private void startAttack(Atk a, Vector2 dir) {
        atk = a;
        atkStep = 0;
        subTimer = 0f;
        counter = 0;
        castName = a.label;
        Vector2 c = center();
        Vector2 p = player.getCenter(new Vector2());

        switch (a) {
            case CHARGE -> {
                counter = 1 + stage;
                atkTimer = 0.85f;
                chargeDir.set(dir);
            }
            case SLAM -> {
                atkTimer = 0.9f;
                hz(Hazard.blast(c.x, c.y, 2.6f, 0.9f, 1.3f, kind.color));
            }
            case ROCKFALL -> atkTimer = 2.6f;
            case SPIRAL -> {
                atkTimer = 3.2f;
                spiralAngle = MathUtils.random(MathUtils.PI2);
            }
            case WELL -> {
                atkTimer = 4.5f;
                subTimer = 0.8f;
                int n = stage >= 1 ? 2 : 1;
                for (int i = 0; i < n; i++) {
                    float ang = MathUtils.random(MathUtils.PI2);
                    float wx = MathUtils.clamp(p.x + MathUtils.cos(ang) * 3.2f, 1.5f, arenaWidth - 1.5f);
                    float wy = MathUtils.clamp(p.y + MathUtils.sin(ang) * 3.2f, 1.5f, arenaHeight - 1.5f);
                    hz(Hazard.well(wx, wy, 2.6f, 4.5f, 3.2f, kind.color));
                }
            }
            case RIFT -> {
                atkTimer = 1.1f + 3.0f;
                int n = stage >= 2 ? 4 : 3;
                float base = MathUtils.random(MathUtils.PI2);
                float dirSign = MathUtils.randomBoolean() ? 1f : -1f;
                for (int i = 0; i < n; i++) {
                    hz(Hazard.beam(c.x, c.y, base + i * MathUtils.PI2 / n, 18f, 0.5f, 1.1f, 3.0f,
                        0.8f * dirSign, 1.0f, kind.color));
                }
            }
            case BLINK_RING -> {
                atkTimer = 0.6f;
                counter = stage >= 1 ? 2 : 1;
                hz(Hazard.blast(c.x, c.y, 1.8f, 0.6f, 1.0f, kind.color));
            }
            case BROOD -> atkTimer = 0.9f;
            case ACID -> atkTimer = 0.8f;
            case SPORES -> {
                atkTimer = stage >= 1 ? 3.0f : 2.2f;
                subTimer = 0.1f;
            }
            case GUARD -> atkTimer = 1.0f;
            case LIGHTNING_RAIN -> {
                atkTimer = 2.8f;
                subTimer = 0.1f;
            }
            case BEAM_SWEEP -> {
                atkTimer = 3.1f;
                float aim = MathUtils.atan2(p.y - c.y, p.x - c.x);
                int n = stage >= 1 ? 2 : 1;
                for (int i = 0; i < n; i++) {
                    hz(Hazard.beam(c.x, c.y, aim - 1.2f + i * MathUtils.PI, 20f, 0.8f, 1.0f, 2.0f, 1.2f, 1.1f, kind.color));
                }
            }
            case ORB_VOLLEY -> {
                atkTimer = 2.2f;
                subTimer = 0.2f;
            }
            case STORM_STEP -> {
                atkTimer = 0.5f;
                counter = 3;
                hz(Hazard.lightning(c.x, c.y, 1.4f, 0.5f, 1.0f, kind.color));
            }
            default -> {
            }
        }
    }

    private void runAttack(float dt, Vector2 dir) {
        atkTimer -= dt;
        subTimer -= dt;
        Vector2 c = center();
        Vector2 p = player.getCenter(new Vector2());

        switch (atk) {
            case CHARGE -> {
                if (atkStep == 0) {
                    if (atkTimer > 0.3f) chargeDir.set(dir);
                    if (atkTimer <= 0f) {
                        atkStep = 1;
                        atkTimer = 0.75f;
                    }
                } else {
                    moveDir(chargeDir, 9f * aggression(), dt);
                    if (atkTimer <= 0f) {
                        counter--;
                        if (stage >= 1) hz(Hazard.ring(c.x, c.y, 3.2f, 5f, 0.7f, kind.color));
                        if (counter > 0) {
                            atkStep = 0;
                            atkTimer = 0.5f;
                            chargeDir.set(dir);
                        } else {
                            stun(1.1f);
                            endAttack();
                        }
                    }
                }
            }
            case SLAM -> {
                if (atkTimer <= 0f) {
                    hz(Hazard.ring(c.x, c.y, 8.5f, 5.5f, 0.9f, kind.color));
                    if (stage >= 1) {
                        for (int i = 0; i < 6; i++) {
                            float a = MathUtils.random(MathUtils.PI2);
                            float r = MathUtils.random(1.5f, 4f);
                            hz(Hazard.blast(p.x + MathUtils.cos(a) * r, p.y + MathUtils.sin(a) * r, 1.1f, 0.8f + 0.12f * i, 0.9f, kind.color));
                        }
                    }
                    impact = true;
                    stun(1.0f);
                    endAttack();
                }
            }
            case ROCKFALL -> {
                moveDir(dir, 0.8f, dt);
                if (subTimer <= 0f) {
                    subTimer = stage >= 2 ? 0.22f : 0.32f;
                    hz(Hazard.blast(p.x + MathUtils.random(-0.6f, 0.6f), p.y + MathUtils.random(-0.6f, 0.6f), 1.0f, 0.85f, 0.9f, kind.color));
                }
                if (atkTimer <= 0f) endAttack();
            }
            case SPIRAL -> {
                moveDir(new Vector2(-dir.y, dir.x), 0.4f, dt);
                if (subTimer <= 0f) {
                    subTimer = 0.07f;
                    spiralAngle += 0.42f;
                    shot(spiralAngle, 3.4f, 0.1f);
                    if (stage >= 1) shot(spiralAngle + MathUtils.PI, 3.4f, 0.1f);
                    if (stage >= 2) {
                        shot(spiralAngle + MathUtils.HALF_PI, 3.4f, 0.1f);
                        shot(spiralAngle - MathUtils.HALF_PI, 3.4f, 0.1f);
                    }
                }
                if (atkTimer <= 0f) endAttack();
            }
            case WELL -> {
                if (subTimer <= 0f) {
                    subTimer = 1.0f;
                    fanShots(3, 0.3f, 4.2f, MathUtils.atan2(p.y - c.y, p.x - c.x));
                }
                if (atkTimer <= 0f) endAttack();
            }
            case RIFT -> {
                if (atkTimer <= 0f) endAttack();
            }
            case BLINK_RING -> {
                if (atkStep == 0 && atkTimer <= 0f) {
                    blink();
                    Vector2 n = center();
                    ringShots(12, 2.8f, MathUtils.random(MathUtils.PI2));
                    fanShots(3, 0.3f, 4.4f, MathUtils.atan2(p.y - n.y, p.x - n.x));
                    counter--;
                    if (counter > 0) {
                        hz(Hazard.blast(n.x, n.y, 1.8f, 0.6f, 1.0f, kind.color));
                        atkTimer = 0.6f;
                    } else {
                        atkStep = 1;
                        atkTimer = 0.5f;
                    }
                } else if (atkStep == 1 && atkTimer <= 0f) {
                    endAttack();
                }
            }
            case BROOD -> {
                if (atkTimer <= 0f) {
                    int n = 4 + stage * 2;
                    for (int i = 0; i < n; i++) {
                        float a = MathUtils.random(MathUtils.PI2);
                        spawn(Type.MINI, c.x + MathUtils.cos(a) * rect.width * 0.5f, c.y + MathUtils.sin(a) * rect.width * 0.5f);
                    }
                    hz(Hazard.ring(c.x, c.y, 2.5f, 4f, 0.5f, kind.color));
                    endAttack();
                }
            }
            case ACID -> {
                if (atkTimer <= 0f) {
                    int n = 3 + stage;
                    for (int i = 0; i < n; i++) {
                        float a = MathUtils.random(MathUtils.PI2);
                        float r = i == 0 ? 0f : MathUtils.random(0.8f, 2.5f);
                        hz(Hazard.pool(p.x + MathUtils.cos(a) * r, p.y + MathUtils.sin(a) * r, 1.2f, 0.9f, 6f, 0.5f, kind.color, true));
                    }
                    endAttack();
                }
            }
            case SPORES -> {
                moveDir(dir, 0.4f, dt);
                if (subTimer <= 0f) {
                    subTimer = 0.6f;
                    ringShots(14, 2.1f, (counter++ % 2) * 0.22f);
                    if (stage >= 1) fanShots(3, 0.3f, 3.6f, MathUtils.atan2(p.y - c.y, p.x - c.x));
                }
                if (atkTimer <= 0f) endAttack();
            }
            case GUARD -> {
                if (atkTimer <= 0f) {
                    spawn(Type.SPITTER, c.x - rect.width * 0.6f, c.y);
                    spawn(stage >= 2 ? Type.BOMBER : Type.SPITTER, c.x + rect.width * 0.6f, c.y);
                    if (stage >= 2) spawn(Type.SUMMONER, c.x, c.y - rect.height * 0.6f);
                    endAttack();
                }
            }
            case LIGHTNING_RAIN -> {
                if (subTimer <= 0f) {
                    subTimer = stage >= 1 ? 0.12f : 0.16f;
                    boolean aimed = MathUtils.randomBoolean(0.3f);
                    float jx = aimed ? 0f : MathUtils.random(-1.2f, 1.2f);
                    float jy = aimed ? 0f : MathUtils.random(-1.2f, 1.2f);
                    hz(Hazard.lightning(p.x + jx, p.y + jy, 0.95f, 0.8f, 1.0f, kind.color));
                }
                if (atkTimer <= 0f) endAttack();
            }
            case BEAM_SWEEP -> {
                if (atkTimer <= 0f) endAttack();
            }
            case ORB_VOLLEY -> {
                idleMove(dt, dir, 4.5f);
                if (subTimer <= 0f) {
                    subTimer = 0.2f;
                    int n = stage >= 2 ? 5 : stage >= 1 ? 3 : 1;
                    fanShots(n, 0.22f, 6.5f, MathUtils.atan2(p.y - c.y, p.x - c.x));
                }
                if (atkTimer <= 0f) endAttack();
            }
            case STORM_STEP -> {
                if (atkTimer <= 0f) {
                    blink();
                    Vector2 n = center();
                    ringShots(8, 2.8f, MathUtils.random(MathUtils.PI2));
                    counter--;
                    if (counter > 0) {
                        hz(Hazard.lightning(n.x, n.y, 1.4f, 0.5f, 1.0f, kind.color));
                        atkTimer = 0.5f;
                    } else {
                        endAttack();
                    }
                }
            }
            default -> endAttack();
        }
    }

    private void blink() {
        Vector2 p = player.getCenter(new Vector2());
        for (int tries = 0; tries < 12; tries++) {
            float nx = MathUtils.random(rect.width / 2f, Math.max(rect.width / 2f + 0.1f, arenaWidth - rect.width / 2f));
            float ny = MathUtils.random(rect.height / 2f, Math.max(rect.height / 2f + 0.1f, arenaHeight - rect.height / 2f));
            if (Vector2.dst(nx, ny, p.x, p.y) > 3.5f) {
                rect.setCenter(nx, ny);
                break;
            }
        }
        applyStatusFlash(kind.color, 0.4f);
    }

    // ------------------------------------------------------------ helpers

    private Vector2 center() {
        return getCenter(new Vector2());
    }

    private void hz(Hazard h) {
        if (hazardSink != null) hazardSink.add(h);
    }

    private void spawn(Type t, float x, float y) {
        if (spawnSink == null) return;
        Enemy e = new Enemy(x - baseSize * t.sizeMultiplier * 0.5f, y - baseSize * t.sizeMultiplier * 0.5f,
            sprites.enemy(t), player, t);
        spawnSink.add(e);
    }

    private void shot(float angle, float speed, float radius) {
        if (projectileSink == null) return;
        Vector2 c = center();
        projectileSink.add(new EnemyProjectile(c.x, c.y, angle, speed, GameConfig.PROJECTILE_DAMAGE, kind.color, radius));
    }

    private void ringShots(int n, float speed, float offset) {
        for (int i = 0; i < n; i++) {
            shot(offset + i * MathUtils.PI2 / n, speed, 0.12f);
        }
    }

    private void fanShots(int n, float spread, float speed, float base) {
        for (int i = 0; i < n; i++) {
            shot(base + (i - (n - 1) / 2f) * spread, speed, 0.12f);
        }
    }

    // ------------------------------------------------------------ rendering

    @Override
    protected void computePose() {
        float t = age;
        poseSx = 1f;
        poseSy = 1f;
        poseRot = 0f;
        poseY = 0f;

        switch (kind) {
            case JUGGERNAUT -> {
                float step = Math.abs(MathUtils.sin(t * 3f));
                poseY = step * 0.05f * rect.height;
                poseSx = 1f + 0.02f * (1f - step);
            }
            case WARLOCK, STORM_WRAITH, HIVE_QUEEN ->
                poseY = (0.5f + 0.5f * MathUtils.sin(t * 2f)) * 0.1f * rect.height;
        }

        if (introTimer > 0f) {
            float p = 1f - introTimer / GameConfig.BOSS_INTRO_TIME;
            poseSy = 1f + 0.15f * p;
            poseSx = 1f - 0.08f * p;
        } else if (landed && age < 0f) {
            poseSy = 1f;
        }

        if (atk == Atk.CHARGE && atkStep == 0) {
            poseSx = 1.1f + 0.03f * MathUtils.sin(t * 50f);
            poseSy = 0.9f;
        } else if (atk == Atk.CHARGE) {
            poseSx = 1.3f;
            poseSy = 0.85f;
        } else if (atk == Atk.SLAM) {
            float p = 1f - Math.max(0f, atkTimer) / 0.9f;
            poseY += p * 0.3f * rect.height;
            poseSy = 1f + 0.15f * p;
            poseSx = 1f - 0.08f * p;
        } else if (stunTimer > 0f) {
            poseSy = 0.86f;
            poseSx = 1.1f;
            poseRot = 4f * MathUtils.sin(t * 14f);
            poseY = 0f;
        } else if (atk == Atk.BROOD || atk == Atk.GUARD || atk == Atk.ACID) {
            float s = 1f + 0.05f * MathUtils.sin(t * 25f);
            poseSx = s;
            poseSy = s;
        }
    }

    @Override
    protected Color resolveTint() {
        if (statusTimer > 0f && statusColor != null) {
            tint.set(((int) (statusTimer * 40f)) % 2 == 0 ? Color.WHITE : statusColor);
        } else if (hitFlashTimer > 0f) {
            tint.set(Color.WHITE);
        } else if (stunTimer > 0f) {
            tint.set(1f, 1f, 0.7f, 1f);
        } else if (atk == Atk.CHARGE && atkStep == 0) {
            tint.set(((int) (age * 16f)) % 2 == 0 ? Color.RED : Color.WHITE);
        } else {
            tint.set(Color.WHITE);
            if (stage >= 1) {
                float pulse = 0.5f + 0.5f * MathUtils.sin(age * 8f);
                tint.lerp(1f, 0.3f, 0.3f, 1f, (stage == 1 ? 0.18f : 0.32f) * pulse);
            }
            if (frozenVisual) tint.lerp(0.5f, 0.8f, 1f, 1f, 0.5f);
        }
        tint.a = visibility();
        return tint;
    }

    @Override
    public void drawGround(ShapeRenderer sr) {
        float cx = rect.x + rect.width * 0.5f;
        float cy = rect.y + rect.height * 0.5f;
        float pulse = 0.5f + 0.5f * MathUtils.sin(age * 3f);

        if (introTimer > 0f) {
            float p = 1f - introTimer / GameConfig.BOSS_INTRO_TIME;
            sr.setColor(kind.color.r, kind.color.g, kind.color.b, 0.10f + 0.20f * p);
            sr.circle(cx, targetY, 7f * p, 40);
            sr.setColor(0f, 0f, 0f, 0.35f * p);
            sr.ellipse(cx - rect.width * 0.4f * p, targetY - rect.height * 0.5f, rect.width * 0.8f * p, rect.height * 0.25f * p);
            return;
        }

        sr.setColor(0f, 0f, 0f, 0.32f);
        sr.ellipse(rect.x + rect.width * 0.1f, rect.y - rect.height * 0.04f, rect.width * 0.8f, rect.height * 0.22f);

        sr.setColor(kind.color.r, kind.color.g, kind.color.b, 0.10f + 0.08f * pulse);
        sr.circle(cx, cy, rect.width * (0.8f + 0.08f * pulse), 32);

        if (stage >= 1) {
            sr.setColor(1f, 0.1f, 0.1f, stage == 1 ? 0.12f : 0.2f);
            sr.circle(cx, cy, rect.width * 1.05f, 32);
        }
        if (atk == Atk.CHARGE) {
            // Warning phase: the lane FLASHES (faster as the charge nears). During the charge itself it is faint.
            float flash = ((int) (age * (10f + 8f * (1f - Math.max(0f, atkTimer)))) % 2 == 0) ? 1f : 0.15f;
            float alpha = atkStep == 0 ? 0.20f * flash : 0.07f;
            sr.setColor(1f, 0.2f, 0.2f, alpha);
            sr.rectLine(cx, cy, cx + chargeDir.x * 14f, cy + chargeDir.y * 14f, rect.width * 0.9f);
            if (atkStep == 0) {
                sr.setColor(1f, 0.3f, 0.3f, 0.35f + 0.5f * flash);
                float nx = -chargeDir.y * rect.width * 0.45f;
                float ny = chargeDir.x * rect.width * 0.45f;
                sr.rectLine(cx + nx, cy + ny, cx + nx + chargeDir.x * 14f, cy + ny + chargeDir.y * 14f, 0.05f);
                sr.rectLine(cx - nx, cy - ny, cx - nx + chargeDir.x * 14f, cy - ny + chargeDir.y * 14f, 0.05f);
            }
        }
    }

    @Override
    public void drawOverlay(ShapeRenderer sr) {
        if (stunTimer > 0f) {
            float cx = rect.x + rect.width * 0.5f;
            float top = rect.y + rect.height * 0.95f;
            for (int i = 0; i < 4; i++) {
                float a = age * 4f + i * MathUtils.HALF_PI;
                float sx = cx + MathUtils.cos(a) * rect.width * 0.28f;
                float sy = top + MathUtils.sin(a) * 0.12f;
                sr.setColor(1f, 0.95f, 0.3f, 0.95f);
                for (int k = 0; k < 2; k++) {
                    float ang = k * MathUtils.PI / 4f + age * 3f;
                    sr.triangle(sx + MathUtils.cos(ang) * 0.14f, sy + MathUtils.sin(ang) * 0.14f,
                        sx - MathUtils.cos(ang) * 0.14f, sy - MathUtils.sin(ang) * 0.14f,
                        sx - MathUtils.sin(ang) * 0.04f, sy + MathUtils.cos(ang) * 0.04f);
                }
            }
        }
    }

    // ------------------------------------------------------------ state

    @Override
    public boolean takeDamage(float amount) {
        if (introTimer > 0f) return false;
        if (stunTimer > 0f) amount *= 1.5f;
        return super.takeDamage(amount);
    }

    @Override
    public void knockback(float dx, float dy) {
        super.knockback(dx * 0.1f, dy * 0.1f);
    }

    public boolean isIntroducing() {
        return introTimer > 0f;
    }

    public boolean isStunned() {
        return stunTimer > 0f;
    }

    public String getCastName() {
        return introTimer > 0f ? "" : (stunTimer > 0f ? "STUNNED - HIT IT!" : castName);
    }

    public int getStage() {
        return stage;
    }

    /** Returns the new stage once when the boss escalates, otherwise -1. */
    public int consumeStageEvent() {
        int e = stageEvent;
        stageEvent = -1;
        return e;
    }

    public boolean consumeImpact() {
        boolean i = impact;
        impact = false;
        return i;
    }

    public boolean consumeLanding() {
        if (landed && !landedReported) {
            landedReported = true;
            return true;
        }
        return false;
    }

    private boolean landedReported;

    public Kind getKind() {
        return kind;
    }

    public int getTier() {
        return tier;
    }
}
