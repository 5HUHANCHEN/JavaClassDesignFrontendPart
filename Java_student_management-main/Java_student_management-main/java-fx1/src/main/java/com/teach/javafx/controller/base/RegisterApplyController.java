package com.teach.javafx.controller.base;

import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Map;

public class RegisterApplyController {
    private static final String STATUS_PENDING_LABEL = "\u5f85\u5ba1\u6838";
    private static final String STATUS_APPROVED_LABEL = "\u5df2\u901a\u8fc7";
    private static final String STATUS_REJECTED_LABEL = "\u5df2\u9a73\u56de";

    @FXML private TextField keywordField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Label countLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label approvedCountLabel;
    @FXML private Label rejectedCountLabel;
    @FXML private TableView<Map<String, Object>> applyTable;
    @FXML private TableColumn<Map<String, Object>, String> applyIdColumn;
    @FXML private TableColumn<Map<String, Object>, String> usernameColumn;
    @FXML private TableColumn<Map<String, Object>, String> nameColumn;
    @FXML private TableColumn<Map<String, Object>, String> roleColumn;
    @FXML private TableColumn<Map<String, Object>, String> deptColumn;
    @FXML private TableColumn<Map<String, Object>, String> phoneColumn;
    @FXML private TableColumn<Map<String, Object>, String> emailColumn;
    @FXML private TableColumn<Map<String, Object>, String> applyTimeColumn;
    @FXML private TableColumn<Map<String, Object>, String> statusColumn;
    @FXML private TableColumn<Map<String, Object>, String> reasonColumn;
    @FXML private TableColumn<Map<String, Object>, String> actionColumn;

