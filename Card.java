import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.Light.Distant;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.util.Duration;

final class Card implements Comparable<Card> {
    public static final int[] INDEX_BOUND = new int[] { 7, 9, 9, 9 };
    public static final int[][] YAOCHU_INDEXES = new int[][] {
        { 0, 1 }, { 0, 2 }, { 0, 3 }, { 0, 4 }, { 0, 5 }, { 0, 6 }, { 0, 7 },
        { 1, 1 }, { 1, 9 }, { 2, 1 }, { 2, 9 }, { 3, 1 }, { 3, 9 }
    };
    public static final String[][] REPRESENTATION = new String[][] {
        { "", "東", "南", "西", "北", "白", "發", "中", "", "" },
        { "", "一", "二", "三", "四", "五", "六", "七", "八", "九" },
        { "", "１", "２", "３", "４", "５", "６", "７", "８", "９" },
        { "", "壹", "貳", "參", "肆", "伍", "陸", "柒", "捌", "玖" }
    };
    public static final int UNIT = 100;
    public static final int TOTAL = 11;
    
    public final int vi;
    public final int vj;
    public final int id;
    public final int order;
    public final boolean yao9;
    
    private Card(int i, int j) {
        if (imageMap[i][j] == null) {
            imageMap[i][j] = loadImage(String.format("src/%02d.png", 10 * i + j));
        }
        frontImage = imageMap[i][j];
        cardView = new ImageView(frontImage);
        cardView.setPreserveRatio(false);
        cardView.setFitWidth(W);
        cardView.setFitHeight(H);
        doraView = new ImageView(frontImage);
        doraView.setPreserveRatio(false);
        doraView.setFitWidth(W);
        doraView.setFitHeight(H);
        fxNode = new Button("", cardView);
        fxNode.setBackground(null);
        fxNode.setPadding(javafx.geometry.Insets.EMPTY);
        
        vi = i;
        vj = j == 0 ? 5 : j;
        id = getId(vi, vj);
        yao9 = i == 0 || j == 1 || j == 9;
        order = (id << 1) | (j == 0 ? 1 : 0);   // Akadora排在後面
    }
    
    public static int getId(int i, int j) {
        return i * UNIT + j;
    }
    
    public static boolean isYao9(int id) {
        if (id < UNIT)
            return true;
        id %= UNIT;
        return id == 1 || id == 9;
    }
    
    public static void fillWholeCards(List<Card> emptyList) {
        for (int j = 1; j <= 7; ++j) {
            emptyList.add(new Card(0, j));
            emptyList.add(new Card(0, j));
            emptyList.add(new Card(0, j));
            emptyList.add(new Card(0, j));
        }
        int[] fourKind = new int[] { 1, 2, 3, 4, 6, 7, 8, 9 };
        for (int i = 1; i <= 3; ++i) {
            emptyList.add(new Card(i, 0));
            emptyList.add(new Card(i, 5));
            emptyList.add(new Card(i, 5));
            emptyList.add(new Card(i, 5));
            for (int j: fourKind) {
                emptyList.add(new Card(i, j));
                emptyList.add(new Card(i, j));
                emptyList.add(new Card(i, j));
                emptyList.add(new Card(i, j));
            }
        }
        return;
    }
    
    @Override
    public int compareTo(Card o) {
        return Integer.compare(order, o.order);
    }
    
    @Override
    public String toString() {
        return ((order & 1) == 1) ? (REPRESENTATION[vi][vj] + "*") :
                                     REPRESENTATION[vi][vj];
    }
    
    /** =============== The followings are GUI related stuff =============== */
    
    public static final int ANITIME = 192;
    public static final double W = 28;
    public static final double H = 35;
    public static final double ADJUST_X = -(W / 2); // 座標是以左上角為基準
    public static final double ADJUST_Y = -(H / 2); // 但定中心點較易記算而需做轉換
    public static final double HOVER_SCALING_RATIO = 1.5;
    public static final Duration TRANS_TIME = new Duration(ANITIME);
    private static final Effect DISABLED = new ColorAdjust(0, -0.3, -0.5, 0);
    private static final EnumMap<React.Type, Effect> effectMap;
    private static final Image[][] imageMap = new Image[4][10];
    private static final Image backImage = loadImage("src/00.png");
    private final Image frontImage;
    public final ImageView cardView;
    public final ImageView doraView;
    public final Button fxNode;
    
    static {
        effectMap = new EnumMap<>(React.Type.class);
        Effect kanLike = new Lighting(new Distant(0, 90, Color.color(.88,1,.88)));
        effectMap.put(React.Type.ANKAN, kanLike);
        effectMap.put(React.Type.KAKAN, kanLike);
        effectMap.put(React.Type.KAN, kanLike);
        effectMap.put(React.Type.PON, new Lighting(new Distant(0, 90, Color.color(.85,.85,1))));
        for (React.Type rt: React.Type.values()) {
            effectMap.putIfAbsent(rt, null);
        }
    }
    
