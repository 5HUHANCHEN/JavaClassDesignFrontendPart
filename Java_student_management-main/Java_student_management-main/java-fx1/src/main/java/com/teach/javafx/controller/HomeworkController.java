package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class HomeworkController extends ToolController {
    @FXML private TableView<Map> homeworkTable;
    @FXML private TableColumn<Map, String> titleColumn;
    @FXML private TableColumn<Map, String> dueDateColumn;
    @FXML private TableColumn<Map, String> totalScoreColumn;
    @FXML private TableColumn<Map, String> teacherNameColumn;
    @FXML private TableColumn<Map, String> submitStateColumn;
    @FXML private TableColumn<Map, String> myGradeColumn;
    @FXML private TableView<Map> submissionTable;
    @FXML private TableColumn<Map, String> submissionTitleColumn;
    @FXML private TableColumn<Map, String> studentNumColumn;
    @FXML private TableColumn<Map, String> studentNameColumn;
    @FXML private TableColumn<Map, String> submitTimeColumn;
    @FXML private TableColumn<Map, String> stateNameColumn;
    @FXML private TableColumn<Map, String> gradeColumn;
    @FXML private TextField keywordField;
    @FXML private TextField titleField;
    @FXML private TextField dueDateField;
    @FXML private TextField totalScoreField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea contentArea;
    @FXML private TextField submissionKeywordField;
    @FXML private TextField gradeField;
    @FXML private TextArea teacherCommentArea;
    @FXML private ImageView homeworkImageView;
    @FXML private ImageView submissionImageView;
    @FXML private VBox publishPane;
    @FXML private VBox submitPane;
    @FXML private VBox gradePane;
    @FXML private Button saveHomeworkButton;
    @FXML private Button deleteHomeworkButton;
    @FXML private Button uploadHomeworkImageButton;
    @FXML private Button submitHomeworkButton;
    @FXML private Button uploadSubmissionImageButton;
    @FXML private Button gradeButton;
    @FXML private Label statusLabel;

    private final ObservableList<Map> homeworkObservableList = FXCollections.observableArrayList();
    private final ObservableList<Map> submissionObservableList = FXCollections.observableArrayList();
    private ArrayList<Map> homeworkList = new ArrayList<>();
    private ArrayList<Map> submissionList = new ArrayList<>();
    private Integer currentHomeworkId;
    private Integer currentSubmissionId;
    private Double currentTotalScore = 100d;
    private String roleName;

    @FXML
    public void initialize() {
        roleName = AppStore.getJwt() == null ? "" : AppStore.getJwt().getRole();
        setupColumns();
        configureRoleView();
        homeworkTable.getSelectionModel().getSelectedIndices().addListener(this::onHomeworkSelect);
        submissionTable.getSelectionModel().getSelectedIndices().addListener(this::onSubmissionSelect);
        onQueryButtonClick();
    }

    private void setupColumns() {
        titleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        dueDateColumn.setCellValueFactory(new MapValueFactory<>("dueDate"));
        totalScoreColumn.setCellValueFactory(new MapValueFactory<>("totalScore"));
        teacherNameColumn.setCellValueFactory(new MapValueFactory<>("teacherName"));
        submitStateColumn.setCellValueFactory(new MapValueFactory<>("submitState"));
        myGradeColumn.setCellValueFactory(new MapValueFactory<>("grade"));
        submissionTitleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        studentNumColumn.setCellValueFactory(new MapValueFactory<>("studentNum"));
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        submitTimeColumn.setCellValueFactory(new MapValueFactory<>("submitTime"));
        stateNameColumn.setCellValueFactory(new MapValueFactory<>("stateName"));
        gradeColumn.setCellValueFactory(new MapValueFactory<>("grade"));
    }

    private void configureRoleView() {
        boolean isStudent = "ROLE_STUDENT".equals(roleName);
        boolean canTeach = "ROLE_TEACHER".equals(roleName) || "ROLE_ADMIN".equals(roleName);
        publishPane.setVisible(canTeach);
        publishPane.setManaged(canTeach);
        gradePane.setVisible(canTeach);
        gradePane.setManaged(canTeach);
        submitPane.setVisible(isStudent);
        submitPane.setManaged(isStudent);
        submissionTable.setVisible(canTeach);
        submissionTable.setManaged(canTeach);
        saveHomeworkButton.setVisible(canTeach);
        deleteHomeworkButton.setVisible(canTeach);
        uploadHomeworkImageButton.setVisible(canTeach);
        submitHomeworkButton.setVisible(isStudent);
        uploadSubmissionImageButton.setVisible(isStudent);
        gradeButton.setVisible(canTeach);
    }

    @FXML
    protected void onQueryButtonClick() {
        DataRequest request = new DataRequest();
        request.add("keyword", getText(keywordField));
        DataResponse response = HttpRequestUtil.request("/api/homework/getHomeworkList", request);
        if (response != null && response.getCode() == 0) {
            homeworkList = (ArrayList<Map>) response.getData();
            if (homeworkList == null) {
                homeworkList = new ArrayList<>();
            }
            homeworkObservableList.setAll(homeworkList);
            homeworkTable.setItems(homeworkObservableList);
            updateStatus("作业列表已刷新，共 " + homeworkList.size() + " 条。", "success");
        } else {
            MessageDialog.showDialog(response == null ? "作业列表加载失败。" : response.getMsg());
        }
        onQuerySubmissionButtonClick();
    }

    @FXML
    protected void onQuerySubmissionButtonClick() {
        DataRequest request = new DataRequest();
        request.add("homeworkId", currentHomeworkId);
        request.add("keyword", getText(submissionKeywordField));
        DataResponse response = HttpRequestUtil.request("/api/homework/getSubmissionList", request);
        if (response != null && response.getCode() == 0) {
            submissionList = (ArrayList<Map>) response.getData();
            if (submissionList == null) {
                submissionList = new ArrayList<>();
            }
            submissionObservableList.setAll(submissionList);
            submissionTable.setItems(submissionObservableList);
        }
    }

    @FXML
    protected void onNewHomeworkButtonClick() {
        currentHomeworkId = null;
        titleField.clear();
        dueDateField.clear();
        totalScoreField.setText("100");
        descriptionArea.clear();
        homeworkImageView.setImage(null);
        updateStatus("正在新建作业。", "info");
    }

    @FXML
    protected void onSaveHomeworkButtonClick() {
        if (getText(titleField).isEmpty()) {
            MessageDialog.showDialog("请填写作业标题。");
            return;
        }
        Double totalScore = validateScore(getText(totalScoreField), 1000d, "作业满分");
        if (totalScore == null) {
            return;
        }
        if (totalScore <= 0) {
            MessageDialog.showDialog("作业满分必须大于 0。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("homeworkId", currentHomeworkId);
        request.add("title", getText(titleField));
        request.add("description", getText(descriptionArea));
        request.add("dueDate", getText(dueDateField));
        request.add("totalScore", totalScore);
        DataResponse response = HttpRequestUtil.request("/api/homework/homeworkSave", request);
        if (response != null && response.getCode() == 0) {
            Map data = (Map) response.getData();
            currentHomeworkId = CommonMethod.getInteger(data, "homeworkId");
            updateStatus("作业已保存。", "success");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "作业保存失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onDeleteHomeworkButtonClick() {
        if (currentHomeworkId == null) {
            MessageDialog.showDialog("请先选择作业。");
            return;
        }
        if (MessageDialog.choiceDialog("确认删除这份作业吗？") != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("homeworkId", currentHomeworkId);
        DataResponse response = HttpRequestUtil.request("/api/homework/homeworkDelete", request);
        if (response != null && response.getCode() == 0) {
            onNewHomeworkButtonClick();
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "作业删除失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onUploadHomeworkImageButtonClick() {
        if (currentHomeworkId == null) {
            MessageDialog.showDialog("请先保存作业，再上传题图。");
            return;
        }
        File file = chooseImage("选择作业题图");
        if (file == null) {
            return;
        }
        DataResponse response = HttpRequestUtil.uploadHomeworkImage(file.getAbsolutePath(), currentHomeworkId);
        if (response != null && response.getCode() == 0) {
            loadHomeworkImage(currentHomeworkId);
            updateStatus("题图上传成功。", "success");
        } else {
            MessageDialog.showDialog(response == null ? "题图上传失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onSubmitHomeworkButtonClick() {
        if (currentHomeworkId == null) {
            MessageDialog.showDialog("请先选择要提交的作业。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("homeworkId", currentHomeworkId);
        request.add("content", getText(contentArea));
        DataResponse response = HttpRequestUtil.request("/api/homework/submitHomework", request);
        if (response != null && response.getCode() == 0) {
            Map data = (Map) response.getData();
            currentSubmissionId = CommonMethod.getInteger(data, "submissionId");
            updateStatus("作业已提交，可以继续上传提交图片。", "success");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "作业提交失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onUploadSubmissionImageButtonClick() {
        if (currentSubmissionId == null) {
            MessageDialog.showDialog("请先提交作业，再上传图片。");
            return;
        }
        File file = chooseImage("选择提交图片");
        if (file == null) {
            return;
        }
        DataResponse response = HttpRequestUtil.uploadSubmissionImage(file.getAbsolutePath(), currentSubmissionId);
        if (response != null && response.getCode() == 0) {
            loadSubmissionImage(currentSubmissionId);
            updateStatus("提交图片上传成功。", "success");
        } else {
            MessageDialog.showDialog(response == null ? "提交图片上传失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onGradeButtonClick() {
        if (currentSubmissionId == null) {
            MessageDialog.showDialog("请先选择学生提交记录。");
            return;
        }
        Double grade = validateScore(getText(gradeField), currentTotalScore, "评分");
        if (grade == null) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("submissionId", currentSubmissionId);
        request.add("grade", grade);
        request.add("teacherComment", getText(teacherCommentArea));
        DataResponse response = HttpRequestUtil.request("/api/homework/gradeSubmission", request);
        if (response != null && response.getCode() == 0) {
            updateStatus("评分已保存。", "success");
            onQuerySubmissionButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "评分保存失败。" : response.getMsg());
        }
    }

    private void onHomeworkSelect(ListChangeListener.Change<? extends Integer> change) {
        Map selected = homeworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        currentHomeworkId = CommonMethod.getInteger(selected, "homeworkId");
        currentSubmissionId = CommonMethod.getInteger(selected, "submissionId");
        currentTotalScore = parseDouble(CommonMethod.getString(selected, "totalScore"), 100d);
        titleField.setText(CommonMethod.getString(selected, "title"));
        dueDateField.setText(CommonMethod.getString(selected, "dueDate"));
        totalScoreField.setText(CommonMethod.getString(selected, "totalScore"));
        descriptionArea.setText(CommonMethod.getString(selected, "description"));
        contentArea.setText(CommonMethod.getString(selected, "content"));
        teacherCommentArea.setText(CommonMethod.getString(selected, "teacherComment"));
        gradeField.setText(CommonMethod.getString(selected, "grade"));
        loadHomeworkImage(currentHomeworkId);
        loadSubmissionImage(currentSubmissionId);
        onQuerySubmissionButtonClick();
    }

    private void onSubmissionSelect(ListChangeListener.Change<? extends Integer> change) {
        Map selected = submissionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        currentSubmissionId = CommonMethod.getInteger(selected, "submissionId");
        currentHomeworkId = CommonMethod.getInteger(selected, "homeworkId");
        currentTotalScore = parseDouble(CommonMethod.getString(selected, "totalScore"), 100d);
        gradeField.setText(CommonMethod.getString(selected, "grade"));
        teacherCommentArea.setText(CommonMethod.getString(selected, "teacherComment"));
        contentArea.setText(CommonMethod.getString(selected, "content"));
        loadSubmissionImage(currentSubmissionId);
    }

    private void loadHomeworkImage(Integer homeworkId) {
        homeworkImageView.setImage(null);
        if (homeworkId == null) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("homeworkId", homeworkId);
        byte[] data = HttpRequestUtil.requestByteData("/api/homework/getHomeworkImage", request);
        if (data != null && data.length > 0) {
            homeworkImageView.setImage(new Image(new ByteArrayInputStream(data)));
        }
    }

    private void loadSubmissionImage(Integer submissionId) {
        submissionImageView.setImage(null);
        if (submissionId == null) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("submissionId", submissionId);
        byte[] data = HttpRequestUtil.requestByteData("/api/homework/getSubmissionImage", request);
        if (data != null && data.length > 0) {
            submissionImageView.setImage(new Image(new ByteArrayInputStream(data)));
        }
    }

    private File chooseImage(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        return fileChooser.showOpenDialog(homeworkTable.getScene().getWindow());
    }

    private String getText(TextInputControl control) {
        return control == null || control.getText() == null ? "" : control.getText().trim();
    }

    private Double parseDouble(String value, Double defaultValue) {
        try {
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Double validateScore(String value, Double maxValue, String fieldName) {
        if (value == null || value.isBlank()) {
            MessageDialog.showDialog(fieldName + "不能为空。");
            return null;
        }
        Double score = parseDouble(value, null);
        if (score == null || score.isNaN() || score.isInfinite()) {
            MessageDialog.showDialog(fieldName + "必须是有效数字。");
            return null;
        }
        if (score < 0) {
            MessageDialog.showDialog(fieldName + "不能小于 0。");
            return null;
        }
        double max = maxValue == null || maxValue <= 0 ? 100d : maxValue;
        if (score > max) {
            MessageDialog.showDialog(fieldName + "不能超过满分 " + trimNumber(max) + "。");
            return null;
        }
        return score;
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void updateStatus(String text, String type) {
        statusLabel.setText(text);
        switch (type) {
            case "success" -> statusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            case "info" -> statusLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
            default -> statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");
        }
    }
}
