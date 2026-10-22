# 服务端配置与域名游戏模式实施计划

**目标：** 启动时补齐并校验服务端配置，按配置执行登录版本握手，并由服务端裁定域名游戏模式，提供管理员修改命令与可视化入口。

**架构：** 沿用全局 server-config.json、世界目录域名文件和 NetworkCompat 通道。登录握手在玩家进入世界前完成；客户端只发送重新判定通知，服务端使用自己的坐标、维度与文件求出最深层域名及游戏模式。

**技术：** Java、Fabric、Stonecutter 的现有五个活跃版本、Gson、Brigadier、现有向导 GUI。

## 实施步骤

1. 扩展 `config/ServerConfig.java`：统一配置项定义与校验，补齐缺失键、修复非法值并保留未知键；保留 submit_approval，添加 force_use_areahint 与 default_gamemode。
2. 新增握手服务端/客户端模块：使用登录查询检查模组版本，失败直接断开；带服务端版本和下载网址，支持本次功能的客户端提供下载按钮。
3. 扩展 `data/AreaData.java`、`file/JsonHelper.java`、`util/AreaDataConverter.java`，完整保留可选 gamemode 属性。
4. 新增 `gamemode/` 服务端判定与客户端通知：层级、距离、高度、多边形规则保持与现有判定一致；未命中或未设置属性采用 default_gamemode。处理进入、离开、换维度、重生、重载与同步时序。
5. 新增 replaceareagamemode：服务端本地列表、四种模式、修改结果展示、统一 reload；配置开关与管理员/LuckPerms 权限每步复核，接入命令 GUI。
6. 更新全部 11 个语言资源及使用文档，保留原有翻译和文件排版。
7. 不新增测试、不运行游戏、不提交代码；使用现有 Gradle 构建（跳过测试）、差异检查及语言 JSON 核验确认可编译性与资源完整性。

## 已确定的边界

- 缺少 gamemode 的子域名使用全局默认值，不继承父域名属性；域名外同样使用默认值。
- default_gamemode 新建或修复时采用服务器当前默认模式（专用服务端来自 server.properties 的 gamemode）；显式合法配置予以保留。
- 关闭 force_use_areahint 时不强制握手、不自动更改玩家模式，修改命令拒绝执行。
- 未安装模组的原版客户端断开界面无法响应网址点击，只能显示完整网址；支持本次功能的客户端增加下载按钮。
- 遵循现有三种维度文件映射，未知自定义维度不读取主世界文件。
- 所有修改在 main 完成，不修改现有用户工作文件 `forth-prompt.txt`。
