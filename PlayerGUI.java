import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.Light.Distant;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.Node;
import javafx.util.Duration;

final class PlayerGUI extends Player {
    private static final int[] KAWACOL = {
        0, 1, 2, 3, 4, 5,
        0, 1, 2, 3, 4, 5,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12
    };
    private static final int[] KAWAROW = {
        0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
    };
    private static final Font declFont   = Font.font("", FontWeight.BLACK,120);
    private static final Font jifuuFont  = Font.font("", FontWeight.BLACK, 28);
    private static final Font pointFont  = Font.font("", FontWeight.BOLD,  20);
    private static final Font buttonFont = Font.font("", FontWeight.BOLD,  40);
    private static final Effect cpEffect = new InnerShadow(9.0, Color.CRIMSON);
    private static final Effect ptEffect = new Lighting(new Distant(90, 135, Color.PLUM));
    private static final double DECLTEXT_SCELING_RATIO = 0.6;
    private static final double INFO_HEIGHT = Card.W * 6 * .25;
    private static final double INFO_WIDTH  = Card.W * 6 - INFO_HEIGHT;
    private static final String[] JIFUU_TEXT = { "", "東", "南", "西", "北" };
    private static final Duration DECL_TRANS_TIME = new Duration(700);
    
    private final boolean openFuda;
    private final boolean userPlay;
    private final double DEG_V, DEG_H;  // 麻將牌直擺和橫擺的旋轉角度
    private final double FUROX, FUROY;  // 副露起始右下角位置
    private final double FUDAX, FUDAY;  // 手牌起始中心位置
    private final double KAWAX, KAWAY;  // 河底起始中心位置
    private final double ADJVX, ADJVY;  // 橫向增加方式
    private final double ADJHX, ADJHY;  // 縱向增加方式
    private final double PLUSX, PLUSY;  // 摸牌、立直同列增加方式
    private final double FUROVDX, FUROVDY;  // 副露縱向增加方式
    private final double FUROHDX, FUROHDY;  // 副露橫向增加方式
    private final double FUROVAX, FUROVAY;  // 副露縱向更新方式
    private final double FUROHAX, FUROHAY;  // 副露橫向更新方式
    private final double KAKANDX, KAKANDY;  // 加槓相對更新方式
    private final int nextSeat;
    private final Label declLabel = new Label();
    private final Text jifuuText = new Text();
    private final Text pointText = new Text();
    private final HBox infoBox = new HBox(8, jifuuText, pointText);
    private final HBox declBox = new HBox(40);
    private double furoX, furoY;    // 副露右下角基準位置、每放一張牌都會更新一次
    
