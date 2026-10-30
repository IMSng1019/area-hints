# 命令面板搜索与签名提示调整

**目标：** 隐藏签名命令 GUI 域名条目下的管理员模式提示，并支持命令面板模糊搜索。

**设计：** 沿用现有命令列表、分类及签名控制器。搜索在命令 ID、完整命令、显示名称、说明和分类中匹配；每个关键词允许字符按顺序不连续出现，空格分隔的关键词必须全部命中同一命令的任意字段。忽略大小写、全角半角差异及 Minecraft 颜色格式码；空搜索继续显示全部可用命令。

**方案取舍：** 单纯包含匹配无法搜索缩写；顺序字符匹配能以较小改动支持缩写及中文片段，采用此方案；暂不引入编辑距离纠错和相关度排序，以保留原有分类顺序。

**技术：** Java 17、现有 Fabric GUI 与 Stonecutter 构建配置。

## 实施步骤

1. 修改 `src/client/java/areahint/signature/AddSignatureVisualController.java` 和 `DeleteSignatureVisualController.java`，移除管理员模式条目提示，保留普通玩家提示和原有权限判断。
2. 修改 `src/client/java/areahint/commandui/CommandListScreen.java`，统一规范化搜索文本，拆分关键词并实现顺序字符匹配；搜索改变时回到列表顶部。
3. 复核差异和空输入、多关键词、不连续字符的代码路径，执行 Gradle 构建（排除测试）及 `git diff --check`。

## 手动验收参考

- `addsignature`、`deletesignature` 的 GUI 域名条目不再出现管理员模式提示。
- `adsg` 能匹配 `addsignature`；中文界面 `添加 签名` 与 `签名 添加` 均能匹配添加签名。
- 大小写和全角输入能够匹配；空白输入恢复全部可用命令，无匹配时显示原有空结果提示。

按用户要求直接在 main 修改，不新增测试；用户后续已授权提交本次修改，游戏内验收由用户执行。
