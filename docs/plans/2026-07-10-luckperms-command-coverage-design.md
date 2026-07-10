# LuckPerms 指令覆盖设计

## 目标

为当前尚未接入权限服务的 7 个 `/areahint` 顶层指令补充 LuckPerms 节点：

- `areahint.command.on`
- `areahint.command.off`
- `areahint.command.firstdimname`
- `areahint.command.firstdimname_skip`
- `areahint.command.replacebutton`
- `areahint.command.language`
- `areahint.command.boundviz`

## 权限行为

所有节点都通过现有 `PermissionService` 查询。LuckPerms 未安装或节点未定义时，继续使用权限等级 0 的回退规则，保持这些指令当前默认对玩家开放的行为；节点被明确拒绝时，Brigadier 不再向玩家展示或允许执行对应指令。

## 服务端校验

`on`、`off`、`replacebutton`、`language` 和 `boundviz` 只触发执行玩家自己的客户端行为，在顶层指令节点增加 `.requires(...)` 即可。

`firstdimname` 与 `firstdimname_skip` 会写入服务端维度域名数据，因此在指令树增加权限判断。旧客户端仍可能直接发送 `C2S_FIRST_DIMNAME`，服务端网络接收器也必须使用 `areahint.command.firstdimname` 再次校验，防止绕过指令树。

## 兼容性

现有副字幕、描述、域名编辑、传送及别名权限映射保持不变。本次不增加翻译键，不修改任何域名 JSON 字段，也不改变未安装 LuckPerms 时的行为。

## 验证

按仓库要求不新增测试。修改后运行 Gradle 完整构建，并静态核对 7 个指令节点和 `C2S_FIRST_DIMNAME` 网络入口均调用统一权限服务。
