# Java 学生管理项目学习指南

## 文档目标

这份文档是给你学习这个项目用的，目标不是一下子把所有代码都背下来，而是一步一步达到下面这些能力：

- 能看懂这个项目的核心代码
- 能自己把前后端启动起来
- 能顺着一个完整业务流程读代码
- 能独立排查常见问题
- 能安全地做小修改

这份文档按照“最适合学习的顺序”来写，不是按文件夹顺序来写。

---

## 1. 先搞清楚：这个项目到底是什么

在学习细节之前，你必须先知道这个仓库的整体结构。

这个仓库主要有两个核心模块：

- `java-fx1`
  - JavaFX 桌面端前端
  - 负责页面、按钮、表单输入、表格显示、页面切换
- `java-server1`
  - Spring Boot 后端
  - 负责登录、业务逻辑、数据库访问、权限控制、接口返回

整个请求流程可以先理解成：

```text
用户
-> JavaFX 前端
-> HTTP 请求
-> Spring Boot 后端
-> MySQL 数据库
-> 后端返回 JSON
-> 前端更新界面
```

你学习这个项目时，脑子里一定要一直有这张图。

---

## 2. 推荐学习顺序

不要试图一口气把整个项目全部看懂。

正确顺序应该是：

1. Java 基础语法和代码阅读能力
2. 前后端通信基础
3. 这个项目里用到的 JavaFX 基础
4. 这个项目里用到的 Spring Boot 基础
5. 项目启动流程
6. 登录流程
7. 主界面和菜单切换流程
8. 学生管理业务流程
9. 社区发帖业务流程
10. 数据库与实体关系
11. 调试与排错
12. 独立修改代码的方法

按这个顺序学，项目会越来越清楚。

### 2.1 这份项目里真正涉及到的技术名词

你后面会频繁遇到这些词，这里先建立一个“技术地图”。

前端相关：

- JavaFX
- FXML
- Controller
- Scene
- Stage
- FXMLLoader
- CSS
- HttpClient
- Gson

后端相关：

- Spring Boot
- Spring MVC
- Spring Security
- JWT
- JPA
- Hibernate
- Repository
- MySQL
- Maven

辅助理解相关：

- HTTP
- JSON
- REST 接口
- 注解
- 角色权限控制

### 2.2 这些技术按优先级怎么学

你不需要一起学完，建议优先级如下：

第一优先级：

- Java 基础
- HTTP / JSON
- JavaFX 基础
- Spring MVC 基础

第二优先级：

- Spring Boot 启动机制
- Spring Security 基础
- JWT 基础

第三优先级：

- JPA 基础
- Hibernate 基础
- Repository 查询方式

第四优先级：

- 更深入的 Maven
- 更深入的数据库优化
- 更深入的 Spring Security 过滤器链

也就是说：

- 你现在必须尽快理解 `Spring MVC`
- 你需要认识 `Spring Security` 和 `JWT`
- 你要会最基础的 `JPA`
- 但不需要一开始深挖 Hibernate 底层原理

---

## 3. 第一阶段：先补 Java 基础

这是最底层的基础。如果这一层不稳，看项目时会一直卡。

### 3.1 你至少必须熟悉这些

- 类
- 对象
- 方法
- 参数
- 返回值
- 成员变量
- 局部变量
- `public`、`private`、`protected`
- `static`
- `if / else`
- `for`
- `try / catch`
- 构造方法
- `extends`
- 接口的基本概念

### 3.2 你必须能比较轻松读懂这些

- `List`
- `Map`
- 泛型，比如 `List<Map<String, Object>>`
- `Optional`

### 3.3 建议结合项目看的文件

前端示例：

- [`MainApplication.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/MainApplication.java)
- [`LoginController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/controller/base/LoginController.java)

后端示例：

- [`AuthController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/controllers/AuthController.java)
- [`AuthService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/AuthService.java)

### 3.4 这一阶段目标

学完这一阶段后，你至少应该能回答：

- 这个方法接收什么参数
- 这个方法在做什么
- 这个方法返回什么
- 这个方法读了哪些字段、改了哪些字段

