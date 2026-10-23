# 城市化审批实施计划

**目标：** 按用户给定流程新增默认关闭的 `submit_urban_area`，通过 `/areahint applyurbanarea` 申请将当前维度的服务端域名改为 `type: "urban"`；旧文件缺失类型时视为 `rural`。

**架构：** 复用既有审批记录、全局申请目录、世界回复缓存、临时可视化、特殊顶点编辑、离线回复和历史意见。城市化审批独立于 `submit_approval`；普通玩家提交申请，等级至少为 2 的 OP 直接生效。命令和审批重提共同检查配置、LuckPerms 节点及 `base-name` 与申请玩家名的匹配关系。

**技术：** Java、Fabric、现有多版本网络兼容层、Gson、Stonecutter。

1. 在 `ServerConfig`、`AreaData`、`JsonHelper`、`AreaDataConverter` 增加配置和类型兼容，并在几何复制/网络写入入口保留服务端类型。
2. 新增 `urban/ApplyUrbanAreaCommand`，在 `ServerCommands`、`PermissionNodes`、`AreaManagementCapabilityService` 注册；列表和选择仅访问当前服务端世界和维度，提交时重新校验权限，成功后展示参数并重载。
3. 扩展 `ApprovalManager` 和审批客户端，使城市化单独启用；批准和重提复核类型及权限，历史回复在关闭配置后仍可查看。
4. 新增客户端城市化选择与结果界面，沿用服务端命令树隐藏不可用命令，独立网络通道只传服务器查询结果。
5. 同步全部 11 种语言和 `COMMAND_USAGE.md`。
6. 不新增测试，不创建 worktree，不 commit；完成编译打包、翻译 JSON 校验、差异检查和代码复核，游戏内操作由用户验证。

**解释：** 用户给出的实际子指令 `/areahint applyurbanarea` 为实现依据；描述中的 `replaceareagamemode` 已属于另一既有功能。`urban` 作为 `type` 字段值，缺失值兼容为 `rural`。
