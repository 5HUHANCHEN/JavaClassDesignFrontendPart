package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.LoginRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class LoginController {
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_ADMIN_ACCOUNT = "18871031";
    private static final String DEFAULT_STUDENT_ACCOUNT = "2022030001";
    private static final String DEFAULT_TEACHER_ACCOUNT = "022200";

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        rootPane.setStyle(
                "-fx-background-image: url('shanda1.jpg');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;" +
                        "-fx-background-repeat: no-repeat;"
        );
    }

    @FXML
    protected void onAdminLoginButtonClick() {
        loginWithPreferredAccount(DEFAULT_ADMIN_ACCOUNT);
    }

    @FXML
    protected void onStudentLoginButtonClick() {
        loginWithPreferredAccount(DEFAULT_STUDENT_ACCOUNT);
    }

    @FXML
    protected void onTeacherLoginButtonClick() {
        loginWithPreferredAccount(DEFAULT_TEACHER_ACCOUNT);
    }

    @FXML
    protected void onStudentRegisterButtonClick() {
        openScene("base/student-register.fxml", "css/login-view.css", "学生注册");
    }

    @FXML
    protected void onTeacherApplyButtonClick() {
        openApplyRegisterScene("TEACHER", "教师申请注册");
    }

    @FXML
    protected void onAdminApplyButtonClick() {
        openApplyRegisterScene("ADMIN", "管理员申请注册");
    }

    private void loginWithPreferredAccount(String fallbackUsername) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();
        if (username.isEmpty()) {
            username = fallbackUsername;
        }
        if (password.isEmpty()) {
            password = DEFAULT_PASSWORD;
        }
        doLogin(username, password);
    }

    private void doLogin(String username, String password) {
        String message = HttpRequestUtil.login(new LoginRequest(username, password));
        if (message != null) {
            MessageDialog.showDialog(message);
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/main-frame.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), -1, -1);
            AppStore.setMainFrameController(fxmlLoader.getController());
            MainApplication.resetStage("教学综合 管理系统", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openApplyRegisterScene(String role, String title) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/apply-register.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 1280, 760);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("css/apply-register.css").toExternalForm()
            );
            ApplyRegisterController controller = fxmlLoader.getController();
            controller.setApplicationRole(role);
            MainApplication.loginStage(title, scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openScene(String fxmlPath, String cssPath, String title) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(fxmlPath));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 1280, 760);
            scene.getStylesheets().add(
                    MainApplication.class.getResource(cssPath).toExternalForm()
            );
            MainApplication.loginStage(title, scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
