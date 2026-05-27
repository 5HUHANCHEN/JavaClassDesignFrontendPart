package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public abstract class AbstractStudentGrowthRecordController extends ToolController implements Initializable {
    @FXML
    protected Label pageTitleLabel;
    @FXML
    protected Label pageSubtitleLabel;
    @FXML
    protected TextField searchTextField;
    @FXML
    protected Button queryButton;
    @FXML
    protected Button addButton;
    @FXML
    protected Button saveButton;
    @FXML
    protected Button deleteButton;

    @FXML
    protected TableView<Map> dataTableView;
    @FXML
    protected TableColumn<Map, String> studentNumColumn;
    @FXML
    protected TableColumn<Map, String> studentNameColumn;
    @FXML
    protected TableColumn<Map, String> itemTypeColumn;
    @FXML
    protected TableColumn<Map, String> titleColumn;
    @FXML
    protected TableColumn<Map, String> levelColumn;
    @FXML
    protected TableColumn<Map, String> dateRangeColumn;
    @FXML
    protected TableColumn<Map, String> organizationColumn;
    @FXML
    protected TableColumn<Map, String> resultColumn;

    @FXML
    protected HBox typeManageBox;
    @FXML
    protected ComboBox<OptionItem> typeManageComboBox;
    @FXML
    protected TextField typeManageField;
    @FXML
    protected Button typeAddButton;
    @FXML
    protected Button typeSaveButton;
    @FXML
    protected Button typeDeleteButton;

    @FXML
    protected Label studentSelectLabel;
    @FXML
    protected ComboBox<OptionItem> studentComboBox;
    @FXML
    protected TextField studentNumField;
    @FXML
    protected TextField studentNameField;

    @FXML
    protected Label itemTypeLabel;
    @FXML
    protected ComboBox<OptionItem> itemTypeComboBox;
    @FXML
    protected Label titleFieldLabel;
    @FXML
    protected TextField titleField;
    @FXML
    protected Label levelLabel;
    @FXML
    protected TextField levelField;
    @FXML
    protected Label organizationLabel;
    @FXML
    protected TextField organizationField;
    @FXML
    protected Label startDateLabel;
    @FXML
    protected DatePicker startDatePicker;
    @FXML
    protected Label endDateLabel;
    @FXML
    protected DatePicker endDatePicker;
    @FXML
    protected Label placeLabel;
    @FXML
    protected TextField placeField;
    @FXML
    protected Label resultLabel;
    @FXML
    protected TextField resultField;
    @FXML
    protected Label descriptionLabel;
    @FXML
    protected TextArea descriptionArea;

    protected Integer recordId;
    protected final ObservableList<Map> observableList = FXCollections.observableArrayList();
    protected ArrayList<Map> growthRecordList = new ArrayList<>();
    protected List<OptionItem> studentOptionList = new ArrayList<>();
    protected List<OptionItem> itemTypeOptionList = new ArrayList<>();
    protected String roleName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roleName = AppStore.getJwt().getRole();
        configureStaticTexts();
        configureTableColumns();
        configureDatePickers();
        configureRoleView();
        loadStudentOptions();
        loadTypeOptions(null, null);

        dataTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> fillFormFromSelection(newValue));
        studentComboBox.setOnAction(event -> syncStudentFieldsFromSelection());
        if (typeManageComboBox != null) {
            typeManageComboBox.setOnAction(event -> syncTypeManageField());
        }
        onQueryButtonClick();
    }

    protected abstract String getCategoryCode();

    protected abstract String getPageTitle();

    protected abstract String getPageSubtitle();

    protected abstract String getItemTypeLabelText();

    protected abstract String getTitleLabelText();

    protected abstract String getLevelLabelText();

    protected abstract String getOrganizationLabelText();

    protected abstract String getStartDateLabelText();

    protected abstract String getEndDateLabelText();

    protected abstract String getPlaceLabelText();

    protected abstract String getResultLabelText();

    protected abstract String getDescriptionLabelText();

    protected boolean supportsPlaceField() {
        return true;
    }

    protected String getSearchPromptText() {
        return "可按学号、姓名、标题或关键词查询";
    }

    protected void configureStaticTexts() {
        pageTitleLabel.setText(getPageTitle());
        pageSubtitleLabel.setText(getPageSubtitle());
        searchTextField.setPromptText(getSearchPromptText());
        studentSelectLabel.setText("选择学生");
        itemTypeLabel.setText(getItemTypeLabelText());
        titleFieldLabel.setText(getTitleLabelText());
        levelLabel.setText(getLevelLabelText());
        organizationLabel.setText(getOrganizationLabelText());
        startDateLabel.setText(getStartDateLabelText());
        endDateLabel.setText(getEndDateLabelText());
        if (placeLabel != null) {
            placeLabel.setText(getPlaceLabelText());
        }
        resultLabel.setText(getResultLabelText());
        descriptionLabel.setText(getDescriptionLabelText());

        itemTypeColumn.setText(getItemTypeLabelText());
        titleColumn.setText(getTitleLabelText());
        levelColumn.setText(getLevelLabelText());
        organizationColumn.setText(getOrganizationLabelText());
        dateRangeColumn.setText("时间");
        resultColumn.setText(getResultLabelText());
    }

    protected void configureTableColumns() {
        studentNumColumn.setCellValueFactory(new MapValueFactory<>("studentNum"));
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        itemTypeColumn.setCellValueFactory(new MapValueFactory<>("itemType"));
        titleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        levelColumn.setCellValueFactory(new MapValueFactory<>("level"));
        dateRangeColumn.setCellValueFactory(new MapValueFactory<>("dateRange"));
        organizationColumn.setCellValueFactory(new MapValueFactory<>("organization"));
        resultColumn.setCellValueFactory(new MapValueFactory<>("result"));
    }

    protected void configureDatePickers() {
        LocalDateStringConverter converter = new LocalDateStringConverter("yyyy-MM-dd");
        startDatePicker.setConverter(converter);
        endDatePicker.setConverter(converter);
    }

    protected void configureRoleView() {
        boolean isStudent = "ROLE_STUDENT".equals(roleName);
        boolean isAdmin = "ROLE_ADMIN".equals(roleName);
        studentNumField.setDisable(true);
        studentNameField.setDisable(true);
        setVisibleManaged(studentSelectLabel, !isStudent);
        setVisibleManaged(studentComboBox, !isStudent);
        setVisibleManaged(typeManageBox, isAdmin);
        setVisibleManaged(placeLabel, supportsPlaceField());
        setVisibleManaged(placeField, supportsPlaceField());
        setVisibleManaged(addButton, false);
        if (saveButton != null) {
            saveButton.setText("新增");
        }
        setVisibleManaged(typeAddButton, false);
        if (typeSaveButton != null) {
            typeSaveButton.setText("新增");
            typeSaveButton.setMinWidth(72.0);
        }
        if (typeDeleteButton != null) {
            typeDeleteButton.setText("删除");
            typeDeleteButton.setMinWidth(72.0);
        }
        if (typeManageField != null) {
            typeManageField.setPrefWidth(120.0);
        }
    }

    protected void loadStudentOptions() {
        DataRequest request = new DataRequest();
        List<OptionItem> optionList = HttpRequestUtil.requestOptionItemList("/api/studentGrowth/getStudentItemOptionList", request);
        studentOptionList = optionList == null ? new ArrayList<>() : optionList;
        studentComboBox.getItems().setAll(studentOptionList);
        if (!studentOptionList.isEmpty()) {
            studentComboBox.getSelectionModel().selectFirst();
            syncStudentFields(studentOptionList.getFirst());
        } else {
            studentNumField.clear();
            studentNameField.clear();
        }
    }

    protected void loadTypeOptions(Integer preferredRecordTypeId, Integer preferredManageTypeId) {
        DataRequest request = new DataRequest();
        request.add("category", getCategoryCode());
        List<OptionItem> optionList = HttpRequestUtil.requestOptionItemList("/api/studentGrowth/getTypeOptionList", request);
        itemTypeOptionList = optionList == null ? new ArrayList<>() : optionList;

        itemTypeComboBox.getItems().setAll(itemTypeOptionList);
        selectOptionItem(itemTypeComboBox, itemTypeOptionList, preferredRecordTypeId, true);

        if (typeManageComboBox != null) {
            typeManageComboBox.getItems().setAll(itemTypeOptionList);
            selectOptionItem(typeManageComboBox, itemTypeOptionList, preferredManageTypeId, false);
            if (preferredManageTypeId == null) {
                typeManageComboBox.getSelectionModel().clearSelection();
            }
            syncTypeManageField();
        }
    }

    protected void selectOptionItem(ComboBox<OptionItem> comboBox, List<OptionItem> optionList, Integer optionId, boolean selectFirstIfMissing) {
        if (comboBox == null) {
            return;
        }
        if (optionId != null) {
            int index = CommonMethod.getOptionItemIndexById(optionList, optionId);
            if (index >= 0) {
                comboBox.getSelectionModel().select(index);
                return;
            }
        }
        if (selectFirstIfMissing && !optionList.isEmpty()) {
            comboBox.getSelectionModel().selectFirst();
        } else {
            comboBox.getSelectionModel().clearSelection();
        }
    }

    protected void syncStudentFieldsFromSelection() {
        syncStudentFields(studentComboBox.getSelectionModel().getSelectedItem());
    }

    protected void syncStudentFields(OptionItem optionItem) {
        if (optionItem == null || optionItem.getTitle() == null) {
            studentNumField.clear();
            studentNameField.clear();
            return;
        }
        String title = optionItem.getTitle();
        int index = title.indexOf('-');
        if (index > 0) {
            studentNumField.setText(title.substring(0, index));
            studentNameField.setText(title.substring(index + 1));
        } else {
            studentNumField.setText(title);
            studentNameField.setText(title);
        }
    }

    protected void syncTypeManageField() {
        if (typeManageField == null || typeManageComboBox == null) {
            return;
        }
        OptionItem optionItem = typeManageComboBox.getSelectionModel().getSelectedItem();
        typeManageField.setText(optionItem == null ? "" : optionItem.getTitle());
    }

    protected void setTableViewData() {
        observableList.clear();
        observableList.addAll(growthRecordList);
        dataTableView.setItems(observableList);
    }

    protected void clearPanel() {
        recordId = null;
        if (!itemTypeOptionList.isEmpty()) {
            if (itemTypeComboBox.getSelectionModel().getSelectedItem() == null) {
                itemTypeComboBox.getSelectionModel().selectFirst();
            }
        } else {
            itemTypeComboBox.getSelectionModel().clearSelection();
        }
        titleField.clear();
        levelField.clear();
        organizationField.clear();
        clearDatePicker(startDatePicker);
        clearDatePicker(endDatePicker);
        clearTextField(placeField);
        resultField.clear();
        descriptionArea.clear();
        if (!studentOptionList.isEmpty()) {
            if (studentComboBox.getSelectionModel().getSelectedItem() == null) {
                studentComboBox.getSelectionModel().selectFirst();
            }
            syncStudentFields(studentComboBox.getSelectionModel().getSelectedItem());
        } else {
            studentNumField.clear();
            studentNameField.clear();
        }
    }

    protected void fillFormFromSelection(Map<String, Object> selected) {
        if (selected == null) {
            clearPanel();
            return;
        }
        recordId = CommonMethod.getInteger(selected, "recordId");
        Integer itemTypeId = CommonMethod.getInteger(selected, "itemTypeId");
        selectOptionItem(itemTypeComboBox, itemTypeOptionList, itemTypeId, false);
        titleField.setText(CommonMethod.getString(selected, "title"));
        levelField.setText(CommonMethod.getString(selected, "level"));
        organizationField.setText(CommonMethod.getString(selected, "organization"));
        setDatePickerText(startDatePicker, CommonMethod.getString(selected, "startDate"));
        setDatePickerText(endDatePicker, CommonMethod.getString(selected, "endDate"));
        if (placeField != null) {
            placeField.setText(CommonMethod.getString(selected, "place"));
        }
        resultField.setText(CommonMethod.getString(selected, "result"));
        descriptionArea.setText(CommonMethod.getString(selected, "description"));
        Integer studentId = CommonMethod.getInteger(selected, "studentId");
        if (studentId != null) {
            selectOptionItem(studentComboBox, studentOptionList, studentId, false);
        }
        studentNumField.setText(CommonMethod.getString(selected, "studentNum"));
        studentNameField.setText(CommonMethod.getString(selected, "studentName"));
    }

    @FXML
    protected void onQueryButtonClick() {
        DataRequest request = new DataRequest();
        request.add("category", getCategoryCode());
        request.add("search", getText(searchTextField));
        DataResponse response = HttpRequestUtil.request("/api/studentGrowth/getGrowthRecordList", request);
        if (response != null && response.getCode() == 0) {
            growthRecordList = (ArrayList<Map>) response.getData();
            if (growthRecordList == null) {
                growthRecordList = new ArrayList<>();
            }
            setTableViewData();
            dataTableView.getSelectionModel().clearSelection();
            clearPanel();
        } else {
            MessageDialog.showDialog(response == null ? "记录查询失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onAddButtonClick() {
        dataTableView.getSelectionModel().clearSelection();
        clearPanel();
        titleField.requestFocus();
    }

    @FXML
    protected void onSaveButtonClick() {
        saveRecord();
    }

    @FXML
    protected void onDeleteButtonClick() {
        deleteRecord();
    }

    @FXML
    protected void onTypeAddButtonClick() {
        if (typeManageComboBox != null) {
            typeManageComboBox.getSelectionModel().clearSelection();
        }
        if (typeManageField != null) {
            typeManageField.clear();
            typeManageField.requestFocus();
        }
    }

    @FXML
    protected void onTypeSaveButtonClick() {
        String typeName = typeManageField == null ? "" : typeManageField.getText() == null ? "" : typeManageField.getText().trim();
        if (typeName.isEmpty()) {
            MessageDialog.showDialog("类型名称不能为空。");
            return;
        }
        Integer typeId = null;
        if (typeManageComboBox != null && typeManageComboBox.getSelectionModel().getSelectedItem() != null) {
            typeId = typeManageComboBox.getSelectionModel().getSelectedItem().getId();
        }
        DataRequest request = new DataRequest();
        request.add("category", getCategoryCode());
        request.add("typeId", typeId);
        request.add("typeName", typeName);
        DataResponse response = HttpRequestUtil.request("/api/studentGrowth/saveType", request);
        if (response != null && response.getCode() == 0) {
            Integer savedTypeId = CommonMethod.getIntegerFromObject(response.getData());
            loadTypeOptions(savedTypeId, savedTypeId);
            MessageDialog.showDialog("类型保存成功。");
        } else {
            MessageDialog.showDialog(response == null ? "类型保存失败。" : response.getMsg());
        }
    }

    @FXML
    protected void onTypeDeleteButtonClick() {
        OptionItem selected = typeManageComboBox == null ? null : typeManageComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请先选择一个类型。");
            return;
        }
        int ret = MessageDialog.choiceDialog("确认要删除当前类型吗？");
        if (ret != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("category", getCategoryCode());
        request.add("typeId", selected.getId());
        DataResponse response = HttpRequestUtil.request("/api/studentGrowth/deleteType", request);
        if (response != null && response.getCode() == 0) {
            loadTypeOptions(null, null);
            MessageDialog.showDialog("类型已删除。");
        } else {
            MessageDialog.showDialog(response == null ? "类型删除失败。" : response.getMsg());
        }
    }

    protected void saveRecord() {
        Integer studentId = getSelectedStudentId();
        if (studentId == null) {
            MessageDialog.showDialog("请选择学生。");
            return;
        }
        OptionItem selectedType = itemTypeComboBox.getSelectionModel().getSelectedItem();
        if (selectedType == null) {
            MessageDialog.showDialog("请选择" + getItemTypeLabelText() + "。");
            return;
        }
        if (getText(titleField).isEmpty()) {
            MessageDialog.showDialog(getTitleLabelText() + "不能为空。");
            return;
        }
        String startDate = getDateText(startDatePicker);
        String endDate = getDateText(endDatePicker);
        String dateError = validateDateRange(startDate, endDate);
        if (dateError != null) {
            MessageDialog.showDialog(dateError);
            return;
        }

        Map<String, Object> form = new HashMap<>();
        form.put("itemTypeId", selectedType.getId());
        form.put("title", getText(titleField));
        form.put("level", getText(levelField));
        form.put("organization", getText(organizationField));
        form.put("startDate", startDate);
        form.put("endDate", endDate);
        form.put("place", getText(placeField));
        form.put("result", getText(resultField));
        form.put("description", getText(descriptionArea));

        DataRequest request = new DataRequest();
        request.add("category", getCategoryCode());
        request.add("recordId", recordId);
        request.add("studentId", studentId);
        request.add("form", form);
        DataResponse response = HttpRequestUtil.request("/api/studentGrowth/saveGrowthRecord", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("记录新增成功。");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "记录新增失败。" : response.getMsg());
        }
    }

    protected void deleteRecord() {
        if (recordId == null) {
            MessageDialog.showDialog("请先选择一条记录。");
            return;
        }
        int ret = MessageDialog.choiceDialog("确认要删除当前记录吗？");
        if (ret != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("category", getCategoryCode());
        request.add("recordId", recordId);
        DataResponse response = HttpRequestUtil.request("/api/studentGrowth/deleteGrowthRecord", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("记录已删除。");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(response == null ? "记录删除失败。" : response.getMsg());
        }
    }

    protected Integer getSelectedStudentId() {
        OptionItem selected = studentComboBox.getSelectionModel().getSelectedItem();
        return selected == null ? null : CommonMethod.getIntegerFromObject(selected.getValue());
    }

    protected void clearDatePicker(DatePicker datePicker) {
        if (datePicker == null) {
            return;
        }
        datePicker.setValue(null);
        datePicker.getEditor().clear();
    }

    protected void setDatePickerText(DatePicker datePicker, String value) {
        if (datePicker == null) {
            return;
        }
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            clearDatePicker(datePicker);
            return;
        }
        try {
            datePicker.setValue(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            datePicker.setValue(null);
            datePicker.getEditor().setText(text);
        }
    }

    protected String getDateText(DatePicker datePicker) {
        if (datePicker == null || datePicker.getEditor() == null || datePicker.getEditor().getText() == null) {
            return "";
        }
        return datePicker.getEditor().getText().trim();
    }

    protected String validateDateRange(String startDate, String endDate) {
        try {
            LocalDate start = startDate.isEmpty() ? null : LocalDate.parse(startDate);
            LocalDate end = endDate.isEmpty() ? null : LocalDate.parse(endDate);
            if (start != null && end != null && start.isAfter(end)) {
                return "开始日期不能晚于结束日期。";
            }
            return null;
        } catch (DateTimeParseException e) {
            return "日期格式不正确，请使用 yyyy-MM-dd。";
        }
    }

    protected String getText(TextField textField) {
        if (textField == null || textField.getText() == null) {
            return "";
        }
        return textField.getText().trim();
    }

    protected String getText(TextArea textArea) {
        if (textArea == null || textArea.getText() == null) {
            return "";
        }
        return textArea.getText().trim();
    }

    protected void clearTextField(TextField textField) {
        if (textField != null) {
            textField.clear();
        }
    }

    protected void setVisibleManaged(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    @Override
    public void doNew() {
        onAddButtonClick();
    }

    @Override
    public void doSave() {
        onSaveButtonClick();
    }

    @Override
    public void doDelete() {
        onDeleteButtonClick();
    }

    @Override
    public void doRefresh() {
        onQueryButtonClick();
    }
}
