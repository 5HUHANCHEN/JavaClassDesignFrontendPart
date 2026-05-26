package com.teach.javafx.controller;

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
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherController {
    @FXML private BorderPane rootPane;
    @FXML private TableView<Map<String, Object>> dataTableView;
    @FXML private TableColumn<Map<String, Object>, String> numColumn;
    @FXML private TableColumn<Map<String, Object>, String> nameColumn;
    @FXML private TableColumn<Map<String, Object>, String> deptColumn;
    @FXML private TableColumn<Map<String, Object>, String> titleColumn;
    @FXML private TableColumn<Map<String, Object>, String> degreeColumn;
    @FXML private TableColumn<Map<String, Object>, FlowPane> operateColumn;
    @FXML private TextField numNameTextField;
    @FXML private Label statusLabel;

    private List<Map<String, Object>> teacherList = new ArrayList<>();
    private final ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList();

    private class AutoCommitTableCell extends TableCell<Map<String, Object>, String> {
        private final TextField textField = new TextField();

        AutoCommitTableCell() {
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, oldValue, newValue) -> {
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
        setupTextColumn(deptColumn, "dept");
        setupTextColumn(titleColumn, "title");
        setupTextColumn(degreeColumn, "degree");
        operateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>((FlowPane) cellData.getValue().get("operate")));
        dataTableView.setEditable(true);
        updateStatus("系统就绪", "neutral");
        onQueryButtonClick();
    }

    private void setupTextColumn(TableColumn<Map<String, Object>, String> column, String key) {
        column.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get(key);
            return new SimpleStringProperty(value == null ? "" : value.toString());
        });
        column.setCellFactory(col -> new AutoCommitTableCell());
        column.setOnEditCommit(event -> {
            event.getRowValue().put(key, event.getNewValue());
            updateStatus("已暂存表格编辑内容：" + key, "info");
        });
    }

    @FXML
    private void onQueryButtonClick() {
        updateStatus("正在查询教师信息...", "info");
        DataRequest request = new DataRequest();
        request.add("numName", numNameTextField.getText());
        DataResponse response = HttpRequestUtil.request("/api/teacher/getTeacherList", request);
        if (response != null && response.getCode() == 0) {
            teacherList = (List<Map<String, Object>>) response.getData();
            if (teacherList == null) {
                teacherList = new ArrayList<>();
            }
            setTableViewData();
            updateStatus("查询成功，共 " + teacherList.size() + " 条教师记录。", "success");
        } else {
            updateStatus("查询失败", "error");
            MessageDialog.showDialog("教师查询失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    @FXML
    private void onAddButtonClick() {
        Map<String, Object> teacher = new HashMap<>();
        teacher.put("personId", null);
        teacher.put("num", "");
        teacher.put("name", "");
        teacher.put("dept", "");
        teacher.put("title", "");
        teacher.put("degree", "");
        teacherList.add(0, teacher);
        setTableViewData();
        dataTableView.scrollTo(0);
        dataTableView.edit(0, numColumn);
        updateStatus("已新增一条空白教师记录，请先填写并保存。", "info");
    }

    private void setTableViewData() {
        observableList.clear();
        for (int i = 0; i < teacherList.size(); i++) {
            Map<String, Object> teacher = teacherList.get(i);
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(8);
            flowPane.setAlignment(Pos.CENTER);

            int rowIndex = i;
            Button saveButton = new Button("保存");
            saveButton.getStyleClass().add("btn-primary");
            saveButton.setOnAction(e -> saveItem(rowIndex));

            Button uploadPhotoButton = new Button("上传头像");
            uploadPhotoButton.getStyleClass().add("btn-secondary");
            uploadPhotoButton.setOnAction(e -> uploadTeacherPhoto(rowIndex));

            Button deleteButton = new Button("删除");
            deleteButton.getStyleClass().add("btn-danger");
            deleteButton.setOnAction(e -> deleteItem(rowIndex));

            flowPane.getChildren().addAll(saveButton, uploadPhotoButton, deleteButton);
            teacher.put("operate", flowPane);
            observableList.add(teacher);
        }
        dataTableView.setItems(observableList);
    }

    private void saveItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= teacherList.size()) {
            return;
        }
        Map<String, Object> data = teacherList.get(rowIndex);
        String num = CommonMethod.getString(data, "num");
        String name = CommonMethod.getString(data, "name");
        String dept = CommonMethod.getString(data, "dept");
        String title = CommonMethod.getString(data, "title");
        String degree = CommonMethod.getString(data, "degree");
        Integer personId = CommonMethod.getInteger(data, "personId");

        if (num == null || num.trim().isEmpty()) {
            MessageDialog.showDialog("教师工号不能为空。");
            updateStatus("保存失败：教师工号为空。", "error");
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            MessageDialog.showDialog("教师姓名不能为空。");
            updateStatus("保存失败：教师姓名为空。", "error");
            return;
        }

        DataRequest request = new DataRequest();
        request.add("personId", personId);
        request.add("num", num);
        request.add("name", name);
        request.add("dept", dept);
        request.add("title", title);
        request.add("degree", degree);

        updateStatus("正在保存教师信息...", "info");
        DataResponse response = HttpRequestUtil.request("/api/teacher/teacherSave", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("教师信息保存成功。");
            onQueryButtonClick();
            updateStatus("教师信息保存成功。", "success");
        } else {
            MessageDialog.showDialog(response == null ? "教师信息保存失败。" : response.getMsg());
            updateStatus("教师信息保存失败。", "error");
        }
    }

    private void uploadTeacherPhoto(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= teacherList.size()) {
            return;
        }
        Map<String, Object> data = teacherList.get(rowIndex);
        Integer personId = CommonMethod.getInteger(data, "personId");
        if (personId == null) {
            MessageDialog.showDialog("请先保存教师信息，再上传头像。");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择教师头像");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        updateStatus("正在上传教师头像...", "info");
        DataResponse response = HttpRequestUtil.uploadPhotoBlob(selectedFile.getAbsolutePath(), personId);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("教师头像上传成功。");
            updateStatus("教师头像上传成功。", "success");
        } else {
            MessageDialog.showDialog(response == null ? "教师头像上传失败。" : response.getMsg());
            updateStatus("教师头像上传失败。", "error");
        }
    }

    private void deleteItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= teacherList.size()) {
            return;
        }
        Map<String, Object> data = teacherList.get(rowIndex);
        Integer personId = CommonMethod.getInteger(data, "personId");
        if (personId == null) {
            teacherList.remove(rowIndex);
            setTableViewData();
            updateStatus("已删除未保存的教师记录。", "neutral");
            return;
        }
        int result = MessageDialog.choiceDialog("确认删除这位教师吗？");
        if (result != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("personId", personId);
        updateStatus("正在删除教师信息...", "info");
        DataResponse response = HttpRequestUtil.request("/api/teacher/teacherDelete", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("教师信息删除成功。");
            onQueryButtonClick();
            updateStatus("教师信息删除成功。", "success");
        } else {
            MessageDialog.showDialog(response == null ? "教师信息删除失败。" : response.getMsg());
            updateStatus("教师信息删除失败。", "error");
        }
    }

    private void updateStatus(String text, String type) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setText(text);
        switch (type) {
            case "success" -> statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            case "error" -> statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            case "info" -> statusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
            default -> statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        }
    }
}