### 3.5 要细化到哪些具体知识点

这一阶段不是泛泛地学 Java，而是针对这个项目补这些知识点：

- 类和对象
  - 什么是类
  - 什么是对象
  - `new` 到底在做什么
- 字段和方法
  - 成员变量
  - 局部变量
  - 方法参数
  - 返回值
- 访问修饰符
  - `public`
  - `private`
  - `protected`
- 静态成员
  - `static` 方法
  - `static` 变量
- 继承
  - `extends`
  - 为什么 `MainApplication extends Application`
- 集合
  - `List`
  - `ArrayList`
  - `Map`
  - `HashMap`
- 泛型
  - `List<Map<String, Object>>`
  - `Optional<User>`
- 流程控制
  - `if / else`
  - `switch`
  - `for`
- 异常
  - `try / catch`
  - `throw new RuntimeException(...)`
- 面向对象理解
  - 一个类负责什么
  - 一个方法负责什么

### 3.6 你需要达到什么程度

不要求你会手写复杂算法，但至少要达到：

- 看到一个类，知道它是干什么的
- 看到一个方法，知道输入、处理、输出是什么
- 看到 `Map<String,Object>` 不再完全害怕
- 看到 `Optional` 知道是在防空值

---

## 4. 第二阶段：补前后端通信基础

如果不懂请求和响应，就很难理解这个项目。

### 4.1 必须理解这些概念

- 什么是前端
- 什么是后端
- 什么是接口
- 什么是 URL
- 什么是 HTTP 请求
- 什么是 `GET`
- 什么是 `POST`
- 什么是请求体
- 什么是 JSON
- 什么是响应体
- 什么是状态码
- 什么是 `200`
- 什么是 `401`

### 4.2 建议结合项目看的文件

前端请求总入口：

- [`HttpRequestUtil.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/request/HttpRequestUtil.java)

其中这句非常关键：

```java
public static String serverUrl = "http://localhost:22223";
```

登录请求地址：

```java
.uri(URI.create(serverUrl + "/auth/login"))
```

### 4.3 这一阶段目标

学完后你应该能解释：

- 前端登录请求发到哪里
- 为什么后端是 `/auth/login`
- 为什么发送的是 JSON

### 4.4 要细化到哪些具体知识点

这一阶段建议你具体学这些：

- HTTP 是什么
- 客户端和服务端的关系
- URL 是什么
- 路径是什么
- 端口是什么
- 请求方法
  - `GET`
  - `POST`
- 请求头是什么
  - `Content-Type`
  - `Authorization`
- 请求体是什么
- 响应体是什么
- 状态码
  - `200`
  - `400`
  - `401`
  - `500`
- JSON 结构
  - 对象
  - 数组
  - 键值对

### 4.5 这一阶段要结合本项目理解什么

你必须能结合项目理解：

- 为什么登录是 `POST /auth/login`
- 为什么学生信息保存也是 `POST`
- 为什么前端会在请求头里放 `Bearer token`
- 为什么后端返回的数据最后会变成 `DataResponse`

---

## 5. 第三阶段：补 JavaFX 基础

你不需要先把 JavaFX 全学完，只要先学这个项目真正用到的。

### 5.1 必须理解这些概念

- `Application`
- `launch()`
- `start(Stage stage)`
- `Stage`
- `Scene`
- `FXML`
- `FXMLLoader`
- `Controller`
- `@FXML`

### 5.2 先记住一个关键关系

在这个项目里：

- `FXML` 负责描述页面长什么样
- Java 控制器类负责处理页面逻辑

### 5.3 建议重点看这些文件

- [`MainApplication.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/MainApplication.java)
- [`login-view.fxml`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/resources/com/teach/javafx/base/login-view.fxml)
- [`main-frame.fxml`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/resources/com/teach/javafx/base/main-frame.fxml)
- [`LoginController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/controller/base/LoginController.java)
- [`MainFrameController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/controller/base/MainFrameController.java)

### 5.4 这一阶段目标

学完后你应该能解释：

