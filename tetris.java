import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Tetris extends JPanel implements ActionListener, KeyListener {

    private final int ROWS = 20;
    private final int COLS = 10;
    private final int BLOCK = 30;

    private Timer timer; // Swing timer
    private Color[][] board = new Color[ROWS][COLS];

    private Point piecePos;
    private int rotation;
    private Tetromino currentPiece;

    private int score = 0; // <-- SCORE ADDED

    private enum Tetromino {
        I(new int[][][]{
                {{1,1,1,1}},
                {{1},{1},{1},{1}}
        }, Color.cyan),

        O(new int[][][]{
                {{1,1},
                 {1,1}}
        }, Color.yellow),

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
        }, Color.magenta),

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
        }, Color.orange),

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
        }, Color.blue),

        S(new int[][][]{
                {{0,1,1},
                 {1,1,0}},
                {{1,0},
                 {1,1},
                 {0,1}}
        }, Color.green),

        Z(new int[][][]{
                {{1,1,0},
                 {0,1,1}},
                {{0,1},
                 {1,1},
                 {1,0}}
        }, Color.red);

        int[][][] shapes;
        Color color;

        Tetromino(int[][][] s, Color c) {
            shapes = s;
            color = c;
        }
    }

    public Tetris() {
        setPreferredSize(new Dimension(COLS * BLOCK, ROWS * BLOCK));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        spawnPiece();

        timer = new Timer(500, this); // slow drop
        timer.start();
    }

    private void spawnPiece() {
        Tetromino[] pieces = Tetromino.values();
        currentPiece = pieces[(int)(Math.random() * pieces.length)];
        rotation = 0;
        piecePos = new Point(COLS / 2 - 1, 0);

        if (!validMove(piecePos.x, piecePos.y, rotation)) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Game Over\nScore: " + score);
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

    private void lockPiece() {
        int[][] shape = getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    board[piecePos.y + r][piecePos.x + c] = currentPiece.color;
                }
            }
        }
        clearLines();
        spawnPiece();
    }

    // ⭐ UPDATED WITH SCORING ⭐
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

        // Classic Tetris scoring
        switch (linesCleared) {
            case 1: score += 100; break;
            case 2: score += 300; break;
            case 3: score += 500; break;
            case 4: score += 800; break;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        moveDown();
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw board
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] != null) {
                    g.setColor(board[r][c]);
                    g.fillRect(c * BLOCK, r * BLOCK, BLOCK, BLOCK);
                }
            }
        }

        // Draw current piece
        g.setColor(currentPiece.color);
        int[][] shape = getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    g.fillRect((piecePos.x + c) * BLOCK, (piecePos.y + r) * BLOCK, BLOCK, BLOCK);
                }
            }
        }

        // ⭐ DRAW SCORE ⭐
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 25);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

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
            int newRot = (rotation + 1) % currentPiece.shapes.length;
            if (validMove(piecePos.x, piecePos.y, newRot))
                rotation = newRot;
        }
        if (code == KeyEvent.VK_SPACE) {
            hardDrop();
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
