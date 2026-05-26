package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.util.CommonMethod;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EntertainmentCenterController extends ToolController {
    @FXML private TextField cityTextField;
    @FXML private Label weatherTitleLabel;
    @FXML private TextArea weatherTextArea;
    @FXML private Label hitokotoContentLabel;
    @FXML private Label hitokotoMetaLabel;
    @FXML private Label movieFeaturedLabel;
    @FXML private TextArea movieListArea;
    @FXML private Label movieUrlLabel;
    @FXML private ComboBox<String> movieModeComboBox;
    @FXML private TextField movieKeywordTextField;
    @FXML private ComboBox<String> movieChannelComboBox;
    @FXML private Label musicNameLabel;
    @FXML private Label musicMetaLabel;
    @FXML private TextArea musicListArea;
    @FXML private Label musicUrlLabel;
    @FXML private ComboBox<String> musicModeComboBox;
    @FXML private TextField musicKeywordTextField;
    @FXML private ComboBox<String> musicChannelComboBox;
    @FXML private ComboBox<String> copyTypeComboBox;
    @FXML private TextField copyKeywordTextField;
    @FXML private TextArea copyResultArea;
    @FXML private ComboBox<String> horoscopeComboBox;
    @FXML private TextArea horoscopeTextArea;
    @FXML private ComboBox<String> hotCategoryComboBox;
    @FXML private VBox hotListBox;
    @FXML private Label statusLabel;

    private String movieUrl = "";
    private String musicUrl = "";

    @FXML
    public void initialize() {
        copyTypeComboBox.getItems().setAll("朋友圈文案", "土味情话", "发疯文学", "社团宣传语", "表白文案", "请假理由娱乐版");
        copyTypeComboBox.getSelectionModel().select(0);
        movieModeComboBox.getItems().setAll("推荐榜", "搜索榜");
        movieModeComboBox.getSelectionModel().select(0);
        movieKeywordTextField.clear();
        movieChannelComboBox.getItems().setAll("渠道一：Gimy TV", "渠道二：影猫", "渠道三：完美看看", "渠道四：蛋蛋赞 PPnix", "渠道五：我乐电影");
        movieChannelComboBox.getSelectionModel().select(0);
        musicModeComboBox.getItems().setAll("推荐榜", "搜索榜");
        musicModeComboBox.getSelectionModel().select(0);
        musicKeywordTextField.clear();
        musicChannelComboBox.getItems().setAll("渠道一：泡椒音乐", "渠道二：种子音乐", "渠道三：布谷音乐", "渠道四：米兔音乐", "渠道五：HiFiNi音乐");
        musicChannelComboBox.getSelectionModel().select(0);
        horoscopeComboBox.getItems().setAll("白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座");
        horoscopeComboBox.getSelectionModel().select("天蝎座");
        hotCategoryComboBox.getItems().setAll("微博热搜", "知乎热榜", "抖音热点", "B站热榜", "36氪热榜");
        hotCategoryComboBox.getSelectionModel().select(0);
        cityTextField.setText("济南");
        copyKeywordTextField.setText("今天");
        movieChannelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshMovieWhenSelectionChanges(oldValue, newValue));
        movieModeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshMovieWhenSelectionChanges(oldValue, newValue));
        musicChannelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshMusicWhenSelectionChanges(oldValue, newValue));
        musicModeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshMusicWhenSelectionChanges(oldValue, newValue));
        loadAll();
    }

    @Override
    public void doRefresh() {
        loadAll();
    }

    @FXML
    private void onWeatherQueryClick() {
        DataRequest request = new DataRequest();
        request.add("city", cityTextField.getText());
        requestDataAsync("/api/entertainment/weather", request, data -> {
            List<Map<String, Object>> items = asMapList(data.get("items"));
            Map<String, Object> item = items.isEmpty() ? Map.of() : items.get(0);
            weatherTitleLabel.setText(CommonMethod.getString(data, "title") + fallbackTag(data));
            weatherTextArea.setText("天气：" + CommonMethod.getString(item, "weather") + "\n"
                    + "温度：" + CommonMethod.getString(item, "low") + " ~ " + CommonMethod.getString(item, "high") + "\n"
                    + "风力：" + CommonMethod.getString(item, "wind") + "\n"
                    + "空气：" + CommonMethod.getString(item, "air") + " " + CommonMethod.getString(item, "aqi") + "\n"
                    + "提示：" + CommonMethod.getString(item, "tip") + "\n"
                    + "校园建议：" + CommonMethod.getString(item, "suggestion"));
        });
    }

    @FXML
    private void onHitokotoRefreshClick() {
        requestDataAsync("/api/entertainment/hitokoto", new DataRequest(), data -> {
            List<Map<String, Object>> items = asMapList(data.get("items"));
            Map<String, Object> item = items.isEmpty() ? Map.of() : items.get(0);
            hitokotoContentLabel.setText(CommonMethod.getString(item, "content"));
            hitokotoMetaLabel.setText("出处：" + CommonMethod.getString(item, "from")
                    + "  作者：" + CommonMethod.getString(item, "author")
                    + fallbackTag(data));
        });
    }

    @FXML
    private void onMovieRefreshClick() {
        DataRequest request = new DataRequest();
        request.add("channel", movieChannelComboBox.getValue());
        request.add("mode", boardModeValue(movieModeComboBox.getValue()));
        request.add("keyword", movieKeywordTextField.getText());
        requestDataAsync("/api/entertainment/movieRecommend", request, data -> {
            Map<String, Object> featured = CommonMethod.getMap(data, "featured");
            movieUrl = CommonMethod.getString(featured, "url");
            movieFeaturedLabel.setText(CommonMethod.getString(featured, "title") + "  " + CommonMethod.getString(featured, "rating") + fallbackTag(data));
            movieUrlLabel.setText(movieUrl.isBlank() ? "暂无链接" : movieUrl);
            StringBuilder builder = new StringBuilder();
            for (Map<String, Object> item : asMapList(data.get("items"))) {
                builder.append(CommonMethod.getString(item, "rank"))
                        .append(". ")
                        .append(CommonMethod.getString(item, "title"))
                        .append(" | ")
                        .append(CommonMethod.getString(item, "rating"))
                        .append(" | ")
                        .append(CommonMethod.getString(item, "genres"))
                        .append("\n")
                        .append("上映：").append(CommonMethod.getString(item, "pubdate"))
                        .append("  主演：").append(CommonMethod.getString(item, "actor"))
                        .append("\n\n");
            }
            movieListArea.setText(builder.toString().trim());
        });
    }

    @FXML
    private void onMovieOpenClick() {
        openLink(movieUrl);
    }

    @FXML
    private void onMusicRefreshClick() {
        DataRequest request = new DataRequest();
        request.add("channel", musicChannelComboBox.getValue());
        request.add("mode", boardModeValue(musicModeComboBox.getValue()));
        request.add("keyword", musicKeywordTextField.getText());
        requestDataAsync("/api/entertainment/musicRecommend", request, data -> {
            List<Map<String, Object>> items = asMapList(data.get("items"));
            Map<String, Object> item = items.isEmpty() ? Map.of() : items.get(0);
            musicUrl = CommonMethod.getString(item, "url");
            musicNameLabel.setText(CommonMethod.getString(item, "name") + fallbackTag(data));
            musicMetaLabel.setText("歌手：" + CommonMethod.getString(item, "artist") + "  榜单：" + CommonMethod.getString(item, "sort"));
            musicUrlLabel.setText(musicUrl.isBlank() ? "暂无链接" : musicUrl);
            StringBuilder builder = new StringBuilder();
            for (Map<String, Object> row : items) {
                builder.append(builder.length() == 0 ? "" : "\n")
                        .append(CommonMethod.getString(row, "id").isBlank() ? "" : CommonMethod.getString(row, "id") + "  ")
                        .append(CommonMethod.getString(row, "name"))
                        .append(" | ")
                        .append(CommonMethod.getString(row, "artist"))
                        .append(" | ")
                        .append(CommonMethod.getString(row, "sort"));
            }
            musicListArea.setText(builder.toString());
        });
    }

    @FXML
    private void onMusicOpenClick() {
        openLink(musicUrl);
    }

    @FXML
    private void onCopyGenerateClick() {
        DataRequest request = new DataRequest();
        request.add("type", copyTypeComboBox.getValue());
        request.add("keyword", copyKeywordTextField.getText());
        request.add("nonce", String.valueOf(System.nanoTime()));
        requestDataAsync("/api/entertainment/copywriting", request, data -> {
            List<Map<String, Object>> items = asMapList(data.get("items"));
            Map<String, Object> item = items.isEmpty() ? Map.of() : items.get(0);
            copyResultArea.setText(CommonMethod.getString(item, "content"));
        });
    }

    @FXML
    private void onHoroscopeRefreshClick() {
        DataRequest request = new DataRequest();
        request.add("sign", horoscopeComboBox.getValue());
        request.add("time", "today");
        requestDataAsync("/api/entertainment/horoscope", request, data -> {
            List<Map<String, Object>> items = asMapList(data.get("items"));
            Map<String, Object> item = items.isEmpty() ? Map.of() : items.get(0);
            horoscopeTextArea.setText(CommonMethod.getString(item, "title") + " " + CommonMethod.getString(item, "time") + fallbackTag(data) + "\n"
                    + CommonMethod.getString(item, "shortcomment") + "\n"
                    + "宜：" + CommonMethod.getString(item, "yi") + "  忌：" + CommonMethod.getString(item, "ji") + "\n"
                    + "综合：" + CommonMethod.getString(item, "all") + "  爱情：" + CommonMethod.getString(item, "love") + "\n"
                    + "工作：" + CommonMethod.getString(item, "work") + "  财运：" + CommonMethod.getString(item, "money") + "  健康：" + CommonMethod.getString(item, "health") + "\n"
                    + CommonMethod.getString(item, "text"));
        });
    }

    @FXML
    private void onHotListRefreshClick() {
        DataRequest request = new DataRequest();
        request.add("category", hotCategoryValue(hotCategoryComboBox.getValue()));
        requestDataAsync("/api/entertainment/hotList", request, data -> {
            hotListBox.getChildren().clear();
            Label title = new Label(CommonMethod.getString(data, "title") + fallbackTag(data));
            title.getStyleClass().add("module-section-title");
            hotListBox.getChildren().add(title);
            for (Map<String, Object> item : asMapList(data.get("items"))) {
                hotListBox.getChildren().add(createHotRow(item));
            }
        });
    }

    private void loadAll() {
        onWeatherQueryClick();
        onHitokotoRefreshClick();
        onMovieRefreshClick();
        onMusicRefreshClick();
        onCopyGenerateClick();
        onHoroscopeRefreshClick();
        onHotListRefreshClick();
    }

    private HBox createHotRow(Map<String, Object> item) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("entertainment-list-row");
        Label label = new Label(CommonMethod.getString(item, "rank") + ". " + CommonMethod.getString(item, "title")
                + "  " + CommonMethod.getString(item, "hot"));
        label.setWrapText(true);
        HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
        Button button = new Button("打开");
        button.getStyleClass().add("mini-link-button");
        String url = CommonMethod.getString(item, "url");
        button.setDisable(url.isBlank());
        button.setOnAction(event -> openLink(url));
        row.getChildren().addAll(label, button);
        return row;
    }

    private void requestDataAsync(String url, DataRequest request, Consumer<Map<String, Object>> consumer) {
        updateStatus("正在请求娱乐资讯...");
        CompletableFuture.supplyAsync(() -> requestData(url, request))
                .exceptionally(throwable -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("_status", "请求失败：" + throwable.getMessage());
                    return data;
                })
                .thenAccept(data -> Platform.runLater(() -> {
                    String status = CommonMethod.getString(data, "_status");
                    if (!status.isBlank()) {
                        updateStatus(status);
                    }
                    if (data.size() > 1) {
                        consumer.accept(data);
                    }
                }));
    }

    private Map<String, Object> requestData(String url, DataRequest request) {
        DataResponse response = HttpRequestUtil.request(url, request);
        if (response != null && response.getCode() != null && response.getCode() == 0 && response.getData() != null) {
            Map<String, Object> data = new LinkedHashMap<>(asMap(response.getData()));
            data.put("_status", response.getMsg() == null || response.getMsg().isBlank() ? "请求完成" : response.getMsg());
            return data;
        }
        String message = response == null ? "请求失败，请检查后端是否启动。" : response.getMsg();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_status", message == null || message.isBlank() ? "请求失败" : message);
        return data;
    }

    private void openLink(String url) {
        if (url == null || url.isBlank()) {
            MessageDialog.showDialog("当前内容没有可打开的链接。");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            MessageDialog.showDialog("打开链接失败：" + e.getMessage());
        }
    }

    private String hotCategoryValue(String title) {
        return switch (title == null ? "" : title) {
            case "微博热搜" -> "weibo";
            case "知乎热榜" -> "zhihu";
            case "抖音热点" -> "douyin";
            case "B站热榜" -> "bilibili";
            case "36氪热榜" -> "36ke";
            default -> "weibo";
        };
    }

    private String boardModeValue(String title) {
        return "搜索榜".equals(title) ? "search" : "recommend";
    }

    private void refreshMovieWhenSelectionChanges(String oldValue, String newValue) {
        if (oldValue != null && newValue != null && !oldValue.equals(newValue)) {
            onMovieRefreshClick();
        }
    }

    private void refreshMusicWhenSelectionChanges(String oldValue, String newValue) {
        if (oldValue != null && newValue != null && !oldValue.equals(newValue)) {
            onMusicRefreshClick();
        }
    }

    private String fallbackTag(Map<String, Object> data) {
        return CommonMethod.getBoolean(data, "fallback") ? "（降级）" : "";
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;
            return map;
        }
        return Map.of();
    }

    private List<Map<String, Object>> asMapList(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) rawMap;
                    result.add(map);
                }
            }
        }
        return result;
    }
}
