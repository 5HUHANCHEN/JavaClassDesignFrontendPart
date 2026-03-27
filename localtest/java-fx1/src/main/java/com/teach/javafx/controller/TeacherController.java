package com.teach.javafx.controller;

import java.net.URL;

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
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import com.teach.javafx.util.ToastUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<Map<String, Object>> dataTableView;

    @FXML
    private TableColumn<Map<String, Object>, String> numColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> nameColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> deptColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> titleColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> degreeColumn;
    @FXML
    private TableColumn<Map<String, Object>, FlowPane> operateColumn;

    @FXML
    private TextField numNameTextField;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> themeComboBox;

    private List<Map<String, Object>> teacherList = new ArrayList<>();
    private final ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList();

    private class AutoCommitTableCell extends TableCell<Map<String, Object>, String> {
        private final TextField textField = new TextField();

        public AutoCommitTableCell() {
            textField.setOnAction(e -> commitEdit(textField.getText()));

            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && isEditing()) {
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

    private void loadCss() {
        URL cssUrl = getClass().getResource("/com/teach/javafx/css/teacher-panel.css");
        System.out.println("cssUrl = " + cssUrl);

        if (cssUrl == null) {
            MessageDialog.showDialog("CSS 没找到：/com/teach/javafx/css/teacher-panel.css");
            return;
        }

        rootPane.getStylesheets().clear();
        rootPane.getStylesheets().add(cssUrl.toExternalForm());
    }

    @FXML
    public void initialize() {
        loadCss();

        setupTextColumn(numColumn, "num");
        setupTextColumn(nameColumn, "name");
        setupTextColumn(deptColumn, "dept");
        setupTextColumn(titleColumn, "title");
        setupTextColumn(degreeColumn, "degree");

        operateColumn.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get("operate");
            return new SimpleObjectProperty<>((FlowPane) val);
        });

        dataTableView.setEditable(true);

        initThemeBox();
        updateStatus("系统就绪");
        onQueryButtonClick();
    }

    private void initThemeBox() {
        themeComboBox.getItems().addAll("默认模式", "深色模式", "紧凑模式");
        themeComboBox.getSelectionModel().select(lastTheme);

        applyTheme(lastTheme);

        themeComboBox.setOnAction(e -> {
            String mode = themeComboBox.getValue();
            lastTheme = mode;
            applyTheme(mode);
        });
    }

    private void applyTheme(String mode) {
        rootPane.getStyleClass().removeAll("dark-mode", "compact-mode");
        dataTableView.getStyleClass().removeAll("table-dark", "table-compact");

        if ("深色模式".equals(mode)) {
            rootPane.getStyleClass().add("dark-mode");
            dataTableView.getStyleClass().add("table-dark");
            updateStatus("已切换到深色模式");
        } else if ("紧凑模式".equals(mode)) {
            rootPane.getStyleClass().add("compact-mode");
            dataTableView.getStyleClass().add("table-compact");
            updateStatus("已切换到紧凑模式");
        } else {
            updateStatus("已切换到默认模式");
        }
    }
    private void setupTextColumn(TableColumn<Map<String, Object>, String> column, String key) {
        column.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().get(key);
            return new SimpleStringProperty(val == null ? "" : val.toString());
        });

        column.setCellFactory(col -> new AutoCommitTableCell());

        column.setOnEditCommit(event -> {
            event.getRowValue().put(key, event.getNewValue());
            updateStatus("已临时保存当前单元格：" + key);
        });
    }

    @FXML
    private void onQueryButtonClick() {
        updateStatus("正在查询...");

        DataRequest req = new DataRequest();
        req.add("numName", numNameTextField.getText());

        DataResponse res = HttpRequestUtil.request("/api/teacher/getTeacherList", req);
        if (res != null && res.getCode() == 0) {
            teacherList = (List<Map<String, Object>>) res.getData();
            if (teacherList == null) {
                teacherList = new ArrayList<>();
            }
            setTableViewData();
            updateStatus("查询成功，共 " + teacherList.size() + " 条记录");
        } else {
            updateStatus("查询失败");
            MessageDialog.showDialog("查询失败！" + (res != null ? res.getMsg() : ""));
        }
    }

    @FXML
    private void onAddButtonClick() {
        Map<String, Object> map = new HashMap<>();
        map.put("personId", null);
        map.put("num", "");
        map.put("name", "");
        map.put("dept", "");
        map.put("title", "");
        map.put("degree", "");

        teacherList.add(0, map);
        setTableViewData();
        dataTableView.scrollTo(0);
        dataTableView.edit(0, numColumn);

        updateStatus("已新增一行，请填写后保存");
    }

    private void setTableViewData() {
        observableList.clear();

        for (int i = 0; i < teacherList.size(); i++) {
            Map<String, Object> map = teacherList.get(i);

            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(8);
            flowPane.setAlignment(Pos.CENTER);

            final int rowIndex = i;

            Button saveButton = new Button("💾 保存");
            saveButton.getStyleClass().add("btn-primary");
            saveButton.setOnAction(e -> saveItem(rowIndex));

            Button deleteButton = new Button("🗑 删除");
            deleteButton.setStyle("-fx-background-color: linear-gradient(to bottom, #f87171, #ef4444); -fx-text-fill: white; -fx-background-radius: 12; -fx-font-weight: bold;");
            deleteButton.setOnAction(e -> deleteItem(rowIndex));

            flowPane.getChildren().addAll(saveButton, deleteButton);
            map.put("operate", flowPane);

            observableList.add(map);
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
            ToastUtil.showError("保存失败：工号不能为空！");
            updateStatus("保存失败：工号为空"); // 如果你还需要更新底部状态栏，保留此行
            return;
        }

        if (name == null || name.trim().isEmpty()) {
            ToastUtil.showError("保存失败：姓名不能为空！");
            updateStatus("保存失败：姓名为空"); // 如果你还需要更新底部状态栏，保留此行
            return;
        }

// 如果校验全部通过，执行保存逻辑...
// 保存成功后调用：
// ToastUtil.showSuccess("保存成功");

        DataRequest req = new DataRequest();
        req.add("personId", personId);
        req.add("num", num);
        req.add("name", name);
        req.add("dept", dept);
        req.add("title", title);
        req.add("degree", degree);

        updateStatus("正在保存...");

        DataResponse res = HttpRequestUtil.request("/api/teacher/teacherSave", req);

        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("保存成功！");
            onQueryButtonClick();
            updateStatus("保存成功");
        } else {
            String msg = (res == null) ? "网络错误或后端无响应" : res.getMsg();
            MessageDialog.showDialog("保存失败：" + msg);
            updateStatus("保存失败");
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
            updateStatus("已删除未保存的新行");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认要删除这位教师吗？");
        if (ret != MessageDialog.CHOICE_YES) {
            return;
        }

        DataRequest req = new DataRequest();
        req.add("personId", personId);

        updateStatus("正在删除...");

        DataResponse res = HttpRequestUtil.request("/api/teacher/teacherDelete", req);

        if (res != null && res.getCode() == 0) {
            ToastUtil.showSuccess("删除成功！");
            onQueryButtonClick();
            updateStatus("删除成功");
        } else {
            ToastUtil.showError(res == null ? "删除失败！" : res.getMsg());
            updateStatus("删除失败");
        }
    }

    private void updateStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);

            if (text.contains("成功")) {
                statusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            } else if (text.contains("失败")) {
                statusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else if (text.contains("正在")) {
                statusLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");
            }
        }
    }
    private static String lastTheme = "默认模式";
}