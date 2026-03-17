package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.LoginRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * LoginController 登录交互控制类 对应 base/login-view.fxml
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    private VBox vbox;

    @FXML
    private StackPane rootPane;

    /**
     * 页面加载完成后的初始化
     */
    @FXML
    public void initialize() {
        rootPane.setStyle(
                "-fx-background-image: url('shanda1.jpg');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;" +
                        "-fx-background-repeat: no-repeat;"
        );
    }

    /**
     * 管理员登录
     */
    @FXML
    protected void onAdminLoginButtonClick() {
        onLoginButtonClick("admin");
    }

    @FXML
    protected void onLoginButtonClick() {
        String username = usernameField.getText();
        String password = passwordField.getText();

    }

    /**
     * 学生登录
     */
    @FXML
    protected void onStudentLoginButtonClick() {
        onLoginButtonClick("2022030001");
    }

    /**
     * 教师登录
     */
    @FXML
//    protected void onTeacherLoginButtonClick() {
//        onLoginButtonClick("200799013517");
//    }
    protected void onTeacherLoginButtonClick() {
        onLoginButtonClick("022200");
    }

    /**
     * 登录处理
     */
    protected void onLoginButtonClick(String username) {
        LoginRequest loginRequest = new LoginRequest(username, "123456");
        String msg = HttpRequestUtil.login(loginRequest);
        if (msg != null) {
            MessageDialog.showDialog(msg);
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/main-frame.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), -1, -1);
            AppStore.setMainFrameController((MainFrameController) fxmlLoader.getController());
            MainApplication.resetStage("教学管理系统", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    protected void onStudentRegisterButtonClick() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/student-register.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 1280, 760);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("css/login-view.css").toExternalForm()
            );
            MainApplication.loginStage("学生注册", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    protected void onApplyRegisterButtonClick() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/apply-register.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 1280, 760);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("css/apply-register.css").toExternalForm()
            );
            MainApplication.loginStage("账号申请", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}