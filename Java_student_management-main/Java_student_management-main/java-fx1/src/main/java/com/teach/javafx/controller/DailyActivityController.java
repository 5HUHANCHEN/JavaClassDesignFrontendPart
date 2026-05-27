package com.teach.javafx.controller;

import com.teach.javafx.controller.base.AbstractStudentGrowthRecordController;

public class DailyActivityController extends AbstractStudentGrowthRecordController {
    @Override
    protected String getCategoryCode() {
        return "DAILY_ACTIVITY";
    }

    @Override
    protected String getPageTitle() {
        return "日常活动信息";
    }

    @Override
    protected String getPageSubtitle() {
        return "记录体育活动、旅游出行、文艺演出、聚会等日常活动";
    }

    @Override
    protected String getItemTypeLabelText() {
        return "活动类型";
    }

    @Override
    protected String getTitleLabelText() {
        return "活动主题";
    }

    @Override
    protected String getLevelLabelText() {
        return "活动级别";
    }

    @Override
    protected String getOrganizationLabelText() {
        return "组织方";
    }

    @Override
    protected String getStartDateLabelText() {
        return "开始日期";
    }

    @Override
    protected String getEndDateLabelText() {
        return "结束日期";
    }

    @Override
    protected String getPlaceLabelText() {
        return "活动地点";
    }

    @Override
    protected String getResultLabelText() {
        return "活动收获";
    }

    @Override
    protected String getDescriptionLabelText() {
        return "详细描述";
    }
}
