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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Keyboard (W/S, A/D, Enter) and mouse driven options menu. Changes apply live and are saved on exit. */
public class OptionsScreen extends ScreenAdapter {

    private static final class Row {
        final String label;
        final Supplier<String> value;
        final IntConsumer change;
        final Supplier<Float> fill; // optional 0..1 slider bar

        Row(String label, Supplier<String> value, IntConsumer change, Supplier<Float> fill) {
            this.label = label;
            this.value = value;
            this.change = change;
            this.fill = fill;
        }
    }

    private final GdxGame game;
    private final ShapeRenderer sr;
    private final Batch batch;
    private final BitmapFont font;
    private final Viewport viewport = new ScreenViewport();
    private final GlyphLayout layout = new GlyphLayout();
    private final MenuBackdrop backdrop = new MenuBackdrop();
    private final Vector2 mouse = new Vector2();
    private final Color tmp = new Color();
    private final List<Row> rows = new ArrayList<>();
    private int selected;

    public OptionsScreen(GdxGame game) {
        this.game = game;
        this.sr = game.getShapeRenderer();
        this.batch = game.getBatch();
        this.font = game.getFont();

        rows.add(new Row("Music Volume",
            () -> Math.round(Settings.musicVolume * 100) + "%",
            d -> {
                Settings.musicVolume = MathUtils.clamp(Math.round((Settings.musicVolume + d * 0.1f) * 10f) / 10f, 0f, 1f);
                game.applyAudioSettings();
            },
            () -> Settings.musicVolume));
        rows.add(new Row("Sound Effects",
            () -> Math.round(Settings.sfxVolume * 100) + "%",
            d -> Settings.sfxVolume = MathUtils.clamp(Math.round((Settings.sfxVolume + d * 0.1f) * 10f) / 10f, 0f, 1f),
            () -> Settings.sfxVolume));
        rows.add(new Row("Field Theme",
            () -> Settings.fieldTheme.label,
            d -> Settings.fieldTheme = Settings.cycle(Settings.FieldTheme.values(), Settings.fieldTheme, d),
            null));
        rows.add(new Row("Resolution",
            () -> Settings.RESOLUTIONS[Settings.resolutionIndex][0] + " x " + Settings.RESOLUTIONS[Settings.resolutionIndex][1],
            d -> {
                int n = Settings.RESOLUTIONS.length;
                Settings.resolutionIndex = (Settings.resolutionIndex + d + n) % n;
                Settings.applyVideo();
            },
            null));
        rows.add(new Row("Fullscreen",
            () -> Settings.fullscreen ? "ON" : "OFF",
            d -> {
                Settings.fullscreen = !Settings.fullscreen;
                Settings.applyVideo();
            },
            null));
        rows.add(new Row("Difficulty",
            () -> Settings.difficulty.label,
            d -> Settings.difficulty = Settings.cycle(Settings.Difficulty.values(), Settings.difficulty, d),
            null));
        rows.add(new Row("Enemy Speed",
            () -> Math.round(Settings.enemySpeed * 100) + "%",
            d -> Settings.enemySpeed = MathUtils.clamp(Math.round((Settings.enemySpeed + d * 0.25f) * 4f) / 4f, 0.5f, 2f),
            () -> (Settings.enemySpeed - 0.5f) / 1.5f));
        rows.add(new Row("Spawn Rate",
            () -> Math.round(Settings.spawnRate * 100) + "%",
            d -> Settings.spawnRate = MathUtils.clamp(Math.round((Settings.spawnRate + d * 0.25f) * 4f) / 4f, 0.5f, 3f),
            () -> (Settings.spawnRate - 0.5f) / 2.5f));
        rows.add(new Row("Enemy Style",
            () -> Settings.enemyStyle.label,
            d -> Settings.enemyStyle = Settings.cycle(Settings.EnemyStyle.values(), Settings.enemyStyle, d),
            null));
        rows.add(new Row("Ability Transparency",
            () -> Settings.clarity.label,
            d -> Settings.clarity = Settings.cycle(Settings.Clarity.values(), Settings.clarity, d),
            null));
        rows.add(new Row("New Enemy Types",
            () -> Settings.variants ? "ON" : "OFF",
            d -> Settings.variants = !Settings.variants,
            null));
        rows.add(new Row("Boss Fights",
            () -> Settings.bosses ? "ON" : "OFF",
            d -> Settings.bosses = !Settings.bosses,
            null));
        rows.add(new Row("Random Events",
            () -> Settings.events ? "ON" : "OFF",
            d -> Settings.events = !Settings.events,
            null));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    private void goBack() {
        Settings.save();
        game.switchScreen(new MainMenuScreen(game));
    }

    @Override
    public void render(float dt) {
        backdrop.update(dt);

        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        float cx = w / 2f;
        int n = rows.size() + 1; // +1 for BACK
        float step = Math.min(46f, (h - 200f) / n);
        float top = h - 125f;

        viewport.unproject(mouse.set(Gdx.input.getX(), Gdx.input.getY()));
        int hover = -1;
        for (int i = 0; i < n; i++) {
            float yc = top - i * step;
            if (Math.abs(mouse.y - yc) <= step / 2f && Math.abs(mouse.x - cx) <= 440f) hover = i;
        }
        if (hover >= 0 && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
            selected = hover;
        }

        // ---- input ----
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            goBack();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selected = (selected + n - 1) % n;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selected = (selected + 1) % n;
        }
        if (selected < rows.size()) {
            Row row = rows.get(selected);
            if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                row.change.accept(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                row.change.accept(1);
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            goBack();
            return;
        }
        if (Gdx.input.justTouched() && hover >= 0) {
            if (hover == rows.size()) {
                goBack();
                return;
            }
            rows.get(hover).change.accept(mouse.x < cx + 190f ? -1 : 1);
        }

        // ---- draw ----
        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        viewport.apply();

        Color accent = Settings.fieldTheme.rune;
        sr.setProjectionMatrix(viewport.getCamera().combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        backdrop.draw(sr, w, h);

        // panel behind the rows
        sr.setColor(0f, 0f, 0f, 0.35f);
        sr.rect(cx - 460f, top - (n - 1) * step - step / 2f - 10f, 920f, n * step + 20f);

        float selY = top - selected * step;
        sr.setColor(accent.r, accent.g, accent.b, 0.20f);
        sr.rect(cx - 440f, selY - step / 2f + 2f, 880f, step - 4f);
        sr.setColor(accent.r, accent.g, accent.b, 0.85f);
        sr.rect(cx - 440f, selY - step / 2f + 2f, 4f, step - 4f);

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.fill == null) continue;
            float yc = top - i * step;
            float f = MathUtils.clamp(row.fill.get(), 0f, 1f);
            sr.setColor(0.2f, 0.2f, 0.25f, 1f);
            sr.rect(cx + 320f, yc - 3f, 100f, 6f);
            sr.setColor(accent.r, accent.g, accent.b, 1f);
            sr.rect(cx + 320f, yc - 3f, 100f * f, 6f);
        }
        sr.end();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        font.getData().setScale(1.6f);
        layout.setText(font, "OPTIONS", tmp.set(1f, 0.85f, 0.35f, 1f), 0f, Align.left, false);
        font.draw(batch, layout, cx - layout.width / 2f, h - 40f);

        font.getData().setScale(0.85f);
        for (int i = 0; i < n; i++) {
            float yc = top - i * step;
            boolean sel = i == selected;
            if (i == rows.size()) {
                layout.setText(font, "BACK", tmp.set(sel ? 1f : 0.75f, sel ? 0.9f : 0.78f, sel ? 0.4f : 0.85f, 1f), 0f, Align.left, false);
                font.draw(batch, layout, cx - layout.width / 2f, yc + layout.height / 2f);
                continue;
            }
            Row row = rows.get(i);
            tmp.set(sel ? 1f : 0.78f, sel ? 0.92f : 0.8f, sel ? 0.45f : 0.88f, 1f);
            layout.setText(font, row.label, tmp, 0f, Align.left, false);
            font.draw(batch, layout, cx - 420f, yc + layout.height / 2f);

            String v = sel ? "<  " + row.value.get() + "  >" : row.value.get();
            layout.setText(font, v, tmp, 0f, Align.left, false);
            font.draw(batch, layout, cx + 190f - layout.width / 2f, yc + layout.height / 2f);
        }

        font.getData().setScale(0.7f);
        layout.setText(font, "W / S select   -   A / D or click to change   -   ESC back (saves)",
            tmp.set(0.65f, 0.7f, 0.8f, 1f), 0f, Align.left, false);
        font.draw(batch, layout, cx - layout.width / 2f, 40f);
        font.getData().setScale(1f);
        batch.end();
    }
}
