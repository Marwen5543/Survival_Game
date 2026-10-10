package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * All the animated visuals for the player's abilities — no more flat full-screen tints.
 *
 *  Shield     rotating hex barrier with orbiting shards, ripples where it blocks a hit
 *  Haste      afterimages + wind streaks behind you
 *  Magnet     rings contracting toward you and motes spiralling in
 *  Storm      a local thundercloud following you, rain under it, bolts that fall from it
 *  Freeze     frost-nova shockwave with ice spikes, crystals orbiting you, drifting snow
 *  Supernova  charge-up spiral, then a multi-ring blast with rays and a scorch mark
 *  Overdrive  flame aura, orbiting sparks and an RPM gauge ring
 */
public class AbilityFx {
    private enum Kind { GLYPH, RING_BURST, FROST_NOVA, NUKE_CHARGE, NUKE_BLAST, SCORCH, RIPPLE, EXPLOSION_SERIES }

    private static final class Fx {
        final Kind kind;
        final float x, y, dur, size;
        final Color color;
        final AbilityPickup.Type type;
        float t, angle, timer;
        boolean burst;

        Fx(Kind kind, float x, float y, float dur, float size, Color color, AbilityPickup.Type type, float angle) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.dur = dur;
            this.size = size;
            this.color = color;
            this.type = type;
            this.angle = angle;
        }
    }

    private static final int TRAIL = 8;

    private final Array<Fx> list = new Array<>();
    private final Array<Particle> particles;
    private final Vector2 c = new Vector2();
    private final Vector2 prev = new Vector2();
    private final Vector2 vel = new Vector2();
    private final Vector2 dir = new Vector2();
    private final float[] trailX = new float[TRAIL];
    private final float[] trailY = new float[TRAIL];
    private final float[] trailLife = new float[TRAIL];
    private int trailHead;
    private float trailTimer;
    private float time;
    private float stormFlash;
    private float cloudX, cloudY;
    private boolean hasPrev;

    public AbilityFx(Array<Particle> particles) {
        this.particles = particles;
    }

    public void clear() {
        list.clear();
        for (int i = 0; i < TRAIL; i++) trailLife[i] = 0f;
        stormFlash = 0f;
        hasPrev = false;
    }

    // ------------------------------------------------------------ spawners

    public void activate(AbilityPickup.Type type, Player player) {
        player.getCenter(c);
        list.add(new Fx(Kind.GLYPH, c.x, c.y, 0.9f, 1f, type.color, type, 0f));
        if (type == AbilityPickup.Type.FREEZE) {
            list.add(new Fx(Kind.FROST_NOVA, c.x, c.y, 0.9f, 14f, type.color, type, 0f));
        } else if (type == AbilityPickup.Type.NUKE) {
            list.add(new Fx(Kind.NUKE_CHARGE, c.x, c.y, GameConfig.NUKE_CHARGE_TIME, 1f, type.color, type, 0f));
        }
    }

    public void nukeBlast(float x, float y, Color color, float size) {
        list.add(new Fx(Kind.NUKE_BLAST, x, y, 1.2f, size, color, null, MathUtils.random(MathUtils.PI2)));
        list.add(new Fx(Kind.SCORCH, x, y, 2.4f, 1.6f, color, null, MathUtils.random(MathUtils.PI2)));
    }

    public void ringBurst(float x, float y, Color color, float size, float dur) {
        list.add(new Fx(Kind.RING_BURST, x, y, dur, size, color, null, 0f));
    }

    public void explosionSeries(float x, float y, Color color, float radius, float dur) {
        Fx f = new Fx(Kind.EXPLOSION_SERIES, x, y, dur, radius, color, null, 0f);
        list.add(f);
    }

    /** Flash on the shield barrier where a projectile was absorbed. */
    public void ripple(float angle) {
        list.add(new Fx(Kind.RIPPLE, 0f, 0f, 0.45f, 1f, AbilityPickup.Type.SHIELD.color, null, angle));
    }

    public void stormFlash() {
        stormFlash = 1f;
    }

    public float getCloudY() {
        return cloudY;
    }

    // ------------------------------------------------------------ update

    public void update(float dt, Player player) {
        time += dt;
        player.getCenter(c);

        if (hasPrev && dt > 0.0001f) {
            vel.set((c.x - prev.x) / dt, (c.y - prev.y) / dt);
        } else {
            vel.setZero();
        }
        prev.set(c);
        hasPrev = true;

        trailTimer -= dt;
        if (trailTimer <= 0f) {
            trailTimer = 0.04f;
            trailHead = (trailHead + 1) % TRAIL;
            trailX[trailHead] = c.x;
            trailY[trailHead] = c.y;
            trailLife[trailHead] = 0.32f;
        }
        for (int i = 0; i < TRAIL; i++) {
            if (trailLife[i] > 0f) trailLife[i] -= dt;
        }

        if (stormFlash > 0f) stormFlash = Math.max(0f, stormFlash - dt * 3f);
        cloudX = c.x + MathUtils.sin(time * 0.9f) * 0.3f;
        cloudY = c.y + 1.6f;

        for (int i = list.size - 1; i >= 0; i--) {
            Fx f = list.get(i);
            f.t += dt;

            switch (f.kind) {
                case FROST_NOVA -> {
                    float r = f.size * easeOut(f.t / f.dur);
                    for (int k = 0; k < 2; k++) {
                        float a = MathUtils.random(MathUtils.PI2);
                        Particle.burst(particles, f.x + MathUtils.cos(a) * r, f.y + MathUtils.sin(a) * r, f.color, 1);
                    }
                }
                case NUKE_BLAST -> {
                    if (!f.burst) {
                        f.burst = true;
                        Particle.burst(particles, f.x, f.y, f.color, 30);
                        Particle.burst(particles, f.x, f.y, Color.WHITE, 12);
                    }
                }
                case EXPLOSION_SERIES -> {
                    f.timer -= dt;
                    if (f.timer <= 0f) {
                        f.timer = 0.09f;
                        float a = MathUtils.random(MathUtils.PI2);
                        float r = MathUtils.random(f.size);
                        float ex = f.x + MathUtils.cos(a) * r;
                        float ey = f.y + MathUtils.sin(a) * r;
                        Particle.burst(particles, ex, ey, f.color, 10);
                        Particle.burst(particles, ex, ey, Color.WHITE, 3);
                        ringBurst(ex, ey, f.color, 0.9f, 0.4f);
                    }
                }
                default -> {
                }
            }

            if (f.t >= f.dur) {
                list.removeIndex(i);
            }
        }
    }

    // ------------------------------------------------------------ drawing (below sprites)

    /** Ground-level effects. ShapeRenderer must be in Filled mode, world projection. */
    public void drawBelow(ShapeRenderer sr, Player player) {
        for (int i = 0; i < list.size; i++) {
            Fx f = list.get(i);
            float p = MathUtils.clamp(f.t / f.dur, 0f, 1f);

            switch (f.kind) {
                case SCORCH -> {
                    float a = 1f - p;
                    sr.setColor(0.05f, 0.03f, 0.02f, 0.40f * a);
                    sr.circle(f.x, f.y, f.size, 36);
                    sr.setColor(f.color.r, f.color.g, f.color.b, 0.8f * a);
                    for (int k = 0; k < 8; k++) {
                        float ang = f.angle + k * MathUtils.PI2 / 8f;
                        float len = f.size * (0.6f + 0.35f * ((k * 37) % 5) / 5f);
                        sr.rectLine(f.x, f.y, f.x + MathUtils.cos(ang) * len, f.y + MathUtils.sin(ang) * len, 0.05f);
                    }
                }
                case GLYPH -> {
                    float e = easeOut(p);
                    float r = MathUtils.lerp(0.3f, 1.5f, e);
                    float a = 1f - p;
                    sr.setColor(f.color.r, f.color.g, f.color.b, 0.12f * a);
                    sr.circle(f.x, f.y, r, 36);
                    sr.setColor(f.color.r, f.color.g, f.color.b, 0.9f * a);
                    ring(sr, f.x, f.y, r, 0.05f, 40);
                    sr.setColor(f.color.r, f.color.g, f.color.b, 0.5f * a);
                    ring(sr, f.x, f.y, r * 0.72f, 0.03f, 36);
                    symbol(sr, f.type, f.x, f.y, r * 0.55f, f.t * 2.2f, 0.9f * a);
                }
                case FROST_NOVA -> {
                    sr.setColor(f.color.r, f.color.g, f.color.b, 0.09f * (1f - p));
                    sr.circle(f.x, f.y, f.size * easeOut(p), 48);
                }
                case NUKE_CHARGE -> {
                    float a = 0.4f + 0.5f * p;
                    sr.setColor(1f, 0.6f, 0.2f, 0.18f * a);
                    sr.circle(f.x, f.y, 1.6f, 36);
                    sr.setColor(1f, 0.55f, 0.15f, a);
                    ring(sr, f.x, f.y, 1.6f * (1.15f - 0.15f * p), 0.06f, 40);
                    sr.setColor(1f, 0.8f, 0.4f, a * 0.8f);
                    star(sr, f.x, f.y, 1.4f, 0.55f, 8, f.t * 6f);
                }
                default -> {
                }
            }
        }
    }

    // ------------------------------------------------------------ drawing (above sprites)

    /** Auras and one-shot bursts. ShapeRenderer must be in Filled mode, world projection. */
    public void drawAbove(ShapeRenderer sr, Player player, float arenaW, float arenaH) {
        player.getCenter(c);

        drawShield(sr, player);
        drawAfterimages(sr, player);
        drawMagnet(sr, player);
        drawOverdrive(sr, player);
        drawFreezeAura(sr, player, arenaW, arenaH);
        drawStorm(sr, player);

        for (int i = 0; i < list.size; i++) {
            Fx f = list.get(i);
            float p = MathUtils.clamp(f.t / f.dur, 0f, 1f);

            switch (f.kind) {
                case RING_BURST -> {
                    float r = f.size * easeOut(p);
                    sr.setColor(f.color.r, f.color.g, f.color.b, 1f - p);
                    ring(sr, f.x, f.y, r, 0.03f + 0.12f * (1f - p), 32);
                }
                case FROST_NOVA -> drawFrostNova(sr, f, p);
                case NUKE_CHARGE -> drawNukeCharge(sr, f, p);
                case NUKE_BLAST -> drawNukeBlast(sr, f, p);
                case RIPPLE -> {
                    float r = 0.78f + 0.22f * p;
                    sr.setColor(1f, 1f, 1f, 1f - p);
                    arc(sr, c.x, c.y, r, 0.10f * (1f - p) + 0.02f, f.angle - 0.8f, f.angle + 0.8f, 12);
                }
                default -> {
                }
            }
        }
    }

    private void drawShield(ShapeRenderer sr, Player player) {
        if (!player.isShielded()) return;
        float remaining = player.getShieldPercent() * GameConfig.SHIELD_DURATION;
        if (remaining < 1f && ((int) (time * 14f)) % 2 == 0) return;

        Color col = AbilityPickup.Type.SHIELD.color;
        float R = 0.78f + 0.025f * MathUtils.sin(time * 5f);

        sr.setColor(col.r, col.g, col.b, 0.10f);
        sr.circle(c.x, c.y, R, 32);
        sr.setColor(col.r, col.g, col.b, 0.85f);
        ngon(sr, c.x, c.y, R, 6, time * 0.8f, 0.05f);
        sr.setColor(col.r, col.g, col.b, 0.40f);
        ngon(sr, c.x, c.y, R * 0.82f, 6, -time * 1.1f, 0.03f);

        float sweepA = time * 2f;
        sr.setColor(1f, 1f, 1f, 0.9f);
        arc(sr, c.x, c.y, R, 0.06f, sweepA, sweepA + 0.6f, 8);

        for (int i = 0; i < 3; i++) {
            float a = time * 2.2f + i * MathUtils.PI2 / 3f;
            float px = c.x + MathUtils.cos(a) * (R + 0.1f);
            float py = c.y + MathUtils.sin(a) * (R + 0.1f);
            sr.setColor(col.r, col.g, col.b, 0.95f);
            diamond(sr, px, py, a + MathUtils.HALF_PI, 0.13f, 0.05f);
            sr.setColor(1f, 1f, 1f, 0.9f);
            sr.circle(px, py, 0.025f, 6);
        }
    }

    private void drawAfterimages(ShapeRenderer sr, Player player) {
        boolean haste = player.isHasted();
        boolean over = player.isOverdriven();
        if (!haste && !over) return;

        Color g = haste ? AbilityPickup.Type.HASTE.color : AbilityPickup.Type.OVERDRIVE.color;

        for (int i = 0; i < TRAIL; i++) {
            float life = trailLife[i];
            if (life <= 0f) continue;
            float frac = life / 0.32f;
            sr.setColor(g.r, g.g, g.b, 0.28f * frac);
            sr.circle(trailX[i], trailY[i], 0.28f * (0.6f + 0.4f * frac), 14);
        }

        float speed = vel.len();
        if (haste && speed > 0.5f) {
            dir.set(vel).scl(1f / speed);
            for (int k = 0; k < 6; k++) {
                float phase = (time * 3f + k * 0.23f) % 1f;
                float side = (((k * 0.37f) % 1f) - 0.5f) * 0.7f;
                float bx = c.x - dir.x * (0.3f + phase * 0.9f) - dir.y * side;
                float by = c.y - dir.y * (0.3f + phase * 0.9f) + dir.x * side;
                float len = 0.35f + 0.3f * ((k * 13) % 5) / 5f;
                sr.setColor(g.r, g.g, g.b, 0.55f * (1f - phase));
                sr.rectLine(bx, by, bx - dir.x * len, by - dir.y * len, 0.03f);
            }
        }
        if (haste) {
            sr.setColor(g.r, g.g, g.b, 0.55f);
            for (int k = 0; k < 3; k++) {
                float a0 = time * 5f + k * MathUtils.PI2 / 3f;
                arc(sr, c.x, c.y - 0.2f, 0.45f, 0.03f, a0, a0 + 0.9f, 6);
            }
        }
    }

    private void drawMagnet(ShapeRenderer sr, Player player) {
        if (!player.isMagnetActive()) return;
        float R = player.getPickupRadius();
        Color o = AbilityPickup.Type.MAGNET.color;

        for (int i = 0; i < 3; i++) {
            float ph = (time * 0.7f + i / 3f) % 1f;
            float r = R * (1f - ph) + 0.3f * ph;
            sr.setColor(o.r, o.g, o.b, MathUtils.sin(ph * MathUtils.PI) * 0.55f);
            ring(sr, c.x, c.y, r, 0.035f, 40);
        }
        sr.setColor(o.r, o.g, o.b, 0.35f);
        for (int k = 0; k < 24; k++) {
            float a0 = time * 0.5f + k * MathUtils.PI2 / 24f;
            arc(sr, c.x, c.y, R, 0.03f, a0, a0 + 0.12f, 2);
        }
        for (int k = 0; k < 16; k++) {
            float ph = (time * 0.6f + k * 0.137f) % 1f;
            float r = R * (1f - ph) + 0.2f;
            float a = k * 2.4f + ph * 3f;
            sr.setColor(1f, 0.8f, 0.4f, 0.8f * Math.min(1f, ph * 4f));
            sr.circle(c.x + MathUtils.cos(a) * r, c.y + MathUtils.sin(a) * r, 0.03f + 0.02f * (1f - ph), 6);
        }
    }

    private void drawOverdrive(ShapeRenderer sr, Player player) {
        if (!player.isOverdriven()) return;
        Color p = AbilityPickup.Type.OVERDRIVE.color;

        for (int i = 0; i < 10; i++) {
            float a = i * MathUtils.PI2 / 10f + time * 1.5f;
            float h = 0.28f + 0.18f * MathUtils.sin(time * 18f + i * 1.7f);
            float bx = c.x + MathUtils.cos(a) * 0.42f;
            float by = c.y + MathUtils.sin(a) * 0.42f;
            float tx = c.x + MathUtils.cos(a) * (0.42f + h);
            float ty = c.y + MathUtils.sin(a) * (0.42f + h);
            float nx = -MathUtils.sin(a) * 0.09f;
            float ny = MathUtils.cos(a) * 0.09f;
            sr.setColor(p.r, p.g, p.b, 0.65f);
            sr.triangle(bx + nx, by + ny, bx - nx, by - ny, tx, ty);
            sr.setColor(1f, 0.9f, 0.4f, 0.85f);
            sr.triangle(bx + nx * 0.5f, by + ny * 0.5f, bx - nx * 0.5f, by - ny * 0.5f,
                bx + (tx - bx) * 0.6f, by + (ty - by) * 0.6f);
        }
        for (int i = 0; i < 4; i++) {
            float a = -time * 4f + i * MathUtils.HALF_PI;
            sr.setColor(1f, 1f, 0.8f, 0.9f);
            sr.circle(c.x + MathUtils.cos(a) * 0.72f, c.y + MathUtils.sin(a) * 0.72f, 0.04f, 6);
        }
        float pct = Math.min(1f, player.getOverdrivePercent());
        sr.setColor(p.r, p.g, p.b, 0.85f);
        arc(sr, c.x, c.y, 0.95f, 0.05f, -MathUtils.HALF_PI, -MathUtils.HALF_PI + MathUtils.PI2 * pct, 36);
    }

    private void drawFreezeAura(ShapeRenderer sr, Player player, float arenaW, float arenaH) {
        if (!player.isFreezeActive()) return;
        Color ice = AbilityPickup.Type.FREEZE.color;
        float pct = Math.min(1f, player.getFreezePercent());

        for (int i = 0; i < 6; i++) {
            float a = time * 0.9f + i * MathUtils.PI / 3f;
            float px = c.x + MathUtils.cos(a) * 0.85f;
            float py = c.y + MathUtils.sin(a) * 0.85f;
            sr.setColor(ice.r, ice.g, ice.b, 0.85f);
            diamond(sr, px, py, a, 0.2f, 0.07f);
            sr.setColor(1f, 1f, 1f, 0.9f);
            diamond(sr, px, py, a, 0.1f, 0.03f);
        }
        float vis = Math.min(1f, pct * 3f);
        for (int i = 0; i < 40; i++) {
            float x = ((i * 0.7548f) % 1f) * arenaW + MathUtils.sin(time + i) * 0.2f;
            float y = arenaH - ((time * 0.5f * (0.6f + (i % 5) * 0.15f) + i * 0.131f) % 1f) * arenaH;
            sr.setColor(1f, 1f, 1f, 0.55f * vis);
            sr.circle(x, y, 0.025f + 0.015f * (i % 3), 6);
        }
    }

    private void drawStorm(ShapeRenderer sr, Player player) {
        if (!player.isStormActive()) return;
        float f = stormFlash;
        float cx = cloudX;
        float cy = cloudY;

        float dr = 0.18f + 0.45f * f;
        float dg = 0.15f + 0.30f * f;
        float db = 0.30f + 0.55f * f;

        float[][] puffs = {
            { -0.70f, -0.05f, 0.38f }, { -0.30f, 0.12f, 0.48f }, { 0.15f, 0.15f, 0.50f },
            { 0.62f, 0.00f, 0.40f }, { 0.00f, -0.10f, 0.50f }
        };
        sr.setColor(0f, 0f, 0f, 0.18f);
        sr.ellipse(c.x - 0.55f, c.y - 0.45f, 1.1f, 0.3f);

        for (float[] pf : puffs) {
            float bob = 0.03f * MathUtils.sin(time * 2f + pf[0] * 5f);
            sr.setColor(dr, dg, db, 0.92f);
            sr.circle(cx + pf[0], cy + pf[1] + bob, pf[2], 20);
        }
        for (float[] pf : puffs) {
            sr.setColor(Math.min(1f, dr + 0.25f), Math.min(1f, dg + 0.25f), Math.min(1f, db + 0.25f), 0.35f);
            sr.circle(cx + pf[0] - 0.05f, cy + pf[1] + 0.12f, pf[2] * 0.6f, 14);
        }
        if (f > 0f) {
            sr.setColor(0.85f, 0.75f, 1f, 0.5f * f);
            sr.circle(cx, cy, 0.8f, 24);
        }

        float top = cy - 0.35f;
        float fall = top - (c.y - 0.55f);
        for (int i = 0; i < 30; i++) {
            float x = cx - 1.0f + ((i * 0.618034f) % 1f) * 2.0f;
            float ph = (time * 1.7f + i * 0.173f) % 1f;
            float y = top - ph * fall;
            sr.setColor(0.75f, 0.85f, 1f, 0.6f * (1f - ph * 0.3f));
            sr.rectLine(x, y, x - 0.03f, y - 0.16f, 0.02f);
        }
        for (int i = 0; i < 4; i++) {
            float ph = (time * 1.2f + i * 0.25f) % 1f;
            float rx = c.x - 0.9f + ((i * 0.41f) % 1f) * 1.8f;
            sr.setColor(0.75f, 0.85f, 1f, 0.5f * (1f - ph));
            sr.ellipse(rx - 0.15f * ph, c.y - 0.52f - 0.04f * ph, 0.3f * ph, 0.08f * ph);
        }
    }

    private void drawFrostNova(ShapeRenderer sr, Fx f, float p) {
        float r = f.size * easeOut(p);
        float a = 1f - p * 0.8f;
        sr.setColor(f.color.r, f.color.g, f.color.b, 0.9f * a);
        ring(sr, f.x, f.y, r, 0.30f * (1f - p * 0.5f), 56);
        sr.setColor(1f, 1f, 1f, 0.8f * a);
        ring(sr, f.x, f.y, r, 0.06f, 56);
        sr.setColor(f.color.r, f.color.g, f.color.b, 0.4f * a);
        ring(sr, f.x, f.y, r * 0.82f, 0.12f, 48);

        for (int i = 0; i < 28; i++) {
            float ang = i * MathUtils.PI2 / 28f;
            float len = 0.5f + 0.25f * MathUtils.sin(i * 3f);
            float bx = f.x + MathUtils.cos(ang) * (r - 0.05f);
            float by = f.y + MathUtils.sin(ang) * (r - 0.05f);
            float tx = f.x + MathUtils.cos(ang) * (r + len * a);
            float ty = f.y + MathUtils.sin(ang) * (r + len * a);
            float nx = -MathUtils.sin(ang) * 0.09f;
            float ny = MathUtils.cos(ang) * 0.09f;
            sr.setColor(0.85f, 0.97f, 1f, 0.9f * a);
            sr.triangle(bx + nx, by + ny, bx - nx, by - ny, tx, ty);
        }
    }

    private void drawNukeCharge(ShapeRenderer sr, Fx f, float p) {
        float core = 0.15f + 0.55f * p * p;
        sr.setColor(1f, 0.7f, 0.25f, 0.5f + 0.4f * p);
        sr.circle(f.x, f.y, core * 1.6f, 24);
        sr.setColor(1f, 0.95f, 0.8f, 1f);
        sr.circle(f.x, f.y, core, 20);

        for (int i = 0; i < 24; i++) {
            float r = 3.0f * (1f - p) * (1f - (i % 6) / 8f) + 0.2f;
            float a = i * 0.9f + p * 9f;
            sr.setColor(1f, 0.75f, 0.3f, 0.8f);
            sr.circle(f.x + MathUtils.cos(a) * r, f.y + MathUtils.sin(a) * r, 0.045f, 6);
        }
    }

    private void drawNukeBlast(ShapeRenderer sr, Fx f, float p) {
        float e = easeOut(p);

        if (p < 0.25f) {
            float q = p / 0.25f;
            sr.setColor(1f, 1f, 0.9f, 1f - q);
            sr.circle(f.x, f.y, 0.6f + 3.2f * q, 32);
            sr.setColor(f.color.r, f.color.g, f.color.b, 0.6f * (1f - q));
            sr.circle(f.x, f.y, 1.0f + 5.0f * q, 36);
        }

        float[] speedMul = { 1f, 0.78f, 0.55f };
        for (int k = 0; k < 3; k++) {
            float r = f.size * 0.9f * e * speedMul[k];
            float thick = 0.45f * (1f - p) + 0.05f;
            if (k == 0) sr.setColor(1f, 1f, 0.9f, 1f - p);
            else if (k == 1) sr.setColor(f.color.r, f.color.g, f.color.b, 0.9f * (1f - p));
            else sr.setColor(f.color.r * 0.7f, f.color.g * 0.5f, f.color.b * 0.4f, 0.8f * (1f - p));
            ring(sr, f.x, f.y, r, thick, 64);
        }

        float base = f.size * 0.9f * e;
        for (int i = 0; i < 20; i++) {
            float a = f.angle + i * MathUtils.PI2 / 20f + p * 0.4f;
            float r0 = base * 0.7f;
            float r1 = base * (0.9f + 0.3f * ((i % 3) / 3f));
            sr.setColor(f.color.r, f.color.g, f.color.b, 0.8f * (1f - p));
            sr.rectLine(f.x + MathUtils.cos(a) * r0, f.y + MathUtils.sin(a) * r0,
                f.x + MathUtils.cos(a) * r1, f.y + MathUtils.sin(a) * r1, 0.06f * (1f - p) + 0.01f);
        }
    }

    // ------------------------------------------------------------ helpers

    private void symbol(ShapeRenderer sr, AbilityPickup.Type type, float cx, float cy, float r, float rot, float alpha) {
        if (type == null) return;
        Color col = type.color;
        sr.setColor(col.r, col.g, col.b, alpha);

        switch (type) {
            case SHIELD -> {
                ngon(sr, cx, cy, r, 6, rot, 0.05f);
                ngon(sr, cx, cy, r * 0.55f, 6, -rot, 0.04f);
            }
            case HASTE -> {
                for (int i = 0; i < 3; i++) {
                    float a = rot + i * MathUtils.PI2 / 3f;
                    float tx = cx + MathUtils.cos(a) * r;
                    float ty = cy + MathUtils.sin(a) * r;
                    float bx = cx + MathUtils.cos(a) * r * 0.4f;
                    float by = cy + MathUtils.sin(a) * r * 0.4f;
                    float nx = -MathUtils.sin(a) * r * 0.3f;
                    float ny = MathUtils.cos(a) * r * 0.3f;
                    sr.rectLine(bx + nx, by + ny, tx, ty, 0.05f);
                    sr.rectLine(bx - nx, by - ny, tx, ty, 0.05f);
                }
            }
            case MAGNET -> {
                arc(sr, cx, cy, r, 0.07f, rot + 0.5f, rot + MathUtils.PI - 0.5f, 16);
                arc(sr, cx, cy, r, 0.07f, rot + MathUtils.PI + 0.5f, rot + MathUtils.PI2 - 0.5f, 16);
                ring(sr, cx, cy, r * 0.3f, 0.04f, 16);
            }
            case STORM -> {
                for (int i = 0; i < 4; i++) {
                    float a = rot + i * MathUtils.HALF_PI;
                    float x1 = cx + MathUtils.cos(a) * r * 0.2f;
                    float y1 = cy + MathUtils.sin(a) * r * 0.2f;
                    float x2 = cx + MathUtils.cos(a + 0.35f) * r * 0.6f;
                    float y2 = cy + MathUtils.sin(a + 0.35f) * r * 0.6f;
                    float x3 = cx + MathUtils.cos(a - 0.15f) * r;
                    float y3 = cy + MathUtils.sin(a - 0.15f) * r;
                    sr.rectLine(x1, y1, x2, y2, 0.05f);
                    sr.rectLine(x2, y2, x3, y3, 0.05f);
                }
            }
            case FREEZE -> {
                for (int i = 0; i < 6; i++) {
                    float a = rot + i * MathUtils.PI / 3f;
                    float ex = cx + MathUtils.cos(a) * r;
                    float ey = cy + MathUtils.sin(a) * r;
                    sr.rectLine(cx, cy, ex, ey, 0.04f);
                    float mx = cx + MathUtils.cos(a) * r * 0.6f;
                    float my = cy + MathUtils.sin(a) * r * 0.6f;
                    sr.rectLine(mx, my, mx + MathUtils.cos(a + 0.9f) * r * 0.25f, my + MathUtils.sin(a + 0.9f) * r * 0.25f, 0.03f);
                    sr.rectLine(mx, my, mx + MathUtils.cos(a - 0.9f) * r * 0.25f, my + MathUtils.sin(a - 0.9f) * r * 0.25f, 0.03f);
                }
            }
            case NUKE -> star(sr, cx, cy, r, r * 0.45f, 8, rot);
            case OVERDRIVE -> {
                ring(sr, cx, cy, r * 0.7f, 0.05f, 24);
                for (int i = 0; i < 8; i++) {
                    float a = rot + i * MathUtils.PI2 / 8f;
                    sr.rectLine(cx + MathUtils.cos(a) * r * 0.7f, cy + MathUtils.sin(a) * r * 0.7f,
                        cx + MathUtils.cos(a) * r, cy + MathUtils.sin(a) * r, 0.09f);
                }
            }
        }
    }

    private static float easeOut(float p) {
        float q = 1f - MathUtils.clamp(p, 0f, 1f);
        return 1f - q * q * q;
    }

    private static void ring(ShapeRenderer sr, float cx, float cy, float r, float th, int seg) {
        arc(sr, cx, cy, r, th, 0f, MathUtils.PI2, seg);
    }

    private static void arc(ShapeRenderer sr, float cx, float cy, float r, float th, float a0, float a1, int seg) {
        float step = (a1 - a0) / seg;
        for (int i = 0; i < seg; i++) {
            float s = a0 + i * step;
            float e = s + step;
            sr.rectLine(cx + MathUtils.cos(s) * r, cy + MathUtils.sin(s) * r,
                cx + MathUtils.cos(e) * r, cy + MathUtils.sin(e) * r, th);
        }
    }

    private static void ngon(ShapeRenderer sr, float cx, float cy, float r, int n, float rot, float th) {
        for (int i = 0; i < n; i++) {
            float a0 = rot + i * MathUtils.PI2 / n;
            float a1 = rot + (i + 1) * MathUtils.PI2 / n;
            sr.rectLine(cx + MathUtils.cos(a0) * r, cy + MathUtils.sin(a0) * r,
                cx + MathUtils.cos(a1) * r, cy + MathUtils.sin(a1) * r, th);
        }
    }

    private static void diamond(ShapeRenderer sr, float x, float y, float angle, float len, float wid) {
        float dx = MathUtils.cos(angle) * len;
        float dy = MathUtils.sin(angle) * len;
        float nx = -MathUtils.sin(angle) * wid;
        float ny = MathUtils.cos(angle) * wid;
        sr.triangle(x + dx, y + dy, x + nx, y + ny, x - nx, y - ny);
        sr.triangle(x - dx, y - dy, x + nx, y + ny, x - nx, y - ny);
    }

    private static void star(ShapeRenderer sr, float cx, float cy, float r1, float r2, int n, float rot) {
        for (int i = 0; i < n * 2; i++) {
            float aA = rot + i * MathUtils.PI / n;
            float aB = rot + (i + 1) * MathUtils.PI / n;
            float rA = (i % 2 == 0) ? r1 : r2;
            float rB = (i % 2 == 0) ? r2 : r1;
            sr.triangle(cx, cy,
                cx + MathUtils.cos(aA) * rA, cy + MathUtils.sin(aA) * rA,
                cx + MathUtils.cos(aB) * rB, cy + MathUtils.sin(aB) * rB);
        }
    }
}
