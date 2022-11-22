package application;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static List<EstablishedConnection> ecl = new ArrayList<>();
    static List<Player> pl = new ArrayList<>();
    //    static List<Socket> sl = new ArrayList<>();
//    static List<ClientHandler> chl = new ArrayList<>();
    static int idx = 0;

    public static void main(String[] args) throws Exception {

        ServerSocket ss = new ServerSocket(54321);
        while (true) {
            Socket s = null;
            ClientHandler ch = null;
            try {
                s = ss.accept();
                EstablishedConnection ec = null;
                for (int i = 0; i < ecl.size(); i++) {
                    if (!ecl.get(i).ch1.READY && !ecl.get(i).ch1.DEAD) {
                        ec = ecl.get(i);
                        break;
                    }
                }
                DataInputStream din = new DataInputStream(s.getInputStream());
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                System.out.println("A new client " + idx + " th is connected : " + s);
                if (ec == null) {
                    ch = new ClientHandler(idx, s, din, dout);
                    ec = new EstablishedConnection(idx, s, "", ch);
                    ecl.add(ec);
                    Thread t = new Thread(ch);
                    System.out.println("Assigning new thread for this client " + t);
                    t.start();
                } else {
                    ch = ec.ch1;
                    ec.setPlayer2(idx, s, "");
                    ch.matchPlayer(idx, s, din, dout);
                    System.out.println("Match player " + idx + " to " + ec.id1 + " or "+ ec.id2);
                }
                idx++;
            } catch (Exception e) {
                s.close();
                e.printStackTrace();
            }
        }
    }
}

class ClientHandler implements Runnable {
    int id;
    Socket s;
    DataInputStream din;
    DataOutputStream dout;
    Scanner sc;
    PrintWriter pout;
    public boolean ready1 = false;

    int id2;
    Socket s2;
    DataInputStream din2;
    DataOutputStream dout2;
    Scanner sc2;
    PrintWriter pout2;
    public boolean ready2 = false;

    private final int PLAY_1 = 1;
    private final int PLAY_2 = 2;
    private final int EMPTY = 0;
    private final int BOUND = 90;
    private final int OFFSET = 15;

    private boolean TURN = false;

    private final int[][] chessBoard = new int[3][3];
    private final boolean[][] flag = new boolean[3][3];

    public boolean READY = false;
    public boolean DEAD = false;

    public Server ss;

    public Player pl1;
    public Player pl2;


    public ClientHandler(int _id, Socket _s, DataInputStream _din, DataOutputStream _out) {
        this.id = _id;
        this.s = _s;
        this.din = _din;
        this.dout = _out;
        this.sc = new Scanner(din);
        this.pout = new PrintWriter(dout);
        this.ready1 = true;
    }

    public void matchPlayer(int _id, Socket _s, DataInputStream _din, DataOutputStream _out) {
        this.id2 = _id;
        this.s2 = _s;
        this.din2 = _din;
        this.dout2 = _out;
        this.sc2 = new Scanner(din2);
        this.pout2 = new PrintWriter(dout2);
        this.ready2 = true;
        READY = true;
        System.out.println("Set READY = true" + READY);
    }

    public void clientExit(int id) {
        System.out.println("Client " + id + " exit");
        if (id == 1) {
            this.ready1 = false;
            if (this.ready2) {
                this.pout2.println("CLIENT_EXIT");
                this.pout2.flush();
            }
        }
        else {
            this.ready2 = false;
            if (this.ready1) {
                this.pout.println("CLIENT_EXIT");
                this.pout.flush();
            }
        }
        READY = false;
    }


