package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** A hostile orb fired by Spitters and bosses. Drawn with shapes, hurts the player on contact. */
public class EnemyProjectile {
    private final Vector2 position;
    private final Vector2 velocity;
    private final Color color;
    private final float radius;
    private final float damage;
    private final Circle circle = new Circle();
    private float life = 7f;
    private float age;

    public EnemyProjectile(float x, float y, float angleRad, float speed, float damage, Color color, float radius) {
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(MathUtils.cos(angleRad) * speed, MathUtils.sin(angleRad) * speed);
        this.damage = damage;
        this.color = color;
        this.radius = radius;
    }

    public void update(float deltaTime) {
        position.mulAdd(velocity, deltaTime);
        life -= deltaTime;
        age += deltaTime;
    }

    public boolean isDone() {
        return life <= 0f;
    }

    public boolean hits(Player player) {
        circle.set(position.x, position.y, radius);
        return Intersector.overlaps(circle, player.rect);
    }

    public float getDamage() {
        return damage;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public Color getColor() {
        return color;
    }

    /** Assumes the ShapeRenderer is already begun in Filled mode. */
    public void draw(ShapeRenderer sr) {
        float pulse = 0.5f + 0.5f * MathUtils.sin(age * 18f);
        sr.setColor(color.r, color.g, color.b, 0.25f);
        sr.circle(position.x, position.y, radius * (2.0f + 0.4f * pulse), 14);
        sr.setColor(color.r, color.g, color.b, 1f);
        sr.circle(position.x, position.y, radius, 12);
        sr.setColor(1f, 1f, 1f, 1f);
        sr.circle(position.x, position.y, radius * 0.5f, 8);
    }
}
