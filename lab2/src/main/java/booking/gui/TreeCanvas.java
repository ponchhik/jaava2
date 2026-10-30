package booking.gui;

import booking.ds.Trace;
import booking.ds.TreeSnapshot;
import booking.model.Booking;
import booking.model.PastBooking;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.Orientation;

import java.time.LocalDateTime;

// Панель визуализации: полная перерисовка снимка дерева после каждой операции + подсветка следа.
// Холст всегда размером с окно просмотра, а прокрутка — смещением рисунка: у Canvas есть предел размера
// текстуры, и «широкое» дерево на 100+ узлов в один холст не помещается.
public final class TreeCanvas extends BorderPane {
    private static final double NODE_W = 124, NODE_H = 50, GAP_X = 12, GAP_Y = 36, PAD = 16;
    private static final double CORNER = 10, TEXT_X = 5, LINE_1 = 14, LINE_2 = 27, LINE_3 = 41;
    private static final double HIDDEN_MARK_DY = 13, HIDDEN_MARK_DX = 3, FOOTER = 14;
    private static final double FONT_SIZE = 10.5, DASH_ON = 4, DASH_OFF = 3, PRUNED_LINE_WIDTH = 1.6;
    private static final double EMPTY_W = 400, EMPTY_H = 200;
    private static final double WHEEL_STEP = 40;
    private static final String EMPTY_TEXT = "Дерево пусто";
    private static final String HIDDEN_MARK = "⋯";
    private static final String ELLIPSIS = "…";

    private static final Color EDGE = Color.rgb(120, 120, 120);
    private static final Color BORDER = Color.rgb(70, 70, 70);
    private static final Color BORDER_PRUNED = Color.rgb(190, 60, 60);
    private static final Color TEXT_PAST = Color.rgb(110, 110, 110);
    private static final Color TEXT_MAX_END = Color.rgb(40, 90, 160);

    private final Canvas canvas = new Canvas();
    private final Pane holder = new Pane(canvas);
    private final ScrollBar hBar = new ScrollBar();
    private final ScrollBar vBar = new ScrollBar();
    private final Font font = Font.font(FONT_SIZE);
    private final Text measurer = new Text();

    private TreeSnapshot<LocalDateTime, Booking> snap;
    private double contentW = EMPTY_W, contentH = EMPTY_H;

    public TreeCanvas() {
        hBar.setOrientation(Orientation.HORIZONTAL);
        vBar.setOrientation(Orientation.VERTICAL);
        hBar.setMax(1);
        vBar.setMax(1);
        measurer.setFont(font);

        canvas.widthProperty().bind(holder.widthProperty());
        canvas.heightProperty().bind(holder.heightProperty());
        holder.widthProperty().addListener((o, a, b) -> viewChanged());
        holder.heightProperty().addListener((o, a, b) -> viewChanged());
        hBar.valueProperty().addListener((o, a, b) -> draw());
        vBar.valueProperty().addListener((o, a, b) -> draw());
        holder.setOnScroll(e -> {
            boolean horizontal = e.isShiftDown() || e.getDeltaX() != 0;
            double delta = e.getDeltaX() != 0 ? e.getDeltaX() : e.getDeltaY();
            scrollBy(horizontal ? hBar : vBar, horizontal ? contentW - holder.getWidth() : contentH - holder.getHeight(),
                    delta > 0 ? -WHEEL_STEP : WHEEL_STEP);
        });

        setCenter(holder);
        setBottom(hBar);
        setRight(vBar);
    }

    // Новый снимок: пересчитать размеры, прокрутить к найденному/вставленному узлу, перерисовать
    public void setSnapshot(TreeSnapshot<LocalDateTime, Booking> snapshot) {
        snap = snapshot;
        measureContent();
        updateBars();
        scrollToHighlight();
        draw();
    }

    private static double x(int col) { return PAD + col * (NODE_W + GAP_X); }
    private static double y(int depth) { return PAD + depth * (NODE_H + GAP_Y); }

    private void measureContent() {
        if (snap == null || snap.count() == 0) {
            contentW = EMPTY_W;
            contentH = EMPTY_H;
            return;
        }
        int maxDepth = 0;
        for (int i = 0; i < snap.count(); i++) maxDepth = Math.max(maxDepth, snap.get(i).depth);
        contentW = x(snap.count()) + PAD;
        contentH = y(maxDepth + 1) + PAD + FOOTER;
    }

    private void viewChanged() {
        updateBars();
        draw();
    }