    public PlayerGUI(int seat, String name, int startPoints) {
        super(seat, name, startPoints);
        openFuda = seat == 1;
        userPlay = seat == 1;
        nextSeat = Game.SEQUENCE[seat + 1];
        switch (seat) {
            case 1:
                infoBox.setTranslateX(MainGUI.SCENE_WIDTH / 2 - Card.W * 3);
                infoBox.setTranslateY(MainGUI.SCENE_WIDTH / 2 + Card.W * 3 - INFO_HEIGHT);
                declLabel.setTranslateX(MainGUI.SCENE_WIDTH / 2 - MainGUI.SCENE_WIDTH / 4);
                declLabel.setTranslateY(MainGUI.SCENE_WIDTH - MainGUI.PLAYER_AREA * 3);
                DEG_V = 0;
                DEG_H = 270;
                PLUSX = (Card.H - Card.W);
                PLUSY = 0;
                FUROX = MainGUI.SCENE_WIDTH;
                FUROY = MainGUI.SCENE_WIDTH;
                FUDAX = MainGUI.SCENE_WIDTH - 4 * (Card.H + Card.W * 3) - Card.W * 3.5 - PLUSX;
                FUDAY = MainGUI.SCENE_WIDTH - Card.H / 2;
                KAWAX = MainGUI.SCENE_WIDTH / 2 - Card.W * 2.5;
                KAWAY = MainGUI.SCENE_WIDTH / 2 + Card.W * 3 + Card.H / 2;
                ADJVX = +Card.W;
                ADJVY = 0;
                ADJHX = 0;
                ADJHY = +Card.H;
                FUROVDX = -Card.W / 2;
                FUROVDY = -Card.H / 2;
                FUROHDX = -Card.H / 2;
                FUROHDY = -Card.W / 2;
                FUROVAX = -Card.W;
                FUROVAY = 0;
                FUROHAX = -Card.H;
                FUROHAY = 0;
                KAKANDX = 0;
                KAKANDY = -Card.W;
                break;
            case 2:
                infoBox.setTranslateX(MainGUI.SCENE_WIDTH / 2 + Card.W * 3 - (INFO_HEIGHT + INFO_WIDTH) / 2);
                infoBox.setTranslateY(MainGUI.SCENE_WIDTH / 2 + Card.W * 3 - (INFO_HEIGHT + INFO_WIDTH) / 2);
                declLabel.setTranslateX(MainGUI.SCENE_WIDTH - MainGUI.PLAYER_AREA - MainGUI.SCENE_WIDTH / 4);
                declLabel.setTranslateY(MainGUI.SCENE_WIDTH / 2 - MainGUI.PLAYER_AREA / 2);
                DEG_V = 270;
                DEG_H = 180;
                PLUSX = 0;
                PLUSY = (Card.W - Card.H);
                FUROX = MainGUI.SCENE_WIDTH;
                FUROY = 0;
                FUDAX = MainGUI.SCENE_WIDTH - Card.H / 2;
                FUDAY = 0 + 4 * (Card.H + Card.W * 3) + Card.W * 3.5 + PLUSY;
                KAWAX = MainGUI.SCENE_WIDTH / 2 + Card.W * 3 + Card.H / 2;
                KAWAY = MainGUI.SCENE_WIDTH / 2 + Card.W * 2.5;
                ADJVX = 0;
                ADJVY = -Card.W;
                ADJHX = +Card.H;
                ADJHY = 0;
                FUROVDX = -Card.H / 2;
                FUROVDY = +Card.W / 2;
                FUROHDX = -Card.W / 2;
                FUROHDY = +Card.H / 2;
                FUROVAX = 0;
                FUROVAY = +Card.W;
                FUROHAX = 0;
                FUROHAY = +Card.H;
                KAKANDX = -Card.W;
                KAKANDY = 0;
                break;
            case 3:
                infoBox.setTranslateX(MainGUI.SCENE_WIDTH / 2 + Card.W * 3 - INFO_WIDTH);
                infoBox.setTranslateY(MainGUI.SCENE_WIDTH / 2 - Card.W * 3);
                declLabel.setTranslateX(MainGUI.SCENE_WIDTH / 2 - MainGUI.SCENE_WIDTH / 4);
                declLabel.setTranslateY(MainGUI.PLAYER_AREA);
                DEG_V = 180;
                DEG_H = 90;
                PLUSX = (Card.W - Card.H);
                PLUSY = 0;
                FUROX = 0;
                FUROY = 0;
                FUDAX = 0 + 4 * (Card.H + Card.W * 3) + Card.W * 3.5 + PLUSX;
                FUDAY = 0 + Card.H / 2;
                KAWAX = MainGUI.SCENE_WIDTH / 2 + Card.W * 2.5;
                KAWAY = MainGUI.SCENE_WIDTH / 2 - Card.W * 3 - Card.H / 2;
                ADJVX = -Card.W;
                ADJVY = 0;
                ADJHX = 0;
                ADJHY = -Card.H;
                FUROVDX = +Card.W / 2;
                FUROVDY = +Card.H / 2;
                FUROHDX = +Card.H / 2;
                FUROHDY = +Card.W / 2;
                FUROVAX = +Card.W;
                FUROVAY = 0;
                FUROHAX = +Card.H;
                FUROHAY = 0;
                KAKANDX = 0;
                KAKANDY = +Card.W;
                break;
            case 4:
                infoBox.setTranslateX(MainGUI.SCENE_WIDTH / 2 - Card.W * 3 + (INFO_HEIGHT - INFO_WIDTH) / 2);
                infoBox.setTranslateY(MainGUI.SCENE_WIDTH / 2 - Card.W * 3 - (INFO_HEIGHT - INFO_WIDTH) / 2);
                declLabel.setTranslateX(MainGUI.PLAYER_AREA - MainGUI.SCENE_WIDTH / 4);
                declLabel.setTranslateY(MainGUI.SCENE_WIDTH / 2 - MainGUI.PLAYER_AREA / 2);
                DEG_V = 90;
                DEG_H = 0;
                PLUSX = 0;
                PLUSY = (Card.H - Card.W);
                FUROX = 0;
                FUROY = MainGUI.SCENE_WIDTH;
                FUDAX = 0 + Card.H / 2;
                FUDAY = MainGUI.SCENE_WIDTH - 4 * (Card.H + Card.W * 3) - Card.W * 3.5 - PLUSY;
                KAWAX = MainGUI.SCENE_WIDTH / 2 - Card.W * 3 - Card.H / 2;
                KAWAY = MainGUI.SCENE_WIDTH / 2 - Card.W * 2.5;
                ADJVX = 0;
                ADJVY = +Card.W;
                ADJHX = -Card.H;
                ADJHY = 0;
                FUROVDX = +Card.H / 2;
                FUROVDY = -Card.W / 2;
                FUROHDX = +Card.W / 2;
                FUROHDY = -Card.H / 2;
                FUROVAX = 0;
                FUROVAY = -Card.W;
                FUROHAX = 0;
                FUROHAY = -Card.H;
                KAKANDX = +Card.W;
                KAKANDY = 0;
                break;
            default:
                DEG_V = DEG_H = 0;
                PLUSX = PLUSY = 0;
                FUROX = FUROY = 0;
                FUDAX = FUDAY = 0;
                KAWAX = KAWAY = 0;
                ADJVX = ADJVY = 0;
                ADJHX = ADJHY = 0;
                FUROVDX = FUROVDY = 0;
                FUROHDX = FUROHDY = 0;
                FUROVAX = FUROVAY = 0;
                FUROHAX = FUROHAY = 0;
                KAKANDX = KAKANDY = 0;
        }
        
        HBox.setMargin(jifuuText, new Insets(0, 0, 0, 8));
        jifuuText.setFill(Color.WHITE);
        jifuuText.setFont(jifuuFont);
        pointText.setFill(Color.WHITE);
        pointText.setFont(pointFont);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setMaxSize(INFO_WIDTH, INFO_HEIGHT);
        infoBox.setMinSize(INFO_WIDTH, INFO_HEIGHT);
        infoBox.setRotate(DEG_V);
        
        declLabel.setAlignment(Pos.CENTER);
        declLabel.setMinSize(MainGUI.SCENE_WIDTH / 2, MainGUI.PLAYER_AREA);
        declLabel.setFont(declFont);
        declLabel.setTextFill(Color.WHITE);
        declLabel.setVisible(false);
        
        declBox.setOpacity(0.80);
        declBox.setAlignment(Pos.BOTTOM_RIGHT);
        declBox.setMinSize(MainGUI.SCENE_WIDTH - MainGUI.PLAYER_AREA * 2,
                           MainGUI.SCENE_WIDTH - MainGUI.PLAYER_AREA * ++seat);
        declBox.setTranslateX(MainGUI.PLAYER_AREA);
        declBox.setTranslateY(MainGUI.PLAYER_AREA);
    }
    
