package com.teach.javafx.controller.base;

import com.teach.javafx.MainApplication;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ApplyRegisterController {
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageHintLabel;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField deptField;
    @FXML
    private TextField majorField;
    @FXML
    private TextField classNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextArea reasonTextArea;

    private String applicationRole = "TEACHER";

    @FXML
    public void initialize() {
        updatePageText();
    }

    public void setApplicationRole(String applicationRole) {
        this.applicationRole = applicationRole == null ? "TEACHER" : applicationRole;
        updatePageText();
    }

    @FXML
    protected void onSubmitButtonClick() {
        String username = getText(usernameField);
        String name = getText(nameField);
        String email = getText(emailField);
        String dept = getText(deptField);
        String major = getText(majorField);
        String className = getText(classNameField);
        String phone = getText(phoneField);
        String password = getText(passwordField);
        String confirmPassword = getText(confirmPasswordField);
        String reason = getText(reasonTextArea);

        if (username.isEmpty()) {
            MessageDialog.showDialog("请输入申请账号。");
            return;
        }
        if (name.isEmpty()) {
            MessageDialog.showDialog("请输入申请人姓名。");
            return;
        }
        if (email.isEmpty()) {
            MessageDialog.showDialog("请输入电子邮箱。");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            MessageDialog.showDialog("电子邮箱格式不正确。");
            return;
        }
        if (dept.isEmpty()) {
            MessageDialog.showDialog("请输入所在院系。");
            return;
        }
        if (major.isEmpty()) {
            MessageDialog.showDialog("请输入专业信息。");
            return;
        }
        if (className.isEmpty()) {
            MessageDialog.showDialog("请输入班级或岗位信息。");
            return;
        }
        if (phone.isEmpty()) {
            MessageDialog.showDialog("请输入联系电话。");
            return;
        }
        if (password.isEmpty()) {
            MessageDialog.showDialog("请输入登录密码。");
            return;
        }
        if (confirmPassword.isEmpty()) {
            MessageDialog.showDialog("请再次输入登录密码。");
            return;
        }
        if (!password.equals(confirmPassword)) {
            MessageDialog.showDialog("两次输入的密码不一致。");
            return;
        }
        if (reason.isEmpty()) {
            MessageDialog.showDialog("请输入申请说明。");
            return;
        }

        DataRequest request = new DataRequest();
        request.add("username", username);
        request.add("name", name);
        request.add("email", email);
        request.add("dept", dept);
        request.add("major", major);
        request.add("className", className);
        request.add("phone", phone);
        request.add("password", password);
        request.add("reason", reason);
        request.add("role", applicationRole);

        DataResponse response = HttpRequestUtil.request("/auth/applyRegister", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog(getRoleName() + "注册申请已提交，请等待管理员审批。");
            clearForm();
        } else {
            MessageDialog.showDialog(response == null ? "注册申请提交失败，请稍后重试。" : response.getMsg());
        }
    }

    @FXML
    protected void onBackButtonClick() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 1280, 760);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("css/login-view.css").toExternalForm()
            );
            MainApplication.loginStage("登录", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updatePageText() {
        if (pageTitleLabel == null || pageHintLabel == null) {
            return;
        }
        pageTitleLabel.setText(getRoleName() + "注册申请");
        pageHintLabel.setText("请填写完整的申请资料，提交后由管理员统一审核。\n学习测试环境可直接使用默认密码体验后续登录。\n");
    }

    private String getRoleName() {
        return "ADMIN".equals(applicationRole) ? "管理员" : "教师";
    }

    private String getText(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    private String getText(PasswordField passwordField) {
        return passwordField.getText() == null ? "" : passwordField.getText().trim();
    }

    private String getText(TextArea textArea) {
        return textArea.getText() == null ? "" : textArea.getText().trim();
    }

    private void clearForm() {
        usernameField.clear();
        nameField.clear();
        emailField.clear();
        deptField.clear();
        majorField.clear();
        classNameField.clear();
        phoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        reasonTextArea.clear();
    }
}