package application;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Ref;
import java.util.*;

public class Client implements Initializable {
    private static final int PLAY_1 = 1;
    private static final int PLAY_2 = 2;
    private static final int EMPTY = 0;
    private static final int BOUND = 90;
    private static final int OFFSET = 15;

    @FXML
    private Pane base_square;

    @FXML
    private Rectangle game_panel;

    private static boolean TURN = false;

    private static final int[][] chessBoard = new int[3][3];
    private static final boolean[][] flag = new boolean[3][3];

    public static Connect ct = null;

    public Text tt = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        game_panel.setOnMouseClicked(event -> {
            int x = (int) (event.getX() / BOUND);
            int y = (int) (event.getY() / BOUND);
            System.out.println("click on " + x + y);
            if (refreshBoard(x, y, Connect.CTURN, Connect.enable)) {
                ct.pout.println(x + "," + y);
                ct.pout.flush();
                Connect.enable = false;
            }

        });

        Thread t = new Thread(ct = new Connect(this));
        t.start();
    }

    public boolean refreshBoard(int x, int y, boolean _turn, boolean _enable) {
        if (chessBoard[x][y] == EMPTY && _enable) {
            chessBoard[x][y] = _turn ? PLAY_1 : PLAY_2;
            drawChess();
            addText("Opponent's turn");
            return true;
        }
        return false;
    }

    public void shutDown() throws InterruptedException {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Server timeout!");
            alert.show();
        });
        Thread.sleep(3000);
        Platform.exit();
    }

    public void oppoShutDown() throws InterruptedException {
        addText("Opponent Timeout");
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Opponent timeout!");
            alert.show();
        });
        Thread.sleep(5000);
        Platform.exit();
    }

    public void drawResult(String s) throws InterruptedException {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("You " + s);
            alert.show();
        });
        addText("You " + s);
        Thread.sleep(10000);
    }

    public void down() {
        Platform.exit();
    }

    public void drawWaiting() throws InterruptedException {
        addText("Wait for opponent");
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Wait for opponent");
            alert.show();

        });
        Thread.sleep(1500);
    }

    public void addText(String s) {
        Platform.runLater(() -> {
            base_square.getChildren().remove(tt);
            tt = new Text(0, 0, s);
            base_square.getChildren().add(tt);
        });
    }

    public void addText2(String s) {
        Platform.runLater(() -> {
            base_square.getChildren().remove(tt);
            Text tt2 = new Text(100, 0, s);
            base_square.getChildren().add(tt2);
        });
    }

    public void drawMatch() throws InterruptedException {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Matched, Game Start!");
            alert.show();
        });
        Thread.sleep(1500);

    }

    private void drawChess() {
        for (int i = 0; i < chessBoard.length; i++) {
            for (int j = 0; j < chessBoard[0].length; j++) {
                if (flag[i][j]) {
                    // This square has been drawing, ignore.
                    continue;
                }
                switch (chessBoard[i][j]) {
                    case PLAY_1:
                        drawCircle(i, j);
                        break;
                    case PLAY_2:
                        drawLine(i, j);
                        break;
                    case EMPTY:
                        // do nothing
                        break;
                    default:
                        System.err.println("Invalid value!");
                }
            }
        }
    }

    private void drawCircle(int i, int j) {
        Circle circle = new Circle();
        Platform.runLater(() -> {
            base_square.getChildren().add(circle);
        });
        circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
        circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
        circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
        circle.setStroke(Color.RED);
        circle.setFill(Color.TRANSPARENT);
        flag[i][j] = true;
    }

    private void drawLine(int i, int j) {
        Line line_a = new Line();
        Line line_b = new Line();
        Platform.runLater(() -> {
            base_square.getChildren().add(line_a);
            base_square.getChildren().add(line_b);
        });
        line_a.setStartX(i * BOUND + OFFSET * 1.5);
        line_a.setStartY(j * BOUND + OFFSET * 1.5);
        line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
        line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_a.setStroke(Color.BLUE);

        line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
        line_b.setStartY(j * BOUND + OFFSET * 1.5);
        line_b.setEndX(i * BOUND + OFFSET * 1.5);
        line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_b.setStroke(Color.BLUE);
        flag[i][j] = true;
    }
}

