package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** A small heart-like pickup that heals the player on contact. Drawn without needing new art. */
public class HealthPickup extends GameObject {
    private static final float SIZE = 0.2f;

    public HealthPickup(float x, float y) {
        super(x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    @Override
    void update(float deltaTime) {
        // Stationary until picked up.
    }

    public void drawDebug(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.PINK);
        float centerX = rect.x + rect.width / 2f;
        float centerY = rect.y + rect.height / 2f;
        shapeRenderer.circle(centerX, centerY, rect.width / 2f, 12);
    }
}
