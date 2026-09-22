package com.xwintop.xJavaFxTool.game;

import java.util.Random;

/**
 * “猜数字”的核心规则。目标数字在 1 到 100 之间（包含边界）。
 */
public class GuessNumberGame {

    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 100;

    private final Random random;
    private int targetNumber;
    private int attempts;
    private boolean finished;

    public GuessNumberGame() {
        this(new Random());
    }

    GuessNumberGame(Random random) {
        this.random = random;
        reset();
    }

    /**
     * 提交一次猜测。
     *
     * @throws IllegalArgumentException 数字超出游戏范围时抛出
     * @throws IllegalStateException 游戏已经结束时抛出
     */
    public GuessResult guess(int number) {
        if (finished) {
            throw new IllegalStateException("游戏已经结束，请先重新开始");
        }
        if (number < MIN_NUMBER || number > MAX_NUMBER) {
            throw new IllegalArgumentException("请输入 1 到 100 之间的整数");
        }

        attempts++;
        if (number < targetNumber) {
            return GuessResult.TOO_LOW;
        }
        if (number > targetNumber) {
            return GuessResult.TOO_HIGH;
        }

        finished = true;
        return GuessResult.CORRECT;
    }

    public void reset() {
        targetNumber = random.nextInt(MAX_NUMBER - MIN_NUMBER + 1) + MIN_NUMBER;
        attempts = 0;
        finished = false;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isFinished() {
        return finished;
    }

    public enum GuessResult {
        TOO_LOW,
        TOO_HIGH,
        CORRECT
    }
}
