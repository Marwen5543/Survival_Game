package io.github.SurvivalGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Paints a unique sprite for every enemy type and boss at runtime — no art files needed.
 * Drop a PNG named enemy_<type>.png / boss_<kind>.png (lower-case) into assets to override any of them.
 */
public class SpriteFactory {
    private static final int S = 64;   // enemy canvas
    private static final int B = 128;  // boss canvas
    private static final Color OUT = new Color(0.07f, 0.05f, 0.1f, 1f);

    private final Map<String, Texture> cache = new HashMap<>();

    public Texture enemy(Enemy.Type type) {
        return get("enemy_" + type.name().toLowerCase(), () -> paintEnemy(type));
    }

    public Texture boss(Boss.Kind kind) {
        return get("boss_" + kind.name().toLowerCase(), () -> paintBoss(kind));
    }

    public void dispose() {
        for (Texture t : cache.values()) {
            t.dispose();
        }
        cache.clear();
    }

    private Texture get(String key, Supplier<Pixmap> painter) {
        Texture texture = cache.get(key);
        if (texture != null) {
            return texture;
        }
        String file = key + ".png";
        if (Gdx.files.internal(file).exists()) {
            texture = new Texture(Gdx.files.internal(file));
        } else {
            Pixmap pm = painter.get();
            texture = new Texture(pm);
            pm.dispose();
        }
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cache.put(key, texture);
        return texture;
    }

    private Pixmap paintEnemy(Enemy.Type type) {
        return switch (type) {
            case SLIME, BOSS -> slime();
            case RUNNER -> runner();
            case BRUTE -> brute();
            case SPLITTER -> splitter();
            case MINI -> mini();
            case SPITTER -> spitter();
            case BOMBER -> bomber();
            case GOBLIN -> goblin();
            case SHADE -> shade();
            case WARDEN -> warden();
            case SUMMONER -> summoner();
            case SNIPER -> sniper();
        };
    }

    private Pixmap paintBoss(Boss.Kind kind) {
        return switch (kind) {
            case JUGGERNAUT -> juggernaut();
            case WARLOCK -> warlock();
            case HIVE_QUEEN -> hiveQueen();
            case STORM_WRAITH -> stormWraith();
        };
    }

    // ================================================================= helpers

