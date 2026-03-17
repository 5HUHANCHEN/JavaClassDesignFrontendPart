package com.teach.javafx.controller;

import javafx.scene.layout.BorderPane;
import java.net.URL;

import com.teach.javafx.AppStore;
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
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StudentSelfController {
    @FXML
    private BorderPane rootPane;
    @FXML
    private Label numLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label deptLabel;
    @FXML
    private Label majorLabel;
    @FXML
    private Label classLabel;
    @FXML
    private Label statusLabel;

    @FXML
    private TextField numField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField genderField;
    @FXML
    private DatePicker birthdayPick;
    @FXML
    private TextField deptField;
    @FXML
    private TextField majorField;
    @FXML
    private TextField classNameField;
    @FXML
    private TextField cardField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextArea addressArea;

    @FXML
    private Button photoButton;
    @FXML
    private ImageView photoImageView;

    private Integer personId;

    @FXML
    public void initialize() {
        loadCss();
        birthdayPick.setConverter(new LocalDateStringConverter("yyyy-MM-dd"));
        loadMyInfo();
    }

    private void loadCss() {
        URL cssUrl = getClass().getResource("/com/teach/javafx/css/student-self-panel.css");
        System.out.println("student-self cssUrl = " + cssUrl);
        if (cssUrl != null) {
            rootPane.getStylesheets().clear();
            rootPane.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    private void loadMyInfo() {
        String loginUser = AppStore.getJwt().getUsername();

        DataRequest req = new DataRequest();
        req.add("numName", loginUser);

        DataResponse res = HttpRequestUtil.request("/api/student/getStudentList", req);
        if (res == null || res.getCode() != 0 || res.getData() == null) {
            MessageDialog.showDialog("加载个人信息失败！");
            return;
        }

        java.util.List<Map> list = (java.util.List<Map>) res.getData();
        if (list.isEmpty()) {
            MessageDialog.showDialog("未找到当前学生信息！");
            return;
        }

        Map first = list.get(0);
        personId = CommonMethod.getInteger(first, "personId");

        DataRequest infoReq = new DataRequest();
        infoReq.add("personId", personId);
        DataResponse infoRes = HttpRequestUtil.request("/api/student/getStudentInfo", infoReq);
        if (infoRes == null || infoRes.getCode() != 0) {
            MessageDialog.showDialog("加载详细信息失败！");
            return;
        }

        Map form = (Map) infoRes.getData();

        String num = CommonMethod.getString(form, "num");
        String name = CommonMethod.getString(form, "name");
        String dept = CommonMethod.getString(form, "dept");
        String major = CommonMethod.getString(form, "major");
        String className = CommonMethod.getString(form, "className");

        numLabel.setText(num);
        nameLabel.setText(name);
        deptLabel.setText(dept);
        majorLabel.setText(major);
        classLabel.setText(className);

        numField.setText(num);
        nameField.setText(name);
        genderField.setText(CommonMethod.getString(form, "genderName"));
        birthdayPick.getEditor().setText(CommonMethod.getString(form, "birthday"));
        deptField.setText(dept);
        majorField.setText(major);
        classNameField.setText(className);
        cardField.setText(CommonMethod.getString(form, "card"));
        emailField.setText(CommonMethod.getString(form, "email"));
        phoneField.setText(CommonMethod.getString(form, "phone"));
        addressArea.setText(CommonMethod.getString(form, "address"));

        displayPhoto();
    }

    @FXML
    protected void onSaveButtonClick() {
        String loginUser = AppStore.getJwt().getUsername();

        if (!loginUser.equals(numField.getText())) {
            MessageDialog.showDialog("只能修改自己的信息！");
            return;
        }

        Map<String, Object> form = new HashMap<>();
        form.put("num", numField.getText());
        form.put("name", nameField.getText());
        form.put("dept", deptField.getText());
        form.put("major", majorField.getText());
        form.put("className", classNameField.getText());
        form.put("card", cardField.getText());
        form.put("birthday", birthdayPick.getEditor().getText());

        // 只允许学生修改这些字段
        form.put("email", emailField.getText());
        form.put("phone", phoneField.getText());
        form.put("address", addressArea.getText());

        DataRequest req = new DataRequest();
        req.add("personId", personId);
        req.add("form", form);

        DataResponse res = HttpRequestUtil.request("/api/student/studentEditSave", req);
        if (res != null && res.getCode() == 0) {
            statusLabel.setText("保存成功！");
            MessageDialog.showDialog("保存成功！");
        } else {
            statusLabel.setText("保存失败！");
            MessageDialog.showDialog(res == null ? "保存失败！" : res.getMsg());
        }
    }

    public void displayPhoto() {
        if (personId == null) {
            return;
        }
        DataRequest req = new DataRequest();
        req.add("personId", personId + "");
        byte[] bytes = HttpRequestUtil.requestByteData("/api/base/getBlobByteData", req);
        if (bytes != null) {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            Image img = new Image(in);
            photoImageView.setImage(img);
        }
    }

    @FXML
    public void onPhotoButtonClick() {
        if (personId == null) {
            return;
        }

        FileChooser fileDialog = new FileChooser();
        fileDialog.setTitle("图片上传");
        fileDialog.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPG 文件", "*.jpg")
        );

        File file = fileDialog.showOpenDialog(null);
        if (file == null) {
            return;
        }

        DataResponse res = HttpRequestUtil.uploadFile(
                "/api/base/uploadPhotoBlob",
                file.getPath(),
                personId + ""
        );

        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("上传成功！");
            displayPhoto();
        } else {
            MessageDialog.showDialog(res == null ? "上传失败！" : res.getMsg());
        }
    }
}