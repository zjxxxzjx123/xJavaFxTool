package com.xwintop.xJavaFxTool.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/** Main, offline-first workspace for creating an interview record. */
public class EnforcementController implements Initializable {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分");

    @FXML private VBox questionList;
    @FXML private TextArea documentEditor;
    @FXML private TextArea clipboard;
    @FXML private Label saveState;
    @FXML private Label questionCount;
    @FXML private Label activeCase;
    @FXML private TextField location;
    @FXML private TextField interviewer;
    @FXML private TextField recorder;
    @FXML private ComboBox<String> recordType;
    @FXML private Spinner<Integer> recordNumber;

    private final List<QuestionRow> rows = new ArrayList<>();
    private boolean synchronizing;

    public static FXMLLoader getFXMLLoader() {
        return new FXMLLoader(EnforcementController.class.getResource(
            "/com/xwintop/xJavaFxTool/fxmlView/Enforcement.fxml"));
    }

    @Override
    public void initialize(URL locationUrl, ResourceBundle resources) {
        recordType.getItems().setAll("询问笔录", "讯问笔录");
        recordType.getSelectionModel().selectFirst();
        recordNumber.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        addQuestion("我们是海州市公安局的工作人员（出示警察证），今天依法对你进行询问。你明白了吗？", "我明白。", false);
        addQuestion("介绍一下你的个人情况？", "我叫张某，男，1998年01月01日出生，居民身份证号码：320***********1234。", false);
        addQuestion("请你把事情经过详细说一下。", "", false);
        wireHeader();
        rebuildDocument();
        Platform.runLater(() -> rows.get(2).answer.requestFocus());
    }

    private void wireHeader() {
        location.textProperty().addListener((o, a, b) -> rebuildDocument());
        interviewer.textProperty().addListener((o, a, b) -> rebuildDocument());
        recorder.textProperty().addListener((o, a, b) -> rebuildDocument());
        recordType.valueProperty().addListener((o, a, b) -> rebuildDocument());
        recordNumber.valueProperty().addListener((o, a, b) -> rebuildDocument());
        documentEditor.textProperty().addListener((o, oldText, newText) -> {
            if (!synchronizing) parseDocument(newText);
        });
        documentEditor.setOnKeyPressed(event -> {
            if (event.getCode() != KeyCode.ENTER || event.isShiftDown()) return;
            int caret = documentEditor.getCaretPosition();
            String before = documentEditor.getText(0, caret);
            int lineStart = before.lastIndexOf('\n') + 1;
            String currentLine = before.substring(lineStart);
            if (currentLine.startsWith("问：") || currentLine.startsWith("答：")) {
                documentEditor.insertText(caret, currentLine.startsWith("问：") ? "\n答：" : "\n问：");
                event.consume();
            }
        });
    }

    @FXML private void addQuestionAction() { addQuestion("", "", true); }

    private void addQuestion(String question, String answer, boolean focus) {
        QuestionRow row = new QuestionRow(question, answer);
        rows.add(row);
        questionList.getChildren().add(row.root);
        updateCount();
        rebuildDocument();
        if (focus) Platform.runLater(row.question::requestFocus);
    }

    private void remove(QuestionRow row) {
        if (rows.size() == 1) return;
        rows.remove(row);
        questionList.getChildren().remove(row.root);
        updateCount();
        rebuildDocument();
    }

    private void move(QuestionRow row, int delta) {
        int from = rows.indexOf(row), to = from + delta;
        if (to < 0 || to >= rows.size()) return;
        rows.remove(from);
        rows.add(to, row);
        questionList.getChildren().setAll(rows.stream().map(r -> r.root).toArray(javafx.scene.Node[]::new));
        rebuildDocument();
    }

    private void rebuildDocument() {
        if (synchronizing || documentEditor == null) return;
        synchronizing = true;
        int caret = documentEditor.getCaretPosition();
        String title = "讯问笔录".equals(recordType.getValue()) ? "询问  讯问笔录" : "询问笔录  讯问";
        StringBuilder text = new StringBuilder();
        text.append("                         ").append(title).append("                         第 ")
            .append(recordNumber.getValue()).append(" 次\n\n")
            .append("时间：").append(TIME.format(LocalDateTime.now())).append(" 至 保存时自动记录\n")
            .append("地点：").append(location.getText()).append("\n")
            .append("询问/讯问人（签名）：").append(interviewer.getText()).append("、________    工作单位：海州市公安局\n")
            .append("记录人（签名）：").append(recorder.getText()).append("              工作单位：____________\n")
            .append("被询问/讯问人：张某    性别：男    出生日期：1998年01月01日\n")
            .append("身份证号码：320***********1234    联系电话：138****0000\n")
            .append("现住址：海州市滨河区长安路18号\n\n");
        for (QuestionRow row : rows) {
            text.append("问：").append(row.question.getText()).append("\n")
                .append("答：").append(row.answer.getText()).append("\n");
        }
        documentEditor.setText(text.toString());
        documentEditor.positionCaret(Math.min(caret, documentEditor.getLength()));
        synchronizing = false;
        saveState.setText("● 有未保存的修改");
    }

