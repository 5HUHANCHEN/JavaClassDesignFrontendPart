package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class AiAssistantController extends ToolController {
    @FXML
    private TextArea questionArea;
    @FXML
    private TextArea answerArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Button askButton;

    @FXML
    public void initialize() {
        statusLabel.setText("请输入想查询的问题，智能助手会结合系统业务数据进行回答。");
        answerArea.setText("说明：AI API Key 配置在后端 application.yml 中，前端只发送问题，不保存密钥。");
    }

    @FXML
    protected void onAskButtonClick() {
        String question = getText(questionArea);
        if (question.isEmpty()) {
            MessageDialog.showDialog("请输入问题后再发送。");
            questionArea.requestFocus();
            return;
        }

        askButton.setDisable(true);
        statusLabel.setText("正在请求智能助手，请稍候...");
        try {
            DataRequest request = new DataRequest();
            request.add("question", question);
            DataResponse response = HttpRequestUtil.request("/api/ai/chat", request);
            if (response != null && response.getCode() != null && response.getCode() == 0) {
                answerArea.setText(response.getData() == null ? "智能助手暂时没有返回内容。" : response.getData().toString());
                statusLabel.setText(response.getMsg() == null || response.getMsg().isBlank() ? "回答完成。" : response.getMsg());
            } else {
                String message = response == null ? "智能助手请求失败，请检查后端是否启动。" : response.getMsg();
                MessageDialog.showDialog(message == null || message.isBlank() ? "智能助手请求失败。" : message);
                statusLabel.setText("请求失败，已保留当前问题。");
            }
        } finally {
            askButton.setDisable(false);
        }
    }

    @FXML
    protected void onClearButtonClick() {
        questionArea.clear();
        answerArea.clear();
        statusLabel.setText("已清空内容，可以重新输入问题。");
        questionArea.requestFocus();
    }

    @FXML
    protected void onCourseQuestionClick() {
        setQuestion("请帮我查询当前课程和课表相关信息，并用简短列表说明。");
    }

    @FXML
    protected void onLeaveQuestionClick() {
        setQuestion("请帮我查看学生请假相关情况，重点说明待审核和已处理记录。");
    }

    @FXML
    protected void onCommunityQuestionClick() {
        setQuestion("请帮我总结校园社区里的帖子和评论情况，找出最近比较重要的信息。");
    }

    @FXML
    protected void onSystemGuideQuestionClick() {
        setQuestion("这个系统有哪些功能？请按管理员、教师、学生三个身份详细说明。");
    }

    @FXML
    protected void onTodoQuestionClick() {
        setQuestion("我最近有什么待办？请结合首页仪表盘数据回答。");
    }

    @FXML
    protected void onMaterialQuestionClick() {
        setQuestion("某门课有哪些资料？如果我没有指定课程，请总结数据库里课程资料多吗。");
    }

    @FXML
    protected void onSubmitHomeworkQuestionClick() {
        setQuestion("学生怎么提交作业？请按操作步骤说明。");
    }

    @FXML
    protected void onUploadMaterialQuestionClick() {
        setQuestion("教师怎么上传课程资料？请按操作步骤说明。");
    }

    private void setQuestion(String question) {
        questionArea.setText(question);
        questionArea.requestFocus();
    }

    private String getText(TextArea textArea) {
        return textArea == null || textArea.getText() == null ? "" : textArea.getText().trim();
    }
}
