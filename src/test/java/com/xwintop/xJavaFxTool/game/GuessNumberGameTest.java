package com.xwintop.xJavaFxTool.game;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuessNumberGameTest {

    @Test
    public void reportsDirectionAndCountsAttempts() {
        GuessNumberGame game = new GuessNumberGame(new FixedRandom(49));

        assertEquals(GuessNumberGame.GuessResult.TOO_LOW, game.guess(25));
        assertEquals(GuessNumberGame.GuessResult.TOO_HIGH, game.guess(75));
        assertEquals(GuessNumberGame.GuessResult.CORRECT, game.guess(50));
        assertEquals(3, game.getAttempts());
        assertTrue(game.isFinished());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNumbersOutsideRange() {
        new GuessNumberGame(new FixedRandom(0)).guess(101);
    }

    @Test
    public void resetClearsGameState() {
        GuessNumberGame game = new GuessNumberGame(new FixedRandom(0));
        game.guess(1);

        game.reset();

        assertEquals(0, game.getAttempts());
        assertFalse(game.isFinished());
    }

    private static class FixedRandom extends Random {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