    /** Extracts Q/A paragraphs changed in the full-document editor back into the structured list. */
    private void parseDocument(String text) {
        List<String[]> parsed = new ArrayList<>();
        String[] lines = text.split("\\R", -1);
        String question = null;
        for (String line : lines) {
            if (line.startsWith("问：")) question = line.substring(2);
            else if (line.startsWith("答：") && question != null) {
                parsed.add(new String[]{question, line.substring(2)});
                question = null;
            }
        }
        if (parsed.isEmpty()) return;
        synchronizing = true;
        while (rows.size() < parsed.size()) addQuestion("", "", false);
        while (rows.size() > parsed.size()) {
            QuestionRow last = rows.remove(rows.size() - 1);
            questionList.getChildren().remove(last.root);
        }
        for (int i = 0; i < parsed.size(); i++) {
            rows.get(i).question.setText(parsed.get(i)[0]);
            rows.get(i).answer.setText(parsed.get(i)[1]);
        }
        synchronizing = false;
        updateCount();
        saveState.setText("● 有未保存的修改");
    }

    @FXML private void saveAction() {
        saveState.setText("✓ 已保存  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    @FXML private void insertClipboard() {
        if (clipboard.getText().isEmpty()) return;
        if (documentEditor.isFocused()) documentEditor.insertText(documentEditor.getCaretPosition(), clipboard.getText());
        else {
            TextArea target = rows.stream().filter(r -> r.question.isFocused() || r.answer.isFocused()).findFirst()
            .map(r -> r.answer.isFocused() ? r.answer : r.question)
            .orElse(rows.get(rows.size() - 1).answer);
            target.insertText(target.getCaretPosition(), clipboard.getText());
        }
    }

    @FXML private void clearClipboard() { clipboard.clear(); }

    @FXML private void deleteCase() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除案件");
        alert.setHeaderText("确认删除案件  海公（刑）立字〔2026〕018号？");
        alert.setContentText("案件“网络诈骗案”将从案件中心移除，案件文件会移至 backup/已删除案件。此操作不会立即永久删除文件。");
        alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> activeCase.setText("未选择案件"));
    }

    private void updateCount() { questionCount.setText("共 " + rows.size() + " 组问答"); }

    private final class QuestionRow {
        final VBox root = new VBox(4);
        final TextArea question = editor("问", "请输入问题…");
        final TextArea answer = editor("答", "请输入回答…");

        QuestionRow(String q, String a) {
            question.setText(q); answer.setText(a);
            Label index = new Label("问答 " + (rows.size() + 1));
            index.getStyleClass().add("qa-index");
            Button up = small("↑", () -> move(this, -1));
            Button down = small("↓", () -> move(this, 1));
            Button delete = small("删除", () -> remove(this));
            javafx.scene.layout.HBox tools = new javafx.scene.layout.HBox(6, index, spacer(), up, down, delete);
            tools.getStyleClass().add("qa-tools");
            root.getChildren().addAll(tools, question, answer);
            root.getStyleClass().add("qa-row");
        }

        private TextArea editor(String mark, String prompt) {
            TextArea area = new TextArea();
            area.setPromptText(mark + "：" + prompt);
            area.setWrapText(true);
            area.getStyleClass().addAll("qa-editor", "qa-" + mark);
            area.textProperty().addListener((o, oldValue, value) -> {
                int lines = Math.max(1, value.split("\\R", -1).length + value.length() / 35);
                area.setPrefRowCount(Math.min(8, lines));
                area.setPrefHeight(Math.min(190, 38 + Math.max(0, lines - 1) * 24));
                rebuildDocument();
            });
            return area;
        }

        private Button small(String text, Runnable action) {
            Button b = new Button(text); b.getStyleClass().add("flat-button"); b.setOnAction(e -> action.run()); return b;
        }
    }

    private static javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(r, Priority.ALWAYS); return r;
    }
}
