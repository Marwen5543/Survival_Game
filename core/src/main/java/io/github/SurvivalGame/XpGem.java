package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class XpGem extends GameObject {

    private static final float SIZE = 0.18f;
    private static final float MOVE_SPEED = 4f;

    private final int amount;

    public XpGem(float x, float y, int amount) {
        super(
            x - SIZE / 2f,
            y - SIZE / 2f,
            SIZE,
            SIZE
        );

        this.amount = amount;
    }

    @Override
    void update(float deltaTime) {
        // XP gems remain stationary until the player's pickup radius reaches them.
    }

    /**
     * @param pickupRadius how close the player must be before this gem starts flying toward
     *                      them — wider while the Magnet buff is active.
     */
    public boolean collect(Player player, float deltaTime, float pickupRadius) {
        Vector2 playerCenter = player.getCenter(TMP_VEC2);
        Vector2 gemCenter = getCenter(new Vector2());

        float distance = playerCenter.dst(gemCenter);

        if (distance < pickupRadius) {
            Vector2 direction = playerCenter
                .sub(gemCenter)
                .nor();

            rect.x += direction.x * MOVE_SPEED * deltaTime;
            rect.y += direction.y * MOVE_SPEED * deltaTime;
        }

        return overlaps(player);
    }

    public int getAmount() {
        return amount;
    }

    public void drawDebug(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.CYAN);

        float centerX = rect.x + rect.width / 2f;
        float centerY = rect.y + rect.height / 2f;

        shapeRenderer.triangle(
            centerX,
            rect.y + rect.height,
            rect.x,
            centerY,
            centerX,
            rect.y
        );

        shapeRenderer.triangle(
            centerX,
            rect.y + rect.height,
            rect.x + rect.width,
            centerY,
            centerX,
            rect.y
        );
    }
}
