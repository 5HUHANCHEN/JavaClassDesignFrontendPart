package com.teach.javafx.controller.base;

import com.teach.javafx.MainApplication;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ApplyRegisterController {

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
    private ComboBox<String> roleComboBox;

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList("TEACHER", "ADMIN"));
    }

    @FXML
    protected void onSubmitButtonClick() {
        String username = usernameField.getText();
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        if (username == null || username.trim().isEmpty()) {
            MessageDialog.showDialog("请输入账号！");
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
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            MessageDialog.showDialog("请再次输入密码！");
            return;
        }
        if (!password.equals(confirmPassword)) {
            MessageDialog.showDialog("两次输入的密码不一致！");
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            MessageDialog.showDialog("请输入邮箱！");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            MessageDialog.showDialog("邮箱格式不正确！");
            return;
        }
        if (role == null || role.trim().isEmpty()) {
            MessageDialog.showDialog("请选择申请角色！");
            return;
        }
        if ("STUDENT".equals(role)) {
            MessageDialog.showDialog("学生请直接注册，无需申请！");
            return;
        }

        DataRequest req = new DataRequest();
        req.add("username", username.trim());
        req.add("name", name.trim());
        req.add("email", email == null ? "" : email.trim());
        req.add("password", password.trim());
        req.add("role", role);

        DataResponse res = HttpRequestUtil.request("/auth/applyRegister", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("申请提交成功！等待管理员审核。");
        } else {
            MessageDialog.showDialog(res == null ? "申请提交失败！" : res.getMsg());
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