# 阶段 3：NetworkCompat 网络抽象层

> 前置：阶段 2 完成，越界清单已明确。**本阶段是整个方案里唯一允许「深」的抽象层，其它地方一概不许再建第二层。**
>
> **已定稿**：物理隔离用机制 ②（接口 + 不同 FQN 实现，见 `00-总纲与约定.md` 第 2.1 节）；网络层是**唯一真断点**（D6），因此本阶段优先于扩展版本矩阵。

## 目标

把网络 API 的形状差异收口到一个接口里：核心代码只依赖这个接口，各 API 代际各写一份实现。
**抽象的是 API 形状，不是协议**——线上数据格式必须一个字节都不变。

## 边界（务必严格遵守）

- 入口只允许 **4 个**：发送给玩家、注册服务端接收器、注册客户端接收器、能否发送（canSend）。
- 单份实现目标 **≤60 行**，超了就说明抽象粒度错了，先汇报。
- **禁止**把它做成通用数据包框架：不加自动序列化、不加包注册表、不加统一 Packet 基类。
- **禁止**为了统一而改动 `Packets` 里的通道标识符与常量值。
- 那 30 多个只是引用 `Packets` 常量的文件**不需要进 versions/**，也不要包装它们。

### 接口草案（实现时可微调命名，但形态不许膨胀）

```java
package areahint.network;

/** 网络能力统一入口：只描述"发什么、谁收"，不描述怎么编解码 */
public interface NetworkCompat {
    /** 服务端向单个玩家发送一段已编码数据 */
    void sendToPlayer(ServerPlayerEntity player, Channel channel, byte[] data);
    /** 客户端向服务端发送一段已编码数据 */
    void sendToServer(Channel channel, byte[] data);
    /** 注册服务端接收器 */
    void registerServerReceiver(Channel channel, ServerReceiver receiver);
    /** 注册客户端接收器 */
    void registerClientReceiver(Channel channel, ClientReceiver receiver);
    /** 客户端是否可以向该通道发送（等价于旧的 canSend 语义） */
    boolean canSend(Channel channel);
}
```

`Channel` 是从现有 `Identifier` 常量派生出来的轻量标识；接收器回调只拿到"解码后的字段读取器"，**不要把 `PacketByteBuf` 或 `CustomPayload` 暴露进 `src/`**。

## 任务

### Task 1：先定接口与通道模型

**Files:**
- Create: `src/main/java/areahint/network/NetworkCompat.java`
- Create: `src/main/java/areahint/network/Channel.java`
- Create: `src/main/java/areahint/network/ServerReceiver.java`
- Create: `src/client/java/areahint/network/ClientReceiver.java`

**Step 1:** 按上面的形态落接口，**接口文件里不允许出现任何 `net.minecraft.*` import**。
**Step 2:** `Channel` 由现有 `Packets` 常量派生，**不改现有常量的值与命名**；给每个既有通道补一个稳定的字符串 id。
**Step 3:** 定义字段读取/写入的最小契约（读字符串、读布尔、读列表、写字符串、写布尔、写列表），**只覆盖现有协议真正用到的类型**，不要预留未来类型。

### Task 2：每个 API 代际一份实现

**Files:**
- Create: `versions/<最低版本，如 1.20.4>/src/main/java/areahint/network/NetworkCompatImpl.java`
- 视需要 Create: `versions/<新一代最低版本，如 1.21.1>/src/main/java/areahint/network/NetworkCompatImpl.java`

**Step 1:** 第一份实现用旧的 `PacketByteBuf` + `ServerPlayNetworking.send(player, Identifier, buf)` 形态；第二份用 `CustomPayload` + `PacketCodec` 形态。
**Step 2:** 两份实现必须产出**完全相同的线上字节**：字段顺序、字符串编码、布尔编码都与现状逐字一致。写完要逐字段对照现有 `ServerNetworking` / `ClientNetworking` 的读写顺序确认。
**Step 3:** 只有"该代际最小版本"那个目录放实现，同代其它版本目录留空。
**Step 4:** `src/` 里如何拿到实现，用**一处**委托解决，不要在 38 个文件里各写一遍分支。
**实现类命名（机制 ② 硬约束）**：版本目录里的类**不得**与共享 `src/` 中任何类同名同路径。建议 `NetworkCompatImpl1204` / `NetworkCompatImpl1211` 这种带版本后缀的命名。
**交接点（三种接法，按优先级）**：
1. **每个版本目录各放一个同名工厂类**（推荐：每个节点只挂载一个版本目录，同名也不冲突）；
2. 入口处集中写 2–3 行条件注释（入口只有一个时可接受）；
3. 反射用字符串类名（不推荐，丢失编译期检查）。

**已知额外断点（务必纳入设计）**：新网络 API 需要注册 PayloadType（`PayloadTypeRegistry`），**该注册动作在 1.20.4 上不存在**，因此注册入口也必须由版本侧提供，不能写在共享 `src/` 里。

### Task 3：改现有调用点（最小范围）

**Files:**
- Modify: `src/main/java/areahint/network/ServerNetworking.java`
- Modify: `src/client/java/areahint/network/ClientNetworking.java`
- 以及清单里明确需要迁移的其它网络文件

**Step 1:** **先只改这两个核心文件**，验证接口够用；不够用就改接口，不要在旁边加旁路。
**Step 2:** 其余网络文件**分批**迁移，每批一个包，每批后跑构建。
**Step 3:** 迁移过程中**不允许出现新旧两套 API 长期并存**；每批迁移完就把该文件的旧调用删干净。
**Step 4:** 本轮**不切换实际流量**（不改功能行为），只把调用形态收口。

## 验收标准

- `NetworkCompat` 接口零 Minecraft 依赖；每份实现 ≤60 行。
- 每个受影响的 API 代际下都有且仅有一份实现，其余版本目录为空。
- `src/` 中直接 import `ServerPlayNetworking` / `ClientPlayNetworking` / `PacketByteBuf` 的文件数**下降到接近 0**（残余必须逐条说明理由）。
- 所有已声明版本构建通过。
- 网络协议**未发生任何变化**：通道名、字段顺序、字段类型、编码方式与改动前逐字一致；不新增通道。
- 汇报里给出"迁移文件数 / 剩余文件数"的前后对比，以及"两份实现字节一致性"的核对说明。

## 阶段收尾（红灯变绿灯验证，需先汇报）

NetworkCompat 完成后，1.20.5/1.20.6 这一代际的网络断点应当整体消失。届时做一次验证：把 1.20.6 重新放回活跃 `versions` 列表，让它编译完整模组代码，统计剩余错误数。

- 没有剩余错误 → 该代际可转为「必出包」候选（仍需第三方依赖齐备，见阶段 1 的矩阵）。
- 仍有错误 → **不要就地修**，归类后汇报，由我决定是否放进阶段 4。
- 1.21.1 节点同时从 `skeleton=true` 转回完整编译，验证渲染层断点是否如预期（那部分属于阶段 4）。

## 禁止事项

- 不新增自定义包类型、不改通道 id、不加版本握手字段。
- 不写单元测试。
- 不为省事把 `PacketByteBuf` 或 `CustomPayload` 泄漏进 `src/`。
- 不把 30 多个只引用常量的文件也搬进 `versions/`。
