package io.github.SurvivalGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The "choose 1 of 3" screen shown on every level-up. The game is frozen while it's open.
 * Pick with the 1/2/3 keys or by clicking a card. Cards slide in, lift on hover, and show
 * level pips so you can see exactly what the upgrade will do.
 */
public class LevelUpMenu {
    private static final float CARD_W = 300f;
    private static final float CARD_H = 400f;
    private static final float GAP = 36f;
    private static final float OPEN_ANIM = 0.35f;
    private static final float INPUT_DELAY = 0.30f; // stops a stray keypress from instantly picking
    private static final float HOVER_LIFT = 12f;
    private static final int[] KEYS = { Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3 };

    private static final Color BG = new Color(0.08f, 0.08f, 0.12f, 1f);

    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 mouse = new Vector2();
    private final Color tmp = new Color();
    private final Rectangle[] cards = { new Rectangle(), new Rectangle(), new Rectangle() };

    private Upgrade[] choices = new Upgrade[0];
    private int[] currentLevels = new int[0];
    private int playerLevel;
    private boolean open;
    private float openTimer;
    private int hovered = -1;

    public void open(Upgrade[] choices, Player player) {
        this.choices = choices;
        this.currentLevels = new int[choices.length];
        for (int i = 0; i < choices.length; i++) {
            currentLevels[i] = player.getUpgradeLevel(choices[i]);
        }
        this.playerLevel = player.getLevel();
        this.open = true;
        this.openTimer = 0f;
        this.hovered = -1;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    /** @return the upgrade the player just picked this frame, or null if still deciding. */
    public Upgrade update(float deltaTime, Viewport uiViewport) {
        openTimer += deltaTime;
        layoutCards(uiViewport);

        uiViewport.unproject(mouse.set(Gdx.input.getX(), Gdx.input.getY()));
        hovered = -1;
        for (int i = 0; i < choices.length; i++) {
            if (cards[i].contains(mouse)) {
                hovered = i;
            }
        }

        if (openTimer < INPUT_DELAY) {
            return null;
        }

        for (int i = 0; i < choices.length && i < KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(KEYS[i])) {
                return choices[i];
            }
        }
        if (Gdx.input.justTouched() && hovered >= 0) {
            return choices[hovered];
        }
        return null;
    }

    private void layoutCards(Viewport viewport) {
        int n = choices.length;
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();

        float cardW = Math.min(CARD_W, (width - 80f - GAP * (n - 1)) / n);
        float totalW = n * cardW + (n - 1) * GAP;
        float startX = (width - totalW) / 2f;
        float y = height / 2f - CARD_H / 2f - 30f;

        for (int i = 0; i < n; i++) {
            cards[i].set(startX + i * (cardW + GAP), y, cardW, CARD_H);
        }
    }

