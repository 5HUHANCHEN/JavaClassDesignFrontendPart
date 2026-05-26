package com.teach.javafx.request;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpRequestUtil {
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static String serverUrl = "http://localhost:22223";
    //public static String serverUrl = "http://47.105.96.191:22223";


    public static void close() {
    }

    public static String login(LoginRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .headers("Content-Type", "application/json")
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JwtResponse jwt = gson.fromJson(response.body(), JwtResponse.class);
                AppStore.setJwt(jwt);
                return null;
            }
            if (response.statusCode() == 401) {
                return "用户名或密码错误。";
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return "登录失败，请检查服务器连接。";
    }

    public static String switchAccount(String username) {
        DataRequest request = new DataRequest();
        request.add("username", username);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/auth/switchAccount"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .headers("Content-Type", "application/json")
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JwtResponse jwt = gson.fromJson(response.body(), JwtResponse.class);
                AppStore.setJwt(jwt);
                return null;
            }
            DataResponse dataResponse = gson.fromJson(response.body(), DataResponse.class);
            if (dataResponse != null && dataResponse.getMsg() != null && !dataResponse.getMsg().isBlank()) {
                return dataResponse.getMsg();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return "账号切换失败，请检查服务器连接。";
    }

    public static DataResponse request(String url, DataRequest request) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + url))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                    .header("Content-Type", "application/json");

            if (AppStore.getJwt() != null && AppStore.getJwt().getToken() != null) {
                builder.header("Authorization", "Bearer " + AppStore.getJwt().getToken());
            }

            HttpRequest httpRequest = builder.build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MyTreeNode requestTreeNode(String url, DataRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + url))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .headers("Content-Type", "application/json")
                .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), MyTreeNode.class);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<MyTreeNode> requestTreeNodeList(String url, DataRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + url))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .headers("Content-Type", "application/json")
                .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Map<String, Object>> list = gson.fromJson(response.body(), List.class);
                List<MyTreeNode> result = new ArrayList<>();
                for (Map<String, Object> item : list) {
                    result.add(new MyTreeNode(item));
                }
                return result;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<OptionItem> requestOptionItemList(String url, DataRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + url))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .headers("Content-Type", "application/json")
                .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                OptionItemList optionItemList = gson.fromJson(response.body(), OptionItemList.class);
                return optionItemList.getItemList();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<OptionItem> requestOptionItemDataList(String url, DataRequest request) {
        DataResponse response = request(url, request);
        if (response == null || response.getData() == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> mapList = gson.fromJson(gson.toJson(response.getData()), List.class);
        List<OptionItem> itemList = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            itemList.add(new OptionItem(map));
        }
        return itemList;
    }

    public static List<OptionItem> getDictionaryOptionItemList(String code) {
        DataRequest request = new DataRequest();
        request.add("code", code);
        return requestOptionItemList("/api/base/getDictionaryOptionItemList", request);
    }

    public static byte[] requestByteData(String url, DataRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + url))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .headers("Content-Type", "application/json")
                .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                .build();
        try {
            HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DataResponse uploadFile(String uri, String fileName, String remoteFile) {
        try {
            Path file = Path.of(fileName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + uri + "?uploader=HttpTestApp&remoteFile=" + remoteFile + "&fileName=" + file.getFileName()))
                    .POST(HttpRequest.BodyPublishers.ofFile(file))
                    .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DataResponse importData(String url, String fileName, String paras) {
        try {
            Path file = Path.of(fileName);
            String urlStr = serverUrl + url + "?uploader=HttpTestApp&fileName=" + file.getFileName();
            if (paras != null && !paras.isEmpty()) {
                urlStr += "&" + paras;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .POST(HttpRequest.BodyPublishers.ofFile(file))
                    .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DataResponse uploadPhotoBlob(String fileName, Integer personId) {
        try {
            Path file = Path.of(fileName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/base/uploadPhotoBlob?uploader=JavaFxClient&remoteFile=" + personId + "&fileName=" + file.getFileName()))
                    .POST(HttpRequest.BodyPublishers.ofFile(file))
                    .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DataResponse uploadHomeworkImage(String fileName, Integer homeworkId) {
        return uploadImage("/api/homework/uploadHomeworkImage", fileName, "homeworkId", homeworkId);
    }

    public static DataResponse uploadSubmissionImage(String fileName, Integer submissionId) {
        return uploadImage("/api/homework/uploadSubmissionImage", fileName, "submissionId", submissionId);
    }

    public static DataResponse uploadCourseMaterialFile(String fileName, Integer materialId) {
        return uploadImage("/api/courseMaterial/uploadMaterialFile", fileName, "materialId", materialId);
    }

    private static DataResponse uploadImage(String uri, String fileName, String idName, Integer idValue) {
        if (idValue == null) {
            return null;
        }
        try {
            Path file = Path.of(fileName);
            String encodedFileName = URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + uri + "?" + idName + "=" + idValue + "&fileName=" + encodedFileName))
                    .POST(HttpRequest.BodyPublishers.ofFile(file))
                    .headers("Authorization", "Bearer " + AppStore.getJwt().getToken())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