    @Override
    public void run() {
        try {
            ConnectionCheck cc = new ConnectionCheck(s, this);
            Thread tt = new Thread(cc);
            tt.start();

            pout.println("WAIT"); // tell player 1 to wait
            pout.flush();
            boolean end = false;
            while (!end) {
                System.out.println("Waiting for player 2" + READY);
                Thread.sleep(1000);
                if (!ready1 && !ready2) {
                    DEAD = true;
                    break;
                }
                if (READY) {
                    System.out.println("Player 2 is ready");
                    cc.enableS2(s2);
                    pout.println("READY");
                    pout.println("FIRST");
                    pout.println(id);
                    pout2.println("READY");
                    pout2.println("SECOND");
                    pout2.println(id2);
                    pout.flush();
                    pout2.flush();
                    boolean select = false;

                    while (READY) {
                        System.out.println("Select " + select);
                        if (!select) {
                            pout.println("TURN");
                        } else {
                            pout2.println("TURN");
                        }
                        pout.flush();
                        pout2.flush();
                        String[] op = null;
                        if (!select) {
                            if (sc.hasNext()) {
                                op = sc.next().split(",");
                            }
                        } else {
                            if (sc2.hasNext()) {
                                op = sc2.next().split(",");
                            }
                        }
                        System.out.println(op);
                        if (op != null) {
                            int x = Integer.parseInt(op[0]);
                            int y = Integer.parseInt(op[1]);
                            System.out.println("x = " + x + " y = " + y);
                            refreshBoard(x, y, select);
                            if (select) {
                                pout.println(x + "," + y);
                            } else {
                                pout2.println(x + "," + y);
                            }
                            pout.flush();
                            pout2.flush();
                        }
                        int result = state();
                        System.out.println("State: " + result);
                        if (result == PLAY_1) {
                            pout2.println("WIN");
                            pout.println("LOSE");
                            pout.flush();
                            pout2.flush();
                            end = true;
                            break;
                        } else if (result == PLAY_2) {
                            pout2.println("LOSE");
                            pout.println("WIN");
                            pout.flush();
                            pout2.flush();
                            end = true;
                            break;
                        } else if (result == EMPTY) {
                            pout.println("DRAW");
                            pout2.println("DRAW");
                            pout.flush();
                            pout2.flush();
                            end = true;
                            break;
                        }
                        select = !select;

                    }
                }
            }
            System.out.println("Server exit");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Really Stop");
        DEAD = true;
    }

    private int state() {
        for (int i = 0; i < 3; i++) {
            if (chessBoard[i][0] == chessBoard[i][1] && chessBoard[i][1] == chessBoard[i][2]) {
                if (chessBoard[i][0] == PLAY_1) return PLAY_1;
                else if (chessBoard[i][0] == PLAY_2) return PLAY_2;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (chessBoard[0][i] == chessBoard[1][i] && chessBoard[1][i] == chessBoard[2][i]) {
                if (chessBoard[0][i] == PLAY_1) return PLAY_1;
                else if (chessBoard[0][i] == PLAY_2) return PLAY_2;
            }
        }
        if (chessBoard[0][0] == chessBoard[1][1] && chessBoard[1][1] == chessBoard[2][2]) {
            if (chessBoard[0][0] == PLAY_1) return PLAY_1;
            else if (chessBoard[0][0] == PLAY_2) return PLAY_2;
        }
        if (chessBoard[0][2] == chessBoard[1][1] && chessBoard[1][1] == chessBoard[2][0]) {
            if (chessBoard[0][2] == PLAY_1) return PLAY_1;
            else if (chessBoard[0][2] == PLAY_2) return PLAY_2;
        }
        boolean flag = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (chessBoard[i][j] == EMPTY) flag = false;
            }
        }
        if (flag) return EMPTY;
        return -1;
    }

    private boolean refreshBoard(int x, int y, boolean _turn) {
        if (chessBoard[x][y] == EMPTY) {
            chessBoard[x][y] = _turn ? PLAY_1 : PLAY_2;
            return true;
        }
        return false;
    }
}

class ConnectionCheck implements Runnable {
    private Socket s1;
    private Socket s2;
    public ClientHandler ch;
    public boolean s1enable = false;
    public boolean s2enable = false;

    public ConnectionCheck(Socket _s1, ClientHandler _ch) {
        this.s1 = _s1;
        this.ch = _ch;
        this.s1enable = true;
    }

    public void enableS2(Socket _s2) {
        this.s2 = _s2;
        this.s2enable = true;
    }


    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (s1enable) {
                try {
                    s1.sendUrgentData(0xFF); //发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
                } catch (Exception se) {
                    ch.clientExit(1);
                    ch.DEAD = true;
                    s1enable = false;
                }
            }
            if (s2enable) {
                try {
                    s2.sendUrgentData(0xFF); //发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
                } catch (Exception se) {
                    ch.clientExit(2);
                    ch.DEAD = true;
                    s2enable = false;
                }
            }
        }


    }
}

class EstablishedConnection {
    public int id1;
    public int id2;
    public Socket s1;
    public Socket s2;
    public String p1;
    public String p2;
    public ClientHandler ch1;
    public boolean full = false;

    public EstablishedConnection(int _id, Socket _s1, String _p1, ClientHandler _ch1) {
        this.id1 = _id;
        this.s1 = _s1;
        this.p1 = _p1;
        this.ch1 = _ch1;
    }

    public void setPlayer2(int _id2, Socket _s2, String _p2) {
        this.id2 = _id2;
        this.s2 = _s2;
        this.p2 = _p2;
        this.full = true;
    }

}

class Player {
    String name;
    String pwd;
    int win;
    int draw;
    int lose;

    public Player(String _name, String _pwd, int _win, int _draw, int _lose) {
        this.name = _name;
        this.pwd = _pwd;
        this.win = _win;
        this.draw = _draw;
        this.lose = _lose;
    }

    public Player(String _name, String _pwd) {
        this.name = _name;
        this.pwd = _pwd;
        this.win = 0;
        this.draw = 0;
        this.lose = 0;
    }

    public void win() {
        this.win++;
    }

    public void draw() {
        this.draw++;
    }

    public void lose() {
        this.lose++;
    }
}
