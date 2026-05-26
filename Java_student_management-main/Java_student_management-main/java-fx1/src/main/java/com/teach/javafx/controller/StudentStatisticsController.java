package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.MapValueFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public class StudentStatisticsController {
    @FXML private TableView<Map> dataTableView;
    @FXML private TableColumn<Map, String> studentNumColumn;
    @FXML private TableColumn<Map, String> studentNameColumn;
    @FXML private TableColumn<Map, String> courseCountColumn;
    @FXML private TableColumn<Map, String> avgScoreColumn;
    @FXML private TableColumn<Map, String> homeworkCountColumn;
    @FXML private TableColumn<Map, String> homeworkAvgScoreColumn;
    @FXML private TableColumn<Map, String> gpaColumn;
    @FXML private TableColumn<Map, String> noColumn;
    @FXML private TableColumn<Map, String> leaveCountColumn;
    @FXML private BarChart<String, Number> averageScoreChart;
    @FXML private PieChart leaveCountChart;
    @FXML private Label summaryLabel;

    private ArrayList<Map> dataList = new ArrayList<>();
    private final ObservableList<Map> observableList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        studentNumColumn.setCellValueFactory(new MapValueFactory<>("studentNum"));
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        courseCountColumn.setCellValueFactory(new MapValueFactory<>("courseCount"));
        avgScoreColumn.setCellValueFactory(new MapValueFactory<>("avgScore"));
        homeworkCountColumn.setCellValueFactory(new MapValueFactory<>("homeworkCount"));
        homeworkAvgScoreColumn.setCellValueFactory(new MapValueFactory<>("homeworkAvgScore"));
        gpaColumn.setCellValueFactory(new MapValueFactory<>("gpa"));
        noColumn.setCellValueFactory(new MapValueFactory<>("no"));
        leaveCountColumn.setCellValueFactory(new MapValueFactory<>("leaveCount"));
        onQueryButtonClick();
    }

    private void setTableViewData() {
        observableList.clear();
        observableList.addAll(dataList);
        dataTableView.setItems(observableList);
    }

    @FXML
    protected void onQueryButtonClick() {
        DataResponse response = HttpRequestUtil.request("/api/studentStatistics/getStudentStatisticsList", new DataRequest());
        if (response != null && response.getCode() == 0) {
            dataList = (ArrayList<Map>) response.getData();
            if (dataList == null) {
                dataList = new ArrayList<>();
            }
            setTableViewData();
            refreshCharts();
        } else {
            MessageDialog.showDialog(response == null ? "学生统计数据加载失败，请检查服务器连接。" : response.getMsg());
        }
    }

    @FXML
    protected void onStatisticsButtonClick() {
        DataResponse response = HttpRequestUtil.request("/api/studentStatistics/doStudentStatistics", new DataRequest());
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("学生统计已重新计算。");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "学生统计执行失败，请检查服务器连接。" : response.getMsg());
        }
    }

    private void refreshCharts() {
        refreshAverageScoreChart();
        refreshLeaveCountChart();
        summaryLabel.setText(dataList.isEmpty()
                ? "暂无学生统计数据。"
                : "当前共显示 " + dataList.size() + " 条学生统计记录，图表已根据当前数据自动更新。");
    }

    private void refreshAverageScoreChart() {
        averageScoreChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("平均成绩");
        dataList.stream()
                .sorted(Comparator.comparingDouble(this::getAverageScoreValue).reversed())
                .limit(8)
                .forEach(item -> series.getData().add(new XYChart.Data<>(getStudentName(item), getAverageScoreValue(item))));
        averageScoreChart.getData().add(series);
    }

    private void refreshLeaveCountChart() {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        int zeroLeaveCount = 0;
        int lowLeaveCount = 0;
        int highLeaveCount = 0;
        for (Map item : dataList) {
            int leaveCount = getIntegerValue(item, "leaveCount");
            if (leaveCount <= 0) {
                zeroLeaveCount++;
            } else if (leaveCount <= 2) {
                lowLeaveCount++;
            } else {
                highLeaveCount++;
            }
        }
        pieData.add(new PieChart.Data("无请假记录", zeroLeaveCount));
        pieData.add(new PieChart.Data("请假 1-2 次", lowLeaveCount));
        pieData.add(new PieChart.Data("请假 3 次及以上", highLeaveCount));
        leaveCountChart.setData(pieData);
    }

    private double getAverageScoreValue(Map item) {
        Object value = item.get("avgScore");
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private int getIntegerValue(Map item, String key) {
        Object value = item.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (Exception ex) {
            return 0;
        }
    }

    private String getStudentName(Map item) {
        String studentName = String.valueOf(item.getOrDefault("studentName", ""));
        String studentNum = String.valueOf(item.getOrDefault("studentNum", ""));
        return studentName == null || studentName.isBlank() ? studentNum : studentName;
    }
}
