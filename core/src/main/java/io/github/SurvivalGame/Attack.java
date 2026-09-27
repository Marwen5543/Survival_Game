package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Attack extends GameObject {
    private static final float SIZE = 1f;
    private static final float DURATION = 0.4f;

    private float lifeSpan = DURATION;
    private final Animation<Texture> animation;
    // Enemies this specific swing has already damaged, so a single attack
    // instance can't deal damage every frame it happens to overlap a target.
    private final Array<Enemy> hitEnemies = new Array<>();

    public Attack(Vector2 position, Vector2 direction, Animation<Texture> animation) {
        super(
            position.x - SIZE / 2 + direction.x * SIZE * 0.75f,
            position.y - SIZE / 2 + direction.y * SIZE * 0.75f,
            SIZE,
            SIZE
        );

        this.animation = animation;
    }

    @Override
    void update(float deltaTime) {
        this.lifeSpan -= deltaTime;
    }

    @Override
    public void draw(Batch batch) {
        float animationDuration = animation.getAnimationDuration();
        float animationPerc = 1f - (Math.max(0f, lifeSpan) / DURATION);
        float stateTime = animationDuration * animationPerc;

        texture = animation.getKeyFrame(stateTime, true);

        super.draw(batch);
    }

    public boolean isDone() {
        return this.lifeSpan <= 0f;
    }

    /** @return true if this swing has already damaged the given enemy. */
    public boolean hasHit(Enemy enemy) {
        return hitEnemies.contains(enemy, true);
    }

    public void markHit(Enemy enemy) {
        hitEnemies.add(enemy);
    }
}
