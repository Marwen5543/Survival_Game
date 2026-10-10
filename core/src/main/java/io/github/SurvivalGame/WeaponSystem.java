package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * The auto-firing arsenal that sits next to the classic sword: Flintlock Pistol, Spirit Blades,
 * Shockwave and Falling Stars. Levels come from {@link Player#getUpgradeLevel(Upgrade)}; all damage
 * and cooldowns are scaled by the player's passives. Weapons only ever call takeDamage() —
 * GameScreen reaps enemies whose health hit zero, so kill rewards live in exactly one place.
 */
public class WeaponSystem {
    // Pistol
    private static final float PISTOL_RANGE = 5.5f;
    private static final float BULLET_SPEED = 10f;
    private static final float BULLET_SPREAD_DEG = 9f;

    // Spirit Blades
    private static final float BLADE_HIT_RADIUS = 0.24f;
    private static final float BLADE_HIT_INTERVAL = 0.35f;

    // Shockwave
    private static final float NOVA_EXPAND_SPEED = 9f;

    // Falling Stars
    private static final float METEOR_DELAY = 0.8f;
    private static final float METEOR_FLASH = 0.25f;

    private static final class Meteor {
        final float x, y, radius, damage;
        float timer = METEOR_DELAY;
        boolean exploded;
        float flash = METEOR_FLASH;

        Meteor(float x, float y, float radius, float damage) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.damage = damage;
        }
    }

    private final Player player;
    private final Array<Particle> particles;
    private final Array<Bullet> bullets = new Array<>();
    private final Array<Meteor> meteors = new Array<>();

    private final Vector2 center = new Vector2();
    private final Vector2 scratch = new Vector2();
    private final Vector2 scratch2 = new Vector2();
    private final Circle circle = new Circle();

    private float pistolTimer;

    private float bladeAngle;

    private float novaTimer;
    private boolean novaActive;
    private float novaRadius;
    private final Vector2 novaOrigin = new Vector2();
    private final Array<Enemy> novaHit = new Array<>();

    private float meteorTimer;

    public WeaponSystem(Player player, Array<Particle> particles) {
        this.player = player;
        this.particles = particles;
    }

    public void reset() {
        bullets.clear();
        meteors.clear();
        pistolTimer = 0f;
        bladeAngle = 0f;
        novaTimer = 0f;
        novaActive = false;
        novaRadius = 0f;
        novaHit.clear();
        meteorTimer = 1.5f;
    }

    public void update(float deltaTime, Array<Enemy> enemies) {
        player.getCenter(center);

        updatePistol(deltaTime, enemies);
        updateBullets(deltaTime, enemies);
        updateBlades(deltaTime, enemies);
        updateNova(deltaTime, enemies);
        updateMeteors(deltaTime, enemies);
    }

    // ------------------------------------------------------------------ Pistol

    private void updatePistol(float deltaTime, Array<Enemy> enemies) {
        int level = player.getUpgradeLevel(Upgrade.PISTOL);
        if (level <= 0) return;

        pistolTimer -= deltaTime;
        if (pistolTimer > 0f) return;

        Enemy target = nearestEnemy(enemies, PISTOL_RANGE);
        if (target == null) return; // stay loaded until something walks into range

        pistolTimer = Math.max(0.3f, 0.9f - 0.1f * (level - 1)) * player.getCooldownMultiplier();

        int shots = 1 + (level - 1) / 2;
        int pierce = Math.max(0, level - 3);
        float damage = player.getDamage(1f + 0.25f * (level - 1));

        target.getCenter(scratch).sub(center);
        float baseAngle = scratch.angleRad();

        for (int i = 0; i < shots; i++) {
            float offset = (i - (shots - 1) / 2f) * BULLET_SPREAD_DEG * MathUtils.degreesToRadians;
            bullets.add(new Bullet(center.x, center.y, baseAngle + offset,
                BULLET_SPEED, PISTOL_RANGE + 1f, damage, pierce, Upgrade.PISTOL.color));
        }

        // Muzzle flash
        Particle.burst(particles,
            center.x + MathUtils.cos(baseAngle) * 0.4f,
            center.y + MathUtils.sin(baseAngle) * 0.4f,
            Upgrade.PISTOL.color, 2);
    }

    private void updateBullets(float deltaTime, Array<Enemy> enemies) {
        var iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update(deltaTime);

            if (!bullet.isDone()) {
                circle.set(bullet.getX(), bullet.getY(), Bullet.RADIUS);

                for (int i = 0; i < enemies.size && !bullet.isDone(); i++) {
                    Enemy enemy = enemies.get(i);
                    if (enemy.isDead() || bullet.hasHit(enemy)) continue;

                    if (Intersector.overlaps(circle, enemy.rect)) {
                        bullet.markHit(enemy);
                        enemy.takeDamage(bullet.getDamage());
                        Particle.burst(particles, bullet.getX(), bullet.getY(), bullet.getColor(), 3);
                        bullet.consumePierce();
                    }
                }
            }

            if (bullet.isDone()) {
                iterator.remove();
            }
        }
    }

    private Enemy nearestEnemy(Array<Enemy> enemies, float maxRange) {
        Enemy best = null;
        float bestDst2 = maxRange * maxRange;

        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.isDead()) continue;

            float dst2 = enemy.getCenter(scratch).dst2(center);
            if (dst2 <= bestDst2) {
                bestDst2 = dst2;
                best = enemy;
            }
        }
        return best;
    }

    // ------------------------------------------------------------ Spirit Blades

    private static int bladeCount(int level) {
        return level + 1;
    }

    private static float bladeOrbitRadius(int level) {
        return 1.1f + 0.08f * level;
    }

    private void bladePosition(int index, int count, float radius, Vector2 out) {
        float angle = (bladeAngle + index * 360f / count) * MathUtils.degreesToRadians;
        out.set(center.x + MathUtils.cos(angle) * radius, center.y + MathUtils.sin(angle) * radius);
    }

    private void updateBlades(float deltaTime, Array<Enemy> enemies) {
        int level = player.getUpgradeLevel(Upgrade.BLADES);
        if (level <= 0) return;

        bladeAngle = (bladeAngle + (150f + 30f * level) * deltaTime) % 360f;

        int count = bladeCount(level);
        float radius = bladeOrbitRadius(level);
        float damage = player.getDamage(0.8f + 0.2f * level);

        for (int b = 0; b < count; b++) {
            bladePosition(b, count, radius, scratch);
            circle.set(scratch.x, scratch.y, BLADE_HIT_RADIUS);

            for (int i = 0; i < enemies.size; i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.isDead() || !Intersector.overlaps(circle, enemy.rect)) continue;
                if (!enemy.tryPeriodicHit(BLADE_HIT_INTERVAL)) continue;

                enemy.takeDamage(damage);
                Particle.burst(particles, scratch.x, scratch.y, Upgrade.BLADES.color, 2);
            }
        }
    }

    // ---------------------------------------------------------------- Shockwave

    private static float novaMaxRadius(int level) {
        return 1.8f + 0.3f * level;
    }

    private void updateNova(float deltaTime, Array<Enemy> enemies) {
        int level = player.getUpgradeLevel(Upgrade.NOVA);
        if (level <= 0) return;

        if (!novaActive) {
            novaTimer -= deltaTime;
            if (novaTimer > 0f) return;

            novaTimer = Math.max(2.5f, 6.5f - 0.8f * level) * player.getCooldownMultiplier();
            novaActive = true;
            novaRadius = 0.3f;
            novaOrigin.set(center);
            novaHit.clear();
        }

        novaRadius += NOVA_EXPAND_SPEED * deltaTime;

        float damage = player.getDamage(1f + 0.5f * (level - 1));
        float knockback = 0.8f + 0.2f * level;

        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.isDead() || novaHit.contains(enemy, true)) continue;

            enemy.getCenter(scratch);
            float distance = scratch.dst(novaOrigin) - enemy.rect.width * 0.4f;
            if (distance > novaRadius) continue;

            novaHit.add(enemy);
            enemy.takeDamage(damage);

            scratch.sub(novaOrigin);
            if (scratch.isZero()) scratch.set(1f, 0f);
            scratch.nor().scl(knockback);
            enemy.knockback(scratch.x, scratch.y);

            enemy.getCenter(scratch);
            Particle.burst(particles, scratch.x, scratch.y, Upgrade.NOVA.color, 3);
        }

        if (novaRadius >= novaMaxRadius(level)) {
            novaActive = false;
        }
    }

    // -------------------------------------------------------------- Falling Stars

    private void updateMeteors(float deltaTime, Array<Enemy> enemies) {
        int level = player.getUpgradeLevel(Upgrade.METEOR);
        if (level <= 0) return;

        meteorTimer -= deltaTime;
        if (meteorTimer <= 0f && enemies.size > 0) {
            meteorTimer = Math.max(1.5f, 4.2f - 0.55f * level) * player.getCooldownMultiplier();

            int count = 1 + level / 2;
            float radius = 0.95f + 0.12f * level;
            float damage = player.getDamage(2.5f + 0.9f * level);

            for (int i = 0; i < count; i++) {
                Enemy target = enemies.get(MathUtils.random(enemies.size - 1));
                target.getCenter(scratch);
                meteors.add(new Meteor(
                    scratch.x + MathUtils.random(-0.4f, 0.4f),
                    scratch.y + MathUtils.random(-0.4f, 0.4f),
                    radius, damage));
            }
        }

        for (int m = meteors.size - 1; m >= 0; m--) {
            Meteor meteor = meteors.get(m);

            if (meteor.exploded) {
                meteor.flash -= deltaTime;
                if (meteor.flash <= 0f) meteors.removeIndex(m);
                continue;
            }

            meteor.timer -= deltaTime;
            if (meteor.timer > 0f) continue;

            meteor.exploded = true;
            for (int i = 0; i < enemies.size; i++) {
                Enemy enemy = enemies.get(i);
                if (enemy.isDead()) continue;
                enemy.getCenter(scratch2);
                if (Vector2.dst(meteor.x, meteor.y, scratch2.x, scratch2.y) <= meteor.radius + enemy.rect.width * 0.4f) {
                    enemy.takeDamage(meteor.damage);
                }
            }
            Particle.burst(particles, meteor.x, meteor.y, Upgrade.METEOR.color, 12);
            Particle.burst(particles, meteor.x, meteor.y, Color.WHITE, 4);
        }
    }

    // ----------------------------------------------------------------- Rendering

    /** Assumes the ShapeRenderer is already begun in Filled mode (world projection). */
    public void drawWorld(ShapeRenderer sr) {
        drawMeteors(sr);
        drawNova(sr);
        drawBlades(sr);
        for (int i = 0; i < bullets.size; i++) {
            bullets.get(i).draw(sr);
        }
    }

    private void drawMeteors(ShapeRenderer sr) {
        Color c = Upgrade.METEOR.color;
        for (int i = 0; i < meteors.size; i++) {
            Meteor m = meteors.get(i);

            if (m.exploded) {
                float f = m.flash / METEOR_FLASH;
                sr.setColor(1f, 0.9f, 1f, 0.6f * f);
                sr.circle(m.x, m.y, m.radius * (1.2f - 0.2f * f), 32);
                sr.setColor(c.r, c.g, c.b, 0.5f * f);
                sr.circle(m.x, m.y, m.radius, 32);
                continue;
            }

            float p = 1f - m.timer / METEOR_DELAY; // 0 -> 1 as it lands
            sr.setColor(c.r, c.g, c.b, 0.08f + 0.12f * p);
            sr.circle(m.x, m.y, m.radius, 32);
            sr.setColor(c.r, c.g, c.b, 0.7f);
            drawRing(sr, m.x, m.y, m.radius, 0.03f, 32);
            sr.setColor(1f, 1f, 1f, 0.5f);
            drawRing(sr, m.x, m.y, Math.max(0.05f, m.radius * (1f - p)), 0.02f, 32);

            // The falling star itself, coming in on a diagonal
            float rx = m.x + (1f - p) * 1.5f;
            float ry = m.y + (1f - p) * 5f;
            float tx = m.x + (1f - p + 0.15f) * 1.5f;
            float ty = m.y + (1f - p + 0.15f) * 5f;
            sr.setColor(c.r, c.g, c.b, 0.35f);
            sr.rectLine(rx, ry, tx, ty, 0.14f);
            sr.setColor(c.r, c.g, c.b, 0.9f);
            sr.circle(rx, ry, 0.14f, 12);
            sr.setColor(1f, 1f, 1f, 1f);
            sr.circle(rx, ry, 0.07f, 8);
        }
    }

    private void drawBlades(ShapeRenderer sr) {
        int level = player.getUpgradeLevel(Upgrade.BLADES);
        if (level <= 0) return;

        Color c = Upgrade.BLADES.color;
        int count = bladeCount(level);
        float radius = bladeOrbitRadius(level);

        // Faint orbit track
        sr.setColor(c.r, c.g, c.b, 0.10f);
        drawRing(sr, center.x, center.y, radius, 0.012f, 48);

        for (int b = 0; b < count; b++) {
            bladePosition(b, count, radius, scratch);
            float angle = (bladeAngle + b * 360f / count) * MathUtils.degreesToRadians;

            // Glow
            sr.setColor(c.r, c.g, c.b, 0.25f);
            sr.circle(scratch.x, scratch.y, 0.3f, 16);

            // Diamond blade, long axis along the direction of travel
            float tangent = angle + MathUtils.HALF_PI;
            float dx = MathUtils.cos(tangent) * 0.22f;
            float dy = MathUtils.sin(tangent) * 0.22f;
            float nx = MathUtils.cos(angle) * 0.07f;
            float ny = MathUtils.sin(angle) * 0.07f;

            sr.setColor(c);
            sr.triangle(scratch.x + dx, scratch.y + dy, scratch.x + nx, scratch.y + ny, scratch.x - nx, scratch.y - ny);
            sr.triangle(scratch.x - dx, scratch.y - dy, scratch.x + nx, scratch.y + ny, scratch.x - nx, scratch.y - ny);

            sr.setColor(1f, 1f, 1f, 0.9f);
            sr.circle(scratch.x, scratch.y, 0.045f, 8);
        }
    }

    private void drawNova(ShapeRenderer sr) {
        if (!novaActive) return;

        int level = player.getUpgradeLevel(Upgrade.NOVA);
        Color c = Upgrade.NOVA.color;
        float progress = Math.min(1f, novaRadius / novaMaxRadius(level));
        float alpha = 1f - progress;

        sr.setColor(c.r, c.g, c.b, 0.10f * alpha);
        sr.circle(novaOrigin.x, novaOrigin.y, novaRadius, 48);

        sr.setColor(c.r, c.g, c.b, 0.85f * alpha);
        drawRing(sr, novaOrigin.x, novaOrigin.y, novaRadius, 0.02f + 0.07f * alpha, 48);

        sr.setColor(1f, 1f, 1f, 0.6f * alpha);
        drawRing(sr, novaOrigin.x, novaOrigin.y, novaRadius * 0.95f, 0.025f, 48);
    }

    private void drawRing(ShapeRenderer sr, float cx, float cy, float radius, float thickness, int segments) {
        float step = MathUtils.PI2 / segments;
        for (int i = 0; i < segments; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;
            sr.rectLine(
                cx + MathUtils.cos(a0) * radius, cy + MathUtils.sin(a0) * radius,
                cx + MathUtils.cos(a1) * radius, cy + MathUtils.sin(a1) * radius,
                thickness
            );
        }
    }
}
