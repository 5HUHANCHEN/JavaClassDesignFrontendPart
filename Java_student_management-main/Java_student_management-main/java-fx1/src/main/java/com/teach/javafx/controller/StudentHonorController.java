package com.teach.javafx.controller;

import com.teach.javafx.controller.base.AbstractStudentGrowthRecordController;

public class StudentHonorController extends AbstractStudentGrowthRecordController {
    @Override
    protected boolean supportsPlaceField() {
        return false;
    }

    @Override
    protected String getCategoryCode() {
        return "HONOR";
    }

    @Override
    protected String getPageTitle() {
        return "学生荣誉信息";
    }

    @Override
    protected String getPageSubtitle() {
        return "记录各类称号、奖励、表彰和证书等荣誉信息";
    }

    @Override
    protected String getItemTypeLabelText() {
        return "荣誉类型";
    }

    @Override
    protected String getTitleLabelText() {
        return "荣誉名称";
    }

    @Override
    protected String getLevelLabelText() {
        return "荣誉级别";
    }

    @Override
    protected String getOrganizationLabelText() {
        return "授予单位";
    }

    @Override
    protected String getStartDateLabelText() {
        return "获奖日期";
    }

    @Override
    protected String getEndDateLabelText() {
        return "证书日期";
    }

    @Override
    protected String getPlaceLabelText() {
        return "授予地点";
    }

    @Override
    protected String getResultLabelText() {
        return "奖励说明";
    }

    @Override
    protected String getDescriptionLabelText() {
        return "备注说明";
    }
}
