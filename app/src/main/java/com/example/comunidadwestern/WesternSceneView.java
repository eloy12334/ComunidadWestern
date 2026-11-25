package com.example.comunidadwestern;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Ilustración plana del desierto: colores cálidos y siluetas, sin imágenes realistas. */
public class WesternSceneView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WesternSceneView(Context context, AttributeSet attrs) { super(context, attrs); }
    public WesternSceneView(Context context) { super(context); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.save();
        canvas.scale(getWidth() / 360f, getHeight() / 210f);

        p.setShader(new android.graphics.LinearGradient(0, 0, 0, 210,
                0xFFFFC34B, 0xFFDB4A1B, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 360, 210, p);
        p.setShader(null);

        p.setColor(0xFFFFD43D);
        canvas.drawCircle(180, 115, 43, p);

        Path mesaBack = new Path();
        mesaBack.moveTo(0, 98); mesaBack.lineTo(18, 80); mesaBack.lineTo(58, 80);
        mesaBack.lineTo(70, 48); mesaBack.lineTo(118, 48); mesaBack.lineTo(139, 80);
        mesaBack.lineTo(218, 80); mesaBack.lineTo(242, 56); mesaBack.lineTo(305, 56);
        mesaBack.lineTo(326, 88); mesaBack.lineTo(360, 88); mesaBack.lineTo(360, 210);
        mesaBack.lineTo(0, 210); mesaBack.close();
        p.setColor(0xFFB9471E); canvas.drawPath(mesaBack, p);

        Path mesaFront = new Path();
        mesaFront.moveTo(0, 135); mesaFront.lineTo(32, 126); mesaFront.lineTo(55, 104);
        mesaFront.lineTo(106, 104); mesaFront.lineTo(128, 130); mesaFront.lineTo(210, 130);
        mesaFront.lineTo(244, 111); mesaFront.lineTo(298, 111); mesaFront.lineTo(328, 133);
        mesaFront.lineTo(360, 133); mesaFront.lineTo(360, 210); mesaFront.lineTo(0, 210);
        mesaFront.close();
        p.setColor(0xFF8D3218); canvas.drawPath(mesaFront, p);

        p.setColor(0xFF6D2A16);
        drawCactus(canvas, 54, 150, 0.72f);
        drawCactus(canvas, 308, 152, 0.58f);

        // Rival cowboy al fondo.
        drawCowboy(canvas, 244, 165, 0.73f);
        // Silueta de pistola en primer plano.
        p.setColor(0xFF25150F);
        Path gun = new Path();
        gun.moveTo(142, 210); gun.lineTo(153, 169); gun.lineTo(162, 157);
        gun.lineTo(198, 157); gun.lineTo(207, 210); gun.close();
        canvas.drawPath(gun, p);
        p.setColor(0xFF4A2A1B);
        canvas.drawRoundRect(169, 151, 191, 178, 8, 8, p);
        p.setColor(0xFF17100D);
        canvas.drawRoundRect(175, 174, 185, 210, 4, 4, p);

        // Small birds / motion marks.
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0xFF8D3218);
        Path bird = new Path(); bird.moveTo(280, 28); bird.lineTo(287, 34); bird.lineTo(295, 27);
        canvas.drawPath(bird, p); p.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }

    private void drawCactus(Canvas c, float x, float y, float s) {
        int save = c.save();
        c.translate(x, y); c.scale(s, s);
        p.setColor(0xFF6D2A16);
        c.drawRoundRect(-5, -42, 5, 2, 5, 5, p);
        c.drawRoundRect(-19, -29, -10, -5, 5, 5, p);
        c.drawRoundRect(-18, -31, -5, -23, 4, 4, p);
        c.drawRoundRect(10, -37, 19, -10, 5, 5, p);
        c.drawRoundRect(4, -39, 19, -31, 4, 4, p);
        c.restoreToCount(save);
    }

    private void drawCowboy(Canvas c, float x, float y, float s) {
        int save = c.save();
        c.translate(x, y); c.scale(s, s);
        p.setColor(0xFF1C120D);
        // Hat and head.
        c.drawOval(-16, -70, 16, -62, p);
        c.drawRect(-9, -82, 9, -68, p);
        c.drawOval(-7, -68, 7, -53, p);
        // Torso, arms and legs.
        c.drawRoundRect(-12, -54, 12, -22, 6, 6, p);
        c.drawRoundRect(-23, -54, -13, -28, 5, 5, p);
        c.drawRoundRect(13, -54, 23, -30, 5, 5, p);
        c.drawRoundRect(-10, -27, -2, 0, 4, 4, p);
        c.drawRoundRect(2, -27, 10, 0, 4, 4, p);
        // A simple arm holding a small revolver.
        c.drawRect(16, -50, 30, -44, p);
        c.drawRect(27, -56, 34, -44, p);
        c.restoreToCount(save);
    }
}