- 为什么程序一启动先出现登录页
- 为什么 FXML 里的按钮能调用 Java 方法
- 为什么登录成功后能切换到主界面

### 5.5 要细化到哪些具体知识点

这个项目里你需要掌握的 JavaFX 知识点主要是：

- `Application`
  - 为什么 JavaFX 程序入口类要继承它
- `launch()`
  - 为什么 `main()` 里调用它
- `start(Stage stage)`
  - 为什么它是前端启动后的核心入口
- `Stage`
  - 它代表窗口
- `Scene`
  - 它代表窗口里正在显示的页面
- `FXML`
  - 为什么界面不直接全写在 Java 代码里
- `FXMLLoader`
  - 为什么它能加载页面
- `fx:controller`
  - 页面和控制器怎么绑定
- `fx:id`
  - 页面控件和 Java 字段怎么对应
- `onAction="#xxx"`
  - 按钮点击是怎么绑定到控制器方法的
- 控件基础
  - `Button`
  - `TextField`
  - `PasswordField`
  - `TableView`
  - `ComboBox`
  - `TabPane`
  - `TreeView`

### 5.6 你需要达到什么程度

至少达到：

- 能看懂一个 FXML 页面绑定了哪个控制器
- 能看懂一个按钮点击后调哪个 Java 方法
- 能理解场景切换和 Tab 页面切换的区别

---

## 6. 第四阶段：补 Spring Boot 基础

同样，你不需要一下子学完整个 Spring，只要先学这个项目实际用到的。

### 6.1 至少要认识这些注解

- `@SpringBootApplication`
- `@RestController`
- `@RequestMapping`
- `@PostMapping`
- `@RequestBody`
- `@Service`
- `@Configuration`
- `@Bean`
- `@PreAuthorize`

### 6.2 至少要分清这些角色

- controller
  - 接收 HTTP 请求
- service
  - 处理业务逻辑
- repository
  - 操作数据库
- model/entity
  - 表示数据结构

### 6.3 建议重点看这些文件

- [`JavaServerApplication.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/JavaServerApplication.java)
- [`AuthController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/controllers/AuthController.java)
- [`AuthService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/AuthService.java)
- [`SecurityConfiguration.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/configs/SecurityConfiguration.java)

### 6.4 这一阶段目标

学完后你应该能解释：

- `@RestController` 是什么意思
- `/auth/login` 为什么能映射到某个方法
- 为什么 controller 和 service 要分开

### 6.5 这一阶段要细化到哪些具体知识点

你现在最需要先学的是 `Spring MVC` 和 `Spring Boot` 的最小核心。

#### Spring Boot

你必须认识：

- `@SpringBootApplication`
- `SpringApplication.run(...)`
- 配置文件 `application.yml`
- Bean 的基本概念
- 自动装配的基本概念

#### Spring MVC

你必须认识：

- `@RestController`
- `@RequestMapping`
- `@PostMapping`
- `@GetMapping`
- `@RequestBody`
- `@RequestParam`
- `ResponseEntity`

#### Spring 分层思想

你必须认识：

- Controller
- Service
- Repository
- Entity / Model

### 6.6 你需要达到什么程度

至少达到：

- 看到 `@RestController` 知道这是接口类
- 看到 `@RequestMapping("/auth")` 知道是路径前缀
- 看到 `@PostMapping("/login")` 知道是登录接口
- 看到 `@RequestBody` 知道前端 JSON 会自动转成 Java 对象

---

## 6A. 单独说：Spring Security 需要学什么

这个项目明确使用了 `Spring Security`，所以你必须认识它，但不需要一开始学太深。

### 6A.1 你必须先会的知识点

- Spring Security 是做什么的
- 什么是认证
- 什么是授权
- 什么是角色
- 什么是受保护接口
- 什么是 `SecurityContext`
- 什么是过滤器

### 6A.2 这个项目里你会看到的相关知识点

- `SecurityConfiguration`
- `SecurityFilterChain`
- `AuthenticationManager`
- `AuthenticationProvider`
- `UsernamePasswordAuthenticationToken`
- `SecurityContextHolder`
- `@PreAuthorize`

