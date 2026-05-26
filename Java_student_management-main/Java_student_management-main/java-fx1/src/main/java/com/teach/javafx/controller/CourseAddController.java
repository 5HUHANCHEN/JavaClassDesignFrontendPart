package com.teach.javafx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.Map;

public class CourseAddController {
    @FXML
    private Label dialogTitleLabel;
    @FXML
    private TextField courseNameField;
    @FXML
    private ComboBox<String> dayOfWeekComboBox;
    @FXML
    private ComboBox<String> timeSlotComboBox;
    @FXML
    private TextField startWeekField;
    @FXML
    private TextField stopWeekField;

    private CourseScheduleController courseScheduleController;
    private Integer editingScheduleId;

    private final String[] dayOptions = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private final String[] timeSlotOptions = {"8:00-9:50", "10:10-12:00", "14:00-15:50", "16:10-18:00", "19:00-20:50"};

    @FXML
    public void initialize() {
        ObservableList<String> dayList = FXCollections.observableArrayList(dayOptions);
        dayOfWeekComboBox.setItems(dayList);
        ObservableList<String> timeList = FXCollections.observableArrayList(timeSlotOptions);
        timeSlotComboBox.setItems(timeList);
        setDialogTitle("新增课表课程");
    }

    @FXML
    public void okButtonClick() {
        String courseName = courseNameField.getText();
        if (courseName == null || courseName.trim().isEmpty()) {
            showWarning("请输入课程名称。");
            return;
        }
        String dayOfWeek = dayOfWeekComboBox.getValue();
        if (dayOfWeek == null) {
            showWarning("请选择上课星期。");
            return;
        }
        String timeSlot = timeSlotComboBox.getValue();
        if (timeSlot == null) {
            showWarning("请选择上课时间段。");
            return;
        }

        int startWeek;
        int stopWeek;
        try {
            startWeek = Integer.parseInt(startWeekField.getText().trim());
            stopWeek = Integer.parseInt(stopWeekField.getText().trim());
        } catch (Exception exception) {
            showWarning("开始周和结束周必须填写整数。");
            return;
        }

        if (startWeek < 1) {
            showWarning("开始周必须大于或等于 1。");
            return;
        }
        if (stopWeek < startWeek) {
            showWarning("结束周不能小于开始周。");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", editingScheduleId);
        data.put("name", courseName.trim());
        data.put("dayOfWeek", getDayNumber(dayOfWeek));
        data.put("startTime", timeSlot.split("-")[0].trim());
        data.put("startWeek", startWeek);
        data.put("stopWeek", stopWeek);
        courseScheduleController.doClose("ok", data);
    }

    @FXML
    public void cancelButtonClick() {
        courseScheduleController.doClose("cancel", null);
    }

    public void setCourseScheduleController(CourseScheduleController courseScheduleController) {
        this.courseScheduleController = courseScheduleController;
    }

    public void clearForm() {
        editingScheduleId = null;
        setDialogTitle("新增课表课程");
        courseNameField.setText("");
        dayOfWeekComboBox.getSelectionModel().clearSelection();
        timeSlotComboBox.getSelectionModel().clearSelection();
        startWeekField.setText("");
        stopWeekField.setText("");
    }

    public void fillForm(Map<String, Object> data) {
        editingScheduleId = getInteger(data, "id");
        setDialogTitle("编辑课表课程");
        courseNameField.setText(getString(data, "name"));
        dayOfWeekComboBox.getSelectionModel().select(getDayText(getInteger(data, "dayOfWeek")));
        timeSlotComboBox.getSelectionModel().select(getTimeSlotText(getString(data, "startTime")));
        startWeekField.setText(String.valueOf(getInteger(data, "startWeek")));
        stopWeekField.setText(String.valueOf(getInteger(data, "stopWeek")));
    }

    private void setDialogTitle(String title) {
        if (dialogTitleLabel != null) {
            dialogTitleLabel.setText(title);
        }
    }

    private int getDayNumber(String dayOfWeek) {
        return switch (dayOfWeek) {
            case "星期一" -> 1;
            case "星期二" -> 2;
            case "星期三" -> 3;
            case "星期四" -> 4;
            case "星期五" -> 5;
            case "星期六" -> 6;
            case "星期日" -> 7;
            default -> 1;
        };
    }

    private String getDayText(Integer dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            return null;
        }
        return dayOptions[dayOfWeek - 1];
    }

    private String getTimeSlotText(String startTime) {
        if (startTime == null || startTime.isBlank()) {
            return null;
        }
        for (String option : timeSlotOptions) {
            if (option.startsWith(startTime)) {
                return option;
            }
        }
        return null;
    }

    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(value.toString());
        } catch (Exception exception) {
            return null;
        }
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : value.toString();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
