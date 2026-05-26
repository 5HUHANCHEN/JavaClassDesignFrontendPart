package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import com.teach.javafx.util.CommonMethod;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CourseMaterialController extends ToolController {
    @FXML private ComboBox<OptionItem> filterCourseComboBox;
    @FXML private TextField keywordTextField;
    @FXML private TableView<Map<String, Object>> materialTableView;
    @FXML private TableColumn<Map<String, Object>, String> courseColumn;
    @FXML private TableColumn<Map<String, Object>, String> titleColumn;
    @FXML private TableColumn<Map<String, Object>, String> fileNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> fileTypeColumn;
    @FXML private TableColumn<Map<String, Object>, String> fileSizeColumn;
    @FXML private TableColumn<Map<String, Object>, String> uploaderColumn;
    @FXML private TableColumn<Map<String, Object>, String> uploadTimeColumn;
    @FXML private ComboBox<OptionItem> courseComboBox;
    @FXML private TextField titleTextField;
    @FXML private TextArea descriptionTextArea;
    @FXML private TextField fileNameTextField;
    @FXML private TextField fileTypeTextField;
    @FXML private TextField fileSizeTextField;
    @FXML private TextField uploaderTextField;
    @FXML private TextField uploadTimeTextField;
    @FXML private Button newButton;
    @FXML private Button saveButton;
    @FXML private Button uploadButton;
    @FXML private Button deleteButton;
    @FXML private Button downloadButton;
    @FXML private Label statusLabel;

    private final ObservableList<Map<String, Object>> materialObservableList = FXCollections.observableArrayList();
    private final List<OptionItem> courseOptions = new ArrayList<>();
    private Integer currentMaterialId;
    private boolean canEdit;

    @FXML
    public void initialize() {
        canEdit = AppStore.getJwt() != null
                && ("ROLE_ADMIN".equals(AppStore.getJwt().getRole()) || "ROLE_TEACHER".equals(AppStore.getJwt().getRole()));
        setupTableColumns();
        setupRoleUi();
        loadCourseOptions();
        materialTableView.setItems(materialObservableList);
        materialTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> showMaterial(newValue));
        onQueryButtonClick();
    }

    @Override
    public void doRefresh() {
        onQueryButtonClick();
    }

    @Override
    public void doNew() {
        onNewButtonClick();
    }

    @Override
    public void doSave() {
        onSaveButtonClick();
    }

    @Override
    public void doDelete() {
        onDeleteButtonClick();
    }

    @FXML
    private void onQueryButtonClick() {
        DataRequest request = new DataRequest();
        OptionItem filterCourse = filterCourseComboBox.getSelectionModel().getSelectedItem();
        request.add("courseId", filterCourse == null ? 0 : filterCourse.getId());
        request.add("keyword", keywordTextField.getText());
        DataResponse response = HttpRequestUtil.request("/api/courseMaterial/getMaterialList", request);
        materialObservableList.clear();
        if (response != null && response.getCode() == 0) {
            materialObservableList.addAll(asMapList(response.getData()));
            if (!materialObservableList.isEmpty()) {
                materialTableView.getSelectionModel().select(0);
            } else {
                showMaterial(null);
            }
            updateStatus("资料查询成功，共 " + materialObservableList.size() + " 条记录。");
        } else {
            showMaterial(null);
            updateStatus("资料查询失败。");
            MessageDialog.showDialog("资料查询失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    @FXML
    private void onNewButtonClick() {
        if (!canEdit) {
            return;
        }
        materialTableView.getSelectionModel().clearSelection();
        currentMaterialId = null;
        courseComboBox.getSelectionModel().clearSelection();
        titleTextField.clear();
        descriptionTextArea.clear();
        fileNameTextField.clear();
        fileTypeTextField.clear();
        fileSizeTextField.clear();
        uploaderTextField.clear();
        uploadTimeTextField.clear();
        updateStatus("已新建资料记录，请填写课程和标题后保存。");
    }

    @FXML
    private void onSaveButtonClick() {
        saveCurrentMaterial();
    }

    @FXML
    private void onUploadButtonClick() {
        if (!canEdit) {
            return;
        }
        Integer materialId = currentMaterialId;
        if (materialId == null) {
            materialId = saveCurrentMaterial();
        }
        if (materialId == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择课程资料文件");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("常用资料", "*.pdf", "*.ppt", "*.pptx", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.png", "*.jpg", "*.jpeg", "*.txt"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        File file = chooser.showOpenDialog(MainApplication.getMainStage());
        if (file == null) {
            return;
        }
        DataResponse response = HttpRequestUtil.uploadCourseMaterialFile(file.getAbsolutePath(), materialId);
        if (response != null && response.getCode() == 0) {
            updateStatus("文件上传成功：" + file.getName());
            onQueryButtonClick();
            selectMaterial(materialId);
        } else {
            updateStatus("文件上传失败。");
            MessageDialog.showDialog("文件上传失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    @FXML
    private void onDeleteButtonClick() {
        if (!canEdit) {
            return;
        }
        if (currentMaterialId == null) {
            onNewButtonClick();
            return;
        }
        int choice = MessageDialog.choiceDialog("确认删除当前课程资料吗？");
        if (choice != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("materialId", currentMaterialId);
        DataResponse response = HttpRequestUtil.request("/api/courseMaterial/materialDelete", request);
        if (response != null && response.getCode() == 0) {
            updateStatus("资料删除成功。");
            onQueryButtonClick();
        } else {
            updateStatus("资料删除失败。");
            MessageDialog.showDialog("资料删除失败。" + (response == null ? "" : response.getMsg()));
        }
    }

    @FXML
    private void onDownloadButtonClick() {
        if (currentMaterialId == null) {
            MessageDialog.showDialog("请先选择一条资料记录。");
            return;
        }
        Map<String, Object> selected = materialTableView.getSelectionModel().getSelectedItem();
        if (selected == null || !CommonMethod.getBoolean(selected, "hasFile")) {
            MessageDialog.showDialog("当前资料还没有上传文件。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("materialId", currentMaterialId);
        byte[] bytes = HttpRequestUtil.requestByteData("/api/courseMaterial/downloadMaterialFile", request);
        if (bytes == null || bytes.length == 0) {
            MessageDialog.showDialog("资料下载失败。");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("保存课程资料");
        String fileName = CommonMethod.getString(selected, "fileName");
        chooser.setInitialFileName(fileName.isBlank() ? "course-material" : fileName);
        File file = chooser.showSaveDialog(MainApplication.getMainStage());
        if (file == null) {
            return;
        }
        try {
            Files.write(file.toPath(), bytes);
            updateStatus("资料已下载到：" + file.getAbsolutePath());
        } catch (Exception e) {
            MessageDialog.showDialog("资料保存失败。" + e.getMessage());
        }
    }

    private Integer saveCurrentMaterial() {
        if (!canEdit) {
            return null;
        }
        OptionItem course = courseComboBox.getSelectionModel().getSelectedItem();
        String title = titleTextField.getText() == null ? "" : titleTextField.getText().trim();
        if (course == null || course.getId() == null || course.getId() == 0) {
            MessageDialog.showDialog("请选择课程。");
            return null;
        }
        if (title.isBlank()) {
            MessageDialog.showDialog("资料标题不能为空。");
            return null;
        }
        DataRequest request = new DataRequest();
        request.add("materialId", currentMaterialId);
        request.add("courseId", course.getId());
        request.add("title", title);
        request.add("description", descriptionTextArea.getText());
        DataResponse response = HttpRequestUtil.request("/api/courseMaterial/materialSave", request);
        if (response != null && response.getCode() == 0) {
            Map<String, Object> saved = asMap(response.getData());
            currentMaterialId = CommonMethod.getInteger(saved, "materialId");
            updateStatus("资料保存成功。");
            onQueryButtonClick();
            selectMaterial(currentMaterialId);
            return currentMaterialId;
        }
        updateStatus("资料保存失败。");
        MessageDialog.showDialog("资料保存失败。" + (response == null ? "" : response.getMsg()));
        return null;
    }

    private void showMaterial(Map<String, Object> material) {
        if (material == null) {
            currentMaterialId = null;
            courseComboBox.getSelectionModel().clearSelection();
            titleTextField.clear();
            descriptionTextArea.clear();
            fileNameTextField.clear();
            fileTypeTextField.clear();
            fileSizeTextField.clear();
            uploaderTextField.clear();
            uploadTimeTextField.clear();
            return;
        }
        currentMaterialId = CommonMethod.getInteger(material, "materialId");
        Integer courseId = CommonMethod.getInteger(material, "courseId");
        int courseIndex = CommonMethod.getOptionItemIndexById(courseOptions, courseId);
        if (courseIndex >= 0) {
            courseComboBox.getSelectionModel().select(courseIndex);
        } else {
            courseComboBox.getSelectionModel().clearSelection();
        }
        titleTextField.setText(CommonMethod.getString(material, "title"));
        descriptionTextArea.setText(CommonMethod.getString(material, "description"));
        fileNameTextField.setText(CommonMethod.getString(material, "fileName"));
        fileTypeTextField.setText(CommonMethod.getString(material, "fileType"));
        fileSizeTextField.setText(CommonMethod.getString(material, "fileSizeText"));
        uploaderTextField.setText(CommonMethod.getString(material, "uploaderName"));
        uploadTimeTextField.setText(CommonMethod.getString(material, "uploadTime"));
    }

    private void setupRoleUi() {
        setVisibleManaged(newButton, canEdit);
        setVisibleManaged(saveButton, canEdit);
        setVisibleManaged(uploadButton, canEdit);
        setVisibleManaged(deleteButton, canEdit);
        courseComboBox.setDisable(!canEdit);
        titleTextField.setEditable(canEdit);
        descriptionTextArea.setEditable(canEdit);
        fileNameTextField.setEditable(false);
        fileTypeTextField.setEditable(false);
        fileSizeTextField.setEditable(false);
        uploaderTextField.setEditable(false);
        uploadTimeTextField.setEditable(false);
        downloadButton.setDisable(false);
    }

    private void setupTableColumns() {
        setupColumn(courseColumn, "courseName");
        setupColumn(titleColumn, "title");
        setupColumn(fileNameColumn, "fileName");
        setupColumn(fileTypeColumn, "fileType");
        setupColumn(fileSizeColumn, "fileSizeText");
        setupColumn(uploaderColumn, "uploaderName");
        setupColumn(uploadTimeColumn, "uploadTime");
    }

    private void setupColumn(TableColumn<Map<String, Object>, String> column, String key) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(CommonMethod.getString(cellData.getValue(), key)));
    }

    private void loadCourseOptions() {
        DataRequest request = new DataRequest();
        List<OptionItem> options = HttpRequestUtil.requestOptionItemList("/api/score/getCourseItemOptionList", request);
        courseOptions.clear();
        if (options != null) {
            courseOptions.addAll(options);
        }
        filterCourseComboBox.getItems().clear();
        filterCourseComboBox.getItems().add(new OptionItem(0, "0", "全部课程"));
        filterCourseComboBox.getItems().addAll(courseOptions);
        filterCourseComboBox.getSelectionModel().select(0);
        courseComboBox.getItems().setAll(courseOptions);
    }

    private void selectMaterial(Integer materialId) {
        if (materialId == null) {
            return;
        }
        for (Map<String, Object> item : materialObservableList) {
            if (materialId.equals(CommonMethod.getInteger(item, "materialId"))) {
                materialTableView.getSelectionModel().select(item);
                materialTableView.scrollTo(item);
                return;
            }
        }
    }

    private void setVisibleManaged(Control control, boolean visible) {
        control.setVisible(visible);
        control.setManaged(visible);
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;
            return map;
        }
        return Map.of();
    }

    private List<Map<String, Object>> asMapList(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) rawMap;
                    result.add(map);
                }
            }
        }
        return result;
    }
}
