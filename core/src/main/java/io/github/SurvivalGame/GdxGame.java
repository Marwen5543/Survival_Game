package io.github.SurvivalGame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GdxGame extends Game {
    private ShapeRenderer shapeRenderer;
    private Batch batch;
    private BitmapFont font;
    private Music music;

    @Override
    public void create() {
        Settings.load();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Generate font from TrueType file
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(GameConfig.FONT_FILE));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.color = Color.WHITE;
        font = generator.generateFont(parameter);
        generator.dispose();

        // One music track shared by the menus and the game so it never restarts between screens.
        music = Gdx.audio.newMusic(Gdx.files.internal(GameConfig.MUSIC_FILE));
        music.setLooping(true);
        applyAudioSettings();
        music.play();

        Settings.applyVideo();
        setScreen(new MainMenuScreen(this));
    }

    public void applyAudioSettings() {
        if (music != null) {
            music.setVolume(Settings.musicVolume);
        }
    }

    /** Switches screens and frees the old one (libGDX's setScreen() does not dispose it). */
    public void switchScreen(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null) {
            old.dispose();
        }
    }

    @Override
    public void dispose() {
        Settings.save();
        Screen current = getScreen();
        super.dispose();
        if (current != null) {
            current.dispose();
        }
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        music.dispose();
    }

    public Batch getBatch() {
        return batch;
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public BitmapFont getFont() {
        return font;
    }

    public Music getMusic() {
        return music;
    }
}
