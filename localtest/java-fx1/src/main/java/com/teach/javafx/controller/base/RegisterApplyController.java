package com.teach.javafx.controller.base;

import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;

import javafx.animation.FadeTransition;
import javafx.util.Duration;

import javafx.scene.layout.HBox;
import com.teach.javafx.util.ToastUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Map;

public class RegisterApplyController {

    @FXML
    private TextField keywordField;
    @FXML
    private ComboBox<String> statusComboBox;
    @FXML
    private Label countLabel;
    @FXML
    private TableColumn<Map<String, Object>, String> actionColumn;

    @FXML
    private TableView<Map<String, Object>> applyTable;
    @FXML
    private TableColumn<Map<String, Object>, String> applyIdColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> usernameColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> nameColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> roleColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> deptColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> phoneColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> emailColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> applyTimeColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> statusColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> reasonColumn;

    @FXML
    private Label pendingCountLabel;
    @FXML
    private Label approvedCountLabel;
    @FXML
    private Label rejectedCountLabel;

    private final ObservableList<Map<String, Object>> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList("待审核", "已通过", "已拒绝"));
        statusComboBox.setValue("待审核");

        applyIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "applyId")));
        usernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "username")));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "name")));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatRole(getValue(cellData.getValue(), "role"))));
        deptColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "dept")));
        phoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "phone")));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "email")));
        applyTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "applyTime")));
        reasonColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "reason")));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatStatus(getValue(cellData.getValue(), "status"))));

        // 1. 独立渲染状态列
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label tag = new Label(item);
                tag.setStyle(getStatusStyle(item));
                setGraphic(tag);
            }
        });

        // 2. 独立渲染操作列按钮（解除嵌套，扁平化）
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button approveBtn = new Button("通过");
            private final Button rejectBtn = new Button("拒绝");
            private final Button detailBtn = new Button("详情");
            private final HBox buttonBox = new HBox(8, approveBtn, rejectBtn, detailBtn);

            {
                approveBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 4px; -fx-cursor: hand;");
                rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 4px; -fx-cursor: hand;");
                detailBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 4px; -fx-cursor: hand;");

                approveBtn.setOnAction(event -> handleApprove(getTableView().getItems().get(getIndex())));
                rejectBtn.setOnAction(event -> handleReject(getTableView().getItems().get(getIndex())));
                detailBtn.setOnAction(event -> handleViewDetail(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Map<String, Object> rowData = getTableView().getItems().get(getIndex());
                String status = formatStatus(getValue(rowData, "status"));
                // 如果已经处理过，则只显示详情按钮
                if ("已通过".equals(status) || "已拒绝".equals(status)) {
                    setGraphic(new HBox(8, detailBtn));
                } else {
                    setGraphic(buttonBox);
                }
            }
        });

        applyTable.setItems(tableData);
        loadTableData();
        applyTable.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleViewDetail(row.getItem());
                }
            });
            return row;
        });

        FadeTransition ft = new FadeTransition(Duration.millis(550), applyTable);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    @FXML
    protected void onQueryButtonClick() {
        loadTableData();
    }

    @FXML
    protected void onRefreshButtonClick() {
        loadTableData();
    }

    @FXML
    protected void onResetButtonClick() {
        keywordField.clear();
        statusComboBox.setValue("待审核");
        loadTableData();
    }

    private void handleApprove(Map<String, Object> selected) {
        if (selected == null) {
            ToastUtil.showError("请先选择一条申请记录！");
            return;
        }

        Integer applyId = parseInteger(selected.get("applyId"));
        if (applyId == null) {
            ToastUtil.showError("申请ID无效！");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("审核通过");
        confirm.setHeaderText("确认通过该申请？");
        confirm.setContentText(
                "用户名：" + getValue(selected, "username") + "\n" +
                        "姓名：" + getValue(selected, "name") + "\n" +
                        "角色：" + formatRole(getValue(selected, "role"))
        );

        ButtonType yesBtn = new ButtonType("确认通过", ButtonBar.ButtonData.OK_DONE);
        ButtonType noBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yesBtn, noBtn);

        confirm.showAndWait().ifPresent(result -> {
            if (result == yesBtn) {
                DataRequest req = new DataRequest();
                req.add("applyId", applyId);
                req.add("remark", "审核通过");

                DataResponse res = HttpRequestUtil.request("/auth/approveRegisterApply", req);
                if (res != null && res.getCode() == 0) {
                    ToastUtil.showSuccess("审核通过成功！");
                    loadTableData();
                } else {
                    ToastUtil.showError(res == null ? "审核通过失败！" : res.getMsg());
                }
            }
        });
    }

    private void handleReject(Map<String, Object> selected) {
        if (selected == null) {
            ToastUtil.showError("请先选择一条申请记录！");
            return;
        }

        Integer applyId = parseInteger(selected.get("applyId"));
        if (applyId == null) {
            ToastUtil.showError("申请ID无效！");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("拒绝申请");
        dialog.setHeaderText("请输入拒绝原因");

        ButtonType confirmBtn = new ButtonType("确认拒绝", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtn, cancelBtn);

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("请输入拒绝原因...");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(4);
        reasonArea.setPrefWidth(380);

        VBox box = new VBox(10);
        box.getChildren().addAll(
                new Label("申请人：" + getValue(selected, "name")),
                new Label("账号：" + getValue(selected, "username")),
                new Label("拒绝原因："),
                reasonArea
        );
        box.setStyle("-fx-padding: 16; -fx-background-color: white;");

        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(button -> {
            if (button == confirmBtn) return reasonArea.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(remark -> {
            if (remark == null || remark.trim().isEmpty()) {
                ToastUtil.showError("请输入拒绝原因！");
                return;
            }

            DataRequest req = new DataRequest();
            req.add("applyId", applyId);
            req.add("remark", remark.trim());

            DataResponse res = HttpRequestUtil.request("/auth/rejectRegisterApply", req);
            if (res != null && res.getCode() == 0) {
                ToastUtil.showSuccess("已拒绝该申请！");
                loadTableData();
            } else {
                ToastUtil.showError(res == null ? "拒绝申请失败！" : res.getMsg());
            }
        });
    }

    @FXML
    protected void onApproveButtonClick() {
        handleApprove(applyTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    protected void onRejectButtonClick() {
        handleReject(applyTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    protected void onViewDetailButtonClick() {
        Map<String, Object> selected = applyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ToastUtil.showError("请先选择一条申请记录！");
            return;
        }
        handleViewDetail(selected);
    }



    private void handleViewDetail(Map<String, Object> selected) {
        if (selected == null) {
            MessageDialog.showDialog("请先选择一条申请记录！");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("申请详情");
        dialog.setHeaderText("账号申请详细信息");

        ButtonType closeBtn = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeBtn);

        VBox container = new VBox(16);
        container.setStyle(
                "-fx-padding: 20;" +
                        "-fx-background-color: linear-gradient(to bottom right, #f8fbff, #eef4fb);" +
                        "-fx-background-radius: 18;"
        );

        Label titleLabel = new Label("申请信息总览");
        titleLabel.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #1e293b;"
        );

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(12);
        grid.setStyle(
                "-fx-padding: 18;" +
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.2, 0, 2);"
        );

        int row = 0;
        addDetailRow(grid, row++, "申请ID：", getValue(selected, "applyId"));
        addDetailRow(grid, row++, "用户名：", getValue(selected, "username"));
        addDetailRow(grid, row++, "姓名：", getValue(selected, "name"));
        addDetailRow(grid, row++, "角色：", formatRole(getValue(selected, "role")));
        addDetailRow(grid, row++, "院系：", getValue(selected, "dept"));
        addDetailRow(grid, row++, "专业：", getValue(selected, "major"));
        addDetailRow(grid, row++, "班级：", getValue(selected, "className"));
        addDetailRow(grid, row++, "电话：", getValue(selected, "phone"));
        addDetailRow(grid, row++, "邮箱：", getValue(selected, "email"));
        addDetailRow(grid, row++, "申请时间：", getValue(selected, "applyTime"));
        addDetailRow(grid, row++, "状态：", formatStatus(getValue(selected, "status")));

        Label reasonTitle = new Label("申请原因");
        reasonTitle.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #334155;"
        );

        Label reasonLabel = new Label(getValue(selected, "reason"));
        reasonLabel.setWrapText(true);
        reasonLabel.setPrefWidth(420);
        reasonLabel.setStyle(
                "-fx-padding: 14;" +
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: #334155;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0.15, 0, 1);"
        );

        container.getChildren().addAll(titleLabel, grid, reasonTitle, reasonLabel);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(container);
        pane.setPrefWidth(560);
        pane.setPrefHeight(620);

        pane.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #f4f8fd, #e9f0fa);" +
                        "-fx-background-radius: 18;"
        );

        dialog.showAndWait();
    }

    private void addDetailRow(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #475569;"
        );

        Label value = new Label(valueText == null ? "" : valueText);
        value.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #1e293b;"
        );

        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }


    @SuppressWarnings("unchecked")
    private void loadTableData() {
        DataRequest req = new DataRequest();
        req.add("status", mapStatus(statusComboBox.getValue()));

        DataResponse res = HttpRequestUtil.request("/auth/getRegisterApplyList", req);
        if (res == null || res.getCode() != 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("查询失败！");
            alert.showAndWait();
            return;
        }

        Object data = res.getData();
        tableData.clear();

        int pending = 0;
        int approved = 0;
        int rejected = 0;

        if (data instanceof List<?> list) {
            String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();

            for (Object obj : list) {
                if (obj instanceof Map<?, ?> rawMap) {
                    Map<String, Object> row = (Map<String, Object>) rawMap;

                    String status = getValue(row, "status");
                    if ("0".equals(status)) pending++;
                    else if ("1".equals(status)) approved++;
                    else if ("2".equals(status)) rejected++;

                    if (matchKeyword(row, keyword)) {
                        tableData.add(row);
                    }
                }
            }
        }

        countLabel.setText("共 " + tableData.size() + " 条记录");
        pendingCountLabel.setText(String.valueOf(pending));
        approvedCountLabel.setText(String.valueOf(approved));
        rejectedCountLabel.setText(String.valueOf(rejected));
    }


    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }

        String username = getValue(row, "username");
        String name = getValue(row, "name");
        return username.contains(keyword) || name.contains(keyword);
    }

    private String getValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer mapStatus(String text) {
        if ("已通过".equals(text)) {
            return 1;
        }
        if ("已拒绝".equals(text)) {
            return 2;
        }
        return 0;
    }

    private String formatRole(String role) {
        return switch (role) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            default -> role;
        };
    }

    private String formatStatus(String status) {
        return switch (status) {
            case "0" -> "待审核";
            case "1" -> "已通过";
            case "2" -> "已拒绝";
            default -> status;
        };
    }

    private String getStatusStyle(String status) {
        return switch (status) {
            case "待审核" ->
                    "-fx-background-color: linear-gradient(to right, #fff7d6, #ffe9a8);" +
                            "-fx-text-fill: #b45309;" +
                            "-fx-padding: 5 14 5 14;" +
                            "-fx-background-radius: 20;" +
                            "-fx-font-weight: bold;" +
                            "-fx-border-color: #f59e0b;" +
                            "-fx-border-radius: 20;";
            case "已通过" ->
                    "-fx-background-color: linear-gradient(to right, #dcfce7, #bbf7d0);" +
                            "-fx-text-fill: #15803d;" +
                            "-fx-padding: 5 14 5 14;" +
                            "-fx-background-radius: 20;" +
                            "-fx-font-weight: bold;" +
                            "-fx-border-color: #22c55e;" +
                            "-fx-border-radius: 20;";
            case "已拒绝" ->
                    "-fx-background-color: linear-gradient(to right, #fee2e2, #fecaca);" +
                            "-fx-text-fill: #b91c1c;" +
                            "-fx-padding: 5 14 5 14;" +
                            "-fx-background-radius: 20;" +
                            "-fx-font-weight: bold;" +
                            "-fx-border-color: #ef4444;" +
                            "-fx-border-radius: 20;";
            default ->
                    "-fx-background-color: #e5e7eb;" +
                            "-fx-text-fill: #374151;" +
                            "-fx-padding: 5 14 5 14;" +
                            "-fx-background-radius: 20;" +
                            "-fx-font-weight: bold;";
        };
    }
}