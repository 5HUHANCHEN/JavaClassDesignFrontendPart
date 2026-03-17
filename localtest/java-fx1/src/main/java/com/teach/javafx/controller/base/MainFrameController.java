package com.teach.javafx.controller.base;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import java.net.URL;

import java.util.ArrayList;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.MyTreeNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MainFrameController 登录交互控制类 对应 base/main-frame.fxml
 *  @FXML  属性 对应fxml文件中的
 *  @FXML 方法 对应于fxml文件中的 on***Click的属性
 */
public class MainFrameController {
    class ChangePanelHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            changeContent(actionEvent);
        }
    }
    private Map<String,Tab> tabMap = new HashMap<String,Tab>();
    private Map<String,Scene> sceneMap = new HashMap<String,Scene>();
    private Map<String,ToolController> controlMap =new HashMap<String,ToolController>();

    @FXML
    private MenuBar menuBar;
    @FXML
    private TreeView<MyTreeNode> menuTree;
    @FXML
    protected TabPane contentTabPane;
    @FXML
    private Label systemPrompt;

    @FXML
    private BorderPane rootPane;

    @FXML
    private ComboBox<String> themeComboBox;

    @FXML
    private Label rightStatusLabel;

    private static String lastTheme = "默认模式";

    private ChangePanelHandler handler= null;

    void addMenuItems(Menu parent, List<Map> mList) {
        String name, title;
        List sList;
        Map ms;
        Menu menu;
        MenuItem item;
        for ( Map m :mList) {
            sList = (List<Map>)m.get("sList");
            name = (String)m.get("name");
            title = (String)m.get("title");
            if(sList == null || sList.size()== 0) {
                item = new MenuItem();
                item.setId(name);
                item.setText(title);
                item.setOnAction(this::changeContent);
                parent.getItems().add(item);
            }else {
                menu = new Menu();
                menu.setText(title);
                addMenuItems(menu,sList);
                parent.getItems().add(menu);
            }
        }
    }

    public void changeContent(ActionEvent ae) {
        Object obj = ae.getSource();
        String name = null, title = null;

        if (obj instanceof MenuItem menuItem) {
            name = menuItem.getId();
            title = menuItem.getText();
        }

        if (name == null || name.isEmpty()) {
            System.out.println("菜单项ID为空，无法加载内容");
            return;
        }

        changeContent(name, title);
    }

    /**
     * 页面加载对象创建完成初始话方法，页面中控件属性的设置，初始数据显示等初始操作都在这里完成，其他代码都事件处理方法里
     * 系统初始时为没个角色增加了框架已经实现好了基础管理的功能，采用代码显示添加的方法加入，加入完缺省的功能菜单后，通过
     * HttpRequestUtil.request("/api/base/getMenuList",new DataRequest())加载用菜单管理功能，维护的菜单
     * 项目开发过程中，同学可以扩该方法，增肌自己设计的功能菜单，也可以通过菜单管理程序添加菜单，框架自动加载菜单管理维护的菜单，
     * 是新功能扩展
     */

    private void loadCss() {
        URL cssUrl = getClass().getResource("/com/teach/javafx/css/main-frame.css");
        System.out.println("main-frame cssUrl = " + cssUrl);
        if (cssUrl != null) {
            rootPane.getStylesheets().clear();
            rootPane.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    public void addMenuItem(Menu menu, String name, String title){
        MenuItem item;
        item = new MenuItem();
        item.setText(title);
        item.setId(name);
        item.setOnAction(this::changeContent);
        menu.getItems().add(item);
    }

    public void initMenuBar(List<Map> mList){
        Menu menu;
        Map m;
        int i;
        List<Map> sList;
        for(i = 0; i < mList.size();i++) {
            m = mList.get(i);
            sList = (List<Map>)m.get("sList");
            menu = new Menu();
            menu.setText((String)m.get("title"));
            if(sList != null && sList.size()> 0) {
                addMenuItems(menu,sList);
            }
            menuBar.getMenus().add(menu);
        }
    }

    void addMenuItems( TreeItem<MyTreeNode> parent, List<Map> mList) {
        List sList;
        TreeItem<MyTreeNode> menu;
        for ( Map m :mList) {
            sList = (List<Map>)m.get("sList");
            menu = new TreeItem<>(new MyTreeNode(null,(String)m.get("name") ,(String)m.get("title"),0));
            parent.getChildren().add(menu);
            if(sList !=  null && sList.size()> 0) {
                addMenuItems(menu, sList);
            }
        }
    }

    public void initMenuTree(List<Map> mList) {
        String role = AppStore.getJwt().getRole();
        MyTreeNode node = new MyTreeNode(null, null,"菜单",0);
        TreeItem<MyTreeNode> root = new TreeItem<>(node);
        TreeItem<MyTreeNode> treeMenu;
        int i,j;
        Map map;
        List<Map> sList;

        for(i = 0; i < mList.size();i++) {
            map = mList.get(i);
            sList = (List<Map>)map.get("sList");
            treeMenu = new TreeItem<>(new MyTreeNode(null, (String)map.get("name"), (String)map.get("title"), (Integer)map.get("isLeft")));
            if(sList != null && sList.size()> 0) {
                addMenuItems(treeMenu,sList);
            }
            root.getChildren().add(treeMenu);
        }

        menuTree.setRoot(root);
        menuTree.setShowRoot(false);

        // 修改树形菜单点击事件处理
        menuTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                MyTreeNode menu = newValue.getValue();
                if (menu != null) {
                    String name = menu.getValue();
                    if (name != null && !name.isEmpty()) {
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
                    }
                }
            }
        });

        root.setExpanded(true);
        for (TreeItem<MyTreeNode> child : root.getChildren()) {
            child.setExpanded(true);
        }
    }

    private void addTeacherMenuToPersonManage(List<Map> mList) {
        for (Map m : mList) {
            String title = (String) m.get("title");
            if ("人员管理".equals(title)) {
                List<Map> sList = (List<Map>) m.get("sList");
                if (sList == null) {
                    sList = new java.util.ArrayList<>();
                    m.put("sList", sList);
                }

                boolean exists = false;
                for (Map child : sList) {
                    if ("teacher-panel".equals(child.get("name"))) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    Map<String, Object> child = new HashMap<>();
                    child.put("name", "teacher-panel");
                    child.put("title", "教师管理");
                    child.put("isLeft", 1);
                    child.put("sList", new java.util.ArrayList<>());
                    sList.add(child);
                }
                break;
            }
        }
    }

    private void initThemeBox() {
        if (themeComboBox == null) {
            return;
        }

        themeComboBox.getItems().addAll("默认模式", "深色模式", "紧凑模式");
        themeComboBox.getSelectionModel().select(lastTheme);
        applyTheme(lastTheme);

        themeComboBox.setOnAction(e -> {
            String mode = themeComboBox.getValue();
            lastTheme = mode;
            applyTheme(mode);
        });
    }

    private void applyTheme(String mode) {
        if (rootPane == null) {
            return;
        }

        rootPane.getStyleClass().removeAll("dark-mode", "compact-mode");

        if ("深色模式".equals(mode)) {
            rootPane.getStyleClass().add("dark-mode");
            if (rightStatusLabel != null) {
                rightStatusLabel.setText("深色模式");
            }
        } else if ("紧凑模式".equals(mode)) {
            rootPane.getStyleClass().add("compact-mode");
            if (rightStatusLabel != null) {
                rightStatusLabel.setText("紧凑模式");
            }
        } else {
            if (rightStatusLabel != null) {
                rightStatusLabel.setText("默认模式");
            }
        }
    }

    @FXML
    public void initialize() {
        System.out.println("MainFrameController 初始化开始");

        loadCss();
        initThemeBox();

        handler = new ChangePanelHandler();
        DataRequest req = new DataRequest();
        DataResponse res;

        res = HttpRequestUtil.request("/api/base/getDataBaseUserName", req);
        String userName = (String) res.getData();
        systemPrompt.setText("服务器：" + HttpRequestUtil.serverUrl + "    数据库：" + userName);

        if (rightStatusLabel != null) {
            rightStatusLabel.setText("欢迎，" + AppStore.getJwt().getUsername());
        }

        res = HttpRequestUtil.request("/api/base/getMenuList", req);
        @SuppressWarnings("unchecked")
        List<Map> mList = (List<Map>) res.getData();

        if (mList == null) {
            System.out.println("获取菜单列表失败，返回数据为null");
            return;
        }

        System.out.println("获取到菜单数量: " + mList.size());

        // 添加教师菜单
        addTeacherMenuToPersonManage(mList);

        // 给学生添加"我的信息"
        if("ROLE_STUDENT".equals(AppStore.getJwt().getRole())){
            addStudentSelfMenu(mList);
        }
        // 添加账号申请审核菜单
        addRegisterApplyMenu(mList);

        initMenuBar(mList);
        initMenuTree(mList);

        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        System.out.println("MainFrameController 初始化完成");
    }

    private void addRegisterApplyMenu(List<Map> mList) {
        // 只有管理员才显示
        if(!"ROLE_ADMIN".equals(AppStore.getJwt().getRole()))
            return;

        Map<String,Object> menu = new HashMap<>();

        menu.put("name","register-apply-list");
        menu.put("title","账号申请审核");
        menu.put("isLeft",1);
        menu.put("sList", new ArrayList<>());

        mList.add(menu);
    }

    private void addStudentSelfMenu(List<Map> mList) {
        Map<String, Object> rootMenu = new HashMap<>();
        rootMenu.put("name", "student-self-panel");
        rootMenu.put("title", "我的信息");
        rootMenu.put("isLeft", 1);
        rootMenu.put("sList", new ArrayList<>());
        mList.add(rootMenu);
    }

    /**
     * 点击菜单栏中的"退出"菜单，执行onLogoutMenuClick方法 加载登录页面，切换回登录界面
     * @param event
     */
    @FXML
    protected void onLogoutMenuClick(ActionEvent event){
        System.out.println("执行退出登录操作");
        logout();
    }

    protected void logout() {

        System.out.println("开始退出登录流程");

        FXMLLoader fxmlLoader =
                new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));

        try {

            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1024, 768);

            scene.getStylesheets().add(
                    MainApplication.class.getResource("css/login-view.css").toExternalForm()
            );

            MainApplication.loginStage("登录",scene);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    /**
     * 点击菜单栏中的菜单 执行changeContent 在主框架工作区增加和显示一个工作面板
     * @param name  菜单名 name.fxml 对应面板的配置文件
     * @param title 菜单标题 工作区中的TablePane的标题
     */
    public void changeContent(String name, String title) {
        System.out.println("尝试打开菜单: " + name + ", 标题: " + title);

        if (name == null || name.length() == 0) {
            System.out.println("菜单名为空，无法加载内容");
            return;
        }

        Tab tab = tabMap.get(name);
        Scene scene;
        Object controller;

        if (tab == null) {
            scene = sceneMap.get(name);

            if (scene == null) {
                System.out.println("正在加载FXML文件: base/" + name + ".fxml");

                String fxmlPath;
                if (name.startsWith("base/")) {
                    fxmlPath = name + ".fxml";
                } else {
                    fxmlPath = "base/" + name + ".fxml";
                }

                System.out.println("正在加载FXML文件: " + fxmlPath);

                FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(fxmlPath));

                try {
                    Pane root = fxmlLoader.load();
                    scene = new Scene(root, 1024, 768);

// 统一处理模块名
                    String cssName = name.startsWith("base/") ? name.substring(5) : name;

// 先加载公共模块CSS
                    URL commonCssUrl = MainApplication.class.getResource("css/module-common.css");
                    if (commonCssUrl != null) {
                        root.getStylesheets().add(commonCssUrl.toExternalForm());
                        System.out.println("加载公共CSS文件: css/module-common.css");
                    } else {
                        System.out.println("未找到公共CSS文件: css/module-common.css");
                    }

// 再加载页面自己的CSS
                    URL cssUrl = MainApplication.class.getResource("css/" + cssName + ".css");
                    if (cssUrl != null) {
                        root.getStylesheets().add(cssUrl.toExternalForm());
                        System.out.println("加载CSS文件: css/" + cssName + ".css");
                    } else {
                        System.out.println("未找到CSS文件: css/" + cssName + ".css");
                    }
                    sceneMap.put(name, scene);

                    controller = fxmlLoader.getController();
                    System.out.println("加载的控制器类型: " + (controller != null ? controller.getClass().getName() : "null"));

                    if (controller instanceof ToolController) {
                        controlMap.put(name, (ToolController) controller);
                    }
                } catch (IOException e) {
                    System.out.println("加载FXML文件失败: base/" + name + ".fxml");
                    System.out.println("错误信息: " + e.getMessage());
                    e.printStackTrace();
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

            System.out.println("成功创建新标签页: " + title);
        }

        contentTabPane.getSelectionModel().select(tab);
        System.out.println("已选择标签页: " + title);
    }

    public void tabSelectedChanged(Event e) {
        Tab tab = (Tab)e.getSource();
        String name = tab.getId();
        ToolController c = controlMap.get(name);
        if(c != null)
            c.doRefresh();
    }

    /**
     * 点击TablePane 标签页 的关闭图标 执行tabOnClosed方法
     * @param
     */
    public void tabOnClosed(Event e) {
        Tab tab = (Tab)e.getSource();
        String name = tab.getId();
        contentTabPane.getTabs().remove(tab);
        tabMap.remove(name);
    }

    /**
     * ToolController getCurrentToolController() 获取当前显示的面板的控制对象， 如果面板响应编辑菜单中的编辑命名，交互控制需要继承 ToolController， 重写里面的方法
     * @return
     */
    public ToolController getCurrentToolController(){
        Iterator<String> iterator = controlMap.keySet().iterator();
        String name;
        Tab tab;
        while(iterator.hasNext()) {
            name = iterator.next();
            tab = tabMap.get(name);
            if(tab.isSelected()) {
                return controlMap.get(name);
            }
        }
        return null;
    }

    /**
     * 点击编辑菜单中的"新建"菜单，执行doNewCommand方法， 执行当前显示的面板对应的控制类中的doNew()方法
     */
    protected  void doNewCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doNew();
    }

    /**
     * 点击编辑菜单中的"保存"菜单，执行doSaveCommand方法， 执行当前显示的面板对应的控制类中的doSave()方法
     */
    protected  void doSaveCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doSave();
    }

    /**
     * 点击编辑菜单中的"删除"菜单，执行doDeleteCommand方法， 执行当前显示的面板对应的控制类中的doDelete()方法
     */
    protected  void doDeleteCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doDelete();
    }

    /**
     * 点击编辑菜单中的"打印"菜单，执行doPrintCommand方法， 执行当前显示的面板对应的控制类中的doPrint()方法
     */
    protected  void doPrintCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doPrint();
    }

    /**
     * 点击编辑菜单中的"导出"菜单，执行doExportCommand方法， 执行当前显示的面板对应的控制类中的doExport方法
     */
    protected  void doExportCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doExport();
    }

    /**
     * 点击编辑菜单中的"导入"菜单，执行doImportCommand方法， 执行当前显示的面板对应的控制类中的doImport()方法
     */
    protected  void doImportCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doImport();
    }

    /**
     * 点击编辑菜单中的"测试"菜单，执行doTestCommand方法， 执行当前显示的面板对应的控制类中的doImport()方法
     */
    protected  void doTestCommand(){
        ToolController c = getCurrentToolController();
        if(c == null) {
            c= new ToolController(){
            };
        }
        c.doTest();
    }

    public ToolController getToolController(String name){
        return  controlMap.get(name);
    }
}