    // Полоса прокрутки в долях 0..1: размер «бегунка» = видимая доля содержимого
    private void updateBars() {
        updateBar(hBar, contentW, holder.getWidth());
        updateBar(vBar, contentH, holder.getHeight());
    }

    private static void updateBar(ScrollBar bar, double content, double view) {
        boolean scrollable = content > view && view > 0;
        bar.setDisable(!scrollable);
        bar.setVisibleAmount(scrollable ? view / content : 1);
        if (!scrollable) bar.setValue(0);
    }

    private static void scrollBy(ScrollBar bar, double overflow, double pixels) {
        if (overflow <= 0) return;
        bar.setValue(Math.max(0, Math.min(1, bar.getValue() + pixels / overflow)));
    }

    private double offsetX() { return Math.max(0, contentW - holder.getWidth()) * hBar.getValue(); }
    private double offsetY() { return Math.max(0, contentH - holder.getHeight()) * vBar.getValue(); }

    private void scrollToHighlight() {
        if (snap == null) return;
        for (int i = 0; i < snap.count(); i++) {
            var n = snap.get(i);
            if (n.kind == Trace.Kind.MATCH || n.kind == Trace.Kind.INSERTED) {
                centerOn(hBar, x(n.col) + NODE_W / 2, contentW, holder.getWidth());
                centerOn(vBar, y(n.depth) + NODE_H / 2, contentH, holder.getHeight());
                return;
            }
        }
    }

    private static void centerOn(ScrollBar bar, double target, double content, double view) {
        double overflow = content - view;
        if (overflow <= 0) return;
        bar.setValue(Math.max(0, Math.min(1, (target - view / 2) / overflow)));
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFont(font);
        if (snap == null || snap.count() == 0) {
            g.setFill(Color.GRAY);
            g.fillText(EMPTY_TEXT, PAD, PAD + LINE_1);
            return;
        }
        g.save();
        g.translate(-offsetX(), -offsetY());
        drawEdges(g);
        drawNodes(g);
        g.restore();
    }

    private void drawEdges(GraphicsContext g) {
        g.setStroke(EDGE);
        g.setLineWidth(1);
        for (int i = 0; i < snap.count(); i++) {
            var n = snap.get(i);
            if (n.parent != null) {
                g.strokeLine(x(n.parent.col) + NODE_W / 2, y(n.parent.depth) + NODE_H, x(n.col) + NODE_W / 2, y(n.depth));
            }
        }
    }

    private void drawNodes(GraphicsContext g) {
        for (int i = 0; i < snap.count(); i++) {
            var n = snap.get(i);
            double px = x(n.col), py = y(n.depth);
            boolean pruned = n.kind == Trace.Kind.PRUNED;

            g.setFill(TreeStyle.fill(n.kind));
            g.fillRoundRect(px, py, NODE_W, NODE_H, CORNER, CORNER);
            g.setStroke(pruned ? BORDER_PRUNED : BORDER);
            g.setLineWidth(pruned ? PRUNED_LINE_WIDTH : 1);
            g.setLineDashes(pruned ? new double[]{DASH_ON, DASH_OFF} : null);
            g.strokeRoundRect(px, py, NODE_W, NODE_H, CORNER, CORNER);
            g.setLineDashes(null);
            g.setLineWidth(1);

            Booking b = n.value;
            g.setFill(b instanceof PastBooking ? TEXT_PAST : Color.BLACK);
            g.fillText(fit("#" + b.getId() + " " + b.getRoom(), NODE_W - 2 * TEXT_X), px + TEXT_X, py + LINE_1);
            g.fillText(b.getStart().format(Formats.SHORT) + "–" + b.getEnd().format(Formats.TIME), px + TEXT_X, py + LINE_2);
            g.setFill(TEXT_MAX_END);
            g.fillText("max " + n.maxEnd.format(Formats.SHORT), px + TEXT_X, py + LINE_3);
            if (n.hasHidden) {
                g.setFill(Color.DARKGRAY);
                g.fillText(HIDDEN_MARK, px + NODE_W / 2 - HIDDEN_MARK_DX, py + NODE_H + HIDDEN_MARK_DY);
            }
        }
    }

    private double width(String s) {
        measurer.setText(s);
        return measurer.getLayoutBounds().getWidth();
    }

    private String fit(String s, double maxWidth) {
        if (width(s) <= maxWidth) return s;
        while (s.length() > 1 && width(s + ELLIPSIS) > maxWidth) s = s.substring(0, s.length() - 1);
        return s + ELLIPSIS;
    }
}