### 6A.3 建议结合项目看的文件

- [`SecurityConfiguration.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/configs/SecurityConfiguration.java)
- [`AuthService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/AuthService.java)

### 6A.4 你需要达到什么程度

至少达到：

- 知道登录校验不是自己手写 `if`，而是交给 `AuthenticationManager`
- 知道 `@PreAuthorize` 是按角色限制接口
- 知道后端会先过安全检查，再进入业务 Controller

---

## 6B. 单独说：JWT 需要学什么

这个项目的登录状态是靠 `JWT` 实现的。

### 6B.1 你必须先会的知识点

- JWT 是什么
- 为什么登录后后端要返回 token
- 为什么前端后续请求要带 token
- `Authorization: Bearer xxx` 是什么意思

### 6B.2 这个项目里你会看到的相关知识点

- `JwtResponse`
- `JwtService`
- `JwtAuthenticationFilter`
- `AppStore.setJwt(...)`
- `HttpRequestUtil` 给请求头加 token

### 6B.3 建议结合项目看的文件

- [`HttpRequestUtil.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/request/HttpRequestUtil.java)
- [`AppStore.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/AppStore.java)
- [`AuthService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/AuthService.java)

### 6B.4 你需要达到什么程度

至少达到：

- 能讲明白“登录一次，后面靠 token 访问其他接口”
- 知道为什么前端登录成功后要把 JWT 保存起来

---

## 6C. 单独说：JPA 需要学什么

这个项目后端的数据访问是基于 `JPA` 的，你一定会遇到它。

### 6C.1 你必须先会的知识点

- JPA 是做什么的
- Entity 是什么
- Repository 是什么
- `save(...)` 是什么
- `findById(...)` 是什么
- `findAll(...)` 是什么
- 一个实体对象和数据库表之间的基本关系

### 6C.2 这个项目里你会看到的相关知识点

- `PersonRepository`
- `UserRepository`
- `StudentRepository`
- `CommunityPostRepository`
- `CommunityCommentRepository`

### 6C.3 建议结合项目看的文件

- [`StudentService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/StudentService.java)
- [`CommunityService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/CommunityService.java)

### 6C.4 你需要达到什么程度

至少达到：

- 知道 repository 是数据库访问入口
- 知道 `save(...)` 可能是新增，也可能是更新
- 知道为什么一个业务保存动作会同时影响多张表

---

## 6D. 单独说：Hibernate 需要学什么

`Hibernate` 是 JPA 常用实现。在这个项目里，你不需要一开始深挖底层原理。

### 6D.1 现在只需要知道

- Hibernate 是 JPA 背后的实现之一
- `ddl-auto: update` 会影响表结构更新
- 你平时调用的是 JPA 风格接口，但背后通常是 Hibernate 在工作

### 6D.2 你暂时不需要深挖

- 一级缓存
- 延迟加载底层机制
- 脏检查底层细节
- 复杂关联映射底层 SQL 生成

也就是说：

- `JPA` 你现在要会基础
- `Hibernate` 你现在先知道它存在就够

---

## 7. 第五阶段：学习项目启动流程

只有前面基础补完后，再去学启动流程才不会乱。

### 7.1 前端启动

重点看：

- [`MainApplication.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/MainApplication.java)

你必须能回答：

- 前端入口在哪里
- 为什么登录页先出现
- `resetStage(...)` 是干什么的
- `loginStage(...)` 是干什么的

### 7.2 后端启动

重点看：

