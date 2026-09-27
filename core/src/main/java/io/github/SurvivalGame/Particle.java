package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** A tiny expanding-and-fading dot, used for enemy death bursts. */
public class Particle {
    private final Vector2 position;
    private final Vector2 velocity;
    private final Color color;
    private float life;
    private final float maxLife;

    public Particle(float x, float y, Color color) {
        this.position = new Vector2(x, y);
        float angle = MathUtils.random(0f, MathUtils.PI2);
        float speed = MathUtils.random(1f, 3f);
        this.velocity = new Vector2(MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed);
        this.color = color;
        this.maxLife = GameConfig.PARTICLE_LIFETIME;
        this.life = maxLife;
    }

    public void update(float deltaTime) {
        position.mulAdd(velocity, deltaTime);
        life -= deltaTime;
    }

    public boolean isDone() {
        return life <= 0f;
    }

    public void draw(ShapeRenderer shapeRenderer) {
        float perc = Math.max(0f, life / maxLife);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(position.x, position.y, 0.05f * perc + 0.02f, 8);
    }

    public static void burst(com.badlogic.gdx.utils.Array<Particle> target, float x, float y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            target.add(new Particle(x, y, color));
        }
    }
}
