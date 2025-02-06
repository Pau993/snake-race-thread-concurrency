package snakepackage;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Random;
import enums.Direction;
import enums.GridSize;

public class Snake extends Observable implements Runnable {

    private int idt;
    private Cell head;
    private Cell newCell;
    private LinkedList<Cell> snakeBody = new LinkedList<Cell>();
    private Cell start = null;
    private boolean snakeEnd = false;
    private int direction = Direction.NO_DIRECTION;
    private final int INIT_SIZE = 3;
    private boolean hasTurbo = false;
    private int jumps = 0;
    private boolean isSelected = false;
    private int growing = 0;
    public boolean goal = false;
    private boolean isPaused = false;

    public Snake(int idt, Cell head, int direction) {
        this.idt = idt;
        this.direction = direction;
        generateSnake(head);
    }

    public boolean isSnakeEnd() {
        return snakeEnd;
    }

    private void generateSnake(Cell head) {
        start = head;
        snakeBody.add(head);
        growing = INIT_SIZE - 1;
    }

    @Override
    public void run() {
        while (!snakeEnd) {
            synchronized (this) {
                while (isPaused) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            snakeCalc();
            // NOTIFY CHANGES TO GUI
            setChanged();
            notifyObservers();

            try {
                synchronized (this) {
                    if (hasTurbo) {
                        wait(500 / 3);
                    } else {
                        wait(500);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace(); // Si ocurre una interrupción, imprime el stack trace de la excepción
            }
        }

        fixDirection(head);
    }

    public synchronized void pause() {
        isPaused = true;
    }

    public synchronized void resume() {
        isPaused = false;
        notify();
    }

    private void snakeCalc() {
        synchronized (snakeBody) {
            head = snakeBody.peekFirst();
            newCell = head;
            newCell = changeDirection(newCell);
            randomMovement(newCell);
            checkIfFood(newCell);
            checkIfJumpPad(newCell);
            checkIfTurboBoost(newCell);
            checkIfBarrier(newCell);
            snakeBody.push(newCell);

            if (growing <= 0) {
                newCell = snakeBody.peekLast();
                snakeBody.remove(snakeBody.peekLast());
                Board.gameboard[newCell.getX()][newCell.getY()].freeCell();
            } else if (growing != 0) {
                growing--;
            }
        }
    }

    private void checkIfBarrier(Cell newCell) {
        if (Board.gameboard[newCell.getX()][newCell.getY()].isBarrier()) {
            System.out.println("[" + idt + "] " + "CRASHED AGAINST BARRIER " + newCell.toString());
            snakeEnd = true;
        }
    }

    private Cell fixDirection(Cell newCell) {
        if (direction == Direction.LEFT && head.getX() + 1 < GridSize.GRID_WIDTH) {
            newCell = Board.gameboard[head.getX() + 1][head.getY()];
        } else if (direction == Direction.RIGHT && head.getX() - 1 >= 0) {
            newCell = Board.gameboard[head.getX() - 1][head.getY()];
        } else if (direction == Direction.UP && head.getY() + 1 < GridSize.GRID_HEIGHT) {
            newCell = Board.gameboard[head.getX()][head.getY() + 1];
        } else if (direction == Direction.DOWN && head.getY() - 1 >= 0) {
            newCell = Board.gameboard[head.getX()][head.getY() - 1];
        }

        randomMovement(newCell);
        return newCell;
    }

    private boolean checkIfOwnBody(Cell newCell) {
        synchronized (snakeBody) {
            for (Cell c : snakeBody) {
                if (newCell.getX() == c.getX() && newCell.getY() == c.getY()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void randomMovement(Cell newCell) {
        Random random = new Random();
        int tmp = random.nextInt(4) + 1;
        if (tmp == Direction.LEFT && !(direction == Direction.RIGHT)) {
            direction = tmp;
        } else if (tmp == Direction.UP && !(direction == Direction.DOWN)) {
            direction = tmp;
        } else if (tmp == Direction.DOWN && !(direction == Direction.UP)) {
            direction = tmp;
        } else if (tmp == Direction.RIGHT && !(direction == Direction.LEFT)) {
            direction = tmp;
        }
    }

    private void checkIfTurboBoost(Cell newCell) {
        if (Board.gameboard[newCell.getX()][newCell.getY()].isTurbo_boost()) {
            for (int i = 0; i != Board.NR_TURBO_BOOSTS; i++) {
                if (Board.turbo_boosts[i] == newCell) {
                    Board.turbo_boosts[i].setTurbo_boost(false);
                    Board.turbo_boosts[i] = new Cell(-5, -5);
                    synchronized (this) {
                        hasTurbo = true;
                    }
                }
            }
            System.out.println("[" + idt + "] " + "GETTING TURBO BOOST " + newCell.toString());
        }
    }

    private void checkIfJumpPad(Cell newCell) {
        if (Board.gameboard[newCell.getX()][newCell.getY()].isJump_pad()) {
            for (int i = 0; i != Board.NR_JUMP_PADS; i++) {
                if (Board.jump_pads[i] == newCell) {
                    Board.jump_pads[i].setJump_pad(false);
                    Board.jump_pads[i] = new Cell(-5, -5);
                    this.jumps++;
                }
            }
            System.out.println("[" + idt + "] " + "GETTING JUMP PAD " + newCell.toString());
        }
    }

    private void checkIfFood(Cell newCell) {
        Random random = new Random();
        if (Board.gameboard[newCell.getX()][newCell.getY()].isFood()) {
            growing += 3;
            int x = random.nextInt(GridSize.GRID_HEIGHT);
            int y = random.nextInt(GridSize.GRID_WIDTH);
            System.out.println("[" + idt + "] " + "EATING " + newCell.toString());
            for (int i = 0; i != Board.NR_FOOD; i++) {
                if (Board.food[i].getX() == newCell.getX() && Board.food[i].getY() == newCell.getY()) {
                    Board.gameboard[Board.food[i].getX()][Board.food[i].getY()].setFood(false);
                    while (Board.gameboard[x][y].hasElements()) {
                        x = random.nextInt(GridSize.GRID_HEIGHT);
                        y = random.nextInt(GridSize.GRID_WIDTH);
                    }
                    Board.food[i] = new Cell(x, y);
                    Board.gameboard[x][y].setFood(true);
                }
            }
        }
    }

    private Cell changeDirection(Cell newCell) {
        while (direction == Direction.UP && (newCell.getY() - 1) < 0) {
            if ((head.getX() - 1) < 0) {
                this.direction = Direction.RIGHT;
            } else if ((head.getX() + 1) == GridSize.GRID_WIDTH) {
                this.direction = Direction.LEFT;
            } else {
                randomMovement(newCell);
            }
        }
        while (direction == Direction.DOWN && (head.getY() + 1) == GridSize.GRID_HEIGHT) {
            if ((head.getX() - 1) < 0) {
                this.direction = Direction.RIGHT;
            } else if ((head.getX() + 1) == GridSize.GRID_WIDTH) {
                this.direction = Direction.LEFT;
            } else {
                randomMovement(newCell);
            }
        }
        while (direction == Direction.LEFT && (head.getX() - 1) < 0) {
            if ((newCell.getY() - 1) < 0) {
                this.direction = Direction.DOWN;
            } else if ((head.getY() + 1) == GridSize.GRID_HEIGHT) {
                this.direction = Direction.UP;
            } else {
                randomMovement(newCell);
            }
        }
        while (direction == Direction.RIGHT && (head.getX() + 1) == GridSize.GRID_WIDTH) {
            if ((newCell.getY() - 1) < 0) {
                this.direction = Direction.DOWN;
            } else if ((head.getY() + 1) == GridSize.GRID_HEIGHT) {
                this.direction = Direction.UP;
            } else {
                randomMovement(newCell);
            }
        }

        switch (direction) {
            case Direction.UP:
                newCell = Board.gameboard[head.getX()][head.getY() - 1];
                break;
            case Direction.DOWN:
                newCell = Board.gameboard[head.getX()][head.getY() + 1];
                break;
            case Direction.LEFT:
                newCell = Board.gameboard[head.getX() - 1][head.getY()];
                break;
            case Direction.RIGHT:
                newCell = Board.gameboard[head.getX() + 1][head.getY()];
                break;
        }
        return newCell;
    }

    public LinkedList<Cell> getBody() {
        return this.snakeBody;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public int getIdt() {
        return idt;
    }
}