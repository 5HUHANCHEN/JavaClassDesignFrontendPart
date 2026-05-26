package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardController extends ToolController {
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane kpiPane;
    @FXML private VBox todoBox;
    @FXML private VBox recentBox;
    @FXML private PieChart homeworkPieChart;
    @FXML private PieChart leavePieChart;
    @FXML private BarChart<String, Number> scoreBarChart;
    @FXML private VBox scoreValueBox;
    @FXML private VBox homeworkValueBox;
    @FXML private VBox leaveValueBox;

    @FXML
    public void initialize() {
        loadDashboard();
    }

    @Override
    public void doRefresh() {
        loadDashboard();
    }

    @FXML
    private void onRefreshButtonClick() {
        loadDashboard();
    }

    private void loadDashboard() {
        DataResponse response = HttpRequestUtil.request("/api/dashboard/getDashboardSummary", new DataRequest());
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            updateStatus("首页数据加载失败。");
            MessageDialog.showDialog("首页数据加载失败。" + (response == null ? "" : response.getMsg()));
            return;
        }
        Map<String, Object> summary = asMap(response.getData());
        String username = AppStore.getJwt() == null ? "" : AppStore.getJwt().getUsername();
        String role = CommonMethod.getString(summary, "role");
        welcomeLabel.setText("欢迎回来，" + username);
        roleLabel.setText("当前身份：" + (role.isBlank() ? "未知" : role));
        fillKpis(asMapList(summary.get("kpiList")));
        fillTodos(asMapList(summary.get("todoList")));
        fillRecent(asMapList(summary.get("recentList")));
        fillCharts(CommonMethod.getMap(summary, "chartData"));
        updateStatus("首页数据已更新。");
    }

    private void fillKpis(List<Map<String, Object>> kpiList) {
        kpiPane.getChildren().clear();
        for (Map<String, Object> item : kpiList) {
            VBox card = new VBox(6);
            card.getStyleClass().addAll("dashboard-card", "kpi-card", "tone-" + CommonMethod.getString(item, "tone"));
            Label title = new Label(CommonMethod.getString(item, "title"));
            title.getStyleClass().add("kpi-title");
            HBox valueLine = new HBox(6);
            valueLine.setAlignment(Pos.BASELINE_LEFT);
            Label value = new Label(String.valueOf(item.getOrDefault("value", "")));
            value.getStyleClass().add("kpi-value");
            Label unit = new Label(CommonMethod.getString(item, "unit"));
            unit.getStyleClass().add("kpi-unit");
            valueLine.getChildren().addAll(value, unit);
            card.getChildren().addAll(title, valueLine);
            kpiPane.getChildren().add(card);
        }
    }

    private void fillTodos(List<Map<String, Object>> todoList) {
        todoBox.getChildren().clear();
        if (todoList.isEmpty()) {
            todoBox.getChildren().add(emptyLabel("暂无待办"));
            return;
        }
        for (Map<String, Object> item : todoList) {
            String targetPage = CommonMethod.getString(item, "targetPage");
            if (targetPage.isBlank()) {
                continue;
            }
            Button button = new Button(CommonMethod.getString(item, "title") + "  " + String.valueOf(item.getOrDefault("count", 0)));
            button.getStyleClass().add("todo-button");
            button.setMaxWidth(Double.MAX_VALUE);
            String targetTitle = CommonMethod.getString(item, "targetTitle");
            button.setOnAction(event -> {
                if (AppStore.getMainFrameController() != null) {
                    AppStore.getMainFrameController().changeContent(targetPage, targetTitle.isBlank() ? CommonMethod.getString(item, "title") : targetTitle);
                }
            });
            todoBox.getChildren().add(button);
        }
        if (todoBox.getChildren().isEmpty()) {
            todoBox.getChildren().add(emptyLabel("暂无待办"));
        }
    }

    private void fillRecent(List<Map<String, Object>> recentList) {
        recentBox.getChildren().clear();
        if (recentList.isEmpty()) {
            recentBox.getChildren().add(emptyLabel("暂无动态"));
            return;
        }
        for (Map<String, Object> item : recentList) {
            VBox row = new VBox(4);
            row.getStyleClass().add("recent-item");
            Label title = new Label(CommonMethod.getString(item, "title"));
            title.getStyleClass().add("recent-title");
            Label meta = new Label(CommonMethod.getString(item, "subtitle") + "  " + CommonMethod.getString(item, "time"));
            meta.getStyleClass().add("recent-meta");
            row.getChildren().addAll(title, meta);
            recentBox.getChildren().add(row);
        }
    }

    private void fillCharts(Map<String, Object> chartData) {
        List<Map<String, Object>> homeworkStatus = asMapList(chartData.get("homeworkStatus"));
        List<Map<String, Object>> leaveStatus = asMapList(chartData.get("leaveStatus"));
        List<Map<String, Object>> scoreData = asMapList(chartData.get("scoreAvgByCourse"));
        fillPieChart(homeworkPieChart, homeworkStatus);
        fillPieChart(leavePieChart, leaveStatus);
        fillBarChart(scoreData);
        fillValueBox(homeworkValueBox, homeworkStatus, "");
        fillValueBox(leaveValueBox, leaveStatus, "");
        fillValueBox(scoreValueBox, scoreData, " 分");
    }

    private void fillPieChart(PieChart chart, List<Map<String, Object>> dataList) {
        List<PieChart.Data> data = new ArrayList<>();
        for (Map<String, Object> item : dataList) {
            double value = toDouble(item.get("value"));
            if (value > 0) {
                data.add(new PieChart.Data(CommonMethod.getString(item, "name") + " " + formatValue(value), value));
            }
        }
        if (data.isEmpty()) {
            data.add(new PieChart.Data("暂无数据", 1));
        }
        chart.setData(FXCollections.observableArrayList(data));
        chart.setLabelsVisible(true);
    }

    private void fillBarChart(List<Map<String, Object>> dataList) {
        scoreBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("成绩");
        for (Map<String, Object> item : dataList) {
            series.getData().add(new XYChart.Data<>(CommonMethod.getString(item, "name"), toDouble(item.get("value"))));
        }
        if (series.getData().isEmpty()) {
            series.getData().add(new XYChart.Data<>("暂无数据", 0));
        }
        scoreBarChart.getData().add(series);
    }

    private void fillValueBox(VBox valueBox, List<Map<String, Object>> dataList, String suffix) {
        if (valueBox == null) {
            return;
        }
        valueBox.getChildren().clear();
        if (dataList.isEmpty()) {
            valueBox.getChildren().add(emptyLabel("暂无数值"));
            return;
        }
        for (Map<String, Object> item : dataList) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("chart-value-row");
            Label name = new Label(CommonMethod.getString(item, "name"));
            name.getStyleClass().add("chart-value-name");
            Label value = new Label(formatValue(toDouble(item.get("value"))) + suffix);
            value.getStyleClass().add("chart-value-number");
            row.getChildren().addAll(name, value);
            valueBox.getChildren().add(row);
        }
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-label");
        return label;
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

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0d;
        }
    }

    private String formatValue(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