- [`JavaServerApplication.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/JavaServerApplication.java)
- [`application.yml`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/resources/application.yml)

你必须能回答：

- 后端入口在哪里
- 为什么监听 `22223` 端口
- 后端怎么知道连哪个数据库

### 7.3 这一阶段目标

你应该能自己把前后端启动起来，并知道启动时先发生了什么。

### 7.4 这一阶段涉及到的具体知识点

前端侧：

- JavaFX 程序入口
- `main()` 和 `launch()`
- `start(Stage stage)`
- 初始页面加载

后端侧：

- Spring Boot 启动入口
- `application.yml`
- 端口配置
- 数据源初始化
- 安全配置加载
- Controller / Service / Repository 扫描

---

## 8. 第六阶段：先学登录流程

这是你第一个必须完整打通的业务流程。

### 8.1 前端侧重点

重点看：

- [`login-view.fxml`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/resources/com/teach/javafx/base/login-view.fxml)
- [`LoginController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/controller/base/LoginController.java)
- [`HttpRequestUtil.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/request/HttpRequestUtil.java)
- [`AppStore.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/AppStore.java)

### 8.2 后端侧重点

重点看：

- [`AuthController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/controllers/AuthController.java)
- [`AuthService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/AuthService.java)

### 8.3 你必须能回答的问题

- 登录按钮点下去后发生了什么
- 用户名密码在哪里读取
- 请求是怎么发出去的
- `/auth/login` 在后端哪里处理
- 后端怎么验证账号密码
- JWT 在哪里生成
- JWT 在前端哪里保存
- 登录成功后为什么会进入主界面

### 8.4 这一阶段目标

你应该能完整讲出登录流程。

### 8.5 这一阶段会串到哪些具体技术点

前端：

- JavaFX 按钮事件绑定
- 表单读取
- `HttpClient`
- JSON 序列化
- 全局状态存储

后端：

- `@RestController`
- `@RequestBody`
- `AuthenticationManager`
- `SecurityContextHolder`
- JWT 生成
- `ResponseEntity`

---

## 9. 第七阶段：学习主界面和菜单切换

学完登录后，下一步就是学主界面怎么加载业务页。

### 9.1 必看文件

- [`main-frame.fxml`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/resources/com/teach/javafx/base/main-frame.fxml)
- [`MainFrameController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/controller/base/MainFrameController.java)

### 9.2 需要理解的核心点

- 菜单是怎么加载的
- 树形菜单是怎么绑定事件的
- 页面是怎么通过 `FXMLLoader` 动态加载的
- 为什么业务页面是在 `Tab` 里打开的

### 9.3 你必须能回答的问题

- 菜单数据从哪里来
- 为什么不同角色菜单不同
- 点击菜单项为什么会打开一个页面
- 为什么打开的是标签页而不是整个窗口重建

### 9.4 这一阶段目标

你应该能解释“登录成功后，主界面是怎么把业务页一个个打开的”。

---

## 10. 第八阶段：学习第一个完整 CRUD 业务流程

建议第一个模块学学生管理。

### 10.1 前端侧重点

重点看：

- [`student-panel.fxml`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/resources/com/teach/javafx/base/student-panel.fxml)
- [`StudentController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/controller/StudentController.java)

### 10.2 后端侧重点

重点看：

- [`StudentController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/controllers/StudentController.java)
- [`StudentService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/StudentService.java)

### 10.3 建议重点跟踪这些动作

- 打开学生页
- 加载学生列表
- 点击某个学生
- 加载学生详情
- 修改学生信息
- 保存学生信息
- 删除学生

### 10.4 你必须能回答的问题

- 为什么页面一打开学生列表就会加载
- 为什么点某一行会自动切换详情
- 前端表单数据是怎么打包进 `DataRequest` 的
- 后端怎么区分“新增”和“修改”
- 为什么会同时涉及 `Person`、`User`、`Student`

### 10.5 这一阶段目标

你应该能独立跟踪一个完整的增删改查请求。

### 10.6 这一阶段会碰到哪些具体知识点

前端：

- `TableView`
- `MapValueFactory`
- 表单回填
- 行选择监听
- `DataRequest`
- `DataResponse`

后端：

- Controller 到 Service 调用
- JPA Repository
- `save(...)`
- `findById(...)`
- 业务校验
- 多实体联动保存

---

## 11. 第九阶段：学习第二个业务流程

学完学生模块后，再学社区模块。

### 11.1 前端侧重点

重点看：

