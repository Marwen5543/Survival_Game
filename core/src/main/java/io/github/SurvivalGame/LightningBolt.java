package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/** A jagged bolt of lightning striking down onto a single point — used by the Storm buff, one per enemy hit. */
public class LightningBolt {
    private static final float DURATION = 0.18f;
    private static final int SEGMENTS = 6;

    private final float targetX;
    private final float targetY;
    private final float topY;
    private final float[] jitter = new float[SEGMENTS];
    private float timer = DURATION;

    public LightningBolt(float targetX, float targetY, float topY) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.topY = topY;
        for (int i = 0; i < SEGMENTS; i++) {
            jitter[i] = MathUtils.random(-0.25f, 0.25f);
        }
    }

    public void update(float deltaTime) {
        timer -= deltaTime;
    }

    public boolean isDone() {
        return timer <= 0f;
    }

    /** Assumes the ShapeRenderer is already begun in Filled mode. */
    public void draw(ShapeRenderer shapeRenderer) {
        float alpha = Math.max(0f, timer / DURATION);

        float segmentHeight = (topY - targetY) / SEGMENTS;
        float prevX = targetX;
        float prevY = targetY;

        shapeRenderer.setColor(1f, 1f, 1f, alpha);
        for (int i = 1; i <= SEGMENTS; i++) {
            float y = targetY + segmentHeight * i;
            float x = targetX + jitter[i - 1] * (1f - (float) i / SEGMENTS);
            shapeRenderer.rectLine(prevX, prevY, x, y, 0.035f * alpha + 0.01f);
            prevX = x;
            prevY = y;
        }

        // Small impact flash at the strike point.
        shapeRenderer.setColor(0.85f, 0.7f, 1f, alpha);
        shapeRenderer.circle(targetX, targetY, 0.18f * alpha, 12);
    }
}
