package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentLeaveController extends ToolController {
    @FXML
    private TableView<Map> dataTableView;
    @FXML
    private TableColumn<Map, String> studentNumColumn;
    @FXML
    private TableColumn<Map, String> studentNameColumn;
    @FXML
    private TableColumn<Map, String> teacherNameColumn;
    @FXML
    private TableColumn<Map, String> leaveDateColumn;
    @FXML
    private TableColumn<Map, String> reasonColumn;
    @FXML
    private TableColumn<Map, String> stateNameColumn;
    @FXML
    private TableColumn<Map, String> teacherCommentColumn;
    @FXML
    private TableColumn<Map, String> adminCommentColumn;
    @FXML
    private TextField studentNumField;
    @FXML
    private TextField studentNameField;
    @FXML
    private TextField leaveDateField;
    @FXML
    private TextField reasonField;
    @FXML
    private TextField teacherCommentField;
    @FXML
    private TextField adminCommentField;
    @FXML
    private ComboBox<OptionItem> teacherComboBox;
    @FXML
    private ComboBox<OptionItem> stateComboBox;
    @FXML
    private TextField searchTextField;
    @FXML
    private Label searchLabel;
    @FXML
    private Label stateLabel;
    @FXML
    private HBox toolbarBox;
    @FXML
    private Button addButton;
    @FXML
    private Button queryButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button submitButton;
    @FXML
    private Button passButton;
    @FXML
    private Button notPassButton;

    private Integer studentLeaveId;
    private List<OptionItem> teacherList = new ArrayList<>();
    private List<OptionItem> stateList = new ArrayList<>();
    private final ObservableList<Map> observableList = FXCollections.observableArrayList();
    private ArrayList<Map> studentLeaveList = new ArrayList<>();
    private String roleName;

    @FXML
    public void initialize() {
        roleName = AppStore.getJwt().getRole();

        studentNumColumn.setCellValueFactory(new MapValueFactory<>("studentNum"));
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        teacherNameColumn.setCellValueFactory(new MapValueFactory<>("teacherName"));
        leaveDateColumn.setCellValueFactory(new MapValueFactory<>("leaveDate"));
        reasonColumn.setCellValueFactory(new MapValueFactory<>("reason"));
        stateNameColumn.setCellValueFactory(new MapValueFactory<>("stateName"));
        teacherCommentColumn.setCellValueFactory(new MapValueFactory<>("teacherComment"));
        adminCommentColumn.setCellValueFactory(new MapValueFactory<>("adminComment"));

        teacherComboBox.getItems().clear();
        if ("ROLE_STUDENT".equals(roleName)) {
            DataRequest request = new DataRequest();
            List<OptionItem> loadedTeachers = HttpRequestUtil.requestOptionItemList("/api/studentLeave/getTeacherItemOptionList", request);
            if (loadedTeachers != null) {
                teacherList = loadedTeachers;
                teacherComboBox.getItems().addAll(teacherList);
            }
        }

        stateList = HttpRequestUtil.getDictionaryOptionItemList("SHZTM");
        if (stateList == null) {
            stateList = new ArrayList<>();
        }
        stateList.add(0, new OptionItem(-1, "-1", "全部状态"));
        stateComboBox.getItems().setAll(stateList);
        stateComboBox.getSelectionModel().selectFirst();

        ObservableList<Integer> selectedIndices = dataTableView.getSelectionModel().getSelectedIndices();
        selectedIndices.addListener(this::onTableRowSelect);

        studentNumField.setDisable(true);
        studentNameField.setDisable(true);
        configureRoleView();
        onQueryButtonClick();
    }

    private void configureRoleView() {
        boolean isStudent = "ROLE_STUDENT".equals(roleName);
        boolean isTeacher = "ROLE_TEACHER".equals(roleName);
        boolean isAdmin = "ROLE_ADMIN".equals(roleName);

        setVisibleManaged(toolbarBox, !isStudent);
        setVisibleManaged(stateLabel, !isStudent);
        setVisibleManaged(stateComboBox, !isStudent);
        setVisibleManaged(searchLabel, !isStudent);
        setVisibleManaged(searchTextField, !isStudent);
        setVisibleManaged(queryButton, !isStudent);

        setVisibleManaged(addButton, false);
        setVisibleManaged(saveButton, isStudent);
        setVisibleManaged(submitButton, isStudent);
        setVisibleManaged(passButton, isTeacher);
        setVisibleManaged(notPassButton, isTeacher);

        teacherComboBox.setDisable(!isStudent);
        leaveDateField.setDisable(!isStudent);
        reasonField.setDisable(!isStudent);
        teacherCommentField.setDisable(!isTeacher);
        adminCommentField.setDisable(true);

        if (isAdmin) {
            teacherCommentField.setDisable(true);
        }
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setTableViewData() {
        observableList.clear();
        observableList.addAll(studentLeaveList);
        dataTableView.setItems(observableList);
    }

    private void clearPanel() {
        studentLeaveId = null;
        studentNumField.clear();
        studentNameField.clear();
        leaveDateField.clear();
        reasonField.clear();
        teacherCommentField.clear();
        adminCommentField.clear();
        teacherComboBox.getSelectionModel().clearSelection();
    }

    private void fillFormFromSelection() {
        Map<String, Object> selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            clearPanel();
            return;
        }
        studentLeaveId = CommonMethod.getInteger(selected, "studentLeaveId");
        studentNumField.setText(CommonMethod.getString(selected, "studentNum"));
        studentNameField.setText(CommonMethod.getString(selected, "studentName"));
        leaveDateField.setText(CommonMethod.getString(selected, "leaveDate"));
        reasonField.setText(CommonMethod.getString(selected, "reason"));
        teacherCommentField.setText(CommonMethod.getString(selected, "teacherComment"));
        adminCommentField.setText(CommonMethod.getString(selected, "adminComment"));
        teacherComboBox.getSelectionModel().select(
                CommonMethod.getOptionItemIndexByValue(teacherList, CommonMethod.getString(selected, "teacherId"))
        );
    }

    public void onTableRowSelect(ListChangeListener.Change<? extends Integer> change) {
        fillFormFromSelection();
    }

    @FXML
    protected void onQueryButtonClick() {
        DataRequest request = new DataRequest();
        OptionItem selectedState = stateComboBox.getSelectionModel().getSelectedItem();
        if (selectedState != null) {
            request.add("state", Integer.parseInt(selectedState.getValue()));
        }
        request.add("search", getText(searchTextField));

        DataResponse response = HttpRequestUtil.request("/api/studentLeave/getStudentLeaveList", request);
        if (response != null && response.getCode() == 0) {
            studentLeaveList = (ArrayList<Map>) response.getData();
            if (studentLeaveList == null) {
                studentLeaveList = new ArrayList<>();
            }
            setTableViewData();
            clearPanel();
        } else {
            MessageDialog.showDialog(response == null ? "请假记录查询失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onAddButtonClick() {
        dataTableView.getSelectionModel().clearSelection();
        clearPanel();
    }

    @FXML
    protected void onSaveButtonClick() {
        saveStudentLeave();
    }

    @FXML
    protected void onSubmitButtonClick() {
        saveStudentLeave();
    }

    @FXML
    protected void onPassButtonClick() {
        checkStudentLeave(1);
    }

    @FXML
    protected void onNotPassButtonClick() {
        checkStudentLeave(2);
    }

    private void saveStudentLeave() {
        if (!"ROLE_STUDENT".equals(roleName)) {
            MessageDialog.showDialog("只有学生可以提交请假申请。");
            return;
        }
        OptionItem teacherItem = teacherComboBox.getSelectionModel().getSelectedItem();
        if (teacherItem == null) {
            MessageDialog.showDialog("请选择审批教师。");
            return;
        }
        if (getText(leaveDateField).isEmpty()) {
            MessageDialog.showDialog("请输入请假日期。");
            return;
        }
        if (getText(reasonField).isEmpty()) {
            MessageDialog.showDialog("请输入请假原因。");
            return;
        }

        DataRequest request = new DataRequest();
        request.add("studentLeaveId", studentLeaveId);
        request.add("teacherId", Integer.parseInt(teacherItem.getValue()));
        request.add("leaveDate", getText(leaveDateField));
        request.add("reason", getText(reasonField));

        DataResponse response = HttpRequestUtil.request("/api/studentLeave/studentLeaveSave", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("请假申请保存成功。");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "请假申请保存失败。" : response.getMsg());
        }
    }

    private void checkStudentLeave(Integer state) {
        if (!"ROLE_TEACHER".equals(roleName)) {
            MessageDialog.showDialog("只有教师可以审批请假申请。");
            return;
        }
        if (studentLeaveId == null) {
            MessageDialog.showDialog("请先选择一条请假记录。");
            return;
        }
        if (getText(teacherCommentField).isEmpty()) {
            MessageDialog.showDialog("请输入审批意见。");
            return;
        }

        DataRequest request = new DataRequest();
        request.add("studentLeaveId", studentLeaveId);
        request.add("teacherComment", getText(teacherCommentField));
        request.add("state", state);

        DataResponse response = HttpRequestUtil.request("/api/studentLeave/studentLeaveCheck", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog(state == 1 ? "请假申请已审批通过。" : "请假申请已驳回。");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "请假审批失败。" : response.getMsg());
        }
    }

    private String getText(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }
}
