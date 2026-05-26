package com.teach.javafx.controller;

import com.teach.javafx.controller.base.LocalDateStringConverter;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.util.CommonMethod;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class StudentSelfController {
    @FXML private BorderPane rootPane;
    @FXML private Label numLabel;
    @FXML private Label nameLabel;
    @FXML private Label deptLabel;
    @FXML private Label majorLabel;
    @FXML private Label classLabel;
    @FXML private Label statusLabel;
    @FXML private TextField numField;
    @FXML private TextField nameField;
    @FXML private TextField genderField;
    @FXML private DatePicker birthdayPick;
    @FXML private TextField deptField;
    @FXML private TextField majorField;
    @FXML private TextField classNameField;
    @FXML private TextField cardField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressArea;
    @FXML private Button photoButton;
    @FXML private ImageView photoImageView;

    private Integer personId;
    private String genderValue = "";
    private String birthdayValue = "";

    @FXML
    public void initialize() {
        loadCss();
        birthdayPick.setConverter(new LocalDateStringConverter("yyyy-MM-dd"));
        loadMyInfo();
    }

    private void loadCss() {
        URL cssUrl = getClass().getResource("/com/teach/javafx/css/student-self-panel.css");
        if (cssUrl != null) {
            rootPane.getStylesheets().clear();
            rootPane.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    private void loadMyInfo() {
        DataResponse infoResponse = HttpRequestUtil.request("/api/student/getStudentInfo", new DataRequest());
        if (infoResponse == null || infoResponse.getCode() != 0 || !(infoResponse.getData() instanceof Map<?, ?> rawMap)) {
            MessageDialog.showDialog(infoResponse == null ? "加载个人信息失败，请检查服务器连接。" : infoResponse.getMsg());
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> form = (Map<String, Object>) rawMap;
        personId = CommonMethod.getInteger(form, "personId");
        genderValue = CommonMethod.getString(form, "gender");
        birthdayValue = CommonMethod.getString(form, "birthday");

        String num = CommonMethod.getString(form, "num");
        String name = CommonMethod.getString(form, "name");
        String dept = CommonMethod.getString(form, "dept");
        String major = CommonMethod.getString(form, "major");
        String className = CommonMethod.getString(form, "className");

        numLabel.setText(emptyToDefault(num));
        nameLabel.setText(emptyToDefault(name));
        deptLabel.setText(emptyToDefault(dept));
        majorLabel.setText(emptyToDefault(major));
        classLabel.setText(emptyToDefault(className));

        numField.setText(num);
        nameField.setText(name);
        genderField.setText(CommonMethod.getString(form, "genderName"));
        birthdayPick.getEditor().setText(birthdayValue);
        deptField.setText(dept);
        majorField.setText(major);
        classNameField.setText(className);
        cardField.setText(CommonMethod.getString(form, "card"));
        emailField.setText(CommonMethod.getString(form, "email"));
        phoneField.setText(CommonMethod.getString(form, "phone"));
        addressArea.setText(CommonMethod.getString(form, "address"));
        statusLabel.setText("当前仅允许修改联系方式和头像。");

        displayPhoto();
    }

    @FXML
    protected void onSaveButtonClick() {
        if (personId == null) {
            MessageDialog.showDialog("未获取到学生编号，无法保存信息。");
            return;
        }
        Map<String, Object> form = new HashMap<>();
        form.put("num", getText(numField));
        form.put("name", getText(nameField));
        form.put("gender", genderValue);
        form.put("birthday", birthdayValue);
        form.put("dept", getText(deptField));
        form.put("major", getText(majorField));
        form.put("className", getText(classNameField));
        form.put("card", getText(cardField));
        form.put("email", getText(emailField));
        form.put("phone", getText(phoneField));
        form.put("address", getText(addressArea));

        DataRequest request = new DataRequest();
        request.add("personId", personId);
        request.add("form", form);

        DataResponse response = HttpRequestUtil.request("/api/student/studentEditSave", request);
        if (response != null && response.getCode() == 0) {
            statusLabel.setText("保存成功。最新资料已同步到服务器。");
            MessageDialog.showDialog("个人信息保存成功。");
            loadMyInfo();
        } else {
            statusLabel.setText("保存失败，请检查输入内容或稍后重试。");
            MessageDialog.showDialog(response == null ? "个人信息保存失败，请检查服务器连接。" : response.getMsg());
        }
    }

    private void displayPhoto() {
        if (personId == null) {
            photoImageView.setImage(null);
            return;
        }
        DataRequest request = new DataRequest();
        request.add("personId", personId);
        byte[] photoBytes = HttpRequestUtil.requestByteData("/api/base/getBlobByteData", request);
        if (photoBytes != null && photoBytes.length > 0) {
            photoImageView.setImage(new Image(new ByteArrayInputStream(photoBytes)));
        } else {
            photoImageView.setImage(null);
        }
    }

    @FXML
    public void onPhotoButtonClick() {
        if (personId == null) {
            MessageDialog.showDialog("未获取到学生编号，无法上传头像。");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择头像图片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file == null) {
            return;
        }

        DataResponse response = HttpRequestUtil.uploadPhotoBlob(file.getPath(), personId);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("头像上传成功。");
            displayPhoto();
        } else {
            MessageDialog.showDialog(response == null ? "头像上传失败，请检查服务器连接。" : response.getMsg());
        }
    }

    private String getText(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    private String getText(TextArea textArea) {
        return textArea.getText() == null ? "" : textArea.getText().trim();
    }

    private String emptyToDefault(String value) {
        return value == null || value.isBlank() ? "暂无信息" : value;
    }
}