    private static Pixmap blank(int size) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();
        return pm;
    }

    private static Color c(float r, float g, float b) {
        return new Color(r, g, b, 1f);
    }

    private static Color al(Color col, float a) {
        return new Color(col.r, col.g, col.b, a);
    }

    private static Color sh(Color col, float m) {
        return new Color(Math.min(1f, col.r * m), Math.min(1f, col.g * m), Math.min(1f, col.b * m), col.a);
    }

    private static void oval(Pixmap pm, int cx, int cy, int rx, int ry, Color col) {
        if (rx <= 0 || ry <= 0) return;
        pm.setColor(col);
        for (int y = -ry; y <= ry; y++) {
            float t = (float) y / ry;
            int half = Math.round(rx * (float) Math.sqrt(Math.max(0f, 1f - t * t)));
            pm.fillRectangle(cx - half, cy + y, half * 2 + 1, 1);
        }
    }

    /** Outlined, shaded, highlighted blob — the workhorse of the creature art. */
    private static void blob(Pixmap pm, int cx, int cy, int rx, int ry, Color fill) {
        oval(pm, cx, cy, rx + 2, ry + 2, sh(fill, 0.28f));
        oval(pm, cx, cy, rx, ry, fill);
        oval(pm, cx, cy + ry / 3, Math.max(1, rx - 2), Math.max(1, ry * 2 / 3), sh(fill, 0.82f));
        oval(pm, cx, cy - ry / 6, Math.max(1, rx - 3), Math.max(1, ry * 2 / 3), sh(fill, 1.06f));
        oval(pm, cx - rx / 3, cy - ry / 3, Math.max(2, rx / 3), Math.max(1, ry / 4), al(Color.WHITE, 0.32f));
    }

    private static void eye(Pixmap pm, int x, int y, int r, int dx, int dy) {
        pm.setColor(OUT);
        pm.fillCircle(x, y, r + 1);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(x, y, r);
        pm.setColor(0.05f, 0.05f, 0.1f, 1f);
        pm.fillCircle(x + dx, y + dy, Math.max(1, r / 2));
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(x + dx - 1, y + dy - 1, Math.max(1, r / 5));
    }

    private static void tri(Pixmap pm, int x1, int y1, int x2, int y2, int x3, int y3, Color col) {
        pm.setColor(col);
        pm.fillTriangle(x1, y1, x2, y2, x3, y3);
    }

    private static void rect(Pixmap pm, int x, int y, int w, int h, Color col) {
        pm.setColor(col);
        pm.fillRectangle(x, y, w, h);
    }

    private static void line(Pixmap pm, int x1, int y1, int x2, int y2, int w, Color col) {
        pm.setColor(col);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < w; j++) {
                pm.drawLine(x1 + i - w / 2, y1 + j - w / 2, x2 + i - w / 2, y2 + j - w / 2);
            }
        }
    }

    private static void glow(Pixmap pm, int x, int y, int r, Color col) {
        for (int i = r; i > 0; i -= 2) {
            float t = 1f - (float) i / r;
            pm.setColor(col.r, col.g, col.b, 0.10f + 0.35f * t);
            pm.fillCircle(x, y, i);
        }
    }

    // ================================================================= enemies (64x64)

    private static Pixmap slime() {
        Pixmap pm = blank(S);
        Color g = c(0.35f, 0.85f, 0.42f);
        blob(pm, 32, 38, 23, 19, g);
        pm.setColor(1f, 1f, 1f, 0.25f);
        pm.fillCircle(20, 44, 3);
        pm.fillCircle(44, 46, 2);
        pm.fillCircle(38, 51, 2);
        eye(pm, 24, 33, 6, 1, 1);
        eye(pm, 40, 33, 6, -1, 1);
        pm.setColor(0.1f, 0.25f, 0.12f, 1f);
        pm.drawLine(26, 44, 29, 47);
        pm.drawLine(29, 47, 35, 47);
        pm.drawLine(35, 47, 38, 44);
        return pm;
    }

    private static Pixmap runner() {
        Pixmap pm = blank(S);
        Color k = c(0.35f, 0.8f, 1f);
        tri(pm, 2, 34, 22, 20, 22, 48, sh(k, 0.55f));
        tri(pm, 8, 34, 24, 27, 24, 41, c(0.85f, 0.97f, 1f));
        tri(pm, 31, 12, 41, 22, 27, 22, sh(k, 0.7f));
        blob(pm, 37, 35, 19, 15, k);
        eye(pm, 44, 31, 5, 2, 0);
        eye(pm, 33, 31, 4, 2, 0);
        line(pm, 38, 24, 48, 28, 2, OUT);
        line(pm, 30, 25, 37, 27, 2, OUT);
        line(pm, 30, 49, 25, 60, 3, sh(k, 0.5f));
        line(pm, 44, 49, 49, 60, 3, sh(k, 0.5f));
        return pm;
    }

    private static Pixmap brute() {
        Pixmap pm = blank(S);
        Color r = c(0.82f, 0.25f, 0.25f);
        Color bone = c(0.95f, 0.92f, 0.8f);
        oval(pm, 9, 38, 7, 12, sh(r, 0.4f));
        oval(pm, 9, 38, 5, 10, sh(r, 0.85f));
        oval(pm, 55, 38, 7, 12, sh(r, 0.4f));
        oval(pm, 55, 38, 5, 10, sh(r, 0.85f));
        pm.setColor(sh(r, 0.55f));
        pm.fillCircle(9, 50, 5);
        pm.fillCircle(55, 50, 5);
        blob(pm, 32, 37, 19, 21, r);
        tri(pm, 14, 22, 17, 3, 25, 18, bone);
        tri(pm, 50, 22, 47, 3, 39, 18, bone);
        line(pm, 19, 27, 29, 31, 3, OUT);
        line(pm, 45, 27, 35, 31, 3, OUT);
        pm.setColor(1f, 0.85f, 0.2f, 1f);
        pm.fillCircle(25, 33, 3);
        pm.fillCircle(39, 33, 3);
        pm.setColor(0.1f, 0f, 0f, 1f);
        pm.fillCircle(25, 33, 1);
        pm.fillCircle(39, 33, 1);
        rect(pm, 24, 44, 16, 6, OUT);
        tri(pm, 25, 44, 28, 54, 31, 44, bone);
        tri(pm, 33, 44, 36, 54, 39, 44, bone);
        return pm;
    }

    private static Pixmap splitter() {
        Pixmap pm = blank(S);
        Color l = c(0.62f, 0.95f, 0.35f);
        blob(pm, 32, 38, 22, 19, l);
        line(pm, 32, 20, 29, 57, 3, sh(l, 0.3f));
        pm.setColor(sh(l, 0.6f));
        pm.fillCircle(22, 44, 4);
        pm.fillCircle(42, 44, 4);
        eye(pm, 23, 32, 5, 1, 1);
        eye(pm, 41, 32, 5, -1, 1);
        return pm;
    }

    private static Pixmap mini() {
        Pixmap pm = blank(S);
        Color m = c(0.75f, 1f, 0.65f);
        blob(pm, 32, 42, 15, 13, m);
        eye(pm, 32, 39, 8, 0, 1);
        return pm;
    }

    private static Pixmap spitter() {
        Pixmap pm = blank(S);
        Color p = c(0.62f, 0.35f, 0.85f);
        tri(pm, 18, 28, 21, 10, 27, 24, sh(p, 0.55f));
        tri(pm, 46, 28, 43, 10, 37, 24, sh(p, 0.55f));
        blob(pm, 32, 40, 19, 17, p);
        oval(pm, 32, 31, 10, 8, OUT);
        glow(pm, 32, 31, 8, c(1f, 0.4f, 0.9f));
        pm.setColor(1f, 0.85f, 1f, 1f);
        pm.fillCircle(32, 31, 3);
        eye(pm, 19, 40, 4, 1, 0);
        eye(pm, 45, 40, 4, -1, 0);
        return pm;
    }

    private static Pixmap bomber() {
        Pixmap pm = blank(S);
        Color b = c(0.2f, 0.2f, 0.26f);
        blob(pm, 32, 40, 20, 19, b);
        pm.setColor(1f, 0.8f, 0.15f, 0.9f);
        pm.fillRectangle(14, 40, 36, 5);
        line(pm, 32, 22, 38, 13, 3, c(0.5f, 0.35f, 0.2f));
        glow(pm, 39, 11, 9, c(1f, 0.55f, 0.1f));
        pm.setColor(1f, 0.95f, 0.5f, 1f);
        pm.fillCircle(39, 11, 3);
        eye(pm, 25, 35, 5, 1, 1);
        eye(pm, 39, 35, 5, -1, 1);
        line(pm, 19, 28, 29, 32, 2, c(1f, 0.8f, 0.15f));
        line(pm, 45, 28, 35, 32, 2, c(1f, 0.8f, 0.15f));
        return pm;
    }

    private static Pixmap goblin() {
        Pixmap pm = blank(S);
        Color g = c(0.4f, 0.78f, 0.3f);
        oval(pm, 30, 50, 10, 9, sh(g, 0.65f));
        tri(pm, 8, 26, 20, 19, 20, 35, g);
        tri(pm, 52, 26, 40, 19, 40, 35, g);
        blob(pm, 30, 30, 14, 13, g);
        eye(pm, 25, 28, 4, 1, 0);
        eye(pm, 35, 28, 4, -1, 0);
        pm.setColor(OUT);
        pm.drawLine(23, 37, 30, 41);
        pm.drawLine(30, 41, 37, 37);
        rect(pm, 27, 38, 6, 2, c(1f, 1f, 1f));
        blob(pm, 49, 46, 11, 11, c(0.95f, 0.78f, 0.2f));
        pm.setColor(1f, 1f, 0.6f, 1f);
        pm.fillCircle(46, 43, 3);
        pm.fillCircle(52, 49, 2);
        return pm;
    }

    private static Pixmap shade() {
        Pixmap pm = blank(S);
        Color d = c(0.17f, 0.1f, 0.28f);
        tri(pm, 32, 3, 6, 59, 58, 59, OUT);
        tri(pm, 32, 7, 11, 57, 53, 57, d);
        for (int i = 0; i < 5; i++) {
            tri(pm, 10 + i * 9, 56, 14 + i * 9, 63, 18 + i * 9, 56, d);
        }
        line(pm, 32, 8, 14, 46, 1, c(0.45f, 0.3f, 0.7f));
        oval(pm, 32, 27, 9, 11, c(0.02f, 0f, 0.05f));
        glow(pm, 27, 27, 5, c(0.8f, 0.6f, 1f));
        glow(pm, 37, 27, 5, c(0.8f, 0.6f, 1f));
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(27, 27, 2);
        pm.fillCircle(37, 27, 2);
        return pm;
    }

    private static Pixmap warden() {
        Pixmap pm = blank(S);
        Color st = c(0.55f, 0.6f, 0.72f);
        oval(pm, 32, 47, 16, 14, OUT);
        oval(pm, 32, 47, 14, 12, sh(st, 0.8f));
        tri(pm, 32, 2, 27, 15, 37, 15, c(0.9f, 0.25f, 0.25f));
        blob(pm, 32, 26, 12, 12, st);
        rect(pm, 22, 24, 20, 5, OUT);
        rect(pm, 24, 25, 16, 2, c(0.4f, 0.9f, 1f));
        line(pm, 51, 52, 57, 22, 3, c(0.4f, 0.3f, 0.2f));
        pm.setColor(sh(st, 0.9f));
        pm.fillCircle(57, 20, 6);
        tri(pm, 3, 26, 27, 26, 15, 60, OUT);
        tri(pm, 6, 28, 24, 28, 15, 55, c(0.25f, 0.45f, 0.95f));
        line(pm, 15, 29, 15, 52, 2, c(1f, 0.85f, 0.3f));
        line(pm, 8, 36, 22, 36, 2, c(1f, 0.85f, 0.3f));
        glow(pm, 32, 47, 6, c(0.4f, 0.9f, 1f));
        return pm;
    }

    private static Pixmap summoner() {
        Pixmap pm = blank(S);
        Color t = c(0.18f, 0.55f, 0.55f);
        tri(pm, 32, 5, 6, 61, 58, 61, OUT);
        tri(pm, 32, 9, 11, 59, 53, 59, t);
        line(pm, 32, 30, 32, 58, 1, sh(t, 0.6f));
        blob(pm, 32, 22, 11, 11, sh(t, 0.8f));
        oval(pm, 32, 24, 7, 8, c(0.02f, 0.05f, 0.06f));
        glow(pm, 29, 24, 4, c(0.4f, 1f, 0.9f));
        glow(pm, 35, 24, 4, c(0.4f, 1f, 0.9f));
        pm.setColor(0.85f, 1f, 1f, 1f);
        pm.fillCircle(29, 24, 1);
        pm.fillCircle(35, 24, 1);
        glow(pm, 9, 32, 7, c(0.4f, 1f, 0.8f));
        glow(pm, 56, 42, 7, c(0.4f, 1f, 0.8f));
        line(pm, 51, 24, 53, 61, 3, c(0.45f, 0.35f, 0.25f));
        pm.setColor(0.95f, 0.95f, 0.85f, 1f);
        pm.fillCircle(51, 19, 5);
        pm.setColor(OUT);
        pm.fillCircle(49, 19, 1);
        pm.fillCircle(53, 19, 1);
        return pm;
    }

    private static Pixmap sniper() {
        Pixmap pm = blank(S);
        for (int i = 0; i < 4; i++) {
            line(pm, 20 + i * 8, 48, 17 + i * 10, 62, 3, c(0.55f, 0.1f, 0.15f));
        }
        blob(pm, 32, 30, 24, 22, c(0.96f, 0.92f, 0.92f));
        pm.setColor(0.8f, 0.15f, 0.2f, 0.7f);
        pm.drawLine(10, 30, 20, 28);
        pm.drawLine(12, 40, 22, 36);
        pm.drawLine(54, 28, 44, 29);
        pm.drawLine(52, 40, 42, 36);
        glow(pm, 32, 31, 15, c(1f, 0.3f, 0.2f));
        pm.setColor(0.9f, 0.15f, 0.15f, 1f);
        pm.fillCircle(32, 31, 12);
        pm.setColor(0.05f, 0f, 0f, 1f);
        pm.fillCircle(32, 31, 6);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(29, 28, 2);
        tri(pm, 16, 12, 19, 3, 23, 11, OUT);
        tri(pm, 30, 8, 32, 0, 35, 8, OUT);
        tri(pm, 42, 11, 46, 3, 49, 12, OUT);
        return pm;
    }

    // ================================================================= bosses (128x128)

    private static Pixmap juggernaut() {
        Pixmap pm = blank(B);
        Color iron = c(0.52f, 0.52f, 0.6f);
        Color dark = sh(iron, 0.5f);
        Color bone = c(0.95f, 0.92f, 0.8f);
        // legs + feet
        rect(pm, 34, 86, 24, 36, OUT);
        rect(pm, 37, 88, 18, 32, dark);
        rect(pm, 70, 86, 24, 36, OUT);
        rect(pm, 73, 88, 18, 32, dark);
        rect(pm, 30, 112, 32, 12, OUT);
        rect(pm, 32, 114, 28, 8, sh(iron, 0.4f));
        rect(pm, 66, 112, 32, 12, OUT);
        rect(pm, 68, 114, 28, 8, sh(iron, 0.4f));
        // arms + fists
        oval(pm, 20, 74, 15, 26, OUT);
        oval(pm, 20, 74, 12, 23, iron);
        oval(pm, 108, 74, 15, 26, OUT);
        oval(pm, 108, 74, 12, 23, iron);
        oval(pm, 18, 102, 15, 14, OUT);
        oval(pm, 18, 102, 12, 11, sh(iron, 0.7f));
        oval(pm, 110, 102, 15, 14, OUT);
        oval(pm, 110, 102, 12, 11, sh(iron, 0.7f));
        // shoulder spikes
        tri(pm, 6, 52, 16, 26, 30, 54, sh(iron, 0.9f));
        tri(pm, 122, 52, 112, 26, 98, 54, sh(iron, 0.9f));
        // torso
        blob(pm, 64, 70, 38, 34, iron);
        rect(pm, 30, 86, 68, 6, dark);
        // core
        glow(pm, 64, 70, 18, c(1f, 0.45f, 0.1f));
        pm.setColor(1f, 0.92f, 0.55f, 1f);
        pm.fillCircle(64, 70, 7);
        line(pm, 64, 70, 44, 58, 2, c(1f, 0.4f, 0.15f));
        line(pm, 64, 70, 86, 56, 2, c(1f, 0.4f, 0.15f));
        line(pm, 64, 70, 60, 92, 2, c(1f, 0.4f, 0.15f));
        // head + horns
        tri(pm, 47, 26, 36, 4, 56, 20, bone);
        tri(pm, 81, 26, 92, 4, 72, 20, bone);
        blob(pm, 64, 32, 17, 15, sh(iron, 0.85f));
        rect(pm, 51, 29, 26, 7, OUT);
        rect(pm, 53, 31, 22, 3, c(1f, 0.55f, 0.15f));
        glow(pm, 64, 32, 10, c(1f, 0.4f, 0.1f));
        // rivets
        pm.setColor(0.85f, 0.85f, 0.9f, 1f);
        for (int i = 0; i < 4; i++) {
            pm.fillCircle(42 + i * 15, 94, 2);
        }
        return pm;
    }

    private static Pixmap warlock() {
        Pixmap pm = blank(B);
        Color robe = c(0.3f, 0.12f, 0.5f);
        // robe
        tri(pm, 64, 34, 12, 126, 116, 126, OUT);
        tri(pm, 64, 40, 20, 122, 108, 122, robe);
        for (int i = 0; i < 7; i++) {
            tri(pm, 18 + i * 13, 120, 24 + i * 13, 127, 30 + i * 13, 120, robe);
        }
        pm.setColor(0.8f, 0.5f, 1f, 0.8f);
        for (int i = 0; i < 4; i++) {
            pm.drawLine(52 + i * 7, 78 + i * 6, 56 + i * 7, 90 + i * 6);
            pm.drawCircle(64, 100, 6 + i * 2);
        }
        // arms + orbs
        line(pm, 50, 70, 22, 78, 7, sh(robe, 0.7f));
        line(pm, 78, 70, 106, 78, 7, sh(robe, 0.7f));
        glow(pm, 20, 78, 14, c(0.8f, 0.3f, 1f));
        pm.setColor(1f, 0.9f, 1f, 1f);
        pm.fillCircle(20, 78, 5);
        glow(pm, 108, 78, 14, c(0.8f, 0.3f, 1f));
        pm.setColor(1f, 0.9f, 1f, 1f);
        pm.fillCircle(108, 78, 5);
        // horns
        tri(pm, 44, 30, 28, 4, 54, 22, sh(robe, 0.55f));
        tri(pm, 84, 30, 100, 4, 74, 22, sh(robe, 0.55f));
        // mask
        blob(pm, 64, 42, 22, 24, c(0.88f, 0.85f, 0.92f));
        oval(pm, 64, 45, 15, 10, OUT);
        glow(pm, 64, 45, 10, c(0.9f, 0.3f, 1f));
        pm.setColor(1f, 0.85f, 1f, 1f);
        pm.fillCircle(64, 45, 6);
        pm.setColor(OUT);
        pm.fillRectangle(63, 38, 2, 14);
        glow(pm, 64, 22, 6, c(0.5f, 1f, 1f));
        // cheek runes
        pm.setColor(0.5f, 0.2f, 0.8f, 1f);
        pm.drawLine(50, 56, 56, 64);
        pm.drawLine(78, 56, 72, 64);
        return pm;
    }

    private static Pixmap hiveQueen() {
        Pixmap pm = blank(B);
        Color gr = c(0.35f, 0.65f, 0.25f);
        // wings
        oval(pm, 28, 52, 26, 11, al(c(0.7f, 1f, 0.7f), 0.38f));
        oval(pm, 100, 52, 26, 11, al(c(0.7f, 1f, 0.7f), 0.38f));
        oval(pm, 34, 66, 20, 8, al(c(0.7f, 1f, 0.7f), 0.3f));
        oval(pm, 94, 66, 20, 8, al(c(0.7f, 1f, 0.7f), 0.3f));
        // legs
        for (int i = 0; i < 3; i++) {
            line(pm, 50, 62 + i * 6, 16, 74 + i * 12, 3, sh(gr, 0.45f));
            line(pm, 78, 62 + i * 6, 112, 74 + i * 12, 3, sh(gr, 0.45f));
        }
        // abdomen
        blob(pm, 64, 96, 34, 28, gr);
        for (int i = 0; i < 4; i++) {
            line(pm, 36, 82 + i * 8, 92, 82 + i * 8, 2, sh(gr, 0.55f));
        }
        pm.setColor(0.95f, 0.95f, 0.5f, 1f);
        pm.fillCircle(50, 114, 5);
        pm.fillCircle(64, 118, 5);
        pm.fillCircle(78, 114, 5);
        // thorax
        blob(pm, 64, 58, 18, 16, sh(gr, 1.1f));
        // head
        blob(pm, 64, 32, 16, 15, sh(gr, 1.15f));
        pm.setColor(1f, 0.2f, 0.2f, 1f);
        pm.fillCircle(56, 28, 3);
        pm.fillCircle(72, 28, 3);
        pm.fillCircle(50, 35, 2);
        pm.fillCircle(78, 35, 2);
        tri(pm, 55, 42, 58, 58, 64, 45, OUT);
        tri(pm, 73, 42, 70, 58, 64, 45, OUT);
        // crown
        Color gold = c(1f, 0.85f, 0.25f);
        tri(pm, 50, 20, 55, 4, 60, 20, gold);
        tri(pm, 60, 20, 64, 0, 68, 20, gold);
        tri(pm, 68, 20, 73, 4, 78, 20, gold);
        return pm;
    }

    private static Pixmap stormWraith() {
        Pixmap pm = blank(B);
        Color cl = c(0.28f, 0.3f, 0.45f);
        // tail wisps
        tri(pm, 64, 70, 34, 126, 64, 108, sh(cl, 0.7f));
        tri(pm, 64, 70, 94, 126, 64, 108, sh(cl, 0.7f));
        tri(pm, 64, 80, 54, 127, 74, 127, sh(cl, 0.55f));
        // cloud body: outlines first, then fills
        int[][] puffs = { { 64, 62, 34, 28 }, { 36, 56, 20, 18 }, { 92, 56, 20, 18 }, { 64, 40, 22, 18 } };
        for (int[] p : puffs) {
            oval(pm, p[0], p[1], p[2] + 2, p[3] + 2, sh(cl, 0.28f));
        }
        for (int[] p : puffs) {
            oval(pm, p[0], p[1], p[2], p[3], cl);
        }
        oval(pm, 64, 36, 18, 10, sh(cl, 1.25f));
        oval(pm, 50, 34, 8, 4, al(Color.WHITE, 0.25f));
        // lightning veins
        Color yel = c(1f, 0.95f, 0.4f);
        line(pm, 30, 60, 40, 70, 2, yel);
        line(pm, 40, 70, 34, 82, 2, yel);
        line(pm, 98, 60, 88, 72, 2, yel);
        line(pm, 88, 72, 94, 84, 2, yel);
        line(pm, 60, 84, 66, 94, 2, yel);
        line(pm, 66, 94, 58, 104, 2, yel);
        // crown of lightning
        tri(pm, 46, 28, 50, 6, 56, 26, yel);
        tri(pm, 58, 24, 64, 0, 70, 24, yel);
        tri(pm, 72, 26, 78, 6, 82, 28, yel);
        // eyes
        glow(pm, 50, 52, 11, c(1f, 0.95f, 0.3f));
        glow(pm, 78, 52, 11, c(1f, 0.95f, 0.3f));
        oval(pm, 50, 52, 8, 5, c(1f, 1f, 0.85f));
        oval(pm, 78, 52, 8, 5, c(1f, 1f, 0.85f));
        pm.setColor(OUT);
        pm.fillCircle(52, 52, 2);
        pm.fillCircle(76, 52, 2);
        line(pm, 40, 44, 56, 50, 3, OUT);
        line(pm, 88, 44, 72, 50, 3, OUT);
        // jagged mouth
        line(pm, 50, 70, 56, 74, 2, OUT);
        line(pm, 56, 74, 62, 69, 2, OUT);
        line(pm, 62, 69, 68, 74, 2, OUT);
        line(pm, 68, 74, 74, 70, 2, OUT);
        line(pm, 74, 70, 78, 72, 2, OUT);
        return pm;
    }
}
