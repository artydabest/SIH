package com.sih.trial2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.DisplayMetrics;

import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;

/**
 * Custom marker icons drawn programmatically with Canvas:
 * distinct shape + color per layer so the map reads at a glance:
 *   Friend safe    = green circle with ✓
 *   Friend unsafe  = red circle with !
 *   Safe place     = green square with a cross (like a shelter sign)
 *   Volunteer      = blue square with a phone handset dot
 *   You            = blue circle with white ring
 */
public final class MapIcons {

    private static final int SIZE_DP = 26;

    private MapIcons() {
    }

    public static Icon friendSafe(Context context) {
        return circle(context, "#188038", "✓");
    }

    public static Icon friendUnsafe(Context context) {
        return circle(context, "#d93025", "!");
    }

    public static Icon safePlace(Context context) {
        return square(context, "#188038", "+");
    }

    public static Icon volunteer(Context context) {
        return square(context, "#0a84ff", "☎");
    }

    public static Icon self(Context context) {
        return circle(context, "#1a73e8", "•");
    }

    // ---- drawing helpers -------------------------------------------------

    private static Icon circle(Context context, String colorHex, String glyph) {
        int size = px(context);
        float density = context.getResources().getDisplayMetrics().density;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int color = Color.parseColor(colorHex);

        // White outline for contrast on any basemap
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - dp(density, 2), paint);

        drawGlyph(canvas, paint, glyph, size, density);
        return toIcon(context, bitmap);
    }

    private static Icon square(Context context, String colorHex, String glyph) {
        int size = px(context);
        float density = context.getResources().getDisplayMetrics().density;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int color = Color.parseColor(colorHex);
        float inset = dp(density, 2);
        float radius = dp(density, 4);

        // White outline
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(0, 0, size, size, radius, radius, paint);

        paint.setColor(color);
        canvas.drawRoundRect(inset, inset, size - inset, size - inset, radius, radius, paint);

        drawGlyph(canvas, paint, glyph, size, density);
        return toIcon(context, bitmap);
    }

    /** Draws simple glyphs with vector paths (no font dependency). */
    private static void drawGlyph(Canvas canvas, Paint paint, String glyph, int size, float density) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(density, 2));
        float cx = size / 2f;
        float cy = size / 2f;

        switch (glyph) {
            case "✓": { // checkmark
                Path path = new Path();
                path.moveTo(cx - size * 0.22f, cy);
                path.lineTo(cx - size * 0.05f, cy + size * 0.16f);
                path.lineTo(cx + size * 0.24f, cy - size * 0.18f);
                canvas.drawPath(path, paint);
                break;
            }
            case "!": { // exclamation
                paint.setStrokeWidth(dp(density, 2.5f));
                canvas.drawLine(cx, cy - size * 0.22f, cx, cy + size * 0.08f, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy + size * 0.22f, dp(density, 1.6f), paint);
                break;
            }
            case "+": { // shelter cross
                paint.setStrokeWidth(dp(density, 2.5f));
                canvas.drawLine(cx, cy - size * 0.2f, cx, cy + size * 0.2f, paint);
                canvas.drawLine(cx - size * 0.2f, cy, cx + size * 0.2f, cy, paint);
                break;
            }
            case "☎": { // phone: small filled dot
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, dp(density, 3f), paint);
                break;
            }
            case "•": // self dot
            default: {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, dp(density, 2.5f), paint);
                break;
            }
        }
    }

    private static Icon toIcon(Context context, Bitmap bitmap) {
        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }

    private static int px(Context context) {
        return Math.round(SIZE_DP * context.getResources().getDisplayMetrics().density);
    }

    private static float dp(float density, float value) {
        return value * density;
    }
}
