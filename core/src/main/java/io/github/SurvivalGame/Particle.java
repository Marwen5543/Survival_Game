package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * A tiny expanding-and-fading dot. In late game the number of particles is capped and scaled down
 * (see {@link #density}, driven by GameScreen's clarity system) so effects never bury the player.
 */
public class Particle {
    /** 1 = full effects, lower = fewer particles per burst when the screen is crowded. */
    public static float density = 1f;
    public static final int MAX_PARTICLES = 450;

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
        shapeRenderer.setColor(color.r, color.g, color.b, 0.25f + 0.6f * perc);
        shapeRenderer.circle(position.x, position.y, 0.05f * perc + 0.02f, 8);
    }

    public static void burst(Array<Particle> target, float x, float y, Color color, int count) {
        if (target.size >= MAX_PARTICLES) {
            return;
        }
        int n = Math.max(1, Math.round(count * density));
        for (int i = 0; i < n; i++) {
            target.add(new Particle(x, y, color));
        }
    }
}
