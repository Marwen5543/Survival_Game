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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MainMenuScreen extends ScreenAdapter {
    private static final String[] ITEMS = { "PLAY", "OPTIONS", "HOW TO PLAY", "QUIT" };

    private final GdxGame game;
    private final ShapeRenderer sr;
    private final Batch batch;
    private final BitmapFont font;
    private final Viewport viewport = new ScreenViewport();
    private final GlyphLayout layout = new GlyphLayout();
    private final MenuBackdrop backdrop = new MenuBackdrop();
    private final Vector2 mouse = new Vector2();
    private final Rectangle[] hit = new Rectangle[ITEMS.length];
    private final Color tmp = new Color();
    private int selected;

    public MainMenuScreen(GdxGame game) {
        this.game = game;
        this.sr = game.getShapeRenderer();
        this.batch = game.getBatch();
        this.font = game.getFont();
        for (int i = 0; i < hit.length; i++) {
            hit[i] = new Rectangle();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float dt) {
        backdrop.update(dt);

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        float step = 62f;
        float startY = h * 0.50f;

        // Hit boxes
        font.getData().setScale(1.1f);
        for (int i = 0; i < ITEMS.length; i++) {
            layout.setText(font, ITEMS[i]);
            hit[i].set(w / 2f - layout.width / 2f - 40f, startY - i * step - layout.height - 14f,
                layout.width + 80f, layout.height + 28f);
        }
        font.getData().setScale(1f);

        viewport.unproject(mouse.set(Gdx.input.getX(), Gdx.input.getY()));
        int hover = -1;
        for (int i = 0; i < ITEMS.length; i++) {
            if (hit[i].contains(mouse)) hover = i;
        }
        if (hover >= 0 && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
            selected = hover;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected + ITEMS.length - 1) % ITEMS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % ITEMS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activate(selected);
            return;
        }
        if (Gdx.input.justTouched() && hover >= 0) {
            activate(hover);
            return;
        }

        // ---- draw ----
        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        viewport.apply();

        sr.setProjectionMatrix(viewport.getCamera().combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        backdrop.draw(sr, w, h);
        Color accent = Settings.fieldTheme.rune;
        sr.setColor(accent.r, accent.g, accent.b, 0.20f);
        Rectangle r = hit[selected];
        sr.rect(r.x, r.y, r.width, r.height);
        sr.setColor(accent.r, accent.g, accent.b, 0.8f);
        sr.rect(r.x, r.y, 5f, r.height);
        sr.rect(r.x + r.width - 5f, r.y, 5f, r.height);
        sr.end();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        backdrop.drawTitle(batch, font, layout, w, h * 0.86f);

        for (int i = 0; i < ITEMS.length; i++) {
            boolean sel = i == selected;
            font.getData().setScale(sel ? 1.25f : 1.1f);
            tmp.set(sel ? 1f : 0.75f, sel ? 0.9f : 0.78f, sel ? 0.4f : 0.85f, 1f);
            layout.setText(font, ITEMS[i], tmp, 0f, Align.left, false);
            font.draw(batch, layout, w / 2f - layout.width / 2f, startY - i * step);
        }

        font.getData().setScale(0.75f);
        String best = "Best run: " + Settings.highScore + " kills   -   Longest survival: "
            + formatTime(Settings.bestTimeSeconds);
        layout.setText(font, best, tmp.set(0.8f, 0.85f, 0.95f, 1f), 0f, Align.left, false);
        font.draw(batch, layout, w / 2f - layout.width / 2f, 70f);
        layout.setText(font, "W / S + ENTER   or   mouse", tmp.set(0.6f, 0.65f, 0.75f, 1f), 0f, Align.left, false);
        font.draw(batch, layout, w / 2f - layout.width / 2f, 40f);
        font.getData().setScale(1f);
        batch.end();
    }

    private void activate(int index) {
        switch (index) {
            case 0 -> game.switchScreen(new GameScreen(game));
            case 1 -> game.switchScreen(new OptionsScreen(game));
            case 2 -> game.switchScreen(new ControlsScreen(game));
            default -> Gdx.app.exit();
        }
    }

    static String formatTime(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