- [`community-panel.fxml`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/resources/com/teach/javafx/base/community-panel.fxml)
- [`CommunityController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/controller/CommunityController.java)

### 11.2 后端侧重点

重点看：

- [`CommunityController.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/controllers/CommunityController.java)
- [`CommunityService.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/services/CommunityService.java)

### 11.3 建议重点跟踪这些动作

- 加载帖子列表
- 加载帖子详情
- 新建帖子
- 编辑帖子
- 删除帖子
- 发表评论
- 删除评论

### 11.4 这一阶段目标

你应该能对比学生模块和社区模块，发现这个项目重复使用的代码结构模式。

---

## 12. 第十阶段：学习数据库和实体关系

只有在你能顺着流程读代码之后，再去深入数据结构。

### 12.1 必须理解的内容

- 实体类表示什么
- repository 是怎么查数据库的
- 一个业务动作为什么会同时影响多张表

### 12.2 建议重点看这些模型

去看 `java-server1/src/main/java/cn/edu/sdu/java/server/models` 目录。

优先理解这些：

- `Person`
- `User`
- `Student`
- `Teacher`
- `CommunityPost`
- `CommunityComment`

### 12.3 这一阶段目标

你应该能解释：

- 为什么保存学生会更新多个实体
- 为什么登录时主要用 `User`
- 为什么社区帖子会有作者信息

### 12.4 这一阶段建议补的数据库知识点

- 数据库表的基本概念
- 主键
- 外键
- 一对一
- 一对多
- 多对一
- 用户表和人物表分离是什么意思
- 为什么学生、老师和账号不是同一张表

---

## 13. 第十一阶段：学习 JWT 和权限控制

这一块不要学太早，等你登录流程很熟以后再学。

### 13.1 必须理解的内容

- JWT 是什么
- 为什么前端登录成功后要保存 token
- 为什么后端要检查 `Authorization: Bearer ...`
- 什么叫受保护接口
- 什么叫按角色授权

### 13.2 建议重点看这些文件

- [`SecurityConfiguration.java`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/java/cn/edu/sdu/java/server/configs/SecurityConfiguration.java)
- `JwtAuthenticationFilter`
- `JwtService`

### 13.3 这一阶段目标

你应该能解释为什么有些接口必须登录后才能访问。

---

## 14. 第十二阶段：自己把项目跑起来

当你对主流程有概念后，必须开始学会自己运行项目。

### 14.1 需要知道的环境点

- JDK 21
- Maven
- Maven Wrapper
- `application.yml` 里的远程 MySQL
- 端口 `22223`

### 14.2 必须知道的命令

后端：

```powershell
cd E:\java\Java_student_management-main\Java_student_management-main\java-server1
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd E:\java\Java_student_management-main\Java_student_management-main\java-fx1
.\mvnw.cmd javafx:run
```

### 14.3 这一阶段目标

你应该能做到：

- 启动后端
- 确认后端端口在监听
- 启动前端
- 成功登录

---

## 15. 第十三阶段：学习排错和调试

如果想独立操作项目，这一块是必须的。

### 15.1 必须学会排查的问题

- 前端打不开
- 后端启动失败
- 端口被占用
- 数据库连接失败
- 登录失败
- 权限报错
- 请求路径不匹配
- JDK 版本不匹配
- IDEA 编译环境不匹配

### 15.2 必须知道去哪看

- [`application.yml`](E:/java/Java_student_management-main/Java_student_management-main/java-server1/src/main/resources/application.yml)
- [`HttpRequestUtil.java`](E:/java/Java_student_management-main/Java_student_management-main/java-fx1/src/main/java/com/teach/javafx/request/HttpRequestUtil.java)
- 后端启动日志
- IDEA 的 JDK 设置
- Maven 的 JDK 版本
- 端口监听状态

### 15.3 这一阶段目标

你应该能自己缩小问题范围，而不是到处乱猜。

### 15.4 排错阶段会用到的具体知识点

- JDK 版本匹配
- Maven Wrapper
- 端口占用
- 数据库连接
- 请求地址匹配
- 权限失败和登录失败的区别
- IDE 编译环境和命令行编译环境的区别

---

## 16. 第十四阶段：开始做小改动

只有前面这些都基本掌握后，才适合开始独立改代码。

### 16.1 先从最小修改开始

- 改按钮文字
- 改提示语
- 改一个标签显示
- 改一个校验规则
- 改一个表格列

### 16.2 再做小后端改动

- 增加一个返回字段
- 修改一个查询条件
- 增加一个简单接口

### 16.3 最后再做完整功能改动

- 增加一个页面按钮行为
- 增加一个 service 方法
- 增加一个实体字段

### 16.4 这一阶段目标

你应该能在不迷路的情况下做小修改。

---

## 17. 推荐的实战学习方法

不要按文件夹顺序把所有文件从头看到尾。

正确方法是：

1. 先选一个具体动作
2. 看前端从哪里触发
3. 顺着请求路径走
4. 找 controller
5. 找 service
6. 只有需要时再去看 entity/repository
7. 最后回到前端看 UI 怎么刷新

最适合新手先学的动作是：

1. 登录
2. 打开学生页
3. 修改一个学生
4. 打开社区页
5. 发一个帖子

---

## 18. 你现在最应该先学什么

如果你的目标是尽快能独立操作这个项目，那么最推荐的学习顺序就是：

1. Java 基础语法
2. HTTP 和 JSON 基础
3. JavaFX 基础：`Application`、`Stage`、`Scene`、`FXML`、控制器绑定
4. Spring Boot 基础：controller、service、注解映射
5. 前端启动流程
6. 后端启动流程
7. 登录流程
8. 主界面和菜单切换
9. 学生列表 / 详情 / 保存流程
10. 社区发帖流程
11. JWT 和权限控制
12. 环境和排错
13. 小代码修改

除非你对前面已经很熟，否则不建议打乱顺序。

### 18.1 如果你想具体到技术名词，推荐优先顺序是

第一批现在就要开始理解：

- Java 基础
- HTTP
- JSON
- JavaFX
- FXML
- Spring MVC

第二批很快就要跟上：

- Spring Boot
- Spring Security
- JWT

第三批开始做业务时再系统理解：

- JPA
- Repository
- 数据库关系

第四批暂时只认识名字：

- Hibernate
- 过滤器链细节
- 更复杂的 Spring Security 机制

### 18.2 你现在最应该优先搞懂的技术清单

如果你问“我现在最应该先懂哪些词”，答案是：

1. JavaFX
2. FXML
3. Controller
4. HTTP
5. JSON
6. `@RestController`
7. `@RequestMapping`
8. `@PostMapping`
9. `@RequestBody`
10. Spring Security
11. JWT
12. JPA

其中：

- JavaFX / FXML / Controller 决定你能不能看懂前端
- HTTP / JSON / Spring MVC 决定你能不能看懂前后端接口
- Spring Security / JWT 决定你能不能看懂登录和权限
- JPA 决定你能不能看懂数据库业务保存

---

## 19. 推荐学习里程碑

### 里程碑 A

你能解释：

- 为什么程序启动后先出现登录页
- `@RestController` 是什么意思
- 前端登录请求发到哪里

### 里程碑 B

你能解释：

- 登录为什么成功
- JWT 存在哪里
- 主界面为什么会加载出来

### 里程碑 C

你能解释：

- 学生列表为什么会加载
- 学生保存流程怎么走
- 为什么会同时更新多个实体

### 里程碑 D

你已经能做到：

- 自己把整个项目跑起来
- 自己排查常见启动问题
- 自己做一些小修改

---

## 20. 最后建议

你的目标不应该是：

- 一下子看懂所有文件

你的目标应该是：

- 一次只看懂一条小流程
- 看够几条流程后，逐渐发现这个项目的重复模式

这个项目很多地方其实都在重复同一种结构：

```text
FXML
-> JavaFX Controller
-> HttpRequestUtil
-> Spring Controller
-> Service
-> Repository / Entity
-> JSON 返回
-> UI 刷新
```

当你能一眼认出这个结构时，你就不会再觉得整个项目很乱了。
