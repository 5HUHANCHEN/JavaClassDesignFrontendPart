package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.util.CommonMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EntertainmentService {
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=Asia%%2FShanghai";
    private static final String HITOKOTO_URL = "https://v1.hitokoto.cn/?encode=json";
    private static final String MOVIE_URL = "https://api.freejk.com/shuju/hotlist/douban-movie";
    private static final String MUSIC_SEARCH_URL = "https://music.163.com/api/search/get/web?s=%s&type=1&offset=0&limit=12";
    private static final String MUSIC_BILI_HOT_URL = "https://api.freejk.com/shuju/hotlist/bilibili?type=3";
    private static final String FREEJK_HOT_URL = "https://api.freejk.com/shuju/hotlist/%s";
    private static final List<String> MUSIC_KEYWORDS = List.of("华语", "流行", "治愈", "校园", "轻音乐", "热歌", "民谣");
    private static final List<String> MOVIE_SEARCH_KEYWORDS = List.of("周末电影", "高分电影", "喜剧电影", "悬疑电影", "动画电影", "校园电影", "经典电影", "新片");
    private static final List<String> MUSIC_SEARCH_KEYWORDS = List.of("周杰伦", "林俊杰", "邓紫棋", "治愈歌单", "校园民谣", "轻音乐", "华语热歌", "睡前音乐");
    private static final List<MediaChannel> MOVIE_CHANNELS = List.of(
            new MediaChannel("渠道一", "Gimy TV剧迷", "https://gimy.tv", "https://gimy.tv/index.php?m=vod-search-wd-{keyword}.html"),
            new MediaChannel("渠道二", "影猫", "https://www.mvcat.com", "https://www.mvcat.com/search/?type=Title&word={keyword}"),
            new MediaChannel("渠道三", "完美看看", "https://www.wanmeikk.me", "https://www.wanmeikk.me/search.html?wd={keyword}"),
            new MediaChannel("渠道四", "蛋蛋赞 PPnix", "https://www.ppnix.com/cn", "https://www.ppnix.com/cn?s={keyword}"),
            new MediaChannel("渠道五", "我乐电影", "http://www.56dy.com", "http://www.56dy.com/movie/search/")
    );
    private static final List<MediaChannel> MUSIC_CHANNELS = List.of(
            new MediaChannel("渠道一", "泡椒音乐", "https://pjmp3.com", "https://pjmp3.com/search.php?keyword={keyword}"),
            new MediaChannel("渠道二", "种子音乐", "https://www.zz123.com", "https://www.zz123.com/search/?key={keyword}"),
            new MediaChannel("渠道三", "布谷音乐", "https://www.buguyy.top", "https://www.buguyy.top/?keyword={keyword}"),
            new MediaChannel("渠道四", "米兔音乐", "https://www.qqmp3.vip", "https://www.qqmp3.vip/?keyword={keyword}"),
            new MediaChannel("渠道五", "HiFiNi音乐", "https://flac.music.hi.cn", "https://flac.music.hi.cn/search?keyword={keyword}")
    );
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${entertainment.movie.siteName:}")
    private String configuredMovieSiteName;
    @Value("${entertainment.movie.homeUrl:}")
    private String configuredMovieHomeUrl;
    @Value("${entertainment.movie.apiUrl:}")
    private String configuredMovieApiUrl;
    @Value("${entertainment.movie.searchUrlTemplate:}")
    private String configuredMovieSearchUrlTemplate;
    @Value("${entertainment.music.siteName:}")
    private String configuredMusicSiteName;
    @Value("${entertainment.music.homeUrl:}")
    private String configuredMusicHomeUrl;
    @Value("${entertainment.music.apiUrl:}")
    private String configuredMusicApiUrl;
    @Value("${entertainment.music.searchUrlTemplate:}")
    private String configuredMusicSearchUrlTemplate;

    public EntertainmentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public DataResponse weather(DataRequest dataRequest) {
        String city = defaultString(dataRequest.getString("city"), "济南");
        try {
            CityLocation location = cityLocation(city);
            JsonNode root = getJson(String.format(Locale.ROOT, WEATHER_URL, location.latitude(), location.longitude()));
            JsonNode current = root.path("current");
            JsonNode daily = root.path("daily");
            Map<String, Object> item = new LinkedHashMap<>();
            String weather = weatherCodeName(current.path("weather_code").asInt(-1));
            String low = daily.path("temperature_2m_min").path(0).asText("");
            String high = daily.path("temperature_2m_max").path(0).asText("");
            item.put("city", location.displayName());
            item.put("date", daily.path("time").path(0).asText(LocalDate.now().toString()));
            item.put("week", "");
            item.put("weather", weather);
            item.put("low", low.isBlank() ? "" : low + "°C");
            item.put("high", high.isBlank() ? "" : high + "°C");
            item.put("wind", current.path("wind_speed_10m").asText("") + " km/h");
            item.put("air", "暂无空气质量");
            item.put("aqi", "");
            item.put("tip", "天气源：Open-Meteo。城市按内置坐标查询，避免免费接口城市识别错误。");
            item.put("suggestion", buildWeatherSuggestion(weather, low, high));
            return ok(result("Open-Meteo 天气", item.get("city") + " 今日天气", List.of(item), "天气查询成功", false));
        } catch (Exception e) {
            return ok(fallbackWeather(city));
        }
    }

    public DataResponse hitokoto(DataRequest dataRequest) {
        try {
            JsonNode root = getJson(HITOKOTO_URL);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("content", root.path("hitokoto").asText(""));
            item.put("from", root.path("from").asText(""));
            item.put("author", root.path("from_who").asText(""));
            item.put("type", root.path("type").asText(""));
            return ok(result("一言 Hitokoto", "每日一言", List.of(item), "一言获取成功", false));
        } catch (Exception e) {
            return ok(result("本地降级", "每日一言", List.of(mapOf(
                    "content", "今天也要把普通日子过得有一点点发光。",
                    "from", "本地句库",
                    "author", "娱乐资讯中心",
                    "type", "local"
            )), "一言接口暂不可用，已返回本地句子", true));
        }
    }

    public DataResponse movieRecommend(DataRequest dataRequest) {
        String keyword = defaultString(dataRequest.getString("keyword"), "电影");
        String channel = defaultString(dataRequest.getString("channel"), "渠道一");
        String mode = defaultString(dataRequest.getString("mode"), "recommend");
        if ("search".equalsIgnoreCase(mode) || mode.contains("搜索")) {
            return ok(applyMovieChannelLinks(movieSearchBoard(keyword), channel));
        }
        if ("recommend".equalsIgnoreCase(mode) || mode.contains("推荐")) {
            return ok(applyMovieChannelLinks(curatedMovieRecommend(), channel));
        }
        Optional<Map<String, Object>> configured = tryConfiguredMovieSite(keyword);
        if (configured.isPresent()) {
            return ok(applyMovieChannelLinks(configured.get(), channel));
        }
        try {
            JsonNode root = getJson(MOVIE_URL);
            if (root.path("code").asInt(0) != 200) {
                return ok(applyMovieChannelLinks(fallbackMovie(), channel));
            }
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return ok(applyMovieChannelLinks(fallbackMovie(), channel));
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (JsonNode movie : data) {
                if (items.size() >= 8) {
                    break;
                }
                String rawTitle = text(movie, "title", "name", "name_zh", "name_merge", "full name");
                items.add(mapOf(
                        "rank", text(movie, "top").isBlank() ? String.valueOf(items.size() + 1) : text(movie, "top"),
                        "title", movieTitle(rawTitle),
                        "rating", movieRating(rawTitle, text(movie, "rating")),
                        "genres", root.path("type").asText("新片榜"),
                        "pubdate", root.path("updateTime").asText(""),
                        "duration", text(movie, "durations"),
                        "imageUrl", text(movie, "cover", "movie_images"),
                        "url", text(movie, "url", "mobileUrl", "douban_url"),
                        "actor", text(movie, "desc", "actor"),
                        "director", text(movie, "author")
                ));
            }
            Map<String, Object> featured = items.get(ThreadLocalRandom.current().nextInt(items.size()));
            Map<String, Object> payload = result("FreeJK 豆瓣电影新片榜", "电影推荐", items, "电影榜单获取成功", false);
            payload.put("featured", featured);
            return ok(applyMovieChannelLinks(payload, channel));
        } catch (Exception e) {
            return ok(applyMovieChannelLinks(fallbackMovie(), channel));
        }
    }

    public DataResponse musicRecommend(DataRequest dataRequest) {
        String keyword = defaultString(dataRequest.getString("keyword"),
                MUSIC_KEYWORDS.get(ThreadLocalRandom.current().nextInt(MUSIC_KEYWORDS.size())));
        String channel = defaultString(dataRequest.getString("channel"), "渠道一");
        String mode = defaultString(dataRequest.getString("mode"), "recommend");
        if ("search".equalsIgnoreCase(mode) || mode.contains("搜索")) {
            return ok(applyMusicChannelLinks(musicSearchBoard(keyword), channel));
        }
        Optional<Map<String, Object>> configured = tryConfiguredMusicSite(keyword);
        if (configured.isPresent()) {
            return ok(applyMusicChannelLinks(configured.get(), channel));
        }
        try {
            JsonNode root = getJson(String.format(MUSIC_SEARCH_URL, encode(keyword)));
            JsonNode songs = root.path("result").path("songs");
            if (!songs.isArray() || songs.isEmpty()) {
                return ok(applyMusicChannelLinks(bilibiliMusicHot(), channel));
            }
            JsonNode song = songs.get(ThreadLocalRandom.current().nextInt(Math.min(songs.size(), 8)));
            String id = text(song, "id");
            Map<String, Object> item = mapOf(
                    "id", id,
                    "name", text(song, "name"),
                    "artist", artistNames(song.path("artists")),
                    "picUrl", song.path("album").path("picUrl").asText(""),
                    "url", id.isBlank() ? "https://music.163.com/" : "https://music.163.com/#/song?id=" + id,
                    "sort", "网易云搜索：" + keyword
            );
            return ok(applyMusicChannelLinks(result("网易云音乐公开搜索", "音乐推荐", List.of(item), "音乐推荐获取成功", false), channel));
        } catch (Exception e) {
            try {
                return ok(applyMusicChannelLinks(bilibiliMusicHot(), channel));
            } catch (Exception ignored) {
                return ok(applyMusicChannelLinks(fallbackMusic(), channel));
            }
        }
    }

    public DataResponse copywriting(DataRequest dataRequest) {
        String type = defaultString(dataRequest.getString("type"), "朋友圈文案");
        String keyword = defaultString(dataRequest.getString("keyword"), "今天");
        String text = buildCopywriting(type, keyword);
        Map<String, Object> item = mapOf(
                "type", type,
                "keyword", keyword,
                "content", text,
                "date", LocalDate.now().toString()
        );
        return ok(result("本地模板生成", "娱乐文案生成器", List.of(item), "文案生成成功", false));
    }

    public DataResponse horoscope(DataRequest dataRequest) {
        String sign = normalizeSign(dataRequest.getString("sign"));
        return ok(localHoroscope(sign));
    }

    public DataResponse hotList(DataRequest dataRequest) {
        String category = defaultString(dataRequest.getString("category"), "weibo");
        String url = hotListUrl(category);
        try {
            JsonNode root = getJson(url);
            JsonNode data = root.path("data");
            List<Map<String, Object>> items = new ArrayList<>();
            collectHotItems(data, items, 10);
            if (items.isEmpty()) {
                return ok(fallbackHotList(category));
            }
            String title = root.path("title").asText(hotListTitle(category)) + " " + root.path("type").asText("");
            return ok(result("FreeJK 热榜", title.trim(), items, "热榜获取成功", false));
        } catch (Exception e) {
            return ok(fallbackHotList(category));
        }
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://music.163.com/")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private Map<String, Object> result(String source, Object title, List<Map<String, Object>> items, String message, boolean fallback) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", true);
        map.put("source", source);
        map.put("title", title == null ? "" : String.valueOf(title));
        map.put("items", items);
        map.put("message", message);
        map.put("fallback", fallback);
        return map;
    }

    private DataResponse ok(Map<String, Object> map) {
        return CommonMethod.getReturnData(map, String.valueOf(map.getOrDefault("message", "")));
    }

    private Optional<Map<String, Object>> tryConfiguredMovieSite(String keyword) {
        boolean hasConfiguredSite = !isBlank(configuredMovieHomeUrl) || !isBlank(configuredMovieSearchUrlTemplate);
        String siteName = defaultString(configuredMovieSiteName, "自定义电影网站");
        if (!isBlank(configuredMovieApiUrl)) {
            try {
                JsonNode root = getJson(applyKeyword(configuredMovieApiUrl, keyword));
                List<Map<String, Object>> items = parseMovieItems(root, siteName);
                if (!items.isEmpty()) {
                    Map<String, Object> payload = result(siteName + " API", siteName + "电影推荐", items, "电影站点 API 获取成功", false);
                    payload.put("featured", items.get(ThreadLocalRandom.current().nextInt(items.size())));
                    return Optional.of(payload);
                }
            } catch (Exception ignored) {
                // 外部站点 API 不稳定时继续使用站点入口或内置备用源。
            }
        }
        return Optional.empty();
    }

    private Optional<Map<String, Object>> tryConfiguredMusicSite(String keyword) {
        boolean hasConfiguredSite = !isBlank(configuredMusicHomeUrl) || !isBlank(configuredMusicSearchUrlTemplate);
        String siteName = defaultString(configuredMusicSiteName, "自定义音乐网站");
        if (!isBlank(configuredMusicApiUrl)) {
            try {
                JsonNode root = getJson(applyKeyword(configuredMusicApiUrl, keyword));
                List<Map<String, Object>> items = parseMusicItems(root, siteName);
                if (!items.isEmpty()) {
                    return Optional.of(result(siteName + " API", siteName + "音乐推荐", List.of(items.get(0)), "音乐站点 API 获取成功", false));
                }
            } catch (Exception ignored) {
                // 外部站点 API 不稳定时继续使用站点入口或内置备用源。
            }
        }
        return Optional.empty();
    }

    private List<Map<String, Object>> parseMovieItems(JsonNode root, String siteName) {
        List<Map<String, Object>> items = new ArrayList<>();
        JsonNode array = firstArray(root);
        if (array == null) {
            return items;
        }
        for (JsonNode movie : array) {
            if (items.size() >= 8) {
                break;
            }
            String rawTitle = text(movie, "title", "name", "movieName", "vod_name", "videoName", "label");
            String title = movieTitle(rawTitle);
            if (title.isBlank()) {
                continue;
            }
            String url = absoluteUrl(text(movie, "url", "link", "href", "detailUrl", "playUrl", "vod_play_url"), configuredMovieHomeUrl);
            items.add(mapOf(
                    "rank", text(movie, "rank", "top", "index").isBlank() ? String.valueOf(items.size() + 1) : text(movie, "rank", "top", "index"),
                    "title", title,
                    "rating", movieRating(rawTitle, text(movie, "rating", "score", "rate", "doubanScore")),
                    "genres", defaultString(text(movie, "genres", "type", "category", "className", "vod_class"), siteName),
                    "pubdate", text(movie, "pubdate", "year", "date", "time", "updateTime", "vod_year"),
                    "duration", text(movie, "duration", "durations", "vod_duration"),
                    "imageUrl", absoluteUrl(text(movie, "imageUrl", "cover", "pic", "poster", "vod_pic"), configuredMovieHomeUrl),
                    "url", defaultString(url, configuredMovieLink(title)),
                    "actor", text(movie, "actor", "actors", "desc", "description", "vod_actor"),
                    "director", text(movie, "director", "author", "vod_director")
            ));
        }
        return items;
    }

    private List<Map<String, Object>> parseMusicItems(JsonNode root, String siteName) {
        List<Map<String, Object>> items = new ArrayList<>();
        JsonNode array = firstArray(root);
        if (array == null) {
            if (!text(root, "name", "title", "songName", "musicName").isBlank()) {
                array = objectMapper.createArrayNode().add(root);
            } else {
                return items;
            }
        }
        for (JsonNode song : array) {
            if (items.size() >= 8) {
                break;
            }
            String name = text(song, "name", "title", "songName", "musicName", "label");
            if (name.isBlank()) {
                continue;
            }
            String url = absoluteUrl(text(song, "url", "link", "href", "detailUrl", "songUrl", "playUrl"), configuredMusicHomeUrl);
            items.add(mapOf(
                    "id", text(song, "id", "songId", "musicId"),
                    "name", name,
                    "artist", defaultString(text(song, "artist", "author", "singer", "singers", "artists"), siteName),
                    "picUrl", absoluteUrl(text(song, "picUrl", "cover", "pic", "poster", "imageUrl"), configuredMusicHomeUrl),
                    "url", defaultString(url, configuredMusicLink(name)),
                    "sort", defaultString(text(song, "sort", "type", "category", "album"), siteName)
            ));
        }
        return items;
    }

    private Map<String, Object> configuredMovieEntrance(String keyword, String siteName, String message) {
        List<Map<String, Object>> items = List.of(
                mapOf("rank", "1", "title", "打开 " + siteName + " 搜索：" + keyword, "rating", "站点入口", "genres", "电影站点", "pubdate", "", "duration", "", "imageUrl", "", "url", configuredMovieLink(keyword), "actor", "请点击链接到网站查看详情", "director", ""),
                mapOf("rank", "2", "title", "机器人之梦", "rating", "8.9分", "genres", "动画 / 剧情", "pubdate", "本地推荐", "duration", "", "imageUrl", "", "url", configuredMovieLink("机器人之梦"), "actor", "", "director", ""),
                mapOf("rank", "3", "title", "年会不能停！", "rating", "8.1分", "genres", "喜剧 / 剧情", "pubdate", "本地推荐", "duration", "", "imageUrl", "", "url", configuredMovieLink("年会不能停"), "actor", "", "director", "")
        );
        Map<String, Object> payload = result(siteName + " 入口", siteName + "电影推荐", items, message, true);
        payload.put("featured", items.get(0));
        return payload;
    }

    private Map<String, Object> configuredMusicEntrance(String keyword, String siteName, String message) {
        Map<String, Object> item = mapOf(
                "id", "",
                "name", "打开 " + siteName + " 搜索：" + keyword,
                "artist", "请点击链接到网站查看详情",
                "picUrl", "",
                "url", configuredMusicLink(keyword),
                "sort", siteName + "入口"
        );
        return result(siteName + " 入口", siteName + "音乐推荐", List.of(item), message, true);
    }

    private Map<String, Object> applyMovieChannelLinks(Map<String, Object> payload, String channelValue) {
        MediaChannel channel = chooseChannel(MOVIE_CHANNELS, channelValue);
        for (Map<String, Object> item : mapList(payload.get("items"))) {
            String title = defaultString(String.valueOf(item.getOrDefault("title", "")), "电影");
            item.put("url", channel.link(title));
            item.put("actor", appendSourceTip(String.valueOf(item.getOrDefault("actor", "")), "打开链接将跳转到 " + channel.name()));
        }
        payload.put("source", payload.getOrDefault("source", "") + " / 链接：" + channel.name());
        payload.put("message", payload.getOrDefault("message", "电影榜单获取成功") + "，打开链接将跳转到 " + channel.name() + "。");
        return payload;
    }

    private Map<String, Object> applyMusicChannelLinks(Map<String, Object> payload, String channelValue) {
        MediaChannel channel = chooseChannel(MUSIC_CHANNELS, channelValue);
        for (Map<String, Object> item : mapList(payload.get("items"))) {
            String name = defaultString(String.valueOf(item.getOrDefault("name", "")), "音乐");
            item.put("url", channel.link(name));
            item.put("sort", appendSourceTip(String.valueOf(item.getOrDefault("sort", "")), "打开链接将跳转到 " + channel.name()));
        }
        payload.put("source", payload.getOrDefault("source", "") + " / 链接：" + channel.name());
        payload.put("message", payload.getOrDefault("message", "音乐推荐获取成功") + "，打开链接将跳转到 " + channel.name() + "。");
        return payload;
    }

    private MediaChannel chooseChannel(List<MediaChannel> channels, String value) {
        String selected = defaultString(value, "渠道一");
        return channels.stream()
                .filter(channel -> selected.contains(channel.label()) || selected.contains(channel.name()))
                .findFirst()
                .orElse(channels.get(0));
    }

    private List<Map<String, Object>> mapList(Object value) {
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

    private String appendSourceTip(String original, String tip) {
        String value = defaultString(original, "");
        return value.isBlank() ? tip : value + "；" + tip;
    }

    private Map<String, Object> fallbackWeather(String city) {
        Map<String, Object> item = mapOf(
                "city", city,
                "date", LocalDate.now().toString(),
                "week", "",
                "weather", "晴",
                "low", "8°C",
                "high", "18°C",
                "wind", "微风",
                "air", "良",
                "aqi", "",
                "tip", "天气接口暂不可用，先看看本地建议。",
                "suggestion", "适合出门散步，也适合给自己安排一杯热饮。"
        );
        return result("本地降级", city + " 今日天气", List.of(item), "天气接口暂不可用，已返回本地建议", true);
    }

    private CityLocation cityLocation(String city) {
        String value = defaultString(city, "济南");
        Map<String, CityLocation> map = Map.ofEntries(
                Map.entry("北京", new CityLocation("北京", "39.9042", "116.4074")),
                Map.entry("上海", new CityLocation("上海", "31.2304", "121.4737")),
                Map.entry("广州", new CityLocation("广州", "23.1291", "113.2644")),
                Map.entry("深圳", new CityLocation("深圳", "22.5431", "114.0579")),
                Map.entry("杭州", new CityLocation("杭州", "30.2741", "120.1551")),
                Map.entry("南京", new CityLocation("南京", "32.0603", "118.7969")),
                Map.entry("济南", new CityLocation("济南", "36.6512", "117.1201")),
                Map.entry("青岛", new CityLocation("青岛", "36.0671", "120.3826")),
                Map.entry("天津", new CityLocation("天津", "39.3434", "117.3616")),
                Map.entry("成都", new CityLocation("成都", "30.5728", "104.0668")),
                Map.entry("重庆", new CityLocation("重庆", "29.5630", "106.5516")),
                Map.entry("武汉", new CityLocation("武汉", "30.5928", "114.3055")),
                Map.entry("西安", new CityLocation("西安", "34.3416", "108.9398")),
                Map.entry("郑州", new CityLocation("郑州", "34.7466", "113.6254")),
                Map.entry("长沙", new CityLocation("长沙", "28.2282", "112.9388"))
        );
        return map.getOrDefault(value.replace("市", ""), new CityLocation(value, "36.6512", "117.1201"));
    }

    private String weatherCodeName(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1, 2 -> "多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "雨";
            case 71, 73, 75, 77, 85, 86 -> "雪";
            case 95, 96, 99 -> "雷雨";
            default -> "天气变化";
        };
    }

    private Map<String, Object> fallbackMovie() {
        List<Map<String, Object>> items = List.of(
                mapOf("rank", "1", "title", "机器人之梦", "rating", "8.9分", "genres", "动画 / 剧情", "pubdate", "近期推荐", "duration", "", "imageUrl", "", "url", "https://movie.douban.com/", "actor", "", "director", ""),
                mapOf("rank", "2", "title", "年会不能停！", "rating", "8.1分", "genres", "喜剧 / 剧情", "pubdate", "近期推荐", "duration", "", "imageUrl", "", "url", "https://movie.douban.com/", "actor", "", "director", "")
        );
        Map<String, Object> payload = result("本地降级", "电影推荐", items, "电影接口暂不可用，已返回本地推荐", true);
        payload.put("featured", items.get(0));
        return payload;
    }

    private Map<String, Object> curatedMovieRecommend() {
        List<Map<String, Object>> pool = new ArrayList<>(List.of(
                mapOf("title", "疯狂动物城", "rating", "9.2分", "genres", "动画 / 喜剧 / 冒险", "pubdate", "2016", "duration", "", "imageUrl", "", "url", "", "actor", "轻松、明亮，适合想看治愈故事的时候", "director", ""),
                mapOf("title", "寻梦环游记", "rating", "9.1分", "genres", "动画 / 音乐 / 家庭", "pubdate", "2017", "duration", "", "imageUrl", "", "url", "", "actor", "温暖催泪，适合周末慢慢看", "director", ""),
                mapOf("title", "星际穿越", "rating", "9.4分", "genres", "科幻 / 冒险 / 剧情", "pubdate", "2014", "duration", "", "imageUrl", "", "url", "", "actor", "宏大科幻和亲情线结合得很好", "director", ""),
                mapOf("title", "盗梦空间", "rating", "9.4分", "genres", "科幻 / 悬疑 / 动作", "pubdate", "2010", "duration", "", "imageUrl", "", "url", "", "actor", "节奏强，适合喜欢烧脑设定的人", "director", ""),
                mapOf("title", "头号玩家", "rating", "8.7分", "genres", "科幻 / 冒险 / 动作", "pubdate", "2018", "duration", "", "imageUrl", "", "url", "", "actor", "游戏和彩蛋很多，娱乐性很足", "director", ""),
                mapOf("title", "看不见的客人", "rating", "8.8分", "genres", "悬疑 / 犯罪 / 惊悚", "pubdate", "2016", "duration", "", "imageUrl", "", "url", "", "actor", "反转密集，适合悬疑片入门", "director", ""),
                mapOf("title", "调音师", "rating", "8.2分", "genres", "悬疑 / 犯罪 / 喜剧", "pubdate", "2018", "duration", "", "imageUrl", "", "url", "", "actor", "黑色幽默和反转结合得很巧", "director", ""),
                mapOf("title", "利刃出鞘", "rating", "8.1分", "genres", "悬疑 / 喜剧 / 犯罪", "pubdate", "2019", "duration", "", "imageUrl", "", "url", "", "actor", "群像推理，轻松但不无聊", "director", ""),
                mapOf("title", "怦然心动", "rating", "9.1分", "genres", "爱情 / 青春 / 剧情", "pubdate", "2010", "duration", "", "imageUrl", "", "url", "", "actor", "清新青春片，适合放松心情", "director", ""),
                mapOf("title", "爱乐之城", "rating", "8.4分", "genres", "爱情 / 歌舞 / 剧情", "pubdate", "2016", "duration", "", "imageUrl", "", "url", "", "actor", "画面和音乐都很适合夜晚观看", "director", ""),
                mapOf("title", "白日梦想家", "rating", "8.6分", "genres", "冒险 / 喜剧 / 剧情", "pubdate", "2013", "duration", "", "imageUrl", "", "url", "", "actor", "适合想给生活换一点空气的时候", "director", ""),
                mapOf("title", "心灵奇旅", "rating", "8.7分", "genres", "动画 / 奇幻 / 音乐", "pubdate", "2020", "duration", "", "imageUrl", "", "url", "", "actor", "轻柔地讨论生活意义，很适合学生看", "director", ""),
                mapOf("title", "楚门的世界", "rating", "9.4分", "genres", "剧情 / 科幻", "pubdate", "1998", "duration", "", "imageUrl", "", "url", "", "actor", "经典寓言式电影，余味很长", "director", ""),
                mapOf("title", "肖申克的救赎", "rating", "9.7分", "genres", "剧情 / 犯罪", "pubdate", "1994", "duration", "", "imageUrl", "", "url", "", "actor", "经典励志片，适合收藏重看", "director", ""),
                mapOf("title", "绿皮书", "rating", "8.9分", "genres", "剧情 / 喜剧 / 传记", "pubdate", "2018", "duration", "", "imageUrl", "", "url", "", "actor", "轻松但有分量，适合多人一起看", "director", ""),
                mapOf("title", "摔跤吧！爸爸", "rating", "9.0分", "genres", "剧情 / 传记 / 运动", "pubdate", "2016", "duration", "", "imageUrl", "", "url", "", "actor", "热血励志，情绪推动力很强", "director", ""),
                mapOf("title", "寄生虫", "rating", "8.8分", "genres", "剧情 / 惊悚", "pubdate", "2019", "duration", "", "imageUrl", "", "url", "", "actor", "社会寓言感强，适合想看深一点的片", "director", ""),
                mapOf("title", "小偷家族", "rating", "8.7分", "genres", "剧情 / 家庭 / 犯罪", "pubdate", "2018", "duration", "", "imageUrl", "", "url", "", "actor", "细腻克制，适合安静时观看", "director", ""),
                mapOf("title", "哈利·波特与魔法石", "rating", "9.2分", "genres", "奇幻 / 冒险", "pubdate", "2001", "duration", "", "imageUrl", "", "url", "", "actor", "经典奇幻入门，氛围感很强", "director", ""),
                mapOf("title", "海街日记", "rating", "8.8分", "genres", "剧情 / 家庭", "pubdate", "2015", "duration", "", "imageUrl", "", "url", "", "actor", "生活流、舒服、适合慢节奏放松", "director", "")
        ));
        Collections.shuffle(pool);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < Math.min(10, pool.size()); i++) {
            Map<String, Object> item = new LinkedHashMap<>(pool.get(i));
            item.put("rank", String.valueOf(i + 1));
            items.add(item);
        }
        Map<String, Object> payload = result("本地精选电影片库", "电影推荐榜", items, "电影推荐榜已刷新，每次会随机推荐不同影片", false);
        payload.put("featured", items.get(ThreadLocalRandom.current().nextInt(items.size())));
        return payload;
    }

    private Map<String, Object> movieSearchBoard(String keyword) {
        String base = defaultString(keyword, "电影");
        List<String> words = new ArrayList<>();
        words.add(base);
        for (String value : MOVIE_SEARCH_KEYWORDS) {
            if (!words.contains(value)) {
                words.add(value);
            }
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (String word : words) {
            if (items.size() >= 8) {
                break;
            }
            items.add(mapOf(
                    "rank", String.valueOf(items.size() + 1),
                    "title", word,
                    "rating", "搜索入口",
                    "genres", "点击打开后在所选渠道搜索",
                    "pubdate", "实时搜索",
                    "duration", "",
                    "imageUrl", "",
                    "url", "",
                    "actor", "搜索关键词：" + word,
                    "director", ""
            ));
        }
        Map<String, Object> payload = result("本地搜索榜", "电影搜索榜", items, "电影搜索榜已生成", false);
        payload.put("featured", items.get(0));
        return payload;
    }

    private Map<String, Object> fallbackMusic() {
        return result("本地降级", "音乐推荐", List.of(mapOf(
                "id", "",
                "name", "晴天",
                "artist", "周杰伦",
                "picUrl", "",
                "url", "https://music.163.com/",
                "sort", "本地推荐"
        )), "音乐接口暂不可用，已返回本地推荐", true);
    }

    private Map<String, Object> musicSearchBoard(String keyword) {
        String base = defaultString(keyword, MUSIC_SEARCH_KEYWORDS.get(ThreadLocalRandom.current().nextInt(MUSIC_SEARCH_KEYWORDS.size())));
        try {
            JsonNode root = getJson(String.format(MUSIC_SEARCH_URL, encode(base)));
            JsonNode songs = root.path("result").path("songs");
            if (songs.isArray() && !songs.isEmpty()) {
                List<Map<String, Object>> items = new ArrayList<>();
                for (JsonNode song : songs) {
                    if (items.size() >= 8) {
                        break;
                    }
                    items.add(mapOf(
                            "id", text(song, "id"),
                            "name", text(song, "name"),
                            "artist", artistNames(song.path("artists")),
                            "picUrl", song.path("album").path("picUrl").asText(""),
                            "url", "",
                            "sort", "搜索关键词：" + base
                    ));
                }
                return result("网易云音乐公开搜索", "音乐搜索榜", items, "音乐搜索榜获取成功", false);
            }
        } catch (Exception ignored) {
            // 免费搜索接口不可用时，继续返回本地搜索关键词榜。
        }
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> words = new ArrayList<>();
        words.add(base);
        for (String value : MUSIC_SEARCH_KEYWORDS) {
            if (!words.contains(value)) {
                words.add(value);
            }
        }
        for (String word : words) {
            if (items.size() >= 8) {
                break;
            }
            items.add(mapOf(
                    "id", "",
                    "name", word,
                    "artist", "搜索入口",
                    "picUrl", "",
                    "url", "",
                    "sort", "点击打开后在所选渠道搜索"
            ));
        }
        return result("本地搜索榜", "音乐搜索榜", items, "音乐搜索榜已生成", false);
    }

    private Map<String, Object> bilibiliMusicHot() throws Exception {
        JsonNode root = getJson(MUSIC_BILI_HOT_URL);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return fallbackMusic();
        }
        JsonNode video = data.get(ThreadLocalRandom.current().nextInt(Math.min(data.size(), 10)));
        String url = text(video, "url", "mobileUrl");
        Map<String, Object> item = mapOf(
                "id", text(video, "id"),
                "name", text(video, "title"),
                "artist", defaultString(text(video, "author", "desc"), "B站音乐热榜"),
                "picUrl", text(video, "cover"),
                "url", url.isBlank() ? root.path("link").asText("https://www.bilibili.com/v/popular/rank/all") : url,
                "sort", "B站音乐热榜"
        );
        return result("FreeJK B站音乐热榜", "音乐推荐", List.of(item), "网易云搜索暂不可用，已切换到 B站音乐热榜", false);
    }

    private Map<String, Object> fallbackHoroscope(String sign) {
        return result("本地降级", signDisplay(sign) + "运势", List.of(mapOf(
                "title", signDisplay(sign),
                "time", "今日",
                "shortcomment", "适合给生活加一点小惊喜",
                "yi", "尝试新鲜事",
                "ji", "纠结太久",
                "all", "88%",
                "love", "82%",
                "work", "86%",
                "money", "79%",
                "health", "90%",
                "text", "今天适合把想做的小事往前推一步，运气通常偏爱开始行动的人。"
        )), "星座接口暂不可用，已返回本地运势", true);
    }

    private Map<String, Object> localHoroscope(String sign) {
        int seed = Math.abs((sign + LocalDate.now()).hashCode());
        int all = 72 + seed % 24;
        int love = 68 + seed / 3 % 25;
        int work = 70 + seed / 5 % 24;
        int money = 66 + seed / 7 % 25;
        int health = 74 + seed / 11 % 20;
        return result("本地星座生成", signDisplay(sign) + "运势", List.of(mapOf(
                "title", signDisplay(sign),
                "time", "今日",
                "shortcomment", "适合把注意力放回自己能掌控的小事上",
                "yi", "整理计划、主动沟通、早睡",
                "ji", "冲动决定、过度脑补、拖延",
                "all", all + "%",
                "love", love + "%",
                "work", work + "%",
                "money", money + "%",
                "health", health + "%",
                "text", "今天的重点是降低不确定感。先把手头信息整理清楚，再决定下一步；如果遇到分歧，温和表达会比硬碰硬更有效。"
        )), "星座运势已生成", false);
    }

    private Map<String, Object> fallbackHotList(String category) {
        List<Map<String, Object>> items = List.of(
                mapOf("rank", 1, "title", "今日摸鱼指南：先把水杯接满", "hot", "本地热度", "url", ""),
                mapOf("rank", 2, "title", "适合晚饭前听的一首歌", "hot", "本地热度", "url", ""),
                mapOf("rank", 3, "title", "随机快乐事件：天气不错就去走走", "hot", "本地热度", "url", "")
        );
        return result("本地降级", hotListTitle(category), items, "热榜接口暂不可用，已返回本地热榜", true);
    }

    private String buildWeatherSuggestion(String weather, String low, String high) {
        String text = weather == null ? "" : weather;
        if (text.contains("雨")) {
            return "记得带伞，路上慢一点，今天更适合室内活动。";
        }
        if (text.contains("雪")) {
            return "注意保暖和防滑，热饮会让今天变得更舒服。";
        }
        if (text.contains("晴")) {
            return "适合散步、拍照、去操场吹风，也适合给心情充电。";
        }
        if (text.contains("阴") || text.contains("云")) {
            return "天气比较温和，适合处理待办，也适合约朋友吃饭。";
        }
        return "保持好心情，给今天安排一个轻松的小目标。";
    }

    private String buildCopywriting(String type, String keyword) {
        List<String> templates = switch (type) {
            case "土味情话" -> List.of(
                    "我本来想把“" + keyword + "”写进备忘录，后来发现它更适合写进心里。",
                    "你知道“" + keyword + "”和你有什么区别吗？它只能出现在今天，而你会出现在我的每个想法里。",
                    "今天不想讲大道理，只想把“" + keyword + "”和你一起放进温柔里。"
            );
            case "发疯文学" -> List.of(
                    "谁懂啊，今天的关键词竟然是“" + keyword + "”。我宣布从现在开始，世界必须为我的情绪让路三分钟。",
                    "已读乱回：我和“" + keyword + "”之间的关系，属于看似平静实则精神状态正在旋转。",
                    "别管，我现在就是被“" + keyword + "”精准拿捏的人类样本，建议立刻授予快乐补贴。"
            );
            case "社团宣传语" -> List.of(
                    "如果你也对“" + keyword + "”有一点点好奇，那就加入我们。热爱不必很大声，但一定要有人一起回应。",
                    "把“" + keyword + "”从想法变成经历，把独自感兴趣变成一群人一起发光。",
                    "我们不只欢迎擅长“" + keyword + "”的人，也欢迎愿意开始的人。"
            );
            case "表白文案" -> List.of(
                    "我见过很多和“" + keyword + "”有关的瞬间，最喜欢的还是你出现的那一秒。",
                    "如果“" + keyword + "”也有答案，那我的答案大概一直都是你。",
                    "想把“" + keyword + "”讲给你听，也想把普通日子慢慢讲成我们的故事。"
            );
            case "请假理由娱乐版" -> List.of(
                    "由于本人今日被“" + keyword + "”击中灵魂，需要短暂修复精神电量，望批准。",
                    "本人因“" + keyword + "”导致精神缓存过满，申请短暂离线清理后台进程。",
                    "今日状态被“" + keyword + "”强制接管，需请假恢复人类基础运行功能。"
            );
            default -> List.of(
                    "今天的关键词是“" + keyword + "”。普通日子也可以有一点闪光，所以我决定认真记录这一刻。",
                    "把“" + keyword + "”放进今天，像给平凡生活加了一枚小小的书签。",
                    "今天不必完美，只要和“" + keyword + "”有关的这一刻足够真诚。"
            );
        };
        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }

    private String hotListUrl(String category) {
        return switch (category) {
            case "weibo" -> String.format(FREEJK_HOT_URL, "sina");
            case "zhihu" -> String.format(FREEJK_HOT_URL, "zhihu");
            case "douyin" -> String.format(FREEJK_HOT_URL, "douyin");
            case "bilibili" -> String.format(FREEJK_HOT_URL, "bilibili");
            case "36ke" -> String.format(FREEJK_HOT_URL, "36kr");
            default -> String.format(FREEJK_HOT_URL, "sina");
        };
    }

    private String hotListTitle(String category) {
        return switch (category) {
            case "weibo" -> "微博热搜";
            case "zhihu" -> "知乎热榜";
            case "douyin" -> "抖音热点";
            case "bilibili" -> "B站热榜";
            case "36ke" -> "36氪热榜";
            default -> "微博热搜";
        };
    }

    private void collectHotItems(JsonNode node, List<Map<String, Object>> items, int limit) {
        if (node == null || node.isMissingNode() || items.size() >= limit) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectHotItems(child, items, limit);
                if (items.size() >= limit) {
                    return;
                }
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        String title = text(node, "title", "name", "word", "desc", "content");
        if (!title.isBlank()) {
            items.add(mapOf(
                    "rank", items.size() + 1,
                    "title", title,
                    "hot", text(node, "hot", "hotValue", "heat", "views", "index"),
                    "url", text(node, "url", "link", "mobileUrl", "href")
            ));
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext() && items.size() < limit) {
            collectHotItems(fields.next().getValue(), items, limit);
        }
    }

    private String normalizeSign(String sign) {
        String value = defaultString(sign, "scorpio").trim().toLowerCase(Locale.ROOT);
        Map<String, String> map = Map.ofEntries(
                Map.entry("白羊座", "aries"), Map.entry("aries", "aries"),
                Map.entry("金牛座", "taurus"), Map.entry("taurus", "taurus"),
                Map.entry("双子座", "gemini"), Map.entry("gemini", "gemini"),
                Map.entry("巨蟹座", "cancer"), Map.entry("cancer", "cancer"),
                Map.entry("狮子座", "leo"), Map.entry("leo", "leo"),
                Map.entry("处女座", "virgo"), Map.entry("virgo", "virgo"),
                Map.entry("天秤座", "libra"), Map.entry("libra", "libra"),
                Map.entry("天蝎座", "scorpio"), Map.entry("scorpio", "scorpio"),
                Map.entry("射手座", "sagittarius"), Map.entry("sagittarius", "sagittarius"),
                Map.entry("摩羯座", "capricorn"), Map.entry("capricorn", "capricorn"),
                Map.entry("水瓶座", "aquarius"), Map.entry("aquarius", "aquarius"),
                Map.entry("双鱼座", "pisces"), Map.entry("pisces", "pisces")
        );
        return map.getOrDefault(value, "scorpio");
    }

    private String signDisplay(String sign) {
        return switch (sign) {
            case "aries" -> "白羊座";
            case "taurus" -> "金牛座";
            case "gemini" -> "双子座";
            case "cancer" -> "巨蟹座";
            case "leo" -> "狮子座";
            case "virgo" -> "处女座";
            case "libra" -> "天秤座";
            case "sagittarius" -> "射手座";
            case "capricorn" -> "摩羯座";
            case "aquarius" -> "水瓶座";
            case "pisces" -> "双鱼座";
            default -> "天蝎座";
        };
    }

    private String normalizeHoroscopeTime(String time) {
        String value = defaultString(time, "today");
        if ("明日".equals(value) || "nextday".equals(value)) {
            return "nextday";
        }
        if ("本周".equals(value) || "week".equals(value)) {
            return "week";
        }
        if ("本月".equals(value) || "month".equals(value)) {
            return "month";
        }
        return "today";
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull() && !value.asText("").isBlank()) {
                return value.asText("");
            }
        }
        return "";
    }

    private JsonNode firstArray(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        for (String path : List.of("data", "items", "list", "results", "records", "rows", "result.songs", "result.list", "result.data", "result.items")) {
            JsonNode node = path(root, path);
            if (node.isArray()) {
                return node;
            }
        }
        if (!root.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            JsonNode found = firstArray(fields.next().getValue());
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private JsonNode path(JsonNode root, String path) {
        JsonNode node = root;
        for (String part : path.split("\\.")) {
            node = node.path(part);
        }
        return node;
    }

    private String configuredMovieLink(String keyword) {
        return configuredLink(configuredMovieSearchUrlTemplate, configuredMovieHomeUrl, keyword);
    }

    private String configuredMusicLink(String keyword) {
        return configuredLink(configuredMusicSearchUrlTemplate, configuredMusicHomeUrl, keyword);
    }

    private String configuredLink(String searchTemplate, String homeUrl, String keyword) {
        if (!isBlank(searchTemplate)) {
            return applyKeyword(searchTemplate, keyword);
        }
        return defaultString(homeUrl, "");
    }

    private String applyKeyword(String template, String keyword) {
        String encoded = encode(defaultString(keyword, ""));
        String raw = defaultString(keyword, "");
        String result = defaultString(template, "")
                .replace("{keyword}", encoded)
                .replace("{keywordRaw}", raw)
                .replace("${keyword}", encoded)
                .replace("${keywordRaw}", raw);
        if (!result.contains("{q}")) {
            return result;
        }
        return result.replace("{q}", encoded);
    }

    private String absoluteUrl(String url, String homeUrl) {
        String value = defaultString(url, "");
        if (value.isBlank() || value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        String base = defaultString(homeUrl, "");
        if (base.isBlank()) {
            return value;
        }
        URI baseUri = URI.create(base.endsWith("/") ? base : base + "/");
        return baseUri.resolve(value.startsWith("/") ? value.substring(1) : value).toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record MediaChannel(String label, String name, String homeUrl, String searchUrlTemplate) {
        private String link(String keyword) {
            String encoded = URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8);
            if (searchUrlTemplate == null || searchUrlTemplate.isBlank()) {
                return homeUrl;
            }
            return searchUrlTemplate.replace("{keyword}", encoded).replace("{keywordRaw}", keyword == null ? "" : keyword);
        }
    }

    private record CityLocation(String displayName, String latitude, String longitude) {
    }

    private String artistNames(JsonNode artists) {
        if (!artists.isArray() || artists.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (JsonNode artist : artists) {
            String name = text(artist, "name");
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return String.join(" / ", names);
    }

    private String stripBookMarks(String value) {
        return defaultString(value, "").replace("《", "").replace("》", "");
    }

    private String movieTitle(String rawTitle) {
        String title = stripBookMarks(rawTitle);
        return title.replaceFirst("^【[^】]+】", "").trim();
    }

    private String movieRating(String rawTitle, String defaultRating) {
        String title = defaultString(rawTitle, "");
        int start = title.indexOf('【');
        int end = title.indexOf('】');
        if (start >= 0 && end > start) {
            return title.substring(start + 1, end) + "分";
        }
        return defaultString(defaultRating, "暂无评分");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
