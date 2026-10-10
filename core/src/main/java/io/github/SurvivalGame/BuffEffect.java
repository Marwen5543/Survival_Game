package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * A short tinted burst animation played wherever an ability is actually activated (not just
 * picked up) — the visible "you just used Shield/Haste/Magnet/Storm" moment. The underlying
 * spritesheet is plain white so it can be tinted to whichever ability triggered it.
 */
public class BuffEffect {
    private static final float DURATION = 0.5f;
    private static final float SIZE = 1.7f;

    private final float x, y;
    private final Color color;
    private final Animation<TextureRegion> animation;
    private float lifeSpan = DURATION;

    public BuffEffect(float x, float y, Color color, Animation<TextureRegion> animation) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.animation = animation;
    }

    public void update(float deltaTime) {
        lifeSpan -= deltaTime;
    }

    public boolean isDone() {
        return lifeSpan <= 0f;
    }

    public void draw(Batch batch) {
        float perc = 1f - Math.max(0f, lifeSpan) / DURATION;
        TextureRegion frame = animation.getKeyFrame(animation.getAnimationDuration() * perc, false);

        float alpha = Math.max(0f, lifeSpan / DURATION);
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(frame, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
        batch.setColor(1f, 1f, 1f, 1f);
    }
}