    public void setCurrentPlayer(boolean yes) {
        jifuuText.setEffect(yes ? cpEffect : null);
        return;
    }
    
    @Override
    public void newRoundReset(int newJifuu) {
        super.newRoundReset(newJifuu);
        furoX = FUROX;
        furoY = FUROY;
        jifuuText.setText(JIFUU_TEXT[newJifuu]);
        jifuuText.setEffect(newJifuu == 1 ? cpEffect : null);
        pointText.setText(Integer.toString(point));
        pointText.setEffect(null);
        return;
    }
    
    @Override
    public void dealCard(Card c) {
        c.moveTo(openFuda, DEG_V,
            FUDAX + fuda.size() * ADJVX,
            FUDAY + fuda.size() * ADJVY
        );
        super.dealCard(c);
        return;
    }
    
    private void updateFudaView() {
        Platform.runLater(() -> {
            int index = 0;
            for (Card c: fuda) {
                double px = FUDAX + index * ADJVX;
                double py = FUDAY + index * ADJVY;
                c.moveTo(openFuda, DEG_V, px, py);
                ++index;
            }
        });
        return;
    }
    
    @Override
    public void sortFuda(boolean recomputeTenpai) {
        super.sortFuda(recomputeTenpai);
        updateFudaView();
        return;
    }
    
