import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.text.TextAlignment;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// javac -encoding utf8 -Xlint:unchecked MainGUI.java
public class MainGUI extends Application {
    public static final double PLAYER_AREA  = 60;
    public static final double SCENE_WIDTH  = 690;
    public static final double SCENE_HEIGHT = 690;
    public static final double INFO_W = 80;
    public static final double INFO_H = 20;
    private final Label remainInfo = new Label();
    private final Label  kyokuInfo = new Label();
    private final Button bonbaInfo = new Button("", makePointStick(100));
    private final Button richiInfo = new Button("", makePointStick(1000));
    private final PlayerGUI[] slot = new PlayerGUI[5];
    private final List<Card>  deck = new ArrayList<>(136);
    private final AtomicBoolean cheatKeyState = new AtomicBoolean(false);
    private HashMap<String, Integer> gameInfo;
    private ObservableList<Node> sceneObjects;
    private Scene gameScene = null;
    
    @Override
    public void init() {
        Pane gamePane = new Pane();
        gamePane.setMaxSize(SCENE_WIDTH, SCENE_HEIGHT);
        gamePane.setMinSize(SCENE_WIDTH, SCENE_HEIGHT);
        gamePane.setBackground(new Background(
            new BackgroundFill(Color.color(.05, .05, .25), CornerRadii.EMPTY, Insets.EMPTY)));
        sceneObjects = gamePane.getChildren();
        
        Rectangle centerSquare = new Rectangle(Card.W * 6, Card.W * 6);
        centerSquare.setFill(new Color(.1, .1, .1, .9));
        centerSquare.setArcWidth(10);
        centerSquare.setArcHeight(10);
        centerSquare.setTranslateX(SCENE_WIDTH / 2 - Card.W * 3);
        centerSquare.setTranslateY(SCENE_WIDTH / 2 - Card.W * 3);
        sceneObjects.add(centerSquare);
        
        remainInfo.setMinSize(INFO_W + 5, INFO_H);
        remainInfo.setAlignment(Pos.BOTTOM_RIGHT);
        remainInfo.setTranslateX((SCENE_WIDTH - INFO_W) / 2);
        remainInfo.setTranslateY((SCENE_WIDTH - INFO_H * 4.0) / 2);
        remainInfo.setTextFill(Color.SILVER);
        sceneObjects.add(remainInfo);
        
        kyokuInfo.setMaxSize(INFO_W, INFO_H);
        kyokuInfo.setMinSize(INFO_W, INFO_H);
        kyokuInfo.setFont(Font.font("", FontWeight.BOLD, INFO_H));
        kyokuInfo.setAlignment(Pos.CENTER);
        kyokuInfo.setTranslateX((SCENE_WIDTH - INFO_W) / 2);
        kyokuInfo.setTranslateY((SCENE_WIDTH - INFO_H * 4.0) / 2);
        sceneObjects.add(kyokuInfo);
        
        bonbaInfo.setTranslateX((SCENE_WIDTH - INFO_W) / 2);
        bonbaInfo.setTranslateY((SCENE_WIDTH - INFO_H * 1.0) / 2);
        bonbaInfo.setAlignment(Pos.CENTER_LEFT);
        bonbaInfo.setBackground(null);
        bonbaInfo.setTextFill(Color.WHITE);
        sceneObjects.add(bonbaInfo);
        
        richiInfo.setTranslateX((SCENE_WIDTH - INFO_W) / 2);
        richiInfo.setTranslateY((SCENE_WIDTH + INFO_H * 1.5) / 2);
        richiInfo.setAlignment(Pos.CENTER_LEFT);
        richiInfo.setBackground(null);
        richiInfo.setTextFill(Color.WHITE);
        sceneObjects.add(richiInfo);
        
        gameScene = new Scene(gamePane, SCENE_WIDTH, SCENE_HEIGHT);
        gameScene.setOnKeyReleased(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.C)
                cheatKeyState.set(false);
        });
        gameScene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case C:
                    cheatKeyState.set(true);
                    break;
                case DIGIT1:
                    System.out.println(slot[1].cheatInformation());
                    break;
                case DIGIT2:
                    System.out.println(slot[2].cheatInformation());
                    break;
                case DIGIT3:
                    System.out.println(slot[3].cheatInformation());
                    break;
                case DIGIT4:
                    System.out.println(slot[4].cheatInformation());
                    break;
                case ESCAPE:
                    System.exit(0);
                default:
            }
        });
        return;
    }
    
    @Override
    public void start(javafx.stage.Stage stage) {
        GridPane startPane = new GridPane(); {
            startPane.setAlignment(Pos.CENTER);
            startPane.setPadding(new Insets(10));
            startPane.setHgap(10);
            startPane.setVgap(16);
            ColumnConstraints c0 = new ColumnConstraints();
            ColumnConstraints c1 = new ColumnConstraints();
            ColumnConstraints c2 = new ColumnConstraints();
            ColumnConstraints c3 = new ColumnConstraints();
            ColumnConstraints c4 = new ColumnConstraints();
            ColumnConstraints c5 = new ColumnConstraints();
            c0.setFillWidth(true);
            c1.setPercentWidth(15);
            c2.setPercentWidth(15);
            c3.setPercentWidth(15);
            c4.setPercentWidth(15);
            c5.setPercentWidth(15);
            startPane.getColumnConstraints().addAll(c0, c1, c2, c3, c4, c5);
        }
        ObservableList<Node> startList = startPane.getChildren();
        int rowIndex = 0;
        {
            Text text = new Text("MahjongFX");
            text.setEffect(new InnerShadow(6.0, 3.0, 3.0, Color.DARKORCHID));
            text.setFill(Color.AQUA);
            text.setFont(Font.font("Lucida Calligraphy", FontWeight.BLACK, 80));
            GridPane.setConstraints(text, 0, rowIndex, 6, 1, HPos.CENTER, VPos.CENTER);
            startList.add(text);
            ++rowIndex;
        }
        
        Font toolFont = Font.font("", FontWeight.BLACK, 32);
        
        ToggleGroup gameLengthGroup = new ToggleGroup();
        {
            Label label = new Label("遊戲長度");
            label.setFont(toolFont);
            GridPane.setConstraints(label, 0, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            RadioButton len1 = new RadioButton("東風戦");
            len1.setFont(toolFont);
            len1.setToggleGroup(gameLengthGroup);
            len1.setUserData(Integer.valueOf(1));
            len1.setSelected(true);
            GridPane.setConstraints(len1, 1, rowIndex, 2, 1, HPos.CENTER, VPos.CENTER);
            
            RadioButton len2 = new RadioButton("東南戦");
            len2.setFont(toolFont);
            len2.setToggleGroup(gameLengthGroup);
            len2.setUserData(Integer.valueOf(2));
            GridPane.setConstraints(len2, 3, rowIndex, 2, 1, HPos.CENTER, VPos.CENTER);
            
            startList.addAll(label, len1, len2);
            ++rowIndex;
        }
        
        ToggleGroup initOyaGroup = new ToggleGroup();
        {
            Label label = new Label("起始座位");
            label.setFont(toolFont);
            GridPane.setConstraints(label, 0, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            RadioButton seat0 = new RadioButton("？");
            seat0.setFont(toolFont);
            seat0.setToggleGroup(initOyaGroup);
            seat0.setUserData(Integer.valueOf(1 + (int)(Math.random() * 4)));
            seat0.setSelected(true);
            GridPane.setConstraints(seat0, 1, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            RadioButton seat1 = new RadioButton("東");
            seat1.setFont(toolFont);
            seat1.setToggleGroup(initOyaGroup);
            seat1.setUserData(Integer.valueOf(1));
            GridPane.setConstraints(seat1, 2, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            RadioButton seat2 = new RadioButton("南");
            seat2.setFont(toolFont);
            seat2.setToggleGroup(initOyaGroup);
            seat2.setUserData(Integer.valueOf(4));
            GridPane.setConstraints(seat2, 3, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            RadioButton seat3 = new RadioButton("西");
            seat3.setFont(toolFont);
            seat3.setToggleGroup(initOyaGroup);
            seat3.setUserData(Integer.valueOf(3));
            GridPane.setConstraints(seat3, 4, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            RadioButton seat4 = new RadioButton("北");
            seat4.setFont(toolFont);
            seat4.setToggleGroup(initOyaGroup);
            seat4.setUserData(Integer.valueOf(2));
            GridPane.setConstraints(seat4, 5, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            startList.addAll(label, seat0, seat1, seat2, seat3, seat4);
            ++rowIndex;
        }
        
        Text startPointsText = new Text("25000");
        startPointsText.setFont(toolFont);
        startPointsText.setUserData(Integer.valueOf(25000));
        {
            GridPane.setConstraints(startPointsText, 3, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Label label = new Label("配給原点");
            label.setFont(toolFont);
            label.setLabelFor(startPointsText);
            GridPane.setConstraints(label, 0, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button dec2 = new Button("-10000");
            dec2.setOnAction(e -> {
                Integer x = (Integer) startPointsText.getUserData();
                x = Math.max(0, x - 10000);
                startPointsText.setText(Integer.toString(x));
                startPointsText.setUserData(Integer.valueOf(x));
            });
            GridPane.setConstraints(dec2, 1, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button dec1 = new Button("-1000");
            dec1.setOnAction(e -> {
                Integer x = (Integer) startPointsText.getUserData();
                x = Math.max(0, x - 1000);
                startPointsText.setText(Integer.toString(x));
                startPointsText.setUserData(Integer.valueOf(x));
            });
            GridPane.setConstraints(dec1, 2, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button inc1 = new Button("+2000");
            inc1.setOnAction(e -> {
                Integer x = (Integer) startPointsText.getUserData();
                x = x + 2000;
                startPointsText.setText(Integer.toString(x));
                startPointsText.setUserData(Integer.valueOf(x));
            });
            GridPane.setConstraints(inc1, 4, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button inc2 = new Button("+20000");
            inc2.setOnAction(e -> {
                Integer x = (Integer) startPointsText.getUserData();
                x = x + 20000;
                startPointsText.setText(Integer.toString(x));
                startPointsText.setUserData(Integer.valueOf(x));
            });
            GridPane.setConstraints(inc2, 5, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            startList.addAll(label, dec2, dec1, startPointsText, inc1, inc2);
            ++rowIndex;
        }
        
        Text limitPointText = new Text("+5000");
        limitPointText.setFont(toolFont);
        limitPointText.setUserData(Integer.valueOf(5000));
        {
            GridPane.setConstraints(limitPointText, 3, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Label label = new Label("原点");
            label.setFont(toolFont);
            label.setLabelFor(limitPointText);
            GridPane.setConstraints(label, 0, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button dec1 = new Button("-5000");
            dec1.setOnAction(e -> {
                Integer x = (Integer) limitPointText.getUserData();
                x = Math.max(0, x - 5000);
                limitPointText.setText(String.format("%+d", x));
                limitPointText.setUserData(Integer.valueOf(x));
            });
            GridPane.setConstraints(dec1, 2, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button inc1 = new Button("+5000");
            inc1.setOnAction(e -> {
                Integer x = (Integer) limitPointText.getUserData();
                x = x + 5000;
                limitPointText.setText(String.format("%+d", x));
                limitPointText.setUserData(Integer.valueOf(x));
            });
            GridPane.setConstraints(inc1, 4, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            startList.addAll(label, dec1, limitPointText, inc1);
            ++rowIndex;
        }
        
        Text shibariText = new Text("1");
        shibariText.setFont(toolFont);
        GridPane.setConstraints(shibariText, 3, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
        CheckBox shibariCheck = new CheckBox(">4本場+1");
        shibariCheck.setIndeterminate(false);
        shibariCheck.setSelected(true);
        GridPane.setConstraints(shibariCheck, 5, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
        {
            Label label = new Label("Ｎ飜縛り");
            label.setFont(toolFont);
            label.setLabelFor(shibariText);
            GridPane.setConstraints(label, 0, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button dec = new Button("<");
            dec.setOnAction(e -> {
                int x = Integer.parseInt(shibariText.getText());
                x = Math.max(0, x - 1);
                shibariText.setText(Integer.toString(x));
            });
            GridPane.setConstraints(dec, 2, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            Button inc = new Button(">");
            inc.setOnAction(e -> {
                int x = Integer.parseInt(shibariText.getText());
                x = x + 1;
                shibariText.setText(Integer.toString(x));
            });
            GridPane.setConstraints(inc, 4, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER);
            
            startList.addAll(label, dec, shibariText, inc, shibariCheck);
            ++rowIndex;
        }
        
        Button goBtn = new Button("GO");
        goBtn.setFont(toolFont);
        goBtn.setDisable(true);
        goBtn.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY)
                goBtn.setText(goBtn.getText() + (Math.random() < .5 ? "!" : "?"));
        });
        goBtn.setOnAction(e -> {
            int startPoints = (Integer) startPointsText.getUserData();
            slot[1] = new PlayerGUI(1, "Player", startPoints);
            slot[2] = new PlayerGUI(2, "Zako_1", startPoints);
            slot[3] = new PlayerGUI(3, "Zako_2", startPoints);
            slot[4] = new PlayerGUI(4, "Zako_3", startPoints);
            gameInfo = Game.getGameInfo(
                (Integer) gameLengthGroup.getSelectedToggle().getUserData(),
                (Integer) initOyaGroup.getSelectedToggle().getUserData(),
                (Integer) limitPointText.getUserData(),
                startPoints,
                Integer.parseInt(shibariText.getText()),
                shibariCheck.isSelected() ? 1 : 0,
                goBtn.getText().length() & 1
            );
            
            sceneObjects.addAll(slot[1].getFxNodes());
            sceneObjects.addAll(slot[2].getFxNodes());
            sceneObjects.addAll(slot[3].getFxNodes());
            sceneObjects.addAll(slot[4].getFxNodes());
            stage.setScene(gameScene);
            MainGUI.this.gameStart();
        });
        startList.add(goBtn);
        GridPane.setConstraints(goBtn, 0, ++rowIndex, 6, 1, HPos.CENTER, VPos.CENTER);
        
        stage.setScene(new Scene(startPane));
        stage.setTitle("To Be Continued ? ");
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
        
        new Thread(new Task<Void>() {   // background loading
            @Override protected Void call() throws Exception {
                Card.fillWholeCards(deck);
                deck.forEach(c -> sceneObjects.add(c.cardView));
                return null;
            }
            @Override protected void succeeded() {
                goBtn.setDisable(false);
                goBtn.requestFocus();
            }
        }).start();
        return;
    }
    
    public static void main(String[] args) {
        Application.launch(args);
        return;
    }
    
    private static final int[] TAKE_OFFSET = { 0, 102, 68, 34, 0, 102, 68, 34 };
    private static final double[][] ANCHOR_X = {
        { SCENE_WIDTH / 2 - Card.W * 8,     +Card.W },  //    (YamaLayout)    
        { SCENE_WIDTH - Card.H / 2 - PLAYER_AREA, 0 },  //          0         
        { SCENE_WIDTH / 2 + Card.W * 8,     -Card.W },  //    ||||||||||||    
        { PLAYER_AREA + Card.H / 2,               0 }   //  --    (180)   --  
    };                                                  //  --            --  
    private static final double[][] ANCHOR_Y = {        // 3--(90)   (270)--1 
        { PLAYER_AREA + Card.H / 2,               0 },  //  --            --  
        { SCENE_WIDTH / 2 - Card.W * 8,     +Card.W },  //  --     (0)    --  
        { SCENE_WIDTH - Card.H / 2 - PLAYER_AREA, 0 },  //    ||||||||||||    
        { SCENE_WIDTH / 2 + Card.W * 8,     -Card.W }   //          2         
    };
    private static final double[] ROTATION = { 180, 270, 0, 90 };
    private static final char[][] INFO_CHAR = {
        { '？', '東', '南', '西', '北' }, { '？', '１', '２', '３', '４' }
    };
    
    /** invoked in FxApplicationThread */
    private void gameStart() {
        int diceValue = (int)(Math.random() * 6) + (int)(Math.random() * 6) + 2;
        int takeIndex = (2 * diceValue +
            TAKE_OFFSET[diceValue % 4 + gameInfo.get("currOyaSeat")]) % 136;
        if (gameInfo.get("cheatEnabled") == 1) {
            if (gameInfo.get("cheatTakeIndex") == 0) {
                gameInfo.put("cheatTakeIndex", takeIndex);
                cheatEngine();
                return;
            }
            takeIndex = gameInfo.put("cheatTakeIndex", 0);
        } else {
            Collections.shuffle(deck);
        }
        
        remainInfo.setText("70");
        kyokuInfo.setText(String.format("%c%c局",
            INFO_CHAR[0][gameInfo.get("bafuu")], INFO_CHAR[1][gameInfo.get("kyoku")]));
        kyokuInfo.setTextFill(gameInfo.get("allLast") == 0 ? Color.WHITE : Color.ORANGE);
        bonbaInfo.setText(gameInfo.get("bonba").toString());
        richiInfo.setText(gameInfo.get("richi").toString());
        
        for (int i = 0; i < 136; ++i) {
            int j = i / 34;
            int k = i % 34 / 2;
            deck.get(i ^ 1).setInYama(ROTATION[j],
                ANCHOR_X[j][0] + ANCHOR_X[j][1] * k,
                ANCHOR_Y[j][0] + ANCHOR_Y[j][1] * k);
        }
        // 嶺上牌顯示順序調整
        Collections.swap(deck, takeIndex - 1, takeIndex - 2);
        Collections.swap(deck, takeIndex - 3, takeIndex - 4);
        
        // 依拿牌順序加入牌山
        LinkedList<Card> shuffledYama = new LinkedList<>();
        for (int i = takeIndex; i < 136; ++i) {
            shuffledYama.addFirst(deck.get(i));
        }
        for (int i =   0; i < takeIndex; ++i) {
            shuffledYama.addFirst(deck.get(i));
        }
        
        Game game = new Game(slot, shuffledYama) {
            final boolean cheatKeyEnabled = gameInfo.get("cheatEnabled") == 1;
            @Override protected int increaseRichibou() {
                int num = super.increaseRichibou();
                Platform.runLater(() -> richiInfo.setText(Integer.toString(num)));
                return 0;
            }
            @Override protected void changeCurrentPlayer(Player np) {
                PlayerGUI gcp = slot[cp.seat];
                PlayerGUI gnp = slot[np.seat];
                super.changeCurrentPlayer(np);
                Platform.runLater(() -> {
                    gcp.setCurrentPlayer(false);
                    gnp.setCurrentPlayer(true);
                    remainInfo.setText(Integer.toString(yama.size() - 14));
                });
                try {
                    Thread.sleep(Card.ANITIME);
                } catch (Exception ouch) {
                    ouch.printStackTrace();
                }
                if (cheatKeyEnabled && phase == RoundPhase.NORMAL_DRAW_DISCARD &&
                    cheatKeyState.get()) {
                    cheatSwapper(yama);
                }
                return;
            }
            @Override protected Card flipDoraIndicator() {
                Card newDoraIndicator = super.flipDoraIndicator();
                Platform.runLater(() -> newDoraIndicator.setNoResponse(true));
                return null;
            }
        };
        
        new Service<Void>() {
            @Override protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override protected Void call() throws Exception {
                        Thread.sleep(128);
                        return null;
                    }
                };
            }
            @Override protected void succeeded() {
                game.dealCard();
                if (game.isReadyToStart()) {
                    new Thread(new Task<Void>() {
                        @Override protected Void call() throws Exception {
                            game.startRound();
                            return null;
                        }
                        @Override protected void succeeded() {
                            MainGUI.this.gameOver(game);
                        }
                    }).start();
                } else {
                    this.restart();
                }
            }
        }.start();
        return;
    }
    
    private static final Font diffFont = Font.font("", FontWeight.BLACK, 16);
    private static final Font yakuFont = Font.font("", FontWeight.BLACK, 30);
    private static final Font textFont = Font.font("", FontWeight.BLACK, 48);
    private static final Background boardBackground = new Background(
        new BackgroundFill(new Color(.1, .1, .1, .8), new CornerRadii(9), Insets.EMPTY));
    private static final HPos[] diffGridAlignment =
        { null, HPos.CENTER, HPos.LEFT, HPos.CENTER, HPos.RIGHT };
    private static final TextAlignment[] diffTextAlignment =
        { null, TextAlignment.CENTER, TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT };
    
    private static Node makeFxDiff(int s, int[] point, int[] diff) {
        Text text1 = new Text(Integer.toString(point[s]));
        text1.setFill(Color.WHITE);
        text1.setFont(diffFont);
        Text text2 = new Text(diff[s] == 0 ? "" : String.format("%+d", diff[s]));
        text2.setFill(diff[s] > 0 ? Color.CYAN : Color.CRIMSON);
        text2.setFont(diffFont);
        point[s] += diff[s];
        TextFlow tf = new TextFlow(text1, text2);
        tf.setTextAlignment(diffTextAlignment[s]);
        GridPane.setHalignment(tf, diffGridAlignment[s]);
        return tf;
    }
    
    private static Text makeFxText(String content, HPos hAlignment, Font font) {
        Text fxNode = new Text(content);
        fxNode.setFill(Color.WHITE);
        fxNode.setFont(font);
        GridPane.setHalignment(fxNode, hAlignment);
        return fxNode;
    }
    
    private static HBox makeFxDora(List<Card> doraList) {
        HBox hbox = new HBox();
        ObservableList<Node> nodeList = hbox.getChildren();
        nodeList.add(Card.createBackView());
        nodeList.add(Card.createBackView());
        doraList.forEach(c -> nodeList.add(c.doraView));
        doraList.clear();
        while (nodeList.size() < 7) {
            nodeList.add(Card.createBackView());
        }
        hbox.setAlignment(Pos.CENTER);
        GridPane.setHalignment(hbox, HPos.CENTER);
        return hbox;
    }
    
    private static void applyPaneTemplate(Pane pane) {
        pane.setMaxWidth(SCENE_WIDTH - PLAYER_AREA * 2);
        pane.setMinWidth(SCENE_WIDTH - PLAYER_AREA * 2);
        pane.setBackground(boardBackground);
        pane.setTranslateX(PLAYER_AREA);
        pane.setTranslateY(PLAYER_AREA);
        return;
    }
    
    private static GridPane buildBoard(
            Node fxInfo1, Node fxInfo2, int[] oldPoint,
            ArrayList<SimpleEntry<Analyze.Bunkai, int[]>> result) {
        GridPane board = new GridPane();
        applyPaneTemplate(board);
        board.setVgap(6);
        board.setAlignment(Pos.CENTER);
        board.setPadding(new Insets(16));
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        ColumnConstraints col3 = new ColumnConstraints();
        ColumnConstraints col4 = new ColumnConstraints();
        ColumnConstraints col5 = new ColumnConstraints();
        col1.setPercentWidth(20);//  --- -------- --- -------- --- 
        col2.setPercentWidth(20);// |   |        |   |        |   |
        col3.setPercentWidth(20);// |   |   Name |   | Fan    |   |
        col4.setPercentWidth(20);// |   |        |   |        |   |
        col5.setPercentWidth(20);//  --- -------- --- -------- --- 
        board.getColumnConstraints().addAll(col1, col2, col3, col4, col5);
        
        // dora或title
        board.add(fxInfo1, 0, 0, 5, 1);
        if (result.isEmpty()) {
            return board;// 途中流局
        }
        
        SimpleEntry<Analyze.Bunkai, int[]> entry = result.remove(0);
        Analyze.Bunkai bunkai = entry.getKey();
        int rowIndex = 1;
        // 役種列表
        for (String[] s: bunkai.getYakuList()) {
            board.add(makeFxText(s[0], HPos.LEFT,  yakuFont), 1, rowIndex);
            board.add(makeFxText(s[1], HPos.RIGHT, yakuFont), 3, rowIndex);
            ++rowIndex;
        }
        
        // 裏dora
        if (bunkai.containsUraDora()) {
            board.add(fxInfo2, 0, rowIndex, 5, 1);
            ++rowIndex;
        }
        
        // a符b飜c稱號d点
        board.add(makeFxText(bunkai.getSummaryString(), HPos.CENTER, yakuFont),
            0, rowIndex, 5, 1
        );
        rowIndex += 2;
        
        // 各家點數變化
        int[] ptDiff = entry.getValue();
        board.add(makeFxDiff(1, oldPoint, ptDiff), 0, rowIndex + 2, 5, 1);
        board.add(makeFxDiff(2, oldPoint, ptDiff), 3, rowIndex + 1, 2, 1);
        board.add(makeFxDiff(3, oldPoint, ptDiff), 0, rowIndex + 0, 5, 1);
        board.add(makeFxDiff(4, oldPoint, ptDiff), 0, rowIndex + 1, 2, 1);
        return board;
    }
    
    /** invoked in FxApplicationThread */
    private void gameOver(Game game) {
        // 記下當前資訊
        int[] oldPoint = new int[] { 0,
            slot[1].point, slot[2].point, slot[3].point, slot[4].point
        };
        SnapshotParameters canvasSP = new SnapshotParameters();
        canvasSP.setFill(Color.TRANSPARENT);
        ArrayList<SimpleEntry<Analyze.Bunkai, int[]>> result = new ArrayList<>(4);
        ArrayList<Card> finalDoraList = new ArrayList<>(10);
        
        // 處理本局結果
        Node fxBonba = new ImageView(bonbaInfo.snapshot(canvasSP, null));
        String title = game.scoring(result, finalDoraList);
        int halfSize = finalDoraList.size() >> 1;
        Node fxInfo1 = title.isEmpty() ?
                       makeFxDora(finalDoraList.subList(0, halfSize)) :
                       makeFxText(title, HPos.CENTER, textFont);
        Node fxInfo2 = makeFxDora(finalDoraList);
        
        // 可能不只一人和牌、會需要重複顯示記分板
        new Service<Void>() {
            @Override protected Task<Void> createTask() {
                if (!result.isEmpty()) {
                    richiInfo.setText(Integer.toString(result.get(0).getValue()[0]));
                }
                Node fxRichi = new ImageView(richiInfo.snapshot(canvasSP, null));
                return new Task<Void>() {
                    @Override protected Void call() throws Exception {
                        GridPane board = buildBoard(fxInfo1, fxInfo2, oldPoint, result);
                        Button ok = new Button("OK");
                        int rowIndex = board.getRowCount() + 1;
                        board.add(fxBonba, 0, rowIndex, 2, 1);
                        board.add(     ok, 2, rowIndex, 1, 1);
                        board.add(fxRichi, 3, rowIndex, 2, 1);
                        GridPane.setHalignment(fxBonba, HPos.CENTER);
                        GridPane.setHalignment(     ok, HPos.CENTER);
                        GridPane.setHalignment(fxRichi, HPos.CENTER);
                        
                        AtomicInteger cd = new AtomicInteger(30);
                        ok.setOnAction(e -> {
                            synchronized (cd) {
                                cd.set(0);
                                cd.notify();
                            }
                        });
                        Platform.runLater(() -> sceneObjects.add(board));
                        synchronized (cd) {
                            while (cd.decrementAndGet() > 0) {
                                Platform.runLater(() -> ok.setText("OK" + cd.get()));
                                try {
                                    cd.wait(1000);
                                } catch (Exception ouch) {
                                    ouch.printStackTrace();
                                }
                            }
                        }
                        return null;
                    }
                };
            }
            @Override protected void succeeded() {
                sceneObjects.remove(sceneObjects.size() - 1/* board */);
                if (result.isEmpty()) {
                    if (game.updateGameInfo()) {
                        MainGUI.this.gameStart();
                    } else {
                        MainGUI.this.ending();
                    }
                } else {
                    this.restart();
                }
            }
        }.start();
        return;
    }
    
    /** invoked in FxApplicationThread */
    private void ending() {
        GridPane board = new GridPane();
        applyPaneTemplate(board);
        board.setVgap(24);
        board.setHgap(24);
        board.setAlignment(Pos.CENTER);
        board.setPadding(new Insets(24));
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        ColumnConstraints col3 = new ColumnConstraints();
        col1.setPercentWidth(35);//  -------- -------- ------- 
        col2.setPercentWidth(35);// |  Name  | Points | Score |
        col3.setPercentWidth(30);//  -------- -------- ------- 
        board.getColumnConstraints().addAll(col1, col2, col3);
        board.add(makeFxText("終　局", HPos.CENTER, textFont), 0, 0, 3, 1);
        
        double[] finalScore = Game.getFinalScore(slot);
        for (int seat = 1; seat <= 4; ++seat) {
            Text name = new Text(slot[seat].name);
            name.setFont(yakuFont);
            name.setFill(Color.WHITE);
            GridPane.setConstraints(name, 0, seat, 1, 1, HPos.CENTER, VPos.CENTER);
            Text point = new Text(Integer.toString(slot[seat].point));
            point.setFont(yakuFont);
            point.setFill(Color.WHITE);
            GridPane.setConstraints(point, 1, seat, 1, 1, HPos.RIGHT, VPos.CENTER);
            Text score = new Text(String.format("%+.1f", finalScore[seat]));
            score.setFont(yakuFont);
            score.setFill(finalScore[seat] >= 0 ? Color.CYAN : Color.CRIMSON);
            GridPane.setConstraints(score, 2, seat, 1, 1, HPos.RIGHT, VPos.CENTER);
            board.getChildren().addAll(name, point, score);
        }
        Button endButton = new Button("END");
        endButton.setOnAction(e -> System.exit(0));
        board.add(endButton, 0, 5, 3, 1);
        GridPane.setHalignment(endButton, HPos.CENTER);
        
        sceneObjects.clear();
        sceneObjects.add(board);
        return;
    }
    
    private static Canvas makePointStick(int pt) {
        double w = INFO_W * .7;
        double h = INFO_H * .7;
        Canvas c = new Canvas(w, INFO_H);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(0.0, (INFO_H - h) / 2, w, h, h, h);
        gc.setFill(Color.BLACK);
        gc.strokeRoundRect(0.0, (INFO_H - h) / 2, w, h, h, h);
        if (pt == 1000) {
            double s = INFO_H * .4;
            gc.setFill(Color.RED);
            gc.fillOval(w / 2 - s / 2, INFO_H / 2 - s / 2, s, s);
        } else {
            double s = INFO_H * .2;
            for (int i = 3; i <= 6; ++i) {
                gc.fillOval(w / 9 * i - s / 2, INFO_H / 5 * 2 - s * .75, s, s);
                gc.fillOval(w / 9 * i - s / 2, INFO_H / 5 * 3 - s * .25, s, s);
            }
        }
        return c;
    }
    
    private Pane cheatPane = null;
    private final ToggleGroup cheatToggleGroup = new ToggleGroup();
    private final ArrayList<ArrayList<Card>> cheatBox = new ArrayList<>(5);
    private final HashMap<Card, Integer> cheatCardGroup = new HashMap<>();
    
    private void cheatEngine() {
        final double MARGIN = 20;
        final double TEXT_Y = 20;
        final double BASE_X = PLAYER_AREA * 3 - Card.W - Card.ADJUST_X;
        final double BASE_Y = PLAYER_AREA + Card.H *11 - Card.ADJUST_Y + TEXT_Y;
        
        if (cheatPane == null) {
            cheatPane = new Pane();
            applyPaneTemplate(cheatPane);
            cheatPane.setMinHeight(SCENE_WIDTH - PLAYER_AREA * 2);
            ObservableList<Node> children = cheatPane.getChildren();
            
            RadioButton[] btn = new RadioButton[5];
            for (int i = 0; i < 5; ++i) {
                btn[i] = new RadioButton("王牌");
                btn[i].setUserData(Integer.valueOf(i));
                btn[i].setToggleGroup(cheatToggleGroup);
                btn[i].setTranslateX(MARGIN);
                btn[i].setTranslateY(MARGIN + Card.H * i * 2 + TEXT_Y);
                btn[i].setTextFill(Color.WHITE);
                btn[i].setFocusTraversable(false);
                children.add(btn[i]);
                cheatBox.add(new ArrayList<Card>(14));
            }
            btn[1].setSelected(true);
            
            String[] specText = { "嶺2", "嶺1", "嶺4", "嶺3",
                "裏1", "表1", "裏2", "表2", "裏3", "表3", "裏4", "表4", "裏5", "表5"
            };
            for (int i = 0; i < 14; ++i) {
                Label spec = new Label(specText[i]);
                spec.setMinWidth(Card.W);
                spec.setAlignment(Pos.CENTER);
                spec.setTextFill(Color.WHITE);
                spec.setTranslateX(BASE_X + Card.W * i - PLAYER_AREA + Card.ADJUST_X);
                spec.setTranslateY(MARGIN);
                children.add(spec);
            }
            
            Button finishButton = new Button("Finish");
            children.add(finishButton);
            finishButton.setMaxSize(70, 30);
            finishButton.setMinSize(70, 30);
            finishButton.setTranslateX(SCENE_WIDTH - PLAYER_AREA * 2 - 80);
            finishButton.setTranslateY(SCENE_WIDTH - PLAYER_AREA * 2 - 40);
            finishButton.setOnAction(e -> {
                boolean noCheating = true;
                for (List<Card> ar: cheatBox) {
                    noCheating &= ar.isEmpty();
                }
                if (noCheating) {
                    Collections.shuffle(deck);
                } else {
                    List<Card> unused = new ArrayList<>(136);
                    for (Entry<Card, Integer> x: cheatCardGroup.entrySet()) {
                        if (x.getValue() < 0) {
                            unused.add(x.getKey());
                        }
                    }
                    Collections.shuffle(unused);
                    
                    int[] yamaBoxIndex = new int[] {
                        1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4,
                        1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4,
                        1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4,
                        1, 2, 3, 4, 1, 2, 3, 4
                    };
                    int[] boxTaken = new int[6];
                    Card[] buff = new Card[136];
                    int offset = gameInfo.get("cheatTakeIndex");
                    for (int i: yamaBoxIndex) {
                        buff[offset++ % 136] =
                            cheatBox.get(i).size() > boxTaken[i] ?
                            cheatBox.get(i).get(boxTaken[i]++) :
                            unused.get(boxTaken[5]++);
                    }
                    offset = gameInfo.get("cheatTakeIndex");
                    for (Card c: cheatBox.get(0)) {
                        buff[(--offset + 136) % 136] = c;
                    }
                    cheatBox.forEach(b -> b.clear());
                    deck.clear();
                    for (Card b: buff) {
                        deck.add(b == null ? unused.get(boxTaken[5]++) : b);
                    }
                }
                cheatPane.setVisible(false);
                MainGUI.this.gameStart();
            });
            sceneObjects.add(cheatPane);
        }
        for (int i = 0; i < 4; ++i) {
            ((RadioButton) cheatToggleGroup.getToggles().get(i + 1)).setText(
                   slot[Game.SEQUENCE[gameInfo.get("currOyaSeat") + i]].name);
        }
        cheatPane.setVisible(true);
        cheatPane.toFront();
        
        for (Card c: deck) {
            cheatCardGroup.put(c, -1);
            c.moveTo(true, 0, BASE_X + Card.W * c.vj, BASE_Y + Card.H * c.vi);
            c.cardView.toFront();
            c.cardView.setOnMouseClicked(e -> {
                c.cardView.toFront();
                int gid = cheatCardGroup.get(c);
                if (gid >= 0) {
                    Card d = cheatBox.get(gid).remove(cheatBox.get(gid).size() - 1);
                    if (c != d) {
                        cheatBox.get(gid).set(cheatBox.get(gid).indexOf(c), d);
                        d.moveBy(true, 0, 0, c);
                    }
                    cheatToggleGroup.selectToggle(cheatToggleGroup.getToggles().get(gid));
                    c.moveTo(true, 0, BASE_X + Card.W * c.vj, BASE_Y + Card.H * c.vi);
                    cheatCardGroup.put(c, -1);
                } else {
                    gid = (Integer) cheatToggleGroup.getSelectedToggle().getUserData();
                    int size = cheatBox.get(gid).size();
                    if (size >= 14)
                        return;
                    cheatBox.get(gid).add(c);
                    c.moveTo(true, 0, BASE_X + Card.W * size,
                             MARGIN + PLAYER_AREA + Card.H * (gid * 2 + 1));
                    cheatCardGroup.put(c, gid);
                }
                return;
            });
        }
        return;
    }
    
    private void cheatSwapper(LinkedList<Card> yama) {
        GridPane cheatPane = new GridPane();
        applyPaneTemplate(cheatPane);
        cheatPane.setPadding(new Insets(16));
        
        Text text = new Text("選擇要摸的牌");
        text.setFill(Color.WHITE);
        text.setFont(diffFont);
        GridPane.setMargin(text, new Insets(0, 0, 10, 10));
        cheatPane.add(text, 0, 0, 8, 2);
        
        AtomicBoolean waiting = new AtomicBoolean(true);
        int count = 0;
        for (Card c: yama) {
            if (++count >= 14) {
                int index = yama.size() - count;
                Button btn = new Button("", c.doraView);
                btn.setOnAction(e -> {
                    Card lastCard = yama.removeLast();
                    if (c != lastCard) {
                        c.swapLocation(lastCard);
                        yama.set(yama.size() - index, lastCard);
                    }
                    yama.addLast(c);
                    synchronized (waiting) {
                        waiting.set(false);
                        waiting.notify();
                    }
                });
                cheatPane.add(btn, index % 12, index / 12 + 2);
            }
        }
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            synchronized (waiting) {
                waiting.set(false);
                waiting.notify();
            }
        });
        GridPane.setHalignment(cancelButton, HPos.RIGHT);
        GridPane.setMargin(cancelButton, new Insets(10, 0, 0, 0));
        cheatPane.add(cancelButton, 0, cheatPane.getRowCount(), cheatPane.getColumnCount(), 1);
        
        Platform.runLater(() -> sceneObjects.add(cheatPane));
        synchronized (waiting) {
            while (waiting.get()) {
                try {
                    waiting.wait();
                } catch (Exception ouch) {
                    ouch.printStackTrace();
                }
            }
        }
        Platform.runLater(() -> sceneObjects.remove(sceneObjects.size() - 1));
        return;
    }
    
}
