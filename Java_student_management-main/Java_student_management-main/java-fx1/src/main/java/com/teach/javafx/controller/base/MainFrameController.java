package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.MyTreeNode;
import com.teach.javafx.request.OptionItem;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainFrameController {
    private static final String THEME_DEFAULT = "默认模式";
    private static final String THEME_DARK = "深色模式";
    private static final String THEME_COMPACT = "紧凑模式";

    class ChangePanelHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            changeContent(actionEvent);
        }
    }

    private final Map<String, Tab> tabMap = new HashMap<>();
    private final Map<String, Scene> sceneMap = new HashMap<>();
    private final Map<String, ToolController> controlMap = new HashMap<>();

    @FXML private MenuBar menuBar;
    @FXML private TreeView<MyTreeNode> menuTree;
    @FXML protected TabPane contentTabPane;
    @FXML private Label systemPrompt;
    @FXML private BorderPane rootPane;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private ComboBox<String> switchRoleComboBox;
    @FXML private ComboBox<OptionItem> switchAccountComboBox;
    @FXML private Label rightStatusLabel;

    private static String lastTheme = THEME_DEFAULT;
    private ChangePanelHandler handler;
    private boolean updatingSwitchSelector;

    private void loadCss() {
        URL cssUrl = getClass().getResource("/com/teach/javafx/css/main-frame.css");
        if (cssUrl != null) {
            rootPane.getStylesheets().clear();
            rootPane.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    public void addMenuItem(Menu menu, String name, String title) {
        MenuItem item = new MenuItem();
        item.setText(title);
        item.setId(name);
        item.setOnAction(this::changeContent);
        menu.getItems().add(item);
    }

    void addMenuItems(Menu parent, List<Map> menuList) {
        for (Map menuData : menuList) {
            List<Map> childList = (List<Map>) menuData.get("sList");
            String name = (String) menuData.get("name");
            String title = (String) menuData.get("title");
            if (childList == null || childList.isEmpty()) {
                addMenuItem(parent, name, title);
            } else {
                Menu childMenu = new Menu();
                childMenu.setText(title);
                addMenuItems(childMenu, childList);
                parent.getItems().add(childMenu);
            }
        }
    }

    void addMenuItems(TreeItem<MyTreeNode> parent, List<Map> menuList) {
        for (Map menuData : menuList) {
            List<Map> childList = (List<Map>) menuData.get("sList");
            TreeItem<MyTreeNode> menuItem = new TreeItem<>(
                    new MyTreeNode(null, (String) menuData.get("name"), (String) menuData.get("title"), 0)
            );
            parent.getChildren().add(menuItem);
            if (childList != null && !childList.isEmpty()) {
                addMenuItems(menuItem, childList);
            }
        }
    }

    public void initMenuBar(List<Map> menuList) {
        menuBar.getMenus().clear();
        for (Map menuData : menuList) {
            Menu menu = new Menu();
            menu.setText((String) menuData.get("title"));
            List<Map> childList = (List<Map>) menuData.get("sList");
            if (childList != null && !childList.isEmpty()) {
                addMenuItems(menu, childList);
            } else if (menuData.get("name") != null) {
                addMenuItem(menu, (String) menuData.get("name"), (String) menuData.get("title"));
            }
            menuBar.getMenus().add(menu);
        }
    }

    public void initMenuTree(List<Map> menuList) {
        MyTreeNode rootNode = new MyTreeNode(null, null, "菜单", 0);
        TreeItem<MyTreeNode> root = new TreeItem<>(rootNode);
        for (Map menuData : menuList) {
            List<Map> childList = (List<Map>) menuData.get("sList");
            TreeItem<MyTreeNode> treeMenu = new TreeItem<>(
                    new MyTreeNode(null, (String) menuData.get("name"), (String) menuData.get("title"), getInteger(menuData.get("isLeft")))
            );
            if (childList != null && !childList.isEmpty()) {
                addMenuItems(treeMenu, childList);
            }
            root.getChildren().add(treeMenu);
        }
        menuTree.setRoot(root);
        menuTree.setShowRoot(false);
        menuTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.getValue() == null) {
                return;
            }
            MyTreeNode menu = newValue.getValue();
            String name = menu.getValue();
            if (name == null || name.isEmpty()) {
                return;
            }
            if ("logout".equals(name)) {
                logout();
            } else if (name.endsWith("Command")) {
                try {
                    Method method = this.getClass().getMethod(name);
                    method.invoke(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                changeContent(name, menu.getLabel());
            }
        });
        root.setExpanded(true);
        for (TreeItem<MyTreeNode> child : root.getChildren()) {
            child.setExpanded(true);
        }
    }

    private void addTeacherMenuToPersonManage(List<Map> menuList) {
        if (!"ROLE_ADMIN".equals(AppStore.getJwt().getRole())) {
            return;
        }
        for (Map menuData : menuList) {
            String title = String.valueOf(menuData.get("title"));
            if (!title.contains("人员") && !title.contains("人事")) {
                continue;
            }
            List<Map> childList = (List<Map>) menuData.get("sList");
            if (childList == null) {
                childList = new ArrayList<>();
                menuData.put("sList", childList);
            }
            if (!containsMenu(childList, "teacher-panel")) {
                childList.add(createLeafMenu("teacher-panel", "教师管理"));
            }
            return;
        }
        if (!containsMenu(menuList, "teacher-panel")) {
            menuList.add(createLeafMenu("teacher-panel", "教师管理"));
        }
    }

    private void addTeacherProfileMenu(List<Map> menuList) {
        if ("ROLE_TEACHER".equals(AppStore.getJwt().getRole()) && !containsMenu(menuList, "teacher-profile-panel")) {
            menuList.add(createLeafMenu("teacher-profile-panel", "教师个人简介"));
        }
    }

    private void addRegisterApplyMenu(List<Map> menuList) {
        if ("ROLE_ADMIN".equals(AppStore.getJwt().getRole()) && !containsMenu(menuList, "register-apply-list")) {
            menuList.add(createLeafMenu("register-apply-list", "账号申请审核"));
        }
    }

    private void addStudentSelfMenu(List<Map> menuList) {
        if ("ROLE_STUDENT".equals(AppStore.getJwt().getRole()) && !containsMenu(menuList, "student-self-panel")) {
            menuList.add(createLeafMenu("student-self-panel", "我的信息"));
        }
    }

    private void addStudentLeaveMenu(List<Map> menuList) {
        String role = AppStore.getJwt().getRole();
        if (("ROLE_STUDENT".equals(role) || "ROLE_TEACHER".equals(role)) && !containsMenu(menuList, "student-leave-panel")) {
            menuList.add(createLeafMenu("student-leave-panel", "学生请假"));
        }
    }

    private void addStudentStatisticsMenu(List<Map> menuList) {
        String role = AppStore.getJwt().getRole();
        if (("ROLE_ADMIN".equals(role) || "ROLE_TEACHER".equals(role)) && !containsMenu(menuList, "student-statistics-panel")) {
            menuList.add(createLeafMenu("student-statistics-panel", "学生统计"));
        }
    }

    private void addCourseScheduleMenu(List<Map> menuList) {
        if ("ROLE_STUDENT".equals(AppStore.getJwt().getRole()) && !containsMenu(menuList, "course-schedule-panel")) {
            menuList.add(createLeafMenu("course-schedule-panel", "课表管理"));
        }
    }

    private void addCommunityMenu(List<Map> menuList) {
        String role = AppStore.getJwt().getRole();
        if (("ROLE_STUDENT".equals(role) || "ROLE_TEACHER".equals(role) || "ROLE_ADMIN".equals(role))
                && !containsMenu(menuList, "community-panel")) {
            menuList.add(createLeafMenu("community-panel", "校园社区"));
        }
    }

    private void addAiAssistantMenu(List<Map> menuList) {
        String role = AppStore.getJwt().getRole();
        if (("ROLE_STUDENT".equals(role) || "ROLE_TEACHER".equals(role) || "ROLE_ADMIN".equals(role))
                && !containsMenu(menuList, "ai-assistant-panel")) {
            menuList.add(createLeafMenu("ai-assistant-panel", "智能助手"));
        }
    }

    private void ensureCoreMenus(List<Map> menuList) {
        String role = AppStore.getJwt() == null ? "" : AppStore.getJwt().getRole();
        Map<String, Object> personalMenu = ensureRootMenu(menuList, 1, "个人信息");
        ensureChildMenu(personalMenu, 10, "dashboard-panel", "首页仪表盘", role, "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_TEACHER");
        ensureChildMenu(personalMenu, 11, "system_summary_panel", "系统简介", role, "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_TEACHER");
        ensureChildMenu(personalMenu, 12, "base/password-panel", "修改密码", role, "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_TEACHER");
        ensureChildMenu(personalMenu, 15, "logout", "退出", role, "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_TEACHER");

        Map<String, Object> systemMenu = ensureRootMenu(menuList, 2, "系统管理");
        ensureChildMenu(systemMenu, 21, "base/menu-panel", "菜单管理", role, "ROLE_ADMIN");
        ensureChildMenu(systemMenu, 22, "base/dictionary-panel", "字典管理", role, "ROLE_ADMIN");

        Map<String, Object> personMenu = ensureRootMenu(menuList, 3, "人员管理");
        ensureChildMenu(personMenu, 31, "student-panel", "学生管理", role, "ROLE_ADMIN");

        Map<String, Object> teachingMenu = ensureRootMenu(menuList, 4, "教务管理");
        ensureChildMenu(teachingMenu, 41, "course-panel", "课程管理", role, "ROLE_ADMIN");
        ensureChildMenu(teachingMenu, 42, "score-table-panel", "成绩管理", role, "ROLE_ADMIN");
        ensureChildMenu(teachingMenu, 44, "homework-panel", "作业中心", role, "ROLE_ADMIN", "ROLE_TEACHER", "ROLE_STUDENT");
        ensureChildMenu(teachingMenu, 45, "course-material-panel", "课程资料", role, "ROLE_ADMIN", "ROLE_TEACHER", "ROLE_STUDENT");

        Map<String, Object> demoMenu = ensureRootMenu(menuList, 5, "示例程序");
        ensureChildMenu(demoMenu, 51, "base/control-demo-panel", "组件示例", role, "ROLE_ADMIN");

        Map<String, Object> entertainmentMenu = ensureRootMenu(menuList, 6, "生活娱乐");
        ensureChildMenu(entertainmentMenu, 61, "entertainment-center-panel", "娱乐资讯中心", role, "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_TEACHER");
        ensureChildMenu(entertainmentMenu, 62, "divination-panel", "易学与塔罗指南", role, "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_TEACHER");
        ensureChildMenu(entertainmentMenu, 63, "toolbox-panel", "百宝箱", role, "ROLE_ADMIN", "ROLE_STUDENT", "ROLE_TEACHER");
        sortRootMenus(menuList);
    }

    private Map<String, Object> ensureRootMenu(List<Map> menuList, int id, String title) {
        Map<String, Object> menu = findMenuById(menuList, id);
        if (menu == null) {
            menu = new HashMap<>();
            menu.put("id", id);
            menu.put("name", null);
            menu.put("path", "");
            menu.put("sList", new ArrayList<>());
            menuList.add(menu);
        }
        menu.put("title", title);
        if (!(menu.get("sList") instanceof List<?>)) {
            menu.put("sList", new ArrayList<>());
        }
        return menu;
    }

    private void ensureChildMenu(Map<String, Object> parentMenu, int id, String name, String title, String role, String... allowedRoles) {
        if (!isRoleAllowed(role, allowedRoles)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Map> childList = (List<Map>) parentMenu.get("sList");
        Map<String, Object> child = findMenuById(childList, id);
        if (child == null && !containsMenu(childList, name)) {
            child = createLeafMenu(name, title);
            child.put("id", id);
            childList.add(child);
            return;
        }
        if (child != null) {
            child.put("name", name);
            child.put("path", name);
            child.put("title", title);
            if (!(child.get("sList") instanceof List<?>)) {
                child.put("sList", new ArrayList<>());
            }
        }
    }

    private boolean isRoleAllowed(String role, String... allowedRoles) {
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> findMenuById(List<Map> menuList, int id) {
        if (menuList == null) {
            return null;
        }
        for (Map menu : menuList) {
            if (getInteger(menu.get("id")) == id) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMenu = (Map<String, Object>) menu;
                return typedMenu;
            }
        }
        return null;
    }

    private void sortRootMenus(List<Map> menuList) {
        menuList.sort((left, right) -> Integer.compare(getMenuOrder(left), getMenuOrder(right)));
    }

    private int getMenuOrder(Map menu) {
        int id = getInteger(menu.get("id"));
        if (id >= 1 && id <= 5) {
            return id;
        }
        return 100 + id;
    }

    private void removeEmptyContainerMenus(List<Map> menuList) {
        if (menuList == null) {
            return;
        }
        Iterator<Map> iterator = menuList.iterator();
        while (iterator.hasNext()) {
            Map menu = iterator.next();
            Object childObject = menu.get("sList");
            if (childObject instanceof List<?> childList) {
                @SuppressWarnings("unchecked")
                List<Map> children = (List<Map>) childList;
                removeEmptyContainerMenus(children);
            }
            String name = (String) menu.get("name");
            Object children = menu.get("sList");
            boolean hasChildren = children instanceof List<?> childList && !childList.isEmpty();
            if ((name == null || name.isEmpty()) && !hasChildren) {
                iterator.remove();
            }
        }
    }

    private Map<String, Object> createLeafMenu(String name, String title) {
        Map<String, Object> menu = new HashMap<>();
        menu.put("name", name);
        menu.put("title", title);
        menu.put("isLeft", 1);
        menu.put("sList", new ArrayList<>());
        return menu;
    }

    private boolean containsMenu(List<Map> menuList, String name) {
        if (menuList == null) {
            return false;
        }
        for (Map menu : menuList) {
            if (name.equals(menu.get("name"))) {
                return true;
            }
            Object childObject = menu.get("sList");
            if (childObject instanceof List<?> childList) {
                @SuppressWarnings("unchecked")
                List<Map> children = (List<Map>) childList;
                if (containsMenu(children, name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void initThemeBox() {
        if (themeComboBox == null) {
            return;
        }
        themeComboBox.getItems().setAll(THEME_DEFAULT, THEME_DARK, THEME_COMPACT);
        themeComboBox.getSelectionModel().select(lastTheme);
        applyTheme(lastTheme);
        themeComboBox.setOnAction(e -> {
            String mode = themeComboBox.getValue();
            lastTheme = mode;
            applyTheme(mode);
        });
    }

    private void initAccountSwitchBox() {
        if (switchRoleComboBox == null || switchAccountComboBox == null) {
            return;
        }
        updatingSwitchSelector = true;
        switchRoleComboBox.getItems().setAll("管理员", "学生", "教师");
        switchRoleComboBox.getSelectionModel().select(toRoleDisplayName(AppStore.getJwt().getRole()));
        switchAccountComboBox.setPromptText("选择账号");
        switchRoleComboBox.setOnAction(e -> loadSwitchAccountOptions(false, true));
        switchAccountComboBox.setOnAction(e -> handleSwitchAccountSelection());
        updatingSwitchSelector = false;
        loadSwitchAccountOptions(true, false);
    }

    private void loadSwitchAccountOptions(boolean selectCurrentAccount, boolean autoSwitchFirstAccount) {
        if (switchRoleComboBox == null || switchAccountComboBox == null) {
            return;
        }
        String roleType = toRoleTypeCode(switchRoleComboBox.getValue());
        if (roleType == null) {
            switchAccountComboBox.getItems().clear();
            return;
        }
        DataRequest request = new DataRequest();
        request.add("roleType", roleType);
        List<OptionItem> itemList = HttpRequestUtil.requestOptionItemDataList("/auth/getSwitchAccountList", request);
        updatingSwitchSelector = true;
        switchAccountComboBox.getItems().setAll(itemList);
        OptionItem selectedItem = null;
        if (selectCurrentAccount) {
            String currentUsername = AppStore.getJwt() == null ? null : AppStore.getJwt().getUsername();
            OptionItem currentItem = null;
            for (OptionItem item : itemList) {
                if (item != null && item.getValue() != null && item.getValue().equals(currentUsername)) {
                    currentItem = item;
                    break;
                }
            }
            if (currentItem != null) {
                switchAccountComboBox.getSelectionModel().select(currentItem);
                selectedItem = currentItem;
            } else {
                switchAccountComboBox.getSelectionModel().clearSelection();
            }
        } else if (!itemList.isEmpty()) {
            selectedItem = itemList.get(0);
            switchAccountComboBox.getSelectionModel().select(selectedItem);
        } else {
            switchAccountComboBox.getSelectionModel().clearSelection();
        }
        updatingSwitchSelector = false;
        if (autoSwitchFirstAccount && selectedItem != null) {
            switchAccountImmediately(selectedItem);
        }
    }

    private void handleSwitchAccountSelection() {
        if (updatingSwitchSelector || switchAccountComboBox == null) {
            return;
        }
        OptionItem selectedItem = switchAccountComboBox.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getValue() == null || selectedItem.getValue().isBlank()) {
            return;
        }
        switchAccountImmediately(selectedItem);
    }

    private void switchAccountImmediately(OptionItem selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null || selectedItem.getValue().isBlank()) {
            return;
        }
        String currentUsername = AppStore.getJwt() == null ? "" : AppStore.getJwt().getUsername();
        if (selectedItem.getValue().equals(currentUsername)) {
            return;
        }
        String message = HttpRequestUtil.switchAccount(selectedItem.getValue());
        if (message != null) {
            MessageDialog.showDialog(message);
            loadSwitchAccountOptions(true, false);
            return;
        }
        reloadMainFrame();
    }

    private void reloadMainFrame() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/main-frame.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), -1, -1);
            AppStore.setMainFrameController(fxmlLoader.getController());
            MainApplication.resetStage("教学管理系统", scene);
        } catch (IOException e) {
            e.printStackTrace();
            MessageDialog.showDialog("账号切换成功，但主界面刷新失败。");
        }
    }

    private String toRoleDisplayName(String roleName) {
        return switch (roleName) {
            case "ROLE_ADMIN" -> "管理员";
            case "ROLE_STUDENT" -> "学生";
            case "ROLE_TEACHER" -> "教师";
            default -> "学生";
        };
    }

    private String toRoleTypeCode(String displayName) {
        return switch (displayName) {
            case "管理员" -> "ADMIN";
            case "学生" -> "STUDENT";
            case "教师" -> "TEACHER";
            default -> null;
        };
    }

    private void applyTheme(String mode) {
        if (rootPane == null) {
            return;
        }
        applyThemeClasses(rootPane, mode);
        syncOpenedTabsTheme(mode);
    }

    private void syncOpenedTabsTheme(String mode) {
        for (Scene scene : sceneMap.values()) {
            if (scene != null && scene.getRoot() != null) {
                applyThemeClasses(scene.getRoot(), mode);
            }
        }
        for (Tab tab : contentTabPane.getTabs()) {
            if (tab.getContent() instanceof Parent parent) {
                applyThemeClasses(parent, mode);
            }
        }
    }

    private void applyThemeClasses(Parent root, String mode) {
        root.getStyleClass().removeAll("dark-mode", "compact-mode");
        if (THEME_DARK.equals(mode)) {
            root.getStyleClass().add("dark-mode");
        } else if (THEME_COMPACT.equals(mode)) {
            root.getStyleClass().add("compact-mode");
        }
    }

    @FXML
    public void initialize() {
        loadCss();
        initThemeBox();
        initAccountSwitchBox();
        handler = new ChangePanelHandler();

        DataRequest request = new DataRequest();
        DataResponse databaseResponse = HttpRequestUtil.request("/api/base/getDataBaseUserName", request);
        if (databaseResponse != null && systemPrompt != null) {
            systemPrompt.setText("服务器：" + HttpRequestUtil.serverUrl + "    数据库：" + databaseResponse.getData());
        }
        if (rightStatusLabel != null && AppStore.getJwt() != null) {
            rightStatusLabel.setText("当前账号：" + AppStore.getJwt().getUsername() + " | " + toRoleDisplayName(AppStore.getJwt().getRole()));
        }

        DataResponse menuResponse = HttpRequestUtil.request("/api/base/getMenuList", request);
        List<Map> menuList = menuResponse == null ? new ArrayList<>() : (List<Map>) menuResponse.getData();
        if (menuList == null) {
            menuList = new ArrayList<>();
        }

        ensureCoreMenus(menuList);
        addTeacherMenuToPersonManage(menuList);
        addTeacherProfileMenu(menuList);
        addStudentSelfMenu(menuList);
        addRegisterApplyMenu(menuList);
        addStudentLeaveMenu(menuList);
        addStudentStatisticsMenu(menuList);
        addCourseScheduleMenu(menuList);
        addCommunityMenu(menuList);
        addAiAssistantMenu(menuList);
        removeEmptyContainerMenus(menuList);

        initMenuBar(menuList);
        initMenuTree(menuList);
        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    public void changeContent(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source instanceof MenuItem menuItem) {
            String name = menuItem.getId();
            String title = menuItem.getText();
            if (name == null || name.isEmpty()) {
                return;
            }
            if ("logout".equals(name)) {
                logout();
                return;
            }
            changeContent(name, title);
        }
    }

    public void changeContent(String name, String title) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Tab tab = tabMap.get(name);
        if (tab == null) {
            Scene scene = sceneMap.get(name);
            if (scene == null) {
                String fxmlPath = name.startsWith("base/") ? name + ".fxml" : "base/" + name + ".fxml";
                FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(fxmlPath));
                try {
                    Pane root = fxmlLoader.load();
                    scene = new Scene(root, 1024, 768);
                    URL commonCssUrl = MainApplication.class.getResource("css/module-common.css");
                    if (commonCssUrl != null) {
                        root.getStylesheets().add(commonCssUrl.toExternalForm());
                    }
                    String cssName = name.startsWith("base/") ? name.substring(5) : name;
                    URL cssUrl = MainApplication.class.getResource("css/" + cssName + ".css");
                    if (cssUrl != null) {
                        root.getStylesheets().add(cssUrl.toExternalForm());
                    }
                    applyThemeClasses(root, lastTheme);
                    sceneMap.put(name, scene);
                    Object controller = fxmlLoader.getController();
                    if (controller instanceof ToolController toolController) {
                        controlMap.put(name, toolController);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    MessageDialog.showDialog("页面加载失败：" + title);
                    return;
                }
            }
            tab = new Tab(title);
            tab.setId(name);
            tab.setClosable(true);
            tab.setOnSelectionChanged(this::tabSelectedChanged);
            tab.setOnClosed(this::tabOnClosed);
            tab.setContent(scene.getRoot());
            contentTabPane.getTabs().add(tab);
            tabMap.put(name, tab);
        } else if (tab.getContent() instanceof Parent parent) {
            applyThemeClasses(parent, lastTheme);
        }
        contentTabPane.getSelectionModel().select(tab);
    }

    public void tabSelectedChanged(Event event) {
        Tab tab = (Tab) event.getSource();
        ToolController controller = controlMap.get(tab.getId());
        if (controller != null) {
            controller.doRefresh();
        }
    }

    public void tabOnClosed(Event event) {
        Tab tab = (Tab) event.getSource();
        contentTabPane.getTabs().remove(tab);
        tabMap.remove(tab.getId());
    }

    public ToolController getCurrentToolController() {
        Iterator<String> iterator = controlMap.keySet().iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            Tab tab = tabMap.get(name);
            if (tab != null && tab.isSelected()) {
                return controlMap.get(name);
            }
        }
        return null;
    }

    public ToolController getToolController(String name) {
        return controlMap.get(name);
    }

    protected void doNewCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) {
            controller.doNew();
        }
    }

    protected void doSaveCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) {
            controller.doSave();
        }
    }

    protected void doDeleteCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) {
            controller.doDelete();
        }
    }

    protected void doPrintCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) {
            controller.doPrint();
        }
    }

    protected void doExportCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) {
            controller.doExport();
        }
    }

    protected void doImportCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) {
            controller.doImport();
        }
    }

    protected void doTestCommand() {
        ToolController controller = getCurrentToolController();
        if (controller == null) {
            controller = new ToolController() {
            };
        }
        controller.doTest();
    }

    @FXML
    protected void onLogoutMenuClick(ActionEvent event) {
        logout();
    }

    protected void logout() {
        AppStore.setJwt(null);
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        try {
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(MainApplication.class.getResource("css/login-view.css").toExternalForm());
            MainApplication.loginStage("登录", scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }
}
