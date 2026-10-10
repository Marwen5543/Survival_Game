package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** A pistol round. Drawn with shapes (no art needed), can pierce a limited number of enemies. */
public class Bullet {
    public static final float RADIUS = 0.08f;

    private final Vector2 position;
    private final Vector2 velocity;
    private final Color color;
    private final float damage;
    private int pierceLeft;
    private float life;
    private final Array<Enemy> hitEnemies = new Array<>();

    public Bullet(float x, float y, float angleRad, float speed, float range,
                  float damage, int pierce, Color color) {
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(MathUtils.cos(angleRad) * speed, MathUtils.sin(angleRad) * speed);
        this.life = range / speed;
        this.damage = damage;
        this.pierceLeft = pierce;
        this.color = color;
    }

    public void update(float deltaTime) {
        position.mulAdd(velocity, deltaTime);
        life -= deltaTime;
    }

    public boolean isDone() {
        return life <= 0f;
    }

    /** Called after a hit lands: eats one pierce charge, or ends the bullet if none are left. */
    public void consumePierce() {
        if (pierceLeft <= 0) {
            life = 0f;
        } else {
            pierceLeft--;
        }
    }

    public boolean hasHit(Enemy enemy) {
        return hitEnemies.contains(enemy, true);
    }

    public void markHit(Enemy enemy) {
        hitEnemies.add(enemy);
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public float getDamage() {
        return damage;
    }

    public Color getColor() {
        return color;
    }

    /** Assumes the ShapeRenderer is already begun in Filled mode. */
    public void draw(ShapeRenderer sr) {
        float fade = Math.min(1f, life * 8f);

        // Motion trail
        sr.setColor(color.r, color.g, color.b, 0.45f * fade);
        sr.rectLine(position.x - velocity.x * 0.035f, position.y - velocity.y * 0.035f,
            position.x, position.y, RADIUS * 1.2f);

        // Glow, body and hot core
        sr.setColor(color.r, color.g, color.b, 0.25f * fade);
        sr.circle(position.x, position.y, RADIUS * 2f, 12);
        sr.setColor(color.r, color.g, color.b, fade);
        sr.circle(position.x, position.y, RADIUS, 10);
        sr.setColor(1f, 1f, 1f, fade);
        sr.circle(position.x, position.y, RADIUS * 0.5f, 8);
    }
}
