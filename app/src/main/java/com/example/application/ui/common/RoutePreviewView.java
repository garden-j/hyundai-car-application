package com.example.application.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.application.R;
import com.example.application.model.BriefingPoint;
import com.example.application.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * 경로 미리보기(지도 SDK 없이 경로 모양만 그림). 출발(파랑)·도착(초록)과 브리핑 지점 번호를 표시한다.
 * 지도 연동은 디자인 확정 후 정적 지도 이미지 등으로 교체할 수 있다.
 */
public class RoutePreviewView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<GeoPoint> path = new ArrayList<>();
    private List<BriefingPoint> points = new ArrayList<>();

    public RoutePreviewView(Context context) {
        this(context, null);
    }

    public RoutePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(6 * d);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setColor(ContextCompat.getColor(context, R.color.accent));
        label.setColor(ContextCompat.getColor(context, R.color.accent_on));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(14 * d);
        label.setFakeBoldText(true);
        setBackgroundColor(ContextCompat.getColor(context, R.color.surface_muted));
    }

    public void setRoute(List<GeoPoint> path, List<BriefingPoint> points) {
        this.path = path == null ? new ArrayList<>() : path;
        this.points = points == null ? new ArrayList<>() : points;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (path.size() < 2) {
            return;
        }
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE, minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (GeoPoint p : path) {
            minLat = Math.min(minLat, p.latitude);
            maxLat = Math.max(maxLat, p.latitude);
            minLon = Math.min(minLon, p.longitude);
            maxLon = Math.max(maxLon, p.longitude);
        }
        float pad = 32 * getResources().getDisplayMetrics().density;
        float w = getWidth() - 2 * pad;
        float h = getHeight() - 2 * pad;
        double spanLat = Math.max(maxLat - minLat, 1e-6);
        double spanLon = Math.max(maxLon - minLon, 1e-6);
        double scale = Math.min(w / spanLon, h / spanLat);
        float offX = (float) (pad + (w - spanLon * scale) / 2);
        float offY = (float) (pad + (h - spanLat * scale) / 2);

        Path p = new Path();
        for (int i = 0; i < path.size(); i++) {
            float x = (float) (offX + (path.get(i).longitude - minLon) * scale);
            float y = (float) (offY + (maxLat - path.get(i).latitude) * scale);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        canvas.drawPath(p, line);

        float r = 10 * getResources().getDisplayMetrics().density;
        drawDot(canvas, path.get(path.size() - 1), R.color.point_end, r, null, offX, offY, minLon, maxLat, scale);
        drawDot(canvas, path.get(0), R.color.point_start, r, null, offX, offY, minLon, maxLat, scale);
        for (BriefingPoint b : points) {
            if (b.point != null) {
                drawDot(canvas, b.point, R.color.accent, r * 1.3f, String.valueOf(b.order), offX, offY, minLon, maxLat, scale);
            }
        }
    }

    private void drawDot(Canvas c, GeoPoint g, int color, float r, String text, float offX, float offY,
                         double minLon, double maxLat, double scale) {
        float x = (float) (offX + (g.longitude - minLon) * scale);
        float y = (float) (offY + (maxLat - g.latitude) * scale);
        dot.setColor(ContextCompat.getColor(getContext(), color));
        c.drawCircle(x, y, r, dot);
        if (text != null) {
            c.drawText(text, x, y - (label.descent() + label.ascent()) / 2, label);
        }
    }
}
