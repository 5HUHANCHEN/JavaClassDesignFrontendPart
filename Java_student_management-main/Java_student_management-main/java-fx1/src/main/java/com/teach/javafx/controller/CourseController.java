package com.teach.javafx.controller;

import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.util.CommonMethod;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseController {
    @FXML
    private TableView<Map<String, Object>> dataTableView;
    @FXML
    private TableColumn<Map<String, Object>, String> numColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> nameColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> creditColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> coursePathColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> preCourseColumn;
    @FXML
    private TableColumn<Map<String, Object>, FlowPane> operateColumn;
    @FXML
    private TextField numNameTextField;
    @FXML
    private Label statusLabel;

    private final List<Map<String, Object>> courseList = new ArrayList<>();
    private final ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList();
    private final ObservableList<String> preCourseNameOptions = FXCollections.observableArrayList();
    private final Map<String, Integer> preCourseNameIdMap = new HashMap<>();
    private Stage scheduleStage;

    private static class AutoCommitTableCell extends TableCell<Map<String, Object>, String> {
        private final TextField textField = new TextField();

        private AutoCommitTableCell() {
            textField.setOnAction(event -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue && isEditing()) {
                    commitEdit(textField.getText());
                }
            });
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (!isEmpty()) {
                textField.setText(getItem());
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                textField.selectAll();
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                textField.setText(item);
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(item);
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
    }

    @FXML
    public void initialize() {
        setupTextColumn(numColumn, "num");
        setupTextColumn(nameColumn, "name");
        setupTextColumn(creditColumn, "credit");
        setupTextColumn(coursePathColumn, "coursePath");
        preCourseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(CommonMethod.getString(cellData.getValue(), "preCourse")));
        preCourseColumn.setCellFactory(ComboBoxTableCell.forTableColumn(preCourseNameOptions));
        preCourseColumn.setOnEditCommit(event -> {
            Map<String, Object> course = event.getRowValue();
            String selectedName = event.getNewValue();
            course.put("preCourse", selectedName == null ? "" : selectedName);
            course.put("preCourseId", preCourseNameIdMap.get(selectedName));
            updateStatus("已更新前置课程选项。");
        });
        operateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>((FlowPane) cellData.getValue().get("operate")));
        dataTableView.setEditable(true);
        onQueryButtonClick();
    }

    private void setupTextColumn(TableColumn<Map<String, Object>, String> column, String key) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(CommonMethod.getString(cellData.getValue(), key)));
        column.setCellFactory(col -> new AutoCommitTableCell());
        column.setOnEditCommit(event -> {
            event.getRowValue().put(key, event.getNewValue());
            updateStatus("已更新课程字段：" + key);
        });
    }

    @FXML
    private void onQueryButtonClick() {
        updateStatus("正在查询课程信息...");
        DataRequest request = new DataRequest();
        request.add("numName", numNameTextField.getText());
        DataResponse response = HttpRequestUtil.request("/api/course/getCourseList", request);
        courseList.clear();
        if (response != null && response.getCode() == 0) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getData();
            if (data != null) {
                courseList.addAll(data);
            }
            setTableViewData();
            updateStatus("课程查询成功，共 " + courseList.size() + " 条记录。");
        } else {
            setTableViewData();
            updateStatus("课程查询失败。");
            MessageDialog.showDialog("课程查询失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    @FXML
    private void onAddButtonClick() {
        Map<String, Object> course = new HashMap<>();
        course.put("courseId", null);
        course.put("num", "");
        course.put("name", "");
        course.put("credit", "");
        course.put("coursePath", "");
        course.put("preCourse", "");
        course.put("preCourseId", null);
        courseList.add(0, course);
        setTableViewData();
        dataTableView.scrollTo(0);
        dataTableView.edit(0, numColumn);
        updateStatus("已新增一条空白课程记录，请填写后保存。");
    }

    @FXML
    private void onOpenScheduleButtonClick() {
        try {
            if (scheduleStage == null) {
                FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("base/course-schedule-panel.fxml"));
                Scene scene = new Scene(loader.load(), 1020, 620);
                scheduleStage = new Stage();
                scheduleStage.initOwner(MainApplication.getMainStage());
                scheduleStage.initModality(Modality.NONE);
                scheduleStage.setAlwaysOnTop(false);
                scheduleStage.setScene(scene);
                scheduleStage.setTitle("课表管理");
            }
            scheduleStage.show();
            scheduleStage.toFront();
        } catch (IOException exception) {
            MessageDialog.showDialog("课表管理窗口打开失败。" + exception.getMessage());
        }
    }

    private void setTableViewData() {
        rebuildPreCourseOptions();
        observableList.clear();
        for (int i = 0; i < courseList.size(); i++) {
            Map<String, Object> course = courseList.get(i);
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(8);
            flowPane.setAlignment(Pos.CENTER);
            int rowIndex = i;
            Button saveButton = new Button("保存");
            saveButton.setOnAction(event -> saveItem(rowIndex));
            Button deleteButton = new Button("删除");
            deleteButton.setOnAction(event -> deleteItem(rowIndex));
            flowPane.getChildren().addAll(saveButton, deleteButton);
            course.put("operate", flowPane);
            observableList.add(course);
        }
        dataTableView.setItems(observableList);
    }

    private void rebuildPreCourseOptions() {
        preCourseNameOptions.clear();
        preCourseNameIdMap.clear();
        preCourseNameOptions.add("");
        for (Map<String, Object> course : courseList) {
            Integer courseId = CommonMethod.getInteger(course, "courseId");
            String courseName = CommonMethod.getString(course, "name");
            if (!courseName.isBlank()) {
                preCourseNameOptions.add(courseName);
                if (courseId != null) {
                    preCourseNameIdMap.put(courseName, courseId);
                }
            }
        }
    }

    private void saveItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= courseList.size()) {
            return;
        }
        Map<String, Object> course = courseList.get(rowIndex);
        String num = CommonMethod.getString(course, "num").trim();
        String name = CommonMethod.getString(course, "name").trim();
        String coursePath = CommonMethod.getString(course, "coursePath").trim();
        Integer credit = parseInteger(CommonMethod.getString(course, "credit"));
        Integer courseId = CommonMethod.getInteger(course, "courseId");
        Integer preCourseId = CommonMethod.getInteger(course, "preCourseId");
        if (num.isEmpty()) {
            MessageDialog.showDialog("课程编号不能为空。");
            updateStatus("保存失败：课程编号为空。");
            return;
        }
        if (name.isEmpty()) {
            MessageDialog.showDialog("课程名称不能为空。");
            updateStatus("保存失败：课程名称为空。");
            return;
        }
        if (credit == null) {
            MessageDialog.showDialog("课程学分必须为整数。");
            updateStatus("保存失败：课程学分格式不正确。");
            return;
        }
        if (courseId != null && courseId.equals(preCourseId)) {
            MessageDialog.showDialog("前置课程不能选择当前课程本身。");
            updateStatus("保存失败：前置课程选择无效。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("courseId", courseId);
        request.add("num", num);
        request.add("name", name);
        request.add("credit", credit);
        request.add("coursePath", coursePath);
        request.add("preCourseId", preCourseId);
        DataResponse response = HttpRequestUtil.request("/api/course/courseSave", request);
        if (response != null && response.getCode() == 0) {
            updateStatus("课程信息保存成功。");
            onQueryButtonClick();
        } else {
            updateStatus("课程信息保存失败。");
            MessageDialog.showDialog("课程保存失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    private void deleteItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= courseList.size()) {
            return;
        }
        Map<String, Object> course = courseList.get(rowIndex);
        Integer courseId = CommonMethod.getInteger(course, "courseId");
        if (courseId == null) {
            courseList.remove(rowIndex);
            setTableViewData();
            updateStatus("已移除未保存的课程记录。");
            return;
        }
        int choice = MessageDialog.choiceDialog("确认删除课程“" + CommonMethod.getString(course, "name") + "”吗？");
        if (choice != MessageDialog.CHOICE_YES) {
            updateStatus("已取消删除。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("courseId", courseId);
        DataResponse response = HttpRequestUtil.request("/api/course/courseDelete", request);
        if (response != null && response.getCode() == 0) {
            updateStatus("课程删除成功。");
            onQueryButtonClick();
        } else {
            updateStatus("课程删除失败。");
            MessageDialog.showDialog("课程删除失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
