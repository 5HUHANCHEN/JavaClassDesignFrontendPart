package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class ToolboxController extends ToolController {
    @FXML private VBox toolboxListBox;

    private final List<ToolboxItem> items = List.of(
            new ToolboxItem(
                    "偷懒工具",
                    "综合在线工具集合",
                    "这是一个偏实用型的在线工具集合，特点是打开就能用，适合处理临时、零散、但又经常遇到的小任务。它的定位不是某一个单独软件，而是把文本处理、格式转换、图片处理、二维码、编码解码、时间日期、开发辅助等常见工具集中到一个入口里。",
                    "适合在写作业、做课程设计、整理资料、处理图片、生成二维码、转换文本格式、临时查工具时使用。比如你不想专门安装一个软件，只是想快速完成一个小操作，就可以先来这里找。",
                    "https://toolight.cn/"
            ),
            new ToolboxItem(
                    "刘明野的工具箱",
                    "综合娱乐与实用网站导航",
                    "这是一个内容更丰富的导航型工具箱，页面把影视、二次元、音乐、阅读、游戏、娱乐、在线工具、软件等站点按分类整理好。它更像一个经过筛选的网站目录，不需要自己到处收藏链接，进入后可以按分类找资源。音乐区包含听歌、无损音乐、电台等入口，工具区也有热榜、地图、临时邮箱、二维码、文档转换、网络诊断等实用功能。",
                    "适合当作日常入口页使用：想找音乐网站、在线小游戏、阅读资源、临时工具、实用软件或娱乐站点时，可以先从这里进入。它尤其适合作为“备用导航”，当某个单独网站打不开时，可以继续从里面找同类替代入口。",
                    "https://tools.liumingye.cn/music/"
            )
    );

    @FXML
    public void initialize() {
        renderItems();
    }

    @Override
    public void doRefresh() {
        renderItems();
    }

    private void renderItems() {
        if (toolboxListBox == null) {
            return;
        }
        toolboxListBox.getChildren().clear();
        for (ToolboxItem item : items) {
            toolboxListBox.getChildren().add(createCard(item));
        }
    }

    private VBox createCard(ToolboxItem item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("toolbox-card");

        HBox header = new HBox(12);
        Label title = new Label(item.name());
        title.getStyleClass().add("toolbox-card-title");
        Label tag = new Label(item.type());
        tag.getStyleClass().add("toolbox-tag");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button openButton = new Button("打开网站");
        openButton.getStyleClass().add("btn-primary");
        openButton.setOnAction(event -> openLink(item.url()));
        header.getChildren().addAll(title, tag, spacer, openButton);

        Label description = new Label(item.description());
        description.setWrapText(true);
        description.getStyleClass().add("toolbox-description");

        Label scene = new Label("适合场景：" + item.scene());
        scene.setWrapText(true);
        scene.getStyleClass().add("toolbox-scene");

        Label url = new Label(item.url());
        url.setWrapText(true);
        url.getStyleClass().add("toolbox-url");

        card.getChildren().addAll(header, description, scene, url);
        return card;
    }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            MessageDialog.showDialog("打开网站失败：" + e.getMessage());
        }
    }

    private record ToolboxItem(String name, String type, String description, String scene, String url) {
    }
}
