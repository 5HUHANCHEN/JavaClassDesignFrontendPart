package com.teach.javafx.util;

import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToastUtil {

    /**
     * 显示带有旋转动画的成功提示
     * @param message 需要显示的文本内容
     */
    public static void showSuccess(String message) {
        Stage toastStage = new Stage();
        // 设置窗口透明且无边框
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        // 创建一个用于旋转的图标（这里使用 Unicode 字符作为示意，也可替换为 ImageView）
        Text icon = new Text("↻");
        icon.setFont(Font.font(20));
        icon.setFill(Color.web("#4CAF50")); // 绿色成功图标

        // 设置旋转动画，每次旋转花费 800 毫秒
        RotateTransition rt = new RotateTransition(Duration.millis(800), icon);
        rt.setByAngle(360);
        rt.setCycleCount(RotateTransition.INDEFINITE); // 循环旋转直到弹窗关闭
        rt.play();

        // 提示文本
        Label label = new Label(message);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 布局设置
        HBox root = new HBox(12);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10, 25, 10, 25));
        // 半透明黑色背景，圆角
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-background-radius: 8px;");
        root.getChildren().addAll(icon, label);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        toastStage.show();

        // 2秒后自动关闭提示框并清理动画
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), ae -> {
            rt.stop();
            toastStage.close();
        }));
        timeline.play();
    }
    /**
     * 显示错误/警告提示（无旋转动画，红色图标）
     * @param message 需要显示的文本内容
     */
    public static void showError(String message) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        // 使用红色的 ✖ 作为错误图标
        Text icon = new Text("✖");
        icon.setFont(Font.font(20));
        icon.setFill(Color.web("#F44336"));

        Label label = new Label(message);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox root = new HBox(12);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10, 25, 10, 25));
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-background-radius: 8px;");
        root.getChildren().addAll(icon, label);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        toastStage.show();

        // 2秒后自动关闭提示框
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), ae -> {
            toastStage.close();
        }));
        timeline.play();
    }
}