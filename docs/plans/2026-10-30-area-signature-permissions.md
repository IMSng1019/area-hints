# 域名签名权限修订计划

**目标：** 修复收缩及扩展签名管理误用上级域名归属的问题，并允许域名扩展签名者申请城市化。

**架构：** 复用 `AreaData.hasSignature` 和 `AreaPermissionUtil.isSignedBy`，同时识别本域名的 `signature` 与 `signatures`。服务端统一能力判断供命令列表、实际写入、地图 GUI 和审批重提复用；客户端收缩与签名管理选择列表使用同样的签名规则，管理员保持至少 2 级权限。

**范围：** `shrinkarea`、`addsignature`、`deletesignature`、`applyurbanarea`。保留现有 LuckPerms 节点行为、审批开关、删除扩展签名的二次确认和临时预览流程；其他命令的上级域名授权规则不在本次修改范围。

1. 修改 `AreaManagementCapabilityService.java` 中四个操作的归属判断，删除签名管理专用的上级索引参数。
2. 修改 `ShrinkAreaManager.java` 与 `SignatureManager.java` 的活动选择列表及相关注释，移除仅供旧规则使用的辅助方法。
3. 在收缩几何重建和服务端保存前保留原主签名及扩展签名，防止获准收缩后丢失授权；核对 `ApprovalManager` 的草稿、重新申请、正式批准及签名管理实际写入路径，确保新规则按服务端当前域名数据检查。
4. 更新全部 11 种语言以及 `COMMAND_USAGE.md`、两份 README 的对应规则，修正城市化只支持主签名的旧说明。
5. 不新增测试；执行五版本构建、翻译 JSON 与占位符校验、独立代码复核和差异检查，游戏内验证由用户完成。
6. 按本会话已有授权直接在 `main` 提交修订，不创建 worktree，不提交用户已有的 `forth-prompt.txt` 修改。
