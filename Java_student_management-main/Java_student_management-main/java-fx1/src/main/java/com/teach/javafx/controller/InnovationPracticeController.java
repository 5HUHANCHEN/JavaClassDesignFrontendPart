package com.teach.javafx.controller;

import com.teach.javafx.controller.base.AbstractStudentGrowthRecordController;

public class InnovationPracticeController extends AbstractStudentGrowthRecordController {
    @Override
    protected String getCategoryCode() {
        return "INNOVATION_PRACTICE";
    }

    @Override
    protected String getPageTitle() {
        return "创新实践信息";
    }

    @Override
    protected String getPageSubtitle() {
        return "记录社会实践、竞赛、成果、培训讲座、创新项目和校外实习等成长经历";
    }

    @Override
    protected String getItemTypeLabelText() {
        return "实践类型";
    }

    @Override
    protected String getTitleLabelText() {
        return "项目名称";
    }

    @Override
    protected String getLevelLabelText() {
        return "级别分类";
    }

    @Override
    protected String getOrganizationLabelText() {
        return "主办单位";
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
        return "实践地点";
    }

    @Override
    protected String getResultLabelText() {
        return "成果说明";
    }

    @Override
    protected String getDescriptionLabelText() {
        return "详细描述";
    }
}
