package com.teach.javafx.controller;

import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class CourseScheduleController implements Initializable {
    @FXML
    private TableView<Map<String, Object>> dataTableView;
    @FXML
    private TableColumn<Map<String, Object>, String> timeColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> mondayColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> tuesdayColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> wednesdayColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> thursdayColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> fridayColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> saturdayColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> sundayColumn;
    @FXML
    private TextField weekField;
    @FXML
    private Label statusLabel;

    private final String[] timeSlots = {"8:00~9:50", "10:10~12:00", "14:00~15:50", "16:10~18:00", "19:00~20:50"};
    private final List<Map<String, Object>> courseScheduleList = new ArrayList<>();
    private final ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList();

    private CourseAddController courseAddController;
    private Stage addDialogStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumn(timeColumn, "time_slot");
        setupColumn(mondayColumn, "monday");
        setupColumn(tuesdayColumn, "tuesday");
        setupColumn(wednesdayColumn, "wednesday");
        setupColumn(thursdayColumn, "thursday");
        setupColumn(fridayColumn, "friday");
        setupColumn(saturdayColumn, "saturday");
        setupColumn(sundayColumn, "sunday");
        dataTableView.setItems(observableList);
        dataTableView.setRowFactory(tableView -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setStyle("-fx-min-height: 78px; -fx-pref-height: 78px; -fx-max-height: 78px;");
            return row;
        });
        weekField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                normalizeWeekField(true);
            }
        });
        onQueryButtonClick();
    }

    private void setupColumn(TableColumn<Map<String, Object>, String> column, String key) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(getString(cellData.getValue(), key)));
        if (!"time_slot".equals(key)) {
            column.setCellFactory(createScheduleCellFactory(key));
        }
    }

    private Callback<TableColumn<Map<String, Object>, String>, TableCell<Map<String, Object>, String>> createScheduleCellFactory(String key) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setOnMouseClicked(null);
                    return;
                }
                setText(item);
                setWrapText(true);
                setStyle(item == null || item.isBlank()
                        ? "-fx-alignment: center; -fx-background-color: rgba(255,255,255,0.75);"
                        : "-fx-alignment: center; -fx-background-color: rgba(110,196,138,0.70); -fx-font-weight: bold;");
                setOnMouseClicked(event -> {
                    Map<String, Object> rowData = getTableRow() == null ? null : getTableRow().getItem();
                    if (rowData == null) {
                        return;
                    }
                    Integer scheduleId = getInteger(rowData, key + "Id");
                    String scheduleName = getString(rowData, key);
                    if (scheduleId == null || scheduleName.isBlank()) {
                        return;
                    }
                    Map<String, Object> scheduleData = findScheduleById(scheduleId);
                    if (scheduleData == null) {
                        return;
                    }
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                        openEditDialog(scheduleData);
                        return;
                    }
                    if (event.getButton() == MouseButton.SECONDARY && event.getClickCount() == 1) {
                        int choice = MessageDialog.choiceDialog("确认删除课表记录“" + scheduleName + "”吗？删除后将立即从当前周课表中移除。");
                        if (choice == MessageDialog.CHOICE_YES) {
                            deleteSchedule(scheduleId, scheduleName);
                        } else {
                            updateStatus("已取消删除课表记录。");
                        }
                    }
                });
            }
        };
    }

    @FXML
    private void onPlusWeekClick() {
        normalizeWeekField(false);
        weekField.setText(String.valueOf(getCurrentWeek() + 1));
        loadCoursesForWeek(getCurrentWeek());
        updateStatus("已切换到第 " + getCurrentWeek() + " 周。");
    }

    @FXML
    private void onMinusWeekClick() {
        normalizeWeekField(false);
        int nextWeek = Math.max(1, getCurrentWeek() - 1);
        weekField.setText(String.valueOf(nextWeek));
        loadCoursesForWeek(nextWeek);
        updateStatus("已切换到第 " + nextWeek + " 周。");
    }

    @FXML
    private void onQueryButtonClick() {
        normalizeWeekField(false);
        updateStatus("正在刷新课表数据...");
        new Thread(() -> {
            DataResponse response = HttpRequestUtil.request("/api/course/getCourseScheduleList", new DataRequest());
            if (response != null && response.getCode() == 0) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getData();
                synchronized (courseScheduleList) {
                    courseScheduleList.clear();
                    if (data != null) {
                        courseScheduleList.addAll(data);
                    }
                }
                Platform.runLater(() -> {
                    loadCoursesForWeek(getCurrentWeek());
                    updateStatus("课表刷新成功，当前显示第 " + getCurrentWeek() + " 周。");
                });
            } else {
                Platform.runLater(() -> {
                    updateStatus("课表刷新失败。");
                    MessageDialog.showDialog("课表查询失败。" + (response == null ? "" : response.getMsg()));
                });
            }
        }).start();
    }

    @FXML
    private void onAddCourseButtonClick() {
        initAddDialog();
        courseAddController.clearForm();
        updateStatus("请填写新增课表课程信息。");
        MainApplication.setCanClose(false);
        addDialogStage.showAndWait();
    }

    private void openEditDialog(Map<String, Object> scheduleData) {
        initAddDialog();
        courseAddController.fillForm(scheduleData);
        updateStatus("正在编辑课表课程：" + getString(scheduleData, "name") + "。");
        MainApplication.setCanClose(false);
        addDialogStage.showAndWait();
    }

    private void initAddDialog() {
        if (addDialogStage != null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("base/course-add-dialog.fxml"));
            Scene scene = new Scene(loader.load(), 340, 330);
            addDialogStage = new Stage();
            addDialogStage.initOwner(MainApplication.getMainStage());
            addDialogStage.initModality(Modality.NONE);
            addDialogStage.setAlwaysOnTop(true);
            addDialogStage.setScene(scene);
            addDialogStage.setTitle("课表课程维护");
            addDialogStage.setOnCloseRequest(event -> {
                MainApplication.setCanClose(true);
                updateStatus("已关闭课表课程维护窗口。");
            });
            courseAddController = loader.getController();
            courseAddController.setCourseScheduleController(this);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void doClose(String cmd, Map<String, Object> data) {
        MainApplication.setCanClose(true);
        addDialogStage.close();
        if (!"ok".equals(cmd) || data == null) {
            updateStatus("已取消课表课程维护。");
            return;
        }
        Integer scheduleId = getInteger(data, "id");
        DataRequest request = new DataRequest();
        request.add("id", scheduleId);
        request.add("name", data.get("name"));
        request.add("dayOfWeek", data.get("dayOfWeek"));
        request.add("startTime", data.get("startTime"));
        request.add("startWeek", data.get("startWeek"));
        request.add("stopWeek", data.get("stopWeek"));
        DataResponse response = HttpRequestUtil.request("/api/course/courseScheduleSave", request);
        if (response != null && response.getCode() == 0) {
            if (scheduleId == null) {
                MessageDialog.showDialog("课表课程添加成功。");
                updateStatus("课表课程添加成功。");
            } else {
                MessageDialog.showDialog("课表课程修改成功。");
                updateStatus("课表课程修改成功。");
            }
            onQueryButtonClick();
        } else {
            updateStatus("课表课程保存失败。");
            MessageDialog.showDialog(response == null ? "课表课程保存失败，请检查网络连接。" : response.getMsg());
        }
    }

    private void deleteSchedule(Integer scheduleId, String scheduleName) {
        DataRequest request = new DataRequest();
        request.add("id", scheduleId);
        DataResponse response = HttpRequestUtil.request("/api/course/courseScheduleDelete", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("课表记录删除成功。");
            updateStatus("已删除课表记录：" + scheduleName + "。");
            onQueryButtonClick();
        } else {
            updateStatus("课表记录删除失败。");
            MessageDialog.showDialog("课表记录删除失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    private void loadCoursesForWeek(int week) {
        observableList.clear();
        List<Map<String, Object>> snapshot;
        synchronized (courseScheduleList) {
            snapshot = new ArrayList<>(courseScheduleList);
        }
        for (String timeSlot : timeSlots) {
            Map<String, Object> rowData = new HashMap<>();
            rowData.put("time_slot", timeSlot);
            for (int dayNumber = 1; dayNumber <= 7; dayNumber++) {
                String dayKey = getDayKeyFromNumber(dayNumber);
                Map<String, Object> course = findCourseByTimeAndDay(snapshot, timeSlot, dayNumber, week);
                if (course == null) {
                    rowData.put(dayKey, "");
                    rowData.put(dayKey + "Id", null);
                } else {
                    rowData.put(dayKey, getString(course, "name"));
                    rowData.put(dayKey + "Id", getInteger(course, "id"));
                }
            }
            observableList.add(rowData);
        }
        dataTableView.refresh();
    }

    private Map<String, Object> findCourseByTimeAndDay(List<Map<String, Object>> list, String timeSlot, int dayOfWeek, int week) {
        String slotStart = timeSlot.split("~")[0].trim();
        for (Map<String, Object> course : list) {
            Integer courseDayOfWeek = getInteger(course, "dayOfWeek");
            Integer startWeek = getInteger(course, "startWeek");
            Integer stopWeek = getInteger(course, "stopWeek");
            String startTime = getString(course, "startTime");
            if (courseDayOfWeek == null || startWeek == null || stopWeek == null) {
                continue;
            }
            if (courseDayOfWeek == dayOfWeek
                    && normalizeTime(slotStart).equals(normalizeTime(startTime))
                    && week >= startWeek && week <= stopWeek) {
                return course;
            }
        }
        return null;
    }

    private Map<String, Object> findScheduleById(Integer scheduleId) {
        synchronized (courseScheduleList) {
            for (Map<String, Object> schedule : courseScheduleList) {
                Integer id = getInteger(schedule, "id");
                if (id != null && id.equals(scheduleId)) {
                    return new HashMap<>(schedule);
                }
            }
        }
        return null;
    }

    private int getCurrentWeek() {
        try {
            return Math.max(1, Integer.parseInt(weekField.getText()));
        } catch (NumberFormatException exception) {
            weekField.setText("1");
            return 1;
        }
    }

    private void normalizeWeekField(boolean showMessage) {
        String value = weekField.getText() == null ? "" : weekField.getText().trim();
        try {
            int week = Integer.parseInt(value);
            if (week < 1) {
                weekField.setText("1");
                if (showMessage) {
                    MessageDialog.showDialog("周次不能小于 1，已自动调整为第 1 周。");
                }
            }
        } catch (Exception exception) {
            weekField.setText("1");
            if (showMessage) {
                MessageDialog.showDialog("周次输入无效，已自动恢复为第 1 周。");
            }
        }
    }

    private String getDayKeyFromNumber(int dayNumber) {
        return switch (dayNumber) {
            case 1 -> "monday";
            case 2 -> "tuesday";
            case 3 -> "wednesday";
            case 4 -> "thursday";
            case 5 -> "friday";
            case 6 -> "saturday";
            case 7 -> "sunday";
            default -> "";
        };
    }

    private String normalizeTime(String time) {
        return time == null ? "" : time.replaceFirst("^0+(?!$)", "");
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(value.toString());
        } catch (Exception exception) {
            return null;
        }
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
