package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommunityController extends ToolController {
    @FXML
    private ListView<Map> postListView;
    @FXML
    private ComboBox<String> scopeComboBox;
    @FXML
    private ComboBox<String> filterCategoryComboBox;
    @FXML
    private TextField keywordField;
    @FXML
    private Label feedSummaryLabel;
    @FXML
    private ComboBox<String> editCategoryComboBox;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentArea;
    @FXML
    private ComboBox<String> mediaTypeComboBox;
    @FXML
    private TextField mediaUrlField;
    @FXML
    private TextField linkUrlField;
    @FXML
    private Label postInfoLabel;
    @FXML
    private Hyperlink mediaLink;
    @FXML
    private Hyperlink externalLink;
    @FXML
    private ListView<Map> commentListView;
    @FXML
    private TextArea commentArea;
    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button deleteCommentButton;

    private final ObservableList<Map> postData = FXCollections.observableArrayList();
    private final ObservableList<Map> commentData = FXCollections.observableArrayList();
    private final List<String> categoryTitleList = new ArrayList<>();

    private Integer editingPostId;
    private Integer selectedCommentId;
    private boolean adminUser;

    @FXML
    public void initialize() {
        adminUser = AppStore.getJwt() != null && "ROLE_ADMIN".equals(AppStore.getJwt().getRole());

        scopeComboBox.getItems().setAll("全部帖子", "我的帖子");
        scopeComboBox.getSelectionModel().selectFirst();

        mediaTypeComboBox.getItems().setAll("无媒体", "图片", "视频");
        mediaTypeComboBox.getSelectionModel().selectFirst();

        postListView.setItems(postData);
        postListView.setCellFactory(listView -> new PostCardCell());
        postListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadPostDetail(CommonMethod.getInteger(newValue, "communityPostId"));
            }
        });

        commentListView.setItems(commentData);
        commentListView.setCellFactory(listView -> new CommentCardCell());
        commentListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedCommentId = newValue == null ? null : CommonMethod.getInteger(newValue, "communityCommentId");
            refreshCommentDeleteButton();
        });

        mediaLink.setOnAction(event -> openExternalLink(mediaLink.getText()));
        externalLink.setOnAction(event -> openExternalLink(externalLink.getText()));

        loadCategoryList();
        resetEditorForNewPost();
        loadPostList();
    }

    @FXML
    protected void onQueryButtonClick() {
        loadPostList();
    }

    @FXML
    protected void onRefreshButtonClick() {
        loadPostList();
        if (editingPostId != null) {
            loadPostDetail(editingPostId);
        }
    }

    @FXML
    protected void onNewPostButtonClick() {
        postListView.getSelectionModel().clearSelection();
        resetEditorForNewPost();
    }

    @FXML
    protected void onSaveButtonClick() {
        String title = getTrimmedText(titleField);
        String category = editCategoryComboBox.getSelectionModel().getSelectedItem();
        String content = getTrimmedText(contentArea);
        String mediaType = mediaTypeComboBox.getSelectionModel().getSelectedItem();
        String mediaUrl = getTrimmedText(mediaUrlField);
        String linkUrl = getTrimmedText(linkUrlField);

        if (title.isEmpty()) {
            MessageDialog.showDialog("请输入帖子标题。");
            return;
        }
        if (category == null || category.isBlank()) {
            MessageDialog.showDialog("请选择帖子分类。");
            return;
        }
        if (content.isEmpty()) {
            MessageDialog.showDialog("请输入帖子内容。");
            return;
        }
        if (!"无媒体".equals(mediaType) && mediaUrl.isEmpty()) {
            MessageDialog.showDialog("已选择媒体类型时，请填写媒体地址。");
            return;
        }

        DataRequest request = new DataRequest();
        request.add("communityPostId", editingPostId);
        request.add("title", title);
        request.add("category", category);
        request.add("content", content);
        request.add("mediaType", "无媒体".equals(mediaType) ? "" : mediaType);
        request.add("mediaUrl", mediaUrl);
        request.add("linkUrl", linkUrl);

        DataResponse response = HttpRequestUtil.request("/api/community/postSave", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog(response.getMsg() == null ? "帖子保存成功。" : response.getMsg());
            loadPostList();
            if (response.getData() instanceof Map<?, ?> mapData) {
                Integer postId = CommonMethod.getInteger((Map<String, Object>) mapData, "communityPostId");
                if (postId != null) {
                    loadPostDetail(postId);
                }
            }
        } else {
            MessageDialog.showDialog(response == null ? "帖子保存失败，请检查服务器连接。" : response.getMsg());
        }
    }

    @FXML
    protected void onDeleteButtonClick() {
        if (editingPostId == null) {
            MessageDialog.showDialog("请先选择要删除的帖子。");
            return;
        }
        int choice = MessageDialog.choiceDialog("确定删除当前帖子吗？删除后其评论也会一并移除。");
        if (choice != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("communityPostId", editingPostId);
        DataResponse response = HttpRequestUtil.request("/api/community/postDelete", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog(response.getMsg() == null ? "帖子删除成功。" : response.getMsg());
            loadPostList();
            resetEditorForNewPost();
        } else {
            MessageDialog.showDialog(response == null ? "帖子删除失败，请检查服务器连接。" : response.getMsg());
        }
    }

    @FXML
    protected void onCommentButtonClick() {
        if (editingPostId == null) {
            MessageDialog.showDialog("请先选择要评论的帖子。");
            return;
        }
        String comment = getTrimmedText(commentArea);
        if (comment.isEmpty()) {
            MessageDialog.showDialog("请输入评论内容。");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("communityPostId", editingPostId);
        request.add("content", comment);
        DataResponse response = HttpRequestUtil.request("/api/community/commentSave", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog(response.getMsg() == null ? "评论发布成功。" : response.getMsg());
            commentArea.clear();
            loadPostDetail(editingPostId);
            loadPostList();
        } else {
            MessageDialog.showDialog(response == null ? "评论发布失败，请检查服务器连接。" : response.getMsg());
        }
    }

    @FXML
    protected void onDeleteCommentButtonClick() {
        if (!adminUser) {
            MessageDialog.showDialog("只有管理员可以删除评论。");
            return;
        }
        if (selectedCommentId == null) {
            MessageDialog.showDialog("请先选择要删除的评论。");
            return;
        }
        int choice = MessageDialog.choiceDialog("确定删除当前评论吗？");
        if (choice != MessageDialog.CHOICE_YES) {
            return;
        }
        DataRequest request = new DataRequest();
        request.add("communityCommentId", selectedCommentId);
        DataResponse response = HttpRequestUtil.request("/api/community/commentDelete", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog(response.getMsg() == null ? "评论删除成功。" : response.getMsg());
            loadPostDetail(editingPostId);
            loadPostList();
        } else {
            MessageDialog.showDialog(response == null ? "评论删除失败，请检查服务器连接。" : response.getMsg());
        }
    }

    @Override
    public void doRefresh() {
        loadPostList();
        if (editingPostId != null) {
            loadPostDetail(editingPostId);
        }
    }

    private void loadCategoryList() {
        DataRequest request = new DataRequest();
        List<OptionItem> itemList = HttpRequestUtil.requestOptionItemList("/api/community/getCategoryOptionList", request);
        categoryTitleList.clear();
        if (itemList != null) {
            for (OptionItem item : itemList) {
                if (item != null && item.getTitle() != null && !item.getTitle().isBlank()) {
                    categoryTitleList.add(item.getTitle());
                }
            }
        }
        if (categoryTitleList.isEmpty()) {
            categoryTitleList.add("综合交流");
            categoryTitleList.add("课程讨论");
            categoryTitleList.add("校园互助");
            categoryTitleList.add("学习分享");
        }
        filterCategoryComboBox.getItems().setAll("全部");
        filterCategoryComboBox.getItems().addAll(categoryTitleList);
        filterCategoryComboBox.getSelectionModel().selectFirst();

        editCategoryComboBox.getItems().setAll(categoryTitleList);
        editCategoryComboBox.getSelectionModel().selectFirst();
    }

    private void loadPostList() {
        DataRequest request = new DataRequest();
        request.add("category", filterCategoryComboBox.getSelectionModel().getSelectedItem());
        request.add("keyword", getTrimmedText(keywordField));
        request.add("onlyMine", "我的帖子".equals(scopeComboBox.getSelectionModel().getSelectedItem()));
        DataResponse response = HttpRequestUtil.request("/api/community/getPostList", request);
        if (response != null && response.getCode() == 0) {
            List<Map> postList = (List<Map>) response.getData();
            postData.setAll(postList == null ? new ArrayList<>() : postList);
            feedSummaryLabel.setText("共 " + postData.size() + " 条帖子");
        } else {
            MessageDialog.showDialog(response == null ? "帖子列表加载失败，请检查服务器连接。" : response.getMsg());
        }
    }

    private void loadPostDetail(Integer postId) {
        if (postId == null || postId <= 0) {
            resetEditorForNewPost();
            return;
        }
        DataRequest request = new DataRequest();
        request.add("communityPostId", postId);
        DataResponse response = HttpRequestUtil.request("/api/community/getPostDetail", request);
        if (response == null || response.getCode() != 0) {
            MessageDialog.showDialog(response == null ? "帖子详情加载失败，请检查服务器连接。" : response.getMsg());
            return;
        }
        Map<String, Object> data = (Map<String, Object>) response.getData();
        Map<String, Object> postMap = CommonMethod.getMap(data, "post");
        List<Map<String, Object>> commentMapList = (List<Map<String, Object>>) data.get("comments");

        editingPostId = CommonMethod.getInteger(postMap, "communityPostId");
        titleField.setText(CommonMethod.getString(postMap, "title"));
        editCategoryComboBox.getSelectionModel().select(CommonMethod.getString(postMap, "category"));
        contentArea.setText(CommonMethod.getString(postMap, "content"));
        mediaTypeComboBox.getSelectionModel().select(resolveMediaType(CommonMethod.getString(postMap, "mediaType")));
        mediaUrlField.setText(CommonMethod.getString(postMap, "mediaUrl"));
        linkUrlField.setText(CommonMethod.getString(postMap, "linkUrl"));
        postInfoLabel.setText(buildPostInfo(postMap));

        updateHyperlink(mediaLink, CommonMethod.getString(postMap, "mediaUrl"));
        updateHyperlink(externalLink, CommonMethod.getString(postMap, "linkUrl"));

        boolean canEdit = Boolean.TRUE.equals(CommonMethod.getBoolean(postMap, "canEdit"));
        titleField.setDisable(!canEdit);
        editCategoryComboBox.setDisable(!canEdit);
        contentArea.setDisable(!canEdit);
        mediaTypeComboBox.setDisable(!canEdit);
        mediaUrlField.setDisable(!canEdit);
        linkUrlField.setDisable(!canEdit);
        saveButton.setDisable(!canEdit);
        deleteButton.setDisable(!canEdit);

        commentData.setAll(commentMapList == null ? new ArrayList<>() : commentMapList);
        selectedCommentId = null;
        refreshCommentDeleteButton();
    }

    private void resetEditorForNewPost() {
        editingPostId = null;
        selectedCommentId = null;
        titleField.clear();
        if (!editCategoryComboBox.getItems().isEmpty()) {
            editCategoryComboBox.getSelectionModel().selectFirst();
        }
        contentArea.clear();
        mediaTypeComboBox.getSelectionModel().selectFirst();
        mediaUrlField.clear();
        linkUrlField.clear();
        commentArea.clear();
        commentData.clear();
        postInfoLabel.setText("当前正在创建新帖子。");
        updateHyperlink(mediaLink, "");
        updateHyperlink(externalLink, "");
        titleField.setDisable(false);
        editCategoryComboBox.setDisable(false);
        contentArea.setDisable(false);
        mediaTypeComboBox.setDisable(false);
        mediaUrlField.setDisable(false);
        linkUrlField.setDisable(false);
        saveButton.setDisable(false);
        deleteButton.setDisable(true);
        refreshCommentDeleteButton();
    }

    private void updateHyperlink(Hyperlink hyperlink, String url) {
        String text = getTrimmedText(url);
        hyperlink.setText(text.isEmpty() ? "暂无" : text);
        hyperlink.setDisable(text.isEmpty());
    }

    private void refreshCommentDeleteButton() {
        deleteCommentButton.setDisable(!(adminUser && selectedCommentId != null));
    }

    private void openExternalLink(String url) {
        String link = getTrimmedText(url);
        if (link.isEmpty() || "暂无".equals(link)) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(link));
        } catch (Exception exception) {
            MessageDialog.showDialog("链接打开失败，请检查地址格式是否正确。");
        }
    }

    private String buildPostInfo(Map<String, Object> postMap) {
        return "发布人：" + CommonMethod.getString(postMap, "authorName")
                + "（" + CommonMethod.getString(postMap, "authorRoleName") + "）"
                + "    发布时间：" + CommonMethod.getString(postMap, "createdTime")
                + "    最后更新：" + CommonMethod.getString(postMap, "updatedTime");
    }

    private String resolveMediaType(String mediaType) {
        String value = getTrimmedText(mediaType);
        return value.isEmpty() ? "无媒体" : value;
    }

    private String getTrimmedText(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    private String getTrimmedText(TextArea textArea) {
        return textArea.getText() == null ? "" : textArea.getText().trim();
    }

    private String getTrimmedText(String value) {
        return value == null ? "" : value.trim();
    }

    private static class PostCardCell extends ListCell<Map> {
        @Override
        protected void updateItem(Map item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            VBox card = new VBox();
            card.getStyleClass().add("post-card");

            Label categoryLabel = new Label(CommonMethod.getString(item, "category"));
            categoryLabel.getStyleClass().add("post-category");

            Label titleLabel = new Label(CommonMethod.getString(item, "title"));
            titleLabel.getStyleClass().add("post-title");
            titleLabel.setWrapText(true);

            Label summaryLabel = new Label(CommonMethod.getString(item, "summary"));
            summaryLabel.getStyleClass().add("post-summary");
            summaryLabel.setWrapText(true);

            Label metaLabel = new Label(
                    CommonMethod.getString(item, "authorName")
                            + "    "
                            + CommonMethod.getString(item, "updatedTime")
                            + "    评论 "
                            + CommonMethod.getString(item, "commentCount")
            );
            metaLabel.getStyleClass().add("post-meta");

            card.getChildren().addAll(categoryLabel, titleLabel, summaryLabel, metaLabel);
            setGraphic(card);
        }
    }

    private static class CommentCardCell extends ListCell<Map> {
        @Override
        protected void updateItem(Map item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            VBox card = new VBox();
            card.getStyleClass().add("comment-item-card");

            Label authorLabel = new Label(
                    CommonMethod.getString(item, "authorName")
                            + "（" + CommonMethod.getString(item, "authorRoleName") + "）"
            );
            authorLabel.getStyleClass().add("comment-author");

            Label timeLabel = new Label(CommonMethod.getString(item, "createdTime"));
            timeLabel.getStyleClass().add("comment-time");

            Label contentLabel = new Label(CommonMethod.getString(item, "content"));
            contentLabel.getStyleClass().add("comment-content");
            contentLabel.setWrapText(true);

            card.getChildren().addAll(authorLabel, timeLabel, contentLabel);
            setGraphic(card);
        }
    }
}
