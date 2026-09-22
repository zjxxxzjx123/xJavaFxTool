package com.xwintop.xJavaFxTool.controller.game;

import com.xwintop.xJavaFxTool.game.GuessNumberGame;
import com.xwintop.xJavaFxTool.game.GuessNumberGame.GuessResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 最简单的猜数字游戏界面控制器。
 */
public class GuessNumberGameController {

    private final GuessNumberGame game = new GuessNumberGame();

    @FXML
    private TextField guessField;

    @FXML
    private Label messageLabel;

    @FXML
    private Label attemptsLabel;

    @FXML
    private Button guessButton;

    @FXML
    private void initialize() {
        updateAttempts();
    }

    @FXML
    private void submitGuess() {
        String input = guessField.getText().trim();
        int number;
        try {
            number = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            showMessage("请输入一个整数", "game-message-error");
            guessField.requestFocus();
            return;
        }

        try {
            GuessResult result = game.guess(number);
            updateAttempts();
            if (result == GuessResult.TOO_LOW) {
                showMessage("太小了，再大一点！", "game-message-hint");
            } else if (result == GuessResult.TOO_HIGH) {
                showMessage("太大了，再小一点！", "game-message-hint");
            } else {
                showMessage("猜对了！你一共猜了 " + game.getAttempts() + " 次。", "game-message-success");
                guessButton.setDisable(true);
                guessField.setDisable(true);
            }
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage(), "game-message-error");
        }

        guessField.selectAll();
        guessField.requestFocus();
    }

    @FXML
    private void restartGame() {
        game.reset();
        guessField.clear();
        guessField.setDisable(false);
        guessButton.setDisable(false);
        showMessage("新游戏开始了，试试看吧！", "game-message-hint");
        updateAttempts();
        guessField.requestFocus();
    }

    private void updateAttempts() {
        attemptsLabel.setText("已猜次数：" + game.getAttempts());
    }

    private void showMessage(String message, String styleClass) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll(
            "game-message-hint", "game-message-error", "game-message-success"
        );
        messageLabel.getStyleClass().add(styleClass);
    }
}
