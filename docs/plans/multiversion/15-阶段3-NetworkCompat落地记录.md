# 15 · 阶段 3 NetworkCompat 落地记录（2026-10-03）

> 对应计划：`04-阶段3-NetworkCompat网络抽象层.md`。本文件记录**实际做法**与计划草案的差异、验证证据、以及留给后续阶段的接口。

## 一、结果速览

| 项 | 迁移前 | 迁移后 |
|---|---|---|
| 引用 `PacketByteBuf` / `ServerPlayNetworking` / `ClientPlayNetworking` 的文件 | **38** | **0** |
| `PacketByteBuf` 出现次数 | 219 | 0 |
| `ServerPlayNetworking` / `ClientPlayNetworking` 出现次数 | 77 / 84 | 0 / 0 |
| `Packets` 里的 `Identifier` 通道常量 | 52 | 0（全部由 `Channel` 承接） |
| 1.20.4 产物条目 | 456（基线） | 471 = 456 + 15 个版本适配层类 |

- `gradlew build` 退出码 **0**；`tools/verify-baseline.ps1` → **PASS**（基线 456 条全部存在，内容变化仅限被迁移的类）。
- **通道一致性 PASS**：`tools/verify-channels.ps1` 对 42 个改动文件逐个比对通道标识多重集，与迁移前**完全一致**。
- **字段序列 PASS**：`tools/verify-protocol.ps1` 对同样 42 个文件比对 `writeX/readX` 调用序列，**完全一致**（方法名编码了字段类型，故顺序与类型都没变）。

## 二、最终形态

```
src/main/java/areahint/network/     ← 共享侧：零 Minecraft 缓冲区依赖
├── NetworkCompat.java      服务端入口：newBuffer / sendToPlayer / registerServerReceiver
├── ClientNetworkCompat.java（在 src/client）客户端入口：newBuffer / sendToServer / registerClientReceiver / canSend
├── Channel.java            通道标识（命名空间 + 路径），零 MC 类型
├── NetworkBuf.java         版本中立缓冲区句柄 = NetworkWriter + NetworkReader
├── NetworkWriter/Reader.java  五种字段类型（String/boolean/int/long/double）+ readString(int) + isReadable
├── ServerReceiver.java     服务端接收器 (MinecraftServer, ServerPlayerEntity, NetworkReader)
├── ClientReceiver.java（在 src/client）接收器 (MinecraftClient, NetworkReader)
└── Packets.java            只剩维度常量与文件名工具方法（零 MC 依赖）

versions/1.20.4/src/main/java/areahint/network/
├── NetworkCompatFactory.java       同名工厂（机制 ② 交接点）
├── NetworkCompatImpl1204.java      Identifier + PacketByteBuf 形态
└── PacketByteBufHandle.java        NetworkBuf → PacketByteBuf 转发
versions/1.20.4/src/client/java/areahint/network/
├── ClientNetworkCompatFactory.java
└── ClientNetworkCompatImpl1204.java
```

交接点只有两处：`Areashint.onInitialize()` 与 `AreashintClient.onInitializeClient()` 各一行 `install(...)`。

## 三、与计划草案的三处偏差（都已论证）

1. **入口拆成两个接口**。草案把 `sendToServer` / `canSend` 放进 `NetworkCompat`，但它们依赖 `ClientPlayNetworking`（客户端专属），在 `splitEnvironmentSourceSets` 下不可能出现在 `src/main`。故拆成 `NetworkCompat`（main）+ `ClientNetworkCompat`（client）。
2. **用「缓冲区句柄」而不是 `Consumer<NetworkWriter>` / `byte[]`**。草案签名是 `sendToPlayer(player, channel, byte[] data)`。实测发现调用点几乎都是「建缓冲区 → 写若干字段 → 发送」，用 `NetworkBuf` 句柄后每处只换类型与调用名，**字段写序原样不动**，把协议风险降到最低；用 `byte[]` 则要在共享侧重实现 VarInt/字符串编码，风险高得多。计划里「唯一允许深的抽象层」的精神不变。
3. **接口保留 `ServerPlayerEntity`**。草案 Task 1 要求「接口文件里不允许出现任何 `net.minecraft.*` import」，但草案自己的签名就带 `ServerPlayerEntity`。该类型在 1.20.1–1.21.11 全程稳定（不构成版本分支），改成 `Object` 只会丢编译期检查。**缓冲区类型绝不进入接口**这一条没有妥协。

## 四、过程中的两个真坑（后续阶段注意）

1. **通道命名空间不是 MOD_ID**。`ServerWorldNetworking` / `DimensionalNameNetworking` / 两个客户端对应文件里的三个通道用的是 `"areashint:…"`（**没有连字符**），而 `Areashint.MOD_ID` 是 `"areas-hint"`。第一版 `Channel` 写死 MOD_ID，等于**静默改掉了三个通道名**（新旧版本不互通）。发现方式是把逐文件通道多重集与 HEAD 对比（`tools/verify-channels.ps1`）；现在 `Channel` 带命名空间字段，这三个通道显式写 `of("areashint", …)`。
   → **教训**：改名类重构必须做「改前/改后集合对比」，肉眼读 diff 会漏。
2. **接收 lambda 少了一个参数就会丢语义**。旧客户端接收器有 `(client, handler, buf, responseSender)`，`handler` 被 `ClientNetworking.handleAreaData` 用来判断「连接是否已失效」。新接口只给 `(client, reader)`，编译报「找不到符号 handler」，改用 `client.getNetworkHandler()` 补偿（与旧实参同一个对象）。

## 五、留给阶段 4+ 的接口

- 1.20.5+ 实现：在 `versions/1.20.6/src/main|client/java/areahint/network/` 放同名工厂 + `CustomPayload/PacketCodec` 版实现即可，共享侧一行不用动。
- 新增通道：只改 `Channel.java`，不要再往 `Packets` 加东西。
- 需要新字段类型时：同时改 `NetworkWriter`/`NetworkReader` 与**每个代际**的缓冲区句柄实现。
