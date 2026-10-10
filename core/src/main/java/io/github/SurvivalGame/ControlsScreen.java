package io.github.SurvivalGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ControlsScreen extends ScreenAdapter {
    private static final String[] LINES = {
        "W / A / S / D - Move        SHIFT - Dash (invincible: dodges EVERY hazard)",
        "ESC - Pause     M - Main menu (paused or dead)     R - Restart (dead)",
        "",
        "Weapons fire automatically. Level up to unlock more!",
        "1 Shield  2 Haste  3 Magnet  4 Storm  5 Freeze  6 Supernova  7 Overdrive",
        "Frozen enemies take +50% damage. Supernova charges for a beat, then wipes the screen.",
        "",
        "WATCH THE GROUND: red circles, rings, beams and wells are telegraphed - move or dash!",
        "Runners lunge. Brutes slam. Shades blink behind you. Snipers lock on with a laser.",
        "Wardens shield everything near them - kill them first. Summoners keep raising minions.",
        "Golden Elites carry a trait: Swift, Volatile (explodes) or Armored.",
        "",
        "A BOSS drops in every ~75 seconds - each has its own attacks and 3 phases.",
        "Stunned bosses take +50% damage. Beat one for a huge XP haul and a FREE upgrade.",
        "Waves, Swarms, Blood Moons and the Treasure Goblin keep things unpredictable.",
        "",
        "Press SPACE to start   -   ESC to go back"
    };


    private final GdxGame game;
    private final ShapeRenderer sr;
    private final Batch batch;
    private final BitmapFont font;
    private final Viewport viewport = new ScreenViewport();
    private final GlyphLayout layout = new GlyphLayout();
    private final MenuBackdrop backdrop = new MenuBackdrop();
    private final Color tmp = new Color();

    public ControlsScreen(GdxGame game) {
        this.game = game;
        this.sr = game.getShapeRenderer();
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
            game.switchScreen(new GameScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.switchScreen(new MainMenuScreen(game));
            return;
        }

        backdrop.update(deltaTime);

        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        viewport.apply();

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        float centerX = w / 2f;

        sr.setProjectionMatrix(viewport.getCamera().combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        backdrop.draw(sr, w, h);
        sr.setColor(0f, 0f, 0f, 0.4f);
        sr.rect(centerX - 520f, 90f, 1040f, h - 150f);
        sr.end();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        font.getData().setScale(1.5f);
        layout.setText(font, "HOW TO PLAY", tmp.set(1f, 0.85f, 0.35f, 1f), 0f, Align.left, false);
        font.draw(batch, layout, centerX - layout.width / 2f, h - 30f);

        font.getData().setScale(0.8f);
        float step = Math.min(36f, (h - 200f) / LINES.length);
        float y = h - 105f;
        for (String line : LINES) {
            layout.setText(font, line, tmp.set(0.92f, 0.92f, 0.97f, 1f), 0f, Align.left, false);
            font.draw(batch, layout, centerX - layout.width / 2f, y);
            y -= step;
        }

        font.getData().setScale(0.7f);
        layout.setText(font, "This game was developed by an HP agent working from 8:00 AM to 5:00 PM,",
            tmp.set(0.65f, 0.68f, 0.78f, 1f), 0f, Align.left, false);
        font.draw(batch, layout, centerX - layout.width / 2f, 70f);
        layout.setText(font, "who is currently experiencing the most difficult period of his life.");
        font.draw(batch, layout, centerX - layout.width / 2f, 45f);
        font.getData().setScale(1f);
        batch.end();
    }
}