    private final ObservableList<Map<String, Object>> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList("待审批", "已通过", "已驳回"));
        statusComboBox.setValue("待审批");
        statusComboBox.setItems(FXCollections.observableArrayList(
                STATUS_PENDING_LABEL,
                STATUS_APPROVED_LABEL,
                STATUS_REJECTED_LABEL
        ));
        statusComboBox.setValue(STATUS_PENDING_LABEL);
        applyIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "applyId")));
        usernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "username")));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "name")));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatRole(getValue(cellData.getValue(), "role"))));
        deptColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "dept")));
        phoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "phone")));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "email")));
        applyTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "applyTime")));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatStatus(cellData.getValue().get("status"))));
        reasonColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getValue(cellData.getValue(), "reason")));
        actionColumn.setCellValueFactory(cellData -> new SimpleStringProperty("actions"));
        actionColumn.setCellFactory(column -> new ActionCell());
        applyTable.setItems(tableData);
        loadTableData();
    }

    @FXML protected void onQueryButtonClick() { loadTableData(); }
    @FXML protected void onRefreshButtonClick() { loadTableData(); }
    @FXML protected void onApproveButtonClick() { approveSelected(applyTable.getSelectionModel().getSelectedItem()); }
    @FXML protected void onRejectButtonClick() { rejectSelected(applyTable.getSelectionModel().getSelectedItem()); }
    @FXML protected void onViewDetailButtonClick() { viewSelected(applyTable.getSelectionModel().getSelectedItem()); }

    @FXML
    protected void onResetButtonClick() {
        keywordField.clear();
        statusComboBox.setValue("待审批");
        loadTableData();
    }

    private void loadTableData() {
        DataRequest request = new DataRequest();
        request.add("status", mapStatus(statusComboBox.getValue()));
        DataResponse response = HttpRequestUtil.request("/auth/getRegisterApplyList", request);
        if (response == null) {
            tableData.clear();
            updateSummaryCounts(0, 0, 0);
            countLabel.setText("共 0 条记录");
            return;
        }
        if (response.getCode() != 0 || !(response.getData() instanceof List<?> list)) {
            tableData.clear();
            updateSummaryCounts(0, 0, 0);
            countLabel.setText("共 0 条记录");
            MessageDialog.showDialog(response.getMsg());
            return;
        }

        tableData.clear();
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) rawMap;
            Integer status = parseInteger(row.get("status"));
            if (status != null && status == 0) pending++;
            else if (status != null && status == 1) approved++;
            else if (status != null && status == 2) rejected++;
            if (keyword.isEmpty() || getValue(row, "username").contains(keyword) || getValue(row, "name").contains(keyword)) {
                tableData.add(row);
            }
        }
        updateSummaryCounts(pending, approved, rejected);
        countLabel.setText("共 " + tableData.size() + " 条记录");
    }

    private void updateSummaryCounts(int pending, int approved, int rejected) {
        pendingCountLabel.setText(String.valueOf(pending));
        approvedCountLabel.setText(String.valueOf(approved));
        rejectedCountLabel.setText(String.valueOf(rejected));
    }

    private void approveSelected(Map<String, Object> selected) {
        if (selected == null) {
            MessageDialog.showDialog("请先选择一条申请记录。");
            return;
        }
        Integer applyId = parseInteger(selected.get("applyId"));
        if (applyId == null) {
            MessageDialog.showDialog("申请编号无效。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("applyId", applyId);
        request.add("remark", "审批通过");
        DataResponse response = HttpRequestUtil.request("/auth/approveRegisterApply", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("账号申请已审批通过。");
            loadTableData();
        } else {
            MessageDialog.showDialog(response == null ? "账号申请审批失败，请检查服务器连接。" : response.getMsg());
        }
    }

    private void rejectSelected(Map<String, Object> selected) {
        if (selected == null) {
            MessageDialog.showDialog("请先选择一条申请记录。");
            return;
        }
        Integer applyId = parseInteger(selected.get("applyId"));
        if (applyId == null) {
            MessageDialog.showDialog("申请编号无效。");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("驳回申请");
        dialog.setHeaderText("请输入驳回原因");
        dialog.setContentText("审批意见：");
        dialog.showAndWait().ifPresent(remark -> {
            if (remark == null || remark.trim().isEmpty()) {
                MessageDialog.showDialog("驳回原因不能为空。");
                return;
            }
            DataRequest request = new DataRequest();
            request.add("applyId", applyId);
            request.add("remark", remark.trim());
            DataResponse response = HttpRequestUtil.request("/auth/rejectRegisterApply", request);
            if (response != null && response.getCode() == 0) {
                MessageDialog.showDialog("账号申请已驳回。");
                loadTableData();
            } else {
                MessageDialog.showDialog(response == null ? "驳回申请失败，请检查服务器连接。" : response.getMsg());
            }
        });
    }

    private void viewSelected(Map<String, Object> selected) {
        if (selected == null) {
            MessageDialog.showDialog("请先选择一条申请记录。");
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("申请详情");
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE));
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        addDetailRow(grid, 0, "申请编号", getValue(selected, "applyId"));
        addDetailRow(grid, 1, "账号", getValue(selected, "username"));
        addDetailRow(grid, 2, "姓名", getValue(selected, "name"));
        addDetailRow(grid, 3, "角色", formatRole(getValue(selected, "role")));
        addDetailRow(grid, 4, "院系", getValue(selected, "dept"));
        addDetailRow(grid, 5, "专业", getValue(selected, "major"));
        addDetailRow(grid, 6, "班级/岗位", getValue(selected, "className"));
        addDetailRow(grid, 7, "电话", getValue(selected, "phone"));
        addDetailRow(grid, 8, "邮箱", getValue(selected, "email"));
        addDetailRow(grid, 9, "申请时间", getValue(selected, "applyTime"));
        addDetailRow(grid, 10, "状态", formatStatus(getValue(selected, "status")));
        addDetailRow(grid, 11, "申请说明", getValue(selected, "reason"));
        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void addDetailRow(GridPane grid, int rowIndex, String labelText, String valueText) {
        grid.add(new Label(labelText + "："), 0, rowIndex);
        Label valueLabel = new Label(valueText == null || valueText.isBlank() ? "暂无信息" : valueText);
        valueLabel.setWrapText(true);
        grid.add(valueLabel, 1, rowIndex);
    }

    private String getValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Integer mapStatus(String statusLabel) {
        if (STATUS_APPROVED_LABEL.equals(statusLabel)) return 1;
        if (STATUS_REJECTED_LABEL.equals(statusLabel)) return 2;
        if ("已通过".equals(statusLabel)) return 1;
        if ("已驳回".equals(statusLabel)) return 2;
        return 0;
    }

    private String formatRole(String role) {
        if (role == null) return "学生";
        String upper = role.toUpperCase();
        if (upper.contains("ADMIN")) return "管理员";
        if (upper.contains("TEACHER")) return "教师";
        return "学生";
    }

    private String formatStatus(Object statusValue) {
        Integer status = parseInteger(statusValue);
        if (status != null && status == 1) {
            return STATUS_APPROVED_LABEL;
        }
        if (status != null && status == 2) {
            return STATUS_REJECTED_LABEL;
        }
        return STATUS_PENDING_LABEL;
    }

    private String formatStatusLegacy(String status) {
        return switch (status) {
            case "1" -> "已通过";
            case "2" -> "已驳回";
            default -> "待审批";
        };
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isPending(Map<String, Object> row) {
        Integer status = row == null ? null : parseInteger(row.get("status"));
        return status != null && status == 0;
    }

    private class ActionCell extends TableCell<Map<String, Object>, String> {
        private final Button detailButton = new Button("详情");
        private final Button approveButton = new Button("通过");
        private final Button rejectButton = new Button("驳回");
        private final HBox actionBox = new HBox(8, detailButton, approveButton, rejectButton);

        private ActionCell() {
            detailButton.setText("\u8be6\u60c5");
            approveButton.setText("\u540c\u610f");
            rejectButton.setText("\u9a73\u56de");
            detailButton.setOnAction(event -> viewSelected(getCurrentRow()));
            approveButton.setOnAction(event -> approveSelected(getCurrentRow()));
            rejectButton.setOnAction(event -> rejectSelected(getCurrentRow()));
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            Map<String, Object> row = getCurrentRow();
            boolean pending = isPending(row);
            approveButton.setDisable(!pending);
            rejectButton.setDisable(!pending);
            setGraphic(actionBox);
        }

        private Map<String, Object> getCurrentRow() {
            return getTableRow() == null ? null : getTableRow().getItem();
        }
    }
}
