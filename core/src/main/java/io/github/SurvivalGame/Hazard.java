package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Telegraphed area attacks used by bosses and special enemies. Every hazard first WARNS (so the player can react),
 * then hurts. Dashing (i-frames) dodges all of them.
 */
public class Hazard {
    public enum Kind { BLAST, POOL, RING, BEAM, WELL }

    private static final float BLAST_FLASH = 0.3f;
    private static final float PLAYER_RADIUS = 0.25f;
    private static final float POOL_TICK = 0.7f;

    /** 1 = full telegraph fill; GameScreen lowers it when the screen is crowded (outlines stay crisp). */
    public static float fillScale = 1f;

    private final Kind kind;
    private float x, y;
    private final float radius;          // blast/pool/well radius, ring max radius, beam length
    private final Color color;
    private final float damage;
    private float delay;                 // telegraph time left
    private final float totalDelay;
    private float life;
    private final float activeTime;
    private final float width;           // beam width
    private final float sweep;           // beam rotation speed (rad/s) while active
    private float angle;
    private float speed;                 // ring expansion speed / well pull strength
    private float ringR;
    private float tick;
    private float time;
    private boolean triggered;
    private boolean hurtOnce;
    private boolean lightning;
    private boolean slows;
    private boolean done;
    private Enemy owner;
    private boolean tracking;
    private float lockTime;
    private final float[] seed = new float[8];
    private final Vector2 tmp = new Vector2();