    public static ImageView createBackView() {
        ImageView iv = new ImageView(backImage);
        iv.setPreserveRatio(false);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        return iv;
    }
    
    public static Image loadImage(String path) {
        Image src = null;
        try {
            src = new Image(Card.class.getResource(path).openStream());
        } catch (Exception ouch) {
            ouch.printStackTrace();
            System.err.println("Exception in loading " + path);
        }
        return src;
    }
    
    public Node getFxNode() {
        return fxNode;
    }
    
    public void setInYama(double r, double x, double y) {
        cardView.setImage(backImage);
        fxNode.toFront();
        fxNode.setRotate(r);
        fxNode.setTranslateX(x + ADJUST_X);
        fxNode.setTranslateY(y + ADJUST_Y);
        fxNode.setScaleX(1.0);
        fxNode.setScaleY(1.0);
        fxNode.setEffect(null);
        fxNode.setOnAction(null);
        fxNode.setOnMouseClicked(null);
        return;
    }
    
    public void setNoResponse(boolean front) {
        cardView.setImage(front ? frontImage : backImage);
        fxNode.setScaleX(1.0);
        fxNode.setScaleY(1.0);
        fxNode.setEffect(null);
        fxNode.setOnAction(null);
        fxNode.setOnMouseEntered(null);
        fxNode.setOnMouseExited(null);
        return;
    }
    
    public void setUnclickable() {
        fxNode.setScaleX(1.0);
        fxNode.setScaleY(1.0);
        fxNode.setEffect(DISABLED);
        fxNode.setOnAction(null);
        fxNode.setOnMouseEntered(null);
        fxNode.setOnMouseExited(null);
        return;
    }
    
    public void fxNodeRequestFocus() {
        Platform.runLater(() -> this.fxNode.requestFocus());
        return;
    }
    
    public Card setReact(SimpleEntry<AtomicBoolean, React> entry, React ra) {
        Platform.runLater(() -> {
            fxNode.setEffect(effectMap.get(ra.type));
            fxNode.setOnMouseEntered(e -> {
                for (Card c: ra.cardList) {
                    c.fxNode.toFront();
                    c.fxNode.setScaleX(HOVER_SCALING_RATIO);
                    c.fxNode.setScaleY(HOVER_SCALING_RATIO);
                }
                fxNode.toFront();
                fxNode.setScaleX(HOVER_SCALING_RATIO);
                fxNode.setScaleY(HOVER_SCALING_RATIO);
            });
            fxNode.setOnMouseExited(e -> {
                for (Card c: ra.cardList) {
                    c.fxNode.setScaleX(1.0);
                    c.fxNode.setScaleY(1.0);
                }
                fxNode.setScaleX(1.0);
                fxNode.setScaleY(1.0);
            });
            fxNode.setOnAction(e -> {
                synchronized (entry) {
                    entry.setValue(ra);
                    entry.getKey().set(false);
                    entry.notify();
                }
            });
        });
        return this;
    }
    
    public void moveTo(boolean front, double r, double x, double y) {
        double fromA = fxNode.getRotate();
        double fromX = fxNode.getTranslateX();
        double fromY = fxNode.getTranslateY();
        double dA = r - fromA;
        double dX = x + ADJUST_X - fromX;
        double dY = y + ADJUST_Y - fromY;
        new Transition() {
            {
                this.setCycleDuration(TRANS_TIME);
                this.setOnFinished(e ->
                    cardView.setImage(front ? frontImage : backImage)
                );
            }
            @Override protected void interpolate(double frac) {
                fxNode.setRotate(    fromA + dA * frac);
                fxNode.setTranslateX(fromX + dX * frac);
                fxNode.setTranslateY(fromY + dY * frac);
                return;
            }
        }.play();
        return;
    }
    
    public void moveBy(boolean front, double dx, double dy, Card x) {
        moveTo(front, x.fxNode.getRotate(),
            x.fxNode.getTranslateX() + dx - ADJUST_X,
            x.fxNode.getTranslateY() + dy - ADJUST_Y);
        return;
    }
    
    public void swapLocation(Card that) {
        double thisR = this.fxNode.getRotate();
        double thisX = this.fxNode.getTranslateX();
        double thisY = this.fxNode.getTranslateY();
        this.fxNode.setRotate(    that.fxNode.getRotate());
        this.fxNode.setTranslateX(that.fxNode.getTranslateX());
        this.fxNode.setTranslateY(that.fxNode.getTranslateY());
        that.fxNode.setRotate(    thisR);
        that.fxNode.setTranslateX(thisX);
        that.fxNode.setTranslateY(thisY);
        return;
    }
    
}