    @Override
    public Card discardCard(React ra) {
        int index = kawa.size();
        double px = KAWAX + ADJVX * KAWACOL[index] + ADJHX * KAWAROW[index];
        double py = KAWAY + ADJVY * KAWACOL[index] + ADJHY * KAWAROW[index];
        // TODO: random swap if not TMGR
        Card x = super.discardCard(ra);
        if (richiIndex < 0 || KAWAROW[richiIndex] != KAWAROW[index]) {
            Platform.runLater(() -> x.moveTo(true, DEG_V, px, py));
        } else if (richiIndex < index) {
            Platform.runLater(() -> x.moveTo(true, DEG_V, px + PLUSX, py + PLUSY));
        } else {
            Platform.runLater(() -> x.moveTo(true, DEG_H, px + PLUSX / 2, py + PLUSY / 2));
        }
        return x;
    }
    
    @Override
    public React getDrawReact(List<React> reactList) {
        Card focus = reactList.get(0).join;
        Platform.runLater(() -> focus.moveTo(openFuda, DEG_V,
            FUDAX + fuda.size() * ADJVX + PLUSX,
            FUDAY + fuda.size() * ADJVY + PLUSY)
        );
        return userPlay ? showSelections(reactList, focus) :
            reactList.get(ThreadLocalRandom.current().nextInt(reactList.size()));
    }
    
    @Override
    public React getRichiReact(List<React> reactList) {
        return userPlay ? showSelections(reactList, reactList.get(0).join) :
            reactList.get(ThreadLocalRandom.current().nextInt(reactList.size()));
    }
    
    @Override
    public React getChiponReact(List<React> reactList) {
        return userPlay ? showSelections(reactList, null) :
            reactList.get(ThreadLocalRandom.current().nextInt(reactList.size()));
    }
    
    @Override
    public Card doAnkan(React ra) {
        Card focus = super.doAnkan(ra);
        setFuroCardV(ra.cardList.get(0), false);
        setFuroCardV(ra.cardList.get(1), true);
        setFuroCardV(ra.cardList.get(2), true);
        setFuroCardV(ra.cardList.size() == 3 ? focus :
                     ra.cardList.get(3), false);
        return focus;
    }
    
    @Override
    public Card doKakan(React ra) {
        Card focus = super.doKakan(ra);
        Card base = furo.get(ra.index).focus;
        Platform.runLater(() -> focus.moveBy(true, KAKANDX, KAKANDY, base));
        return focus;
    }
    
    @Override
    public void doRichi() {
        super.doRichi();
        Platform.runLater(() -> {
            pointText.setText(Integer.toString(point));
            pointText.setEffect(ptEffect);
        });
        return;
    }
    
    @Override
    public React getNormalReact(List<React> reactList) {
        return userPlay ? showSelections(reactList, null) :
            reactList.get(ThreadLocalRandom.current().nextInt(reactList.size()));
    }
    
    @Override
    public void doKan(React ra) {
        super.doKan(ra);
        if (game.cp.seat == prevSeat) {
            setFuroCardV(ra.cardList.get(0), true);
            setFuroCardV(ra.cardList.get(1), true);
            setFuroCardV(ra.cardList.get(2), true);
            setFuroCardH(ra.drop, true);
        } else if (game.cp.seat == nextSeat) {
            setFuroCardH(ra.drop, true);
            setFuroCardV(ra.cardList.get(0), true);
            setFuroCardV(ra.cardList.get(1), true);
            setFuroCardV(ra.cardList.get(2), true);
        } else {
            setFuroCardV(ra.cardList.get(0), true);
            setFuroCardV(ra.cardList.get(1), true);
            setFuroCardH(ra.drop, true);
            setFuroCardV(ra.cardList.get(2), true);
        }
        return;
    }
    