    public void draw(ShapeRenderer sr, Batch batch, BitmapFont font, Viewport viewport) {
        if (!open) return;

        float t = Math.min(1f, openTimer / OPEN_ANIM);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();

        // ---------- Shapes ----------
        sr.setProjectionMatrix(viewport.getCamera().combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        sr.setColor(0f, 0f, 0.02f, 0.75f * ease);
        sr.rect(0, 0, width, height);

        for (int i = 0; i < choices.length; i++) {
            Rectangle r = cards[i];
            Upgrade u = choices[i];
            Color c = u.color;
            boolean hot = i == hovered;

            float x = r.x;
            float y = cardY(i, ease);
            float cx = x + r.width / 2f;
            float cy = y + r.height - 95f;

            if (hot) {
                sr.setColor(c.r, c.g, c.b, 0.22f * ease);
                sr.rect(x - 10, y - 10, r.width + 20, r.height + 20);
            }
            sr.setColor(c.r, c.g, c.b, ease);
            sr.rect(x - 3, y - 3, r.width + 6, r.height + 6);
            sr.setColor(BG.r, BG.g, BG.b, ease);
            sr.rect(x, y, r.width, r.height);

            // Tinted header band
            sr.setColor(c.r * 0.35f, c.g * 0.35f, c.b * 0.35f, ease);
            sr.rect(x, y + r.height - 160f, r.width, 160f);

            // Icon badge
            sr.setColor(c.r, c.g, c.b, 0.25f * ease);
            sr.circle(cx, cy, 48f, 32);
            sr.setColor(c.r, c.g, c.b, ease);
            sr.circle(cx, cy, 38f, 32);
            sr.setColor(BG.r, BG.g, BG.b, ease);
            sr.circle(cx, cy, 31f, 32);

            // Level pips
            if (u.hasPips()) {
                int pips = Math.min(u.maxLevel, 5);
                int nextLevel = currentLevels[i] + 1;
                float pipY = y + r.height - 222f;
                float startX = cx - pips * 18f / 2f + 9f;
                for (int p = 0; p < pips; p++) {
                    int pipLevel = p + 1;
                    float px = startX + p * 18f;
                    if (pipLevel < nextLevel) {
                        sr.setColor(c.r, c.g, c.b, ease);
                    } else if (pipLevel == nextLevel) {
                        sr.setColor(1f, 1f, 1f, ease);
                    } else {
                        sr.setColor(0.25f, 0.25f, 0.3f, ease);
                    }
                    sr.circle(px, pipY, 6f, 12);
                }
            }
        }
        sr.end();

        // ---------- Text ----------
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        float cardsTop = cards[0].y + CARD_H;

        font.getData().setScale(1.6f);
        drawCentered(font, batch, "LEVEL UP!", 0, cardsTop + 105f, width, tmp.set(1f, 0.9f, 0.4f, ease));
        font.getData().setScale(0.8f);
        drawCentered(font, batch, "Reached level " + playerLevel + " - choose an upgrade  (1 / 2 / 3 or click)",
            0, cardsTop + 48f, width, tmp.set(0.8f, 0.82f, 0.9f, ease));

        for (int i = 0; i < choices.length; i++) {
            Rectangle r = cards[i];
            Upgrade u = choices[i];
            Color c = u.color;

            float x = r.x;
            float y = cardY(i, ease);
            float cx = x + r.width / 2f;
            float cy = y + r.height - 95f;
            int cur = currentLevels[i];

            // Tag line
            String tag;
            if (u == Upgrade.HEAL) tag = "RECOVER";
            else if (cur == 0) tag = "NEW!";
            else tag = "LEVEL " + cur + " -> " + (cur + 1);
            font.getData().setScale(0.75f);
            drawCentered(font, batch, tag, x, y + r.height - 14f, r.width, tmp.set(1f, 1f, 1f, ease));

            // Icon glyph
            font.getData().setScale(1.5f);
            layout.setText(font, u.icon, tmp.set(c.r, c.g, c.b, ease), 0f, Align.center, false);
            font.draw(batch, layout, cx - layout.width / 2f, cy + layout.height / 2f);

            // Title
            font.getData().setScale(1f);
            drawCentered(font, batch, u.title, x + 15f, y + r.height - 175f, r.width - 30f, tmp.set(1f, 1f, 1f, ease));

            // Description
            font.getData().setScale(0.78f);
            drawCentered(font, batch, u.describe(cur + 1), x + 22f, y + r.height - 248f, r.width - 44f,
                tmp.set(0.78f, 0.8f, 0.85f, ease));

            // Category + key hint
            font.getData().setScale(0.7f);
            drawCentered(font, batch, u.category(), x, y + 66f, r.width, tmp.set(c.r, c.g, c.b, ease));
            font.getData().setScale(0.95f);
            drawCentered(font, batch, "[ " + (i + 1) + " ]", x, y + 40f, r.width, tmp.set(1f, 1f, 1f, ease));
        }

        font.getData().setScale(1f);
        batch.end();
    }

    private float cardY(int index, float ease) {
        float y = cards[index].y - (1f - ease) * 70f;
        return index == hovered ? y + HOVER_LIFT : y;
    }

    private void drawCentered(BitmapFont font, Batch batch, String text, float x, float topY, float width, Color color) {
        layout.setText(font, text, color, width, Align.center, true);
        font.draw(batch, layout, x, topY);
    }
}