class Connect implements Runnable {
    private Client clt;
    public static volatile boolean enable = false;
    public DataInputStream din;
    public DataOutputStream dout;
    public Scanner sc;
    public PrintWriter pout;
    public static boolean CTURN = false;
    public boolean terminate = false;

    public Connect(Client _clt) {
        this.clt = _clt;
    }

    public void terminate() throws IOException, InterruptedException {
        this.terminate = true;
        System.out.println("Server terminate");
        clt.shutDown();
    }

    @Override
    public void run() {
        System.out.println("Enter socket");
        Socket s = null;
        try {
            s = new Socket("localhost", 54321);
            System.out.println("Connected");
            ConnectCheck cc = new ConnectCheck(s, this);
            Thread t = new Thread(cc);
            t.start();
            din = new DataInputStream(s.getInputStream());
            dout = new DataOutputStream(s.getOutputStream());
            sc = new Scanner(din);
            pout = new PrintWriter(dout);
            while (true) {
                while (!sc.hasNext()) {
                    if (terminate) {
                        System.out.println("Server terminate");
                        throw new IOException();
                    }
                    Thread.sleep(100);
                }
                String str = sc.next();
                System.out.println("Server: " + str);
                if (str.equals("WAIT"))
                    clt.drawWaiting();
                if (terminate) {
                    System.out.println("Server terminate");
                    throw new IOException();
                }
                if (str.equals("READY")) {
                    System.out.println("READY");
                    clt.drawMatch();
                    clt.addText("Ready");
                    String str2 = sc.next();
                    String id = sc.next();
                    clt.addText2("Player No. " + id);
                    if (str2.equals("FIRST")) {
                        CTURN = true;
                    } else {
                        CTURN = false;
                    }
                    break;
                }
            }
            String op = sc.next();
            if (op == null || terminate) {
                System.out.println("Server closed");
                throw new IOException();
            }
            while (!op.equals("FINISHED")) {
                if (op.equals("TURN")) {
                    enable = true;
                    clt.addText("Your turn");
                    System.out.println("ENABLE");
                } else if (op.equals("WIN")) {
                    System.out.println("win");
                    clt.drawResult("win");
                    enable = false;
                    break;
                } else if (op.equals("LOSE")) {
                    System.out.println("lose");
                    clt.drawResult("lose");
                    enable = false;
                    break;
                } else if (op.equals("DRAW")) {
                    System.out.println("draw");
                    clt.drawResult("draw");
                    enable = false;
                    break;
                } else {
                    enable = false;

                    if (op.equals("CLIENT_EXIT")) {
                        System.out.println("Opponent timeout");
                        clt.oppoShutDown();
                        break;
                    }
                    String[] pos = op.split(",");
                    int x = Integer.parseInt(pos[0]);
                    int y = Integer.parseInt(pos[1]);
                    clt.refreshBoard(x, y, !CTURN, true);
                }
                op = sc.next();
            }
            System.out.println("Client exit");
            clt.down();
            System.exit(0);
        } catch (Exception e) {
            try {
                s.close();
            } catch (Exception ex) {
                System.out.println("Server missing!");
                clt.down();
            }
        }
    }
}

class ConnectCheck implements Runnable {
    private Socket s;
    public Connect c;

    public ConnectCheck(Socket _s, Connect _c) {
        this.s = _s;
        this.c = _c;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
                s.sendUrgentData(0xFF);
            } catch (Exception e) {
                try {
                    c.terminate();
                    c.sc.close();
                } catch (IOException ex) {
                    ;
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                break;
            }
        }
    }
}