    @Override
    public void doChipon(React ra) {
        super.doChipon(ra);
        if (game.cp.seat == prevSeat) { // CHI here
            setFuroCardV(ra.cardList.get(0), true);
            setFuroCardV(ra.cardList.get(1), true);
            setFuroCardH(ra.drop, true);
        } else if (game.cp.seat == nextSeat) {
            setFuroCardH(ra.drop, true);
            setFuroCardV(ra.cardList.get(0), true);
            setFuroCardV(ra.cardList.get(1), true);
        } else {
            setFuroCardV(ra.cardList.get(0), true);
            setFuroCardH(ra.drop, true);
            setFuroCardV(ra.cardList.get(1), true);
        }
        updateFudaView();
        return;
    }
    
    @Override
    public React getChankanReact(List<React> reactList) {
        return userPlay ? showSelections(reactList, null) :
            reactList.get(ThreadLocalRandom.current().nextInt(reactList.size()));
    }
    
    public void declareAction(String declString) {
        declLabel.setText(declString);
        declLabel.setScaleX(1.0);
        declLabel.setScaleY(1.0);
        declLabel.setVisible(true);
        declLabel.toFront();
        double ds = DECLTEXT_SCELING_RATIO - 1.0;
        new Transition() {
            {
                this.setCycleDuration(DECL_TRANS_TIME);
                this.setOnFinished(e -> declLabel.setVisible(false));
            }
            @Override protected void interpolate(double frac) {
                double s = 1.0 + ds * Math.min(1.0, 3 * frac);
                declLabel.setScaleX(s);
                declLabel.setScaleY(s);
                return;
            }
        }.play();
        return;
    }
    
    private React showSelections(List<React> reactList, Card selfTurnDrawn) {
        React defaultAction = reactList.get(0); // tmgr or pass
        if (reactList.size() == 1) {
            return defaultAction;
        }
        SimpleEntry<AtomicBoolean, React> entry = new SimpleEntry<>(
            new AtomicBoolean(true), defaultAction
        );
        
        List<Card> undo = new ArrayList<>(14);
        undo.addAll(fuda);
        if (selfTurnDrawn != null) {
            undo.add(selfTurnDrawn);
        }
        Platform.runLater(() -> undo.forEach(c -> c.setUnclickable()));
        
        for (React ra: reactList) {
            if (ra.type.buttonType) {
                Button button = new Button(ra.type.text);
                button.setFont(buttonFont);
                button.setOnAction(e -> {
                    synchronized (entry) {
                        entry.setValue(ra);
                        entry.getKey().set(false);
                        entry.notify();
                    }
                });
                Platform.runLater(() -> {
                    declBox.toFront();
                    declBox.getChildren().add(button);
                    button.requestFocus();
                });
            } else switch (ra.type) {
                case ANKAN:
                    ra.cardList.get(1).setReact(entry, ra);
                    break;
                case KAKAN:
                    undo.add(furo.get(ra.index).focus.setReact(entry, ra));
                    break;
                case KIRU:
                case KRGR:
                case TMGR:
                    ra.drop.setReact(entry, ra);
                    break;
                case KAN:
                    ra.cardList.get(1).setReact(entry, ra);
                    break;
                case CHI:
                case PON:
                    ra.cardList.get(0).setReact(entry, ra);
                    break;
                default:
                    System.err.println("Unexpected ReactType: " + ra.type);
            }
        }
        if (selfTurnDrawn != null) {
            selfTurnDrawn.fxNodeRequestFocus();
        }
        
        synchronized (entry) {
            while (entry.getKey().get()) {
                try {
                    entry.wait();
                } catch (Exception excepted) {
                    // by player with higher priority
                } finally {
                    Platform.runLater(() -> {
                        declBox.getChildren().clear();
                        undo.forEach(c -> c.setNoResponse(true));
                    });
                }
            }
        }
        return entry.getValue();
    }
    
    private void setFuroCardV(Card c, boolean front) {
        double px = furoX + FUROVDX;
        double py = furoY + FUROVDY;
        furoX += FUROVAX;
        furoY += FUROVAY;
        Platform.runLater(() -> c.moveTo(front, DEG_V, px, py));
        return;
    }
    
    private void setFuroCardH(Card c, boolean front) {
        double px = furoX + FUROHDX;
        double py = furoY + FUROHDY;
        furoX += FUROHAX;
        furoY += FUROHAY;
        Platform.runLater(() -> c.moveTo(front, DEG_H, px, py));
        return;
    }
    
    public List<Node> getFxNodes() {
        return List.of(infoBox, declBox, declLabel);
    }
    
}
