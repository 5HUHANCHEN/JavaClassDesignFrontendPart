package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.util.CommonMethod;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DivinationController extends ToolController {
    @FXML private TextField plumQuestionField;
    @FXML private TextArea plumBackgroundArea;
    @FXML private ComboBox<String> plumMethodComboBox;
    @FXML private TextField plumInputField;
    @FXML private CheckBox plumStudyCheckBox;
    @FXML private TextArea plumReportArea;
    @FXML private Label plumSummaryLabel;

    @FXML private TextField tarotQuestionField;
    @FXML private TextArea tarotBackgroundArea;
    @FXML private ComboBox<String> tarotSpreadComboBox;
    @FXML private TextArea tarotReportArea;
    @FXML private Label tarotSummaryLabel;

    @FXML private ListView<Map<String, Object>> historyListView;
    @FXML private TextArea historyReportArea;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        plumMethodComboBox.getItems().setAll("当前时间起卦", "数字起卦", "文字起卦");
        plumMethodComboBox.getSelectionModel().select(0);
        tarotSpreadComboBox.getItems().setAll("单张牌", "三张牌", "四元素牌阵", "十字牌阵");
        tarotSpreadComboBox.getSelectionModel().select("三张牌");
        plumQuestionField.setText("周易课期末展示应该选择什么方向？");
        plumBackgroundArea.setText("我正在选修周易课，想把梅花易数和系统功能结合起来展示。");
        tarotQuestionField.setText("我接下来应该怎样准备这个功能展示？");
        tarotBackgroundArea.setText("我希望这个模块既有文化学习价值，也能在系统中实际使用。");
        configureHistoryList();
        loadHistory();
    }

    @Override
    public void doRefresh() {
        loadHistory();
    }

    @FXML
    private void onGeneratePlumClick() {
        DataRequest request = new DataRequest();
        request.add("question", plumQuestionField.getText());
        request.add("background", plumBackgroundArea.getText());
        request.add("method", plumMethodComboBox.getValue());
        request.add("input", plumInputField.getText());
        request.add("includeStudy", plumStudyCheckBox.isSelected());
        requestDataAsync("/api/divination/plumBlossom", request, data -> {
            plumSummaryLabel.setText("本卦：" + CommonMethod.getString(data, "baseHexagram")
                    + "  互卦：" + CommonMethod.getString(data, "mutualHexagram")
                    + "  变卦：" + CommonMethod.getString(data, "changedHexagram")
                    + "  动爻：" + CommonMethod.getString(data, "movingLine"));
            plumReportArea.setText(CommonMethod.getString(data, "reportText"));
            loadHistory();
        });
    }

    @FXML
    private void onGenerateTarotClick() {
        DataRequest request = new DataRequest();
        request.add("question", tarotQuestionField.getText());
        request.add("background", tarotBackgroundArea.getText());
        request.add("spread", tarotSpreadComboBox.getValue());
        requestDataAsync("/api/divination/tarot", request, data -> {
            tarotSummaryLabel.setText("牌阵：" + CommonMethod.getString(data, "spread") + "  报告已生成");
            tarotReportArea.setText(CommonMethod.getString(data, "reportText"));
            loadHistory();
        });
    }

    @FXML
    private void onHistoryRefreshClick() {
        loadHistory();
    }

    @FXML
    private void onHistoryOpenClick() {
        Map<String, Object> item = historyListView.getSelectionModel().getSelectedItem();
        if (item == null) {
            MessageDialog.showDialog("请先选择一条历史报告。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("recordId", CommonMethod.getInteger(item, "recordId"));
        requestDataAsync("/api/divination/getHistoryDetail", request, data -> historyReportArea.setText(CommonMethod.getString(data, "reportText")));
    }

    @FXML
    private void onHistoryDeleteClick() {
        Map<String, Object> item = historyListView.getSelectionModel().getSelectedItem();
        if (item == null) {
            MessageDialog.showDialog("请先选择一条历史报告。");
            return;
        }
        if (MessageDialog.choiceDialog("确认删除当前历史报告吗？") != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("recordId", CommonMethod.getInteger(item, "recordId"));
        requestDataAsync("/api/divination/deleteHistory", request, data -> {
            historyReportArea.clear();
            loadHistory();
        });
    }

    private void loadHistory() {
        requestDataAsync("/api/divination/getHistoryList", new DataRequest(), data -> {
            List<Map<String, Object>> list = asMapList(data.get("_list"));
            historyListView.getItems().setAll(list);
            updateStatus("已加载 " + list.size() + " 条历史报告");
        });
    }

    private void configureHistoryList() {
        historyListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(CommonMethod.getString(item, "typeName") + "｜"
                        + CommonMethod.getString(item, "question") + "\n"
                        + CommonMethod.getString(item, "method") + "  "
                        + CommonMethod.getString(item, "createTime"));
            }
        });
        historyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onHistoryOpenClick();
            }
        });
    }

    private void requestDataAsync(String url, DataRequest request, Consumer<Map<String, Object>> consumer) {
        updateStatus("正在生成或读取报告...");
        CompletableFuture.supplyAsync(() -> requestData(url, request))
                .exceptionally(throwable -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("_status", "请求失败：" + throwable.getMessage());
                    return data;
                })
                .thenAccept(data -> Platform.runLater(() -> {
                    String status = CommonMethod.getString(data, "_status");
                    if (!status.isBlank()) {
                        updateStatus(status);
                    }
                    if (data.size() > 1) {
                        consumer.accept(data);
                    }
                }));
    }

    private Map<String, Object> requestData(String url, DataRequest request) {
        DataResponse response = HttpRequestUtil.request(url, request);
        if (response != null && response.getCode() != null && response.getCode() == 0) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("_ok", true);
            if (response.getData() instanceof List<?>) {
                data.put("_list", response.getData());
            } else {
                data.putAll(asMap(response.getData()));
            }
            data.put("_status", response.getMsg() == null || response.getMsg().isBlank() ? "请求完成" : response.getMsg());
            return data;
        }
        String message = response == null ? "请求失败，请检查后端是否启动。" : response.getMsg();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_status", message == null || message.isBlank() ? "请求失败" : message);
        return data;
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;
            return map;
        }
        return Map.of();
    }

    private List<Map<String, Object>> asMapList(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) rawMap;
                    result.add(map);
                }
            }
        }
        return result;
    }
}