    private Hazard(Kind kind, float x, float y, float radius, Color color, float damage,
                   float delay, float activeTime, float width, float sweep, float angle) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = new Color(color.r, color.g, color.b, 1f);
        this.damage = damage;
        this.delay = delay;
        this.totalDelay = Math.max(0.01f, delay);
        this.activeTime = activeTime;
        this.life = activeTime;
        this.width = width;
        this.sweep = sweep;
        this.angle = angle;
        for (int i = 0; i < seed.length; i++) {
            seed[i] = MathUtils.random();
        }
    }

    // ------------------------------------------------------------ factories

    /** Circle that fills up, then explodes. */
    public static Hazard blast(float x, float y, float radius, float delay, float damage, Color color) {
        return new Hazard(Kind.BLAST, x, y, radius, color, damage, delay, 0f, 0f, 0f, 0f);
    }

    /** Same as blast but visualised as a lightning strike from the sky. */
    public static Hazard lightning(float x, float y, float radius, float delay, float damage, Color color) {
        Hazard h = blast(x, y, radius, delay, damage, color);
        h.lightning = true;
        return h;
    }

    /** Lingering puddle: damages every 0.7s while you stand in it, optionally slows you. */
    public static Hazard pool(float x, float y, float radius, float delay, float duration, float tickDamage,
                              Color color, boolean slows) {
        Hazard h = new Hazard(Kind.POOL, x, y, radius, color, tickDamage, delay, duration, 0f, 0f, 0f);
        h.slows = slows;
        return h;
    }

    /** Expanding shockwave. Dash through it (or keep moving away) to avoid it. */
    public static Hazard ring(float x, float y, float maxRadius, float speed, float damage, Color color) {
        Hazard h = new Hazard(Kind.RING, x, y, maxRadius, color, damage, 0f, 0f, 0f, 0f, 0f);
        h.speed = speed;
        h.ringR = 0.2f;
        return h;
    }

    /** Fixed or sweeping laser. */
    public static Hazard beam(float x, float y, float angle, float length, float width, float delay,
                              float active, float sweep, float damage, Color color) {
        return new Hazard(Kind.BEAM, x, y, length, color, damage, delay, active, width, sweep, angle);
    }

    /** Laser that follows its owner and aims at the player until the final {@code lockTime} seconds. */
    public static Hazard trackingBeam(Enemy owner, float length, float width, float delay, float lockTime,
                                      float active, float damage, Color color) {
        Hazard h = new Hazard(Kind.BEAM, 0f, 0f, length, color, damage, delay, active, width, 0f, 0f);
        h.owner = owner;
        h.tracking = true;
        h.lockTime = lockTime;
        Vector2 c = owner.getCenter(new Vector2());
        h.x = c.x;
        h.y = c.y;
        return h;
    }

    /** Gravity well that drags the player toward its centre. */
    public static Hazard well(float x, float y, float radius, float duration, float pull, Color color) {
        Hazard h = new Hazard(Kind.WELL, x, y, radius, color, 0.8f, 0.5f, duration, 0f, 0f, 0f);
        h.speed = pull;
        return h;
    }

    // ------------------------------------------------------------ simulation

    /** @return true if the player was hurt this frame. */
    public boolean update(float dt, Player player) {
        time += dt;

        if (owner != null) {
            if (owner.isDead()) {
                done = true;
                return false;
            }
            owner.getCenter(tmp);
            x = tmp.x;
            y = tmp.y;
        }

        player.getCenter(tmp);
        float px = tmp.x;
        float py = tmp.y;
        boolean hurt = false;

        switch (kind) {
            case BLAST -> {
                if (!triggered) {
                    delay -= dt;
                    if (delay <= 0f) {
                        triggered = true;
                        life = BLAST_FLASH;
                        if (Vector2.dst(x, y, px, py) <= radius + PLAYER_RADIUS) {
                            hurt = strike(player, damage);
                        }
                    }
                } else {
                    life -= dt;
                    if (life <= 0f) done = true;
                }
            }
            case POOL -> {
                if (delay > 0f) {
                    delay -= dt;
                } else {
                    life -= dt;
                    tick -= dt;
                    if (Vector2.dst(x, y, px, py) <= radius + 0.1f) {
                        if (slows) player.applySlow(0.4f);
                        if (tick <= 0f) {
                            tick = POOL_TICK;
                            hurt = strike(player, damage);
                        }
                    }
                    if (life <= 0f) done = true;
                }
            }
            case RING -> {
                ringR += speed * dt;
                float dist = Vector2.dst(x, y, px, py);
                if (!hurtOnce && Math.abs(dist - ringR) <= 0.22f + PLAYER_RADIUS) {
                    hurtOnce = true;
                    hurt = strike(player, damage);
                }
                if (ringR >= radius) done = true;
            }
            case BEAM -> {
                if (!triggered) {
                    if (tracking && delay > lockTime) {
                        angle = MathUtils.atan2(py - y, px - x);
                    }
                    delay -= dt;
                    if (delay <= 0f) {
                        triggered = true;
                        life = activeTime;
                    }
                } else {
                    life -= dt;
                    angle += sweep * dt;
                    float ex = x + MathUtils.cos(angle) * radius;
                    float ey = y + MathUtils.sin(angle) * radius;
                    if (Intersector.distanceSegmentPoint(x, y, ex, ey, px, py) <= width * 0.5f + PLAYER_RADIUS) {
                        hurt = strike(player, damage);
                    }
                    if (life <= 0f) done = true;
                }
            }
            case WELL -> {
                delay -= dt;
                life -= dt;
                float dist = Vector2.dst(x, y, px, py);
                float reach = radius * 1.8f;
                if (delay <= 0f && dist < reach && dist > 0.001f) {
                    float strength = speed * (1f - dist / reach) + speed * 0.25f;
                    player.applyPull((x - px) / dist * strength * dt, (y - py) / dist * strength * dt);
                }
                if (dist < 0.5f) {
                    tick -= dt;
                    if (tick <= 0f) {
                        tick = POOL_TICK;
                        hurt = strike(player, damage);
                    }
                }
                if (life <= 0f) done = true;
            }
        }
        return hurt;
    }

    private boolean strike(Player player, float amount) {
        float before = player.getLife();
        player.subLife(amount * Settings.difficulty.damageMult);
        return player.getLife() < before;
    }

    public boolean isDone() {
        return done;
    }

    // ------------------------------------------------------------ rendering

    /** Assumes the ShapeRenderer is already begun in Filled mode (world projection). */
    public void draw(ShapeRenderer sr) {
        float cr = color.r;
        float cg = color.g;
        float cb = color.b;
        float blink = flashFactor();

        switch (kind) {
            case BLAST -> {
                if (!triggered) {
                    float p = 1f - Math.max(0f, delay) / totalDelay;
                    sr.setColor(cr, cg, cb, (0.08f + 0.16f * p) * fillScale * blink);
                    sr.circle(x, y, radius, 40);
                    sr.setColor(cr, cg, cb, (0.40f + 0.30f * p) * fillScale * blink);
                    sr.circle(x, y, radius * p, 40);
                    sr.setColor(cr, cg, cb, 0.9f);
                    ring(sr, x, y, radius, 0.045f, 40);
                    float rot = time * 2f;
                    for (int k = 0; k < 4; k++) {
                        float a = rot + k * MathUtils.HALF_PI;
                        sr.triangle(
                            x + MathUtils.cos(a) * radius * 1.08f, y + MathUtils.sin(a) * radius * 1.08f,
                            x + MathUtils.cos(a + 0.12f) * radius * 0.9f, y + MathUtils.sin(a + 0.12f) * radius * 0.9f,
                            x + MathUtils.cos(a - 0.12f) * radius * 0.9f, y + MathUtils.sin(a - 0.12f) * radius * 0.9f);
                    }
                } else {
                    float f = Math.max(0f, life) / BLAST_FLASH;
                    sr.setColor(1f, 1f, 1f, 0.75f * f * fillScale);
                    sr.circle(x, y, radius * (1.15f - 0.15f * f), 40);
                    sr.setColor(cr, cg, cb, 0.55f * f * fillScale);
                    sr.circle(x, y, radius * 1.25f, 40);
                    if (lightning) {
                        float topY = y + 9f;
                        float px = x;
                        float py = y;
                        int segs = 7;
                        for (int i = 1; i <= segs; i++) {
                            float t = (float) i / segs;
                            float nx = x + (seed[i % seed.length] - 0.5f) * 0.9f * (1f - t);
                            float ny = y + (topY - y) * t;
                            sr.setColor(1f, 1f, 1f, f);
                            sr.rectLine(px, py, nx, ny, 0.09f * f + 0.02f);
                            sr.setColor(cr, cg, cb, 0.5f * f);
                            sr.rectLine(px, py, nx, ny, 0.22f * f);
                            px = nx;
                            py = ny;
                        }
                    }
                }
            }
            case POOL -> {
                if (delay > 0f) {
                    float p = 1f - delay / totalDelay;
                    sr.setColor(cr, cg, cb, (0.08f + 0.1f * p) * blink);
                    sr.circle(x, y, radius * (0.4f + 0.6f * p), 32);
                    sr.setColor(cr, cg, cb, 0.7f);
                    ring(sr, x, y, radius, 0.035f, 32);
                } else {
                    float fade = Math.min(1f, Math.min(life, 1f));
                    float pulse = 0.5f + 0.5f * MathUtils.sin(time * 4f);
                    sr.setColor(cr * 0.6f, cg * 0.6f, cb * 0.6f, (0.28f + 0.1f * pulse) * fade * fillScale);
                    sr.circle(x, y, radius, 36);
                    sr.setColor(cr, cg, cb, 0.18f * fade * fillScale);
                    sr.circle(x, y, radius * 0.7f, 28);
                    for (int i = 0; i < 6; i++) {
                        float a = seed[i] * MathUtils.PI2 + time * 0.3f;
                        float r = radius * (0.2f + 0.65f * seed[(i + 3) % 8]);
                        float bp = (time * 0.8f + seed[i]) % 1f;
                        sr.setColor(cr, cg, cb, 0.6f * (1f - bp) * fade);
                        sr.circle(x + MathUtils.cos(a) * r, y + MathUtils.sin(a) * r + bp * 0.25f, 0.05f + 0.07f * bp, 8);
                    }
                    sr.setColor(cr, cg, cb, 0.85f * fade);
                    ring(sr, x, y, radius, 0.04f, 36);
                }
            }
            case RING -> {
                float fade = 1f - 0.6f * (ringR / radius);
                sr.setColor(cr, cg, cb, 0.18f * fade * fillScale);
                ring(sr, x, y, ringR, 0.55f, 56);
                sr.setColor(cr, cg, cb, 0.95f * fade);
                ring(sr, x, y, ringR, 0.24f, 56);
                sr.setColor(1f, 1f, 1f, 0.8f * fade);
                ring(sr, x, y, ringR, 0.07f, 56);
            }
            case BEAM -> {
                float ex = x + MathUtils.cos(angle) * radius;
                float ey = y + MathUtils.sin(angle) * radius;
                if (!triggered) {
                    float p = 1f - Math.max(0f, delay) / totalDelay;
                    boolean flick = delay < 0.4f && ((int) (time * 28f)) % 2 == 0;
                    sr.setColor(cr, cg, cb, flick ? 0.9f : 0.30f + 0.35f * p);
                    sr.rectLine(x, y, ex, ey, 0.05f + 0.05f * p);
                    sr.setColor(cr, cg, cb, (0.10f + 0.10f * p) * fillScale * blink);
                    sr.rectLine(x, y, ex, ey, width * p);
                } else {
                    float f = Math.min(1f, life * 6f);
                    sr.setColor(cr, cg, cb, 0.35f * f * fillScale);
                    sr.rectLine(x, y, ex, ey, width * 1.5f);
                    sr.setColor(cr, cg, cb, 0.9f * f);
                    sr.rectLine(x, y, ex, ey, width);
                    sr.setColor(1f, 1f, 1f, f);
                    sr.rectLine(x, y, ex, ey, width * 0.4f);
                    sr.setColor(1f, 1f, 1f, 0.8f * f);
                    sr.circle(x, y, width * 0.8f, 16);
                }
            }
            case WELL -> {
                float fadeIn = Math.min(1f, time * 2.5f);
                float fadeOut = Math.min(1f, Math.max(0f, life) * 2f);
                float f = Math.min(fadeIn, fadeOut);
                sr.setColor(cr * 0.4f, cg * 0.2f, cb * 0.6f, 0.16f * f * fillScale);
                sr.circle(x, y, radius * 1.8f, 40);
                sr.setColor(cr, cg, cb, 0.10f * f * fillScale);
                sr.circle(x, y, radius, 36);
                for (int arm = 0; arm < 4; arm++) {
                    for (int i = 0; i < 14; i++) {
                        float t = i / 13f;
                        float r = radius * 1.6f * (1f - t) + 0.25f;
                        float a = arm * MathUtils.HALF_PI + t * 3.2f - time * 2.2f;
                        sr.setColor(cr, cg, cb, (0.15f + 0.65f * t) * f);
                        sr.circle(x + MathUtils.cos(a) * r, y + MathUtils.sin(a) * r, 0.04f + 0.06f * t, 8);
                    }
                }
                sr.setColor(0.03f, 0.0f, 0.06f, 0.95f * f);
                sr.circle(x, y, 0.4f, 20);
                sr.setColor(cr, cg, cb, 0.9f * f);
                ring(sr, x, y, 0.42f, 0.05f, 20);
            }
        }
    }

    /** Blinking factor for the warning phase: slow at first, rapid right before it fires. */
    private float flashFactor() {
        if (triggered || kind == Kind.RING || kind == Kind.WELL) return 1f;
        if (kind == Kind.POOL && delay <= 0f) return 1f;
        float p = 1f - Math.max(0f, delay) / totalDelay;
        float rate = 6f + 16f * p;
        return 0.30f + 0.70f * (0.5f + 0.5f * MathUtils.sin(time * rate));
    }

    /**
     * Thin, bright outlines drawn ABOVE sprites and crowds, so every danger zone stays readable
     * even when its soft fill has been dimmed.
     */
    public void drawTop(ShapeRenderer sr) {
        float cr = color.r;
        float cg = color.g;
        float cb = color.b;

        switch (kind) {
            case BLAST -> {
                if (!triggered) {
                    float p = 1f - Math.max(0f, delay) / totalDelay;
                    sr.setColor(cr, cg, cb, 0.45f + 0.5f * flashFactor());
                    ring(sr, x, y, radius, 0.05f, 40);
                    sr.setColor(1f, 1f, 1f, 0.45f + 0.4f * p);
                    ring(sr, x, y, Math.max(0.05f, radius * p), 0.03f, 40);
                }
            }
            case POOL -> {
                sr.setColor(cr, cg, cb, delay > 0f ? 0.7f : 0.9f);
                ring(sr, x, y, radius, 0.04f, 36);
            }
            case RING -> {
                float fade = 1f - 0.6f * (ringR / radius);
                sr.setColor(1f, 1f, 1f, 0.9f * fade);
                ring(sr, x, y, ringR, 0.08f, 56);
            }
            case BEAM -> {
                float ex = x + MathUtils.cos(angle) * radius;
                float ey = y + MathUtils.sin(angle) * radius;
                float nx = -MathUtils.sin(angle) * width * 0.5f;
                float ny = MathUtils.cos(angle) * width * 0.5f;
                if (!triggered) {
                    sr.setColor(cr, cg, cb, 0.9f);
                    sr.rectLine(x, y, ex, ey, 0.05f);
                } else {
                    sr.setColor(1f, 1f, 1f, 0.9f);
                    sr.rectLine(x + nx, y + ny, ex + nx, ey + ny, 0.04f);
                    sr.rectLine(x - nx, y - ny, ex - nx, ey - ny, 0.04f);
                }
            }
            case WELL -> {
                sr.setColor(cr, cg, cb, 0.55f);
                ring(sr, x, y, radius, 0.03f, 40);
                sr.setColor(1f, 1f, 1f, 0.8f);
                ring(sr, x, y, 0.45f, 0.05f, 20);
            }
        }
    }

    private static void ring(ShapeRenderer sr, float cx, float cy, float r, float thickness, int segments) {
        float step = MathUtils.PI2 / segments;
        for (int i = 0; i < segments; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            sr.rectLine(cx + MathUtils.cos(a0) * r, cy + MathUtils.sin(a0) * r,
                cx + MathUtils.cos(a1) * r, cy + MathUtils.sin(a1) * r, thickness);
        }
    }
}
