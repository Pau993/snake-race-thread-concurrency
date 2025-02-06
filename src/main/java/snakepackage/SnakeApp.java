package snakepackage;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JLabel;

import enums.GridSize;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JPanel;
import snakepackage.Snake;


/**
 * @author jd-
 *
 */
public class SnakeApp {

    private static SnakeApp app;
    public static final int MAX_THREADS = 8;
    Snake[] snakes = new Snake[MAX_THREADS];
    private static final Cell[] spawn = {
        new Cell(1, (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell(GridSize.GRID_WIDTH - 2,3 * (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell(3 * (GridSize.GRID_WIDTH / 2) / 2, 1),
        new Cell((GridSize.GRID_WIDTH / 2) / 2, GridSize.GRID_HEIGHT - 2),
        new Cell(1, 3 * (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell(GridSize.GRID_WIDTH - 2, (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell((GridSize.GRID_WIDTH / 2) / 2, 1),
        new Cell(3 * (GridSize.GRID_WIDTH / 2) / 2,GridSize.GRID_HEIGHT - 2)};
    private JFrame frame;
    private static Board board;
    int nr_selected = 0;
    Thread[] threads = new Thread[MAX_THREADS];
    private boolean isRunning = false;
    private JButton startButton, pauseButton, resumeButton;
    private JLabel longestSnakeLabel, worstSnakeLabel;
    private boolean pauseGame = false;

    public SnakeApp() {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        frame = new JFrame("The Snake Race");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.setSize(618, 640);
        frame.setSize(GridSize.GRID_WIDTH * GridSize.WIDTH_BOX + 17,GridSize.GRID_HEIGHT * GridSize.HEIGH_BOX + 40);
        frame.setLocation(dimension.width / 2 - frame.getWidth() / 2,dimension.height / 2 - frame.getHeight() / 2);
        
        board = new Board();
        frame.add(board, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout());
        startButton = new JButton("Iniciar");
        pauseButton = new JButton("Pausar");
        resumeButton = new JButton("Reanudar");
        longestSnakeLabel = new JLabel("Serpiente más larga: -");
        worstSnakeLabel = new JLabel("Peor serpiente: -");
        
        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> pauseGame());
        resumeButton.addActionListener(e -> resumeGame());

        actionsPanel.add(startButton);
        actionsPanel.add(pauseButton);
        actionsPanel.add(resumeButton);
        actionsPanel.add(longestSnakeLabel);
        actionsPanel.add(worstSnakeLabel);

        frame.add(actionsPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

    }

    public synchronized void startGame() {
        if (!isRunning) {
            for (int i = 0; i < MAX_THREADS; i++) {
                snakes[i] = new Snake(i + 1, spawn[i], i + 1);
                snakes[i].addObserver(board);
                threads[i] = new Thread(snakes[i]);
                threads[i].start();
            }
            isRunning = true;
        }
    }

    public synchronized void pauseGame() {
        pauseGame = true;
        for (Snake snake : snakes) {
            snake.pause();
        }
        updateSnakeStats();
    }

    public synchronized void resumeGame() {
        pauseGame = false;
        for (Snake snake : snakes) {
            snake.resume();
        }
        notifyAll();
    }

    private void updateSnakeStats() {
        Snake longest = Arrays.stream(snakes)
            .filter(s -> !s.isSnakeEnd())
            .max(Comparator.comparingInt(s -> s.getBody().size()))
            .orElse(null);
        
        Snake worst = Arrays.stream(snakes)
            .min(Comparator.comparingInt(s -> s.getBody().size()))
            .orElse(null);
        
        longestSnakeLabel.setText("Serpiente más larga: " + (longest != null ? longest.getIdt() : "-"));
        worstSnakeLabel.setText("Peor serpiente: " + (worst != null ? worst.getIdt() : "-"));
    }

    public static void main(String[] args) {
        app = new SnakeApp();
        app.init();
    }

    private void init() {
        
        
        
        for (int i = 0; i != MAX_THREADS; i++) {
            
            snakes[i] = new Snake(i + 1, spawn[i], i + 1);
            snakes[i].addObserver(board);
            threads[i] = new Thread(snakes[i]);
            threads[i].start();
        }

        frame.setVisible(true);

            
        while (true) {
            int x = 0;
            for (int i = 0; i != MAX_THREADS; i++) {
                if (snakes[i].isSnakeEnd() == true) {
                    x++;
                }
            }
            if (x == MAX_THREADS) {
                break;
            }
        }


        System.out.println("Thread (snake) status:");
        for (int i = 0; i != MAX_THREADS; i++) {
            System.out.println("["+i+"] :"+threads[i].getState());
        }
        

    }

    public static SnakeApp getApp() {
        return app;
    }

}
