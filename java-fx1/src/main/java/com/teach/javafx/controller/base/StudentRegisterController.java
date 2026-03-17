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
        String username = usernameField.getText();
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username == null || username.trim().isEmpty()) {
            MessageDialog.showDialog("请输入学号！");
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            MessageDialog.showDialog("请输入姓名！");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            MessageDialog.showDialog("请输入密码！");
            return;
        }
        if (!password.equals(confirmPassword)) {
            MessageDialog.showDialog("两次输入的密码不一致！");
            return;
        }

        DataRequest req = new DataRequest();
        req.add("username", username.trim());
        req.add("perName", name.trim());
        req.add("email", email == null ? "" : email.trim());
        req.add("password", password.trim());
        req.add("role", "STUDENT");

        DataResponse res = HttpRequestUtil.request("/auth/registerUser", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("注册成功！请返回登录。");
        } else {
            MessageDialog.showDialog(res == null ? "注册失败！" : res.getMsg());
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
}