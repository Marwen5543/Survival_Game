package io.github.SurvivalGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ControlsScreen extends ScreenAdapter {
    private final GdxGame game;
    private final Batch batch;
    private final BitmapFont font;
    private final Viewport viewport = new ScreenViewport();
    private final GlyphLayout layout = new GlyphLayout();

    public ControlsScreen(GdxGame game) {
        this.game = game;
        this.batch = game.getBatch();
        this.font = game.getFont();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float deltaTime) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game));
            dispose();
            return;
        }

        ScreenUtils.clear(Color.BLACK);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        float centerX = viewport.getWorldWidth() / 2;
        float y = viewport.getWorldHeight() / 2 + 130;
        float A = viewport.getWorldHeight() / 2 + 500;

        layout.setText(font, "CONTROLS");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 50;

        layout.setText(font, "W / A / S / D - Move");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 40;

        layout.setText(font, "SHIFT - Dash (brief invincibility)");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 40;

        layout.setText(font, "ESC - Pause");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 40;

        layout.setText(font, "R - Restart (when dead)");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 40;

        layout.setText(font, "Collect glowing orbs for buffs!");
        font.draw(batch, layout, centerX - layout.width / 2, y);
        y -= 70;

        layout.setText(font, "Press SPACE to start");
        font.draw(batch, layout, centerX - layout.width / 2, y);


        layout.setText(font, "This game was developed by an HP agent working from 8:00 AM to 5:00 PM,");
        font.draw(batch, layout, centerX - layout.width / 2, A);
        A -= 50;

        layout.setText(font, "who is currently experiencing the most difficult period of his life.");
        font.draw(batch, layout, centerX - layout.width / 2, A);
        A -= 80;
        batch.end();
    }
}
