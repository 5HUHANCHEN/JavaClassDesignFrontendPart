package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class TeacherProfileController {
    @FXML private ImageView teacherPhotoView;
    @FXML private Label teacherNameLabel;
    @FXML private Label teacherNumLabel;
    @FXML private Label teacherDeptLabel;
    @FXML private Label teacherTitleLabel;
    @FXML private Label teacherDegreeLabel;
    @FXML private Label teacherGenderLabel;
    @FXML private Label teacherBirthdayLabel;
    @FXML private Label teacherEmailLabel;
    @FXML private Label teacherPhoneLabel;
    @FXML private Label teacherAddressLabel;
    @FXML private TextArea teacherIntroduceArea;
    @FXML private Label photoTipLabel;

    @FXML
    public void initialize() {
        loadTeacherProfile();
    }

    @FXML
    protected void onRefreshButtonClick() {
        loadTeacherProfile();
    }

    private void loadTeacherProfile() {
        DataResponse response = HttpRequestUtil.request("/api/teacher/getTeacherInfo", new DataRequest());
        if (response == null || response.getCode() != 0 || !(response.getData() instanceof Map<?, ?> rawMap)) {
            MessageDialog.showDialog(response == null ? "教师简介加载失败，请检查服务器连接。" : response.getMsg());
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> teacherInfo = (Map<String, Object>) rawMap;
        teacherNameLabel.setText(getTextValue(teacherInfo, "name"));
        teacherNumLabel.setText(getTextValue(teacherInfo, "num"));
        teacherDeptLabel.setText(getTextValue(teacherInfo, "dept"));
        teacherTitleLabel.setText(getTextValue(teacherInfo, "title"));
        teacherDegreeLabel.setText(getTextValue(teacherInfo, "degree"));
        teacherGenderLabel.setText(getTextValue(teacherInfo, "gender"));
        teacherBirthdayLabel.setText(getTextValue(teacherInfo, "birthday"));
        teacherEmailLabel.setText(getTextValue(teacherInfo, "email"));
        teacherPhoneLabel.setText(getTextValue(teacherInfo, "phone"));
        teacherAddressLabel.setText(getTextValue(teacherInfo, "address"));
        teacherIntroduceArea.setText(getTextValue(teacherInfo, "introduce"));

        Integer personId = toInteger(teacherInfo.get("personId"));
        if (personId == null) {
            teacherPhotoView.setImage(null);
            photoTipLabel.setText("未获取到教师编号，暂时无法加载头像。");
            return;
        }
        loadTeacherPhoto(personId);
    }

    private void loadTeacherPhoto(Integer personId) {
        DataRequest request = new DataRequest();
        request.add("personId", personId);
        byte[] photoBytes = HttpRequestUtil.requestByteData("/api/base/getBlobByteData", request);
        if (photoBytes != null && photoBytes.length > 0) {
            teacherPhotoView.setImage(new Image(new ByteArrayInputStream(photoBytes)));
            photoTipLabel.setText("教师头像已加载。如需更新，请联系管理员在教师管理中重新上传。");
        } else {
            teacherPhotoView.setImage(null);
            photoTipLabel.setText("暂无教师头像。");
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (Exception ex) {
            return null;
        }
    }

    private String getTextValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return "暂无信息";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "暂无信息" : text;
    }
}
