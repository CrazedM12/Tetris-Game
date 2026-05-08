import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class Tetris extends JPanel implements ActionListener, KeyListener {

    private final int ROWS = 20;
    private final int COLS = 10;
    private final int BLOCK = 30;

    private javax.swing.Timer timer;

    private Color[][] board = new Color[ROWS][COLS];

    private Point piecePos;
    private int rotation;
    private Tetromino currentPiece;

    private Tetromino holdPiece = null;
    private boolean holdUsedThisTurn = false;

    private Queue<Tetromino> nextQueue = new LinkedList<>();

    private int score = 0;
    private int level = 1;
    private int totalLinesCleared = 0;

    private int combo = -1;

    private Color[] palette = new Color[7];

    private boolean paused = false;   // ⭐ Pause flag

    private enum Tetromino {
        I(new int[][][]{
                {{1,1,1,1}},
                {{1},{1},{1},{1}}
        }),

        O(new int[][][]{
                {{1,1},
                 {1,1}}
        }),

        T(new int[][][]{
                {{0,1,0},
                 {1,1,1}},
                {{1,0},
                 {1,1},
                 {1,0}},
                {{1,1,1},
                 {0,1,0}},
                {{0,1},
                 {1,1},
                 {0,1}}
        }),

        L(new int[][][]{
                {{1,0},
                 {1,0},
                 {1,1}},
                {{1,1,1},
                 {1,0,0}},
                {{1,1},
                 {0,1},
                 {0,1}},
                {{0,0,1},
                 {1,1,1}}
        }),

        J(new int[][][]{
                {{0,1},
                 {0,1},
                 {1,1}},
                {{1,0,0},
                 {1,1,1}},
                {{1,1},
                 {1,0},
                 {1,0}},
                {{1,1,1},
                 {0,0,1}}
        }),

        S(new int[][][]{
                {{0,1,1},
                 {1,1,0}},
                {{1,0},
                 {1,1},
                 {0,1}}
        }),

        Z(new int[][][]{
                {{1,1,0},
                 {0,1,1}},
                {{0,1},
                 {1,1},
                 {1,0}}
        });

        int[][][] shapes;

        Tetromino(int[][][] s) {
            shapes = s;
        }
    }

    public Tetris() {
        setPreferredSize(new Dimension(COLS * BLOCK + 200, ROWS * BLOCK));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        generatePastelPalette();
        refillQueue();
        spawnPiece();

        timer = new javax.swing.Timer(500, this);
        timer.start();
    }

    private void generatePastelPalette() {
        Random rand = new Random();
        for (int i = 0; i < 7; i++) {
            float hue = rand.nextFloat();
            float sat = 0.4f + rand.nextFloat() * 0.2f;
            float bright = 0.85f + rand.nextFloat() * 0.15f;
            palette[i] = Color.getHSBColor(hue, sat, bright);
        }
    }

    private void recolorBoardForNewLevel() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] != null) {
                    board[r][c] = palette[(r + c) % 7];
                }
            }
        }
    }

    private void refillQueue() {
        Tetromino[] pieces = Tetromino.values();
        while (nextQueue.size() < 5) {
            nextQueue.add(pieces[(int)(Math.random() * pieces.length)]);
        }
    }

    private void spawnPiece() {
        currentPiece = nextQueue.poll();
        refillQueue();

        rotation = 0;
        piecePos = new Point(COLS / 2 - 1, 0);
        holdUsedThisTurn = false;

        if (!validMove(piecePos.x, piecePos.y, rotation)) {
            // ⭐ REAL GAME OVER POPUP RESTORED
            timer.stop();
            JOptionPane.showMessageDialog(this,
                "Game Over\nScore: " + score + "\nLevel: " + level);
            System.exit(0);
        }
    }

    private int[][] getShape() {
        return currentPiece.shapes[rotation];
    }

    private boolean validMove(int newX, int newY, int newRot) {
        int[][] shape = currentPiece.shapes[newRot];

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    int x = newX + c;
                    int y = newY + r;

                    if (x < 0 || x >= COLS || y < 0 || y >= ROWS)
                        return false;
                    if (board[y][x] != null)
                        return false;
                }
            }
        }
        return true;
    }

    private void rotatePiece() {
        int newRot = (rotation + 1) % currentPiece.shapes.length;
        if (validMove(piecePos.x, piecePos.y, newRot)) {
            rotation = newRot;
            repaint();
        }
    }

    private void lockPiece() {
        int[][] shape = getShape();
        Color color = palette[currentPiece.ordinal()];

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    board[piecePos.y + r][piecePos.x + c] = color;
                }
            }
        }
        clearLines();
        spawnPiece();
    }

    private void clearLines() {
        int linesCleared = 0;

        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                linesCleared++;
                for (int rr = r; rr > 0; rr--) {
                    board[rr] = board[rr - 1].clone();
                }
                board[0] = new Color[COLS];
                r++;
            }
        }

        if (linesCleared > 0) {
            combo++;
            if (combo < 0) combo = 0;

            totalLinesCleared += linesCleared;

            int baseScore = 0;
            switch (linesCleared) {
                case 1: baseScore = 100 * level; break;
                case 2: baseScore = 300 * level; break;
                case 3: baseScore = 500 * level; break;
                case 4:
                    baseScore = 800 * level * 500;
                    break;
            }

            int comboBonus = combo * 50 * level;

            if (combo == 2) {
                score += 500_000;
            }

            score += baseScore + comboBonus;

            if (totalLinesCleared >= level * 10) {
                level++;

                int newDelay = Math.max(100, 500 - (level - 1) * 40);
                timer.setDelay(newDelay);

                generatePastelPalette();
                recolorBoardForNewLevel();
            }
        } else {
            combo = -1;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused) moveDown();
    }

    private void moveDown() {
        if (validMove(piecePos.x, piecePos.y + 1, rotation)) {
            piecePos.y++;
        } else {
            lockPiece();
        }
        repaint();
    }

    private void hardDrop() {
        while (validMove(piecePos.x, piecePos.y + 1, rotation)) {
            piecePos.y++;
        }
        lockPiece();
        repaint();
    }

    private void hold() {
        if (holdUsedThisTurn) return;

        if (holdPiece == null) {
            holdPiece = currentPiece;
            spawnPiece();
        } else {
            Tetromino temp = currentPiece;
            currentPiece = holdPiece;
            holdPiece = temp;
            rotation = 0;
            piecePos = new Point(COLS / 2 - 1, 0);
        }

        holdUsedThisTurn = true;
        repaint();
    }

    private Point getGhostPosition() {
        int ghostY = piecePos.y;
        while (validMove(piecePos.x, ghostY + 1, rotation)) {
            ghostY++;
        }
        return new Point(piecePos.x, ghostY);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // ⭐ WHITE BORDER
        g.setColor(Color.white);
        g.drawRect(0, 0, COLS * BLOCK, ROWS * BLOCK);

        if (paused) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("PAUSED", 40, getHeight() / 2);
        }

        // Ghost piece
        if (!paused) {
            Point ghost = getGhostPosition();
            g.setColor(new Color(255, 255, 255, 60));
            int[][] ghostShape = getShape();
            for (int r = 0; r < ghostShape.length; r++) {
                for (int c = 0; c < ghostShape[r].length; c++) {
                    if (ghostShape[r][c] == 1) {
                        g.fillRect((ghost.x + c) * BLOCK, (ghost.y + r) * BLOCK, BLOCK, BLOCK);
                    }
                }
            }
        }

        // Board
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] != null) {
                    g.setColor(board[r][c]);
                    g.fillRect(c * BLOCK, r * BLOCK, BLOCK, BLOCK);
                }
            }
        }

        // Current piece
        if (!paused) {
            g.setColor(palette[currentPiece.ordinal()]);
            int[][] shape = getShape();
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] == 1) {
                        g.fillRect((piecePos.x + c) * BLOCK, (piecePos.y + r) * BLOCK, BLOCK, BLOCK);
                    }
                }
            }
        }

        // UI
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, COLS * BLOCK + 20, 40);
        g.drawString("Level: " + level, COLS * BLOCK + 20, 70);
        g.drawString("Combo: " + Math.max(combo, 0), COLS * BLOCK + 20, 100);

        g.drawString("HOLD:", COLS * BLOCK + 20, 140);
        if (holdPiece != null) drawMiniPiece(g, holdPiece, COLS * BLOCK + 20, 160);

        g.drawString("NEXT:", COLS * BLOCK + 20, 260);
        int offset = 280;
        for (Tetromino t : nextQueue) {
            drawMiniPiece(g, t, COLS * BLOCK + 20, offset);
            offset += 80;
        }
    }

    private void drawMiniPiece(Graphics g, Tetromino t, int x, int y) {
        g.setColor(palette[t.ordinal()]);
        int[][] shape = t.shapes[0];
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    g.fillRect(x + c * 20, y + r * 20, 20, 20);
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

        int code = e.getKeyCode();

        // ⭐ ESC toggles pause
        if (code == KeyEvent.VK_ESCAPE) {
            paused = !paused;
            repaint();
            return;
        }

        if (paused) return;

        if (code == KeyEvent.VK_LEFT) {
            if (validMove(piecePos.x - 1, piecePos.y, rotation))
                piecePos.x--;
        }
        if (code == KeyEvent.VK_RIGHT) {
            if (validMove(piecePos.x + 1, piecePos.y, rotation))
                piecePos.x++;
        }
        if (code == KeyEvent.VK_DOWN) {
            moveDown();
        }
        if (code == KeyEvent.VK_UP) {
            rotatePiece();
        }
        if (code == KeyEvent.VK_SPACE) {
            hardDrop();
        }
        if (code == KeyEvent.VK_C) {
            hold();
        }

        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris");
        Tetris game = new Tetris();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
