package com.teach.javafx.controller.base;

import com.teach.javafx.MainApplication;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class StudentRegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    protected void onRegisterButtonClick() {
        String username = getText(usernameField);
        String name = getText(nameField);
        String email = getText(emailField);
        String password = getText(passwordField);
        String confirmPassword = getText(confirmPasswordField);

        if (username.isEmpty()) {
            MessageDialog.showDialog("请输入学生学号。")
;            return;
        }
        if (name.isEmpty()) {
            MessageDialog.showDialog("请输入学生姓名。");
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

        DataRequest request = new DataRequest();
        request.add("username", username);
        request.add("perName", name);
        request.add("email", email);
        request.add("password", password);
        request.add("role", "STUDENT");

        DataResponse response = HttpRequestUtil.request("/auth/registerUser", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("学生账号注册成功，请返回登录页面进行登录。");
            clearForm();
        } else {
            MessageDialog.showDialog(response == null ? "学生注册失败，请稍后重试。" : response.getMsg());
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

    private String getText(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    private String getText(PasswordField passwordField) {
        return passwordField.getText() == null ? "" : passwordField.getText().trim();
    }

    private void clearForm() {
        usernameField.clear();
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}