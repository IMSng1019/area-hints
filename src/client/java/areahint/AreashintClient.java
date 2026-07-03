package areahint;

import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.data.ConfigData;
import areahint.detection.AreaDetector;
import areahint.detection.AsyncAreaDetector;
import areahint.network.ClientNetworking;
import areahint.render.RenderManager;
import areahint.file.FileManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Identifier;

import areahint.i18n.I18nManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.io.IOException;

/**
 * 区域提示模组 - 客户端主类
 * 负责客户端的初始化、配置管理和玩家位置检测
 */
public class AreashintClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Areashint.MOD_ID + "-client");
	
	// 配置文件名
	private static final String CONFIG_FILE = "config.json";
	
	// 区域检测器和渲染管理器实例
	private static AreaDetector areaDetector;
	private static AsyncAreaDetector asyncAreaDetector;
	private static RenderManager renderManager;
	
	// 当前维度标识符
	private static Identifier currentDimension;
	
	// 当前区域名称（用于比较变化）
	private static String currentAreaName = null;
	// 当前服务器地址（用于检测服务器变化）
	private static String currentServerAddress = null;
	// 上一次tick时玩家是否为null（用于检测进入世界）
	private static boolean wasPlayerNull = true;
	private static boolean hasShownDimensionalName = false; // 是否已经显示过维度域名
	// 首次维度命名相关
	private static String pendingFirstNameDimId = null; // 等待命名的维度ID
	private static int firstNameTimeoutTicks = 0; // 超时计数器
	private static final int FIRST_NAME_TIMEOUT = 6000; // 5分钟 = 6000 ticks
	
	@Override
	public void onInitializeClient() {
		LOGGER.info("区域提示模组客户端初始化中...");

		// 检查并创建配置目录
		initConfigDir();

		// 初始化客户端日志管理器
		areahint.log.ClientLogManager.init();

		// 初始化配置
		ClientConfig.init();
		ClientConfig.correctHintRenderForEnvironment();

		// 初始化区域检测器
		areaDetector = new AreaDetector();
		asyncAreaDetector = new AsyncAreaDetector(areaDetector);

		// 初始化渲染管理器
		renderManager = new RenderManager();
		
		// 初始化维度域名管理器
		areahint.dimensional.ClientDimensionalNameManager.init();
		areahint.dimensional.DimensionalityNameVisualController.register();
		areahint.dimensional.DimensionalityColorVisualController.register();
		
		// 初始化网络处理
		ClientNetworking.init();
		
		// 初始化维度域名网络处理
		areahint.network.ClientDimensionalNameNetworking.init();
		
		// 初始化客户端世界网络处理
		areahint.network.ClientWorldNetworking.init();
		
		// 初始化SetHigh客户端命令
		areahint.command.SetHighClientCommand.init();
		areahint.command.SetHighVisualController.register();

		// 初始化Teleport客户端交互
		areahint.teleport.TeleportClientManager.init();

		// 初始化EasyAdd功能
		initEasyAdd();
		
		// 初始化ExpandArea功能
		initExpandArea();
		
		// 初始化ShrinkArea功能
		initShrinkArea();

		// 初始化AddHint功能
		initAddHint();

		// 初始化DeleteHint功能
		initDeleteHint();

		// 初始化Signature功能
		initSignature();

		// 初始化Description功能
		initDescription();

		// 初始化Subtitle功能
		initSubtitle();

		// 初始化DivideArea功能
		initDivideArea();

		// 初始化Delete功能
		initDelete();

		// 初始化ReplaceButton功能
		initReplaceButton();

		// 初始化Recolor图形流程
		areahint.recolor.RecolorVisualController.register();

		// 初始化Rename图形流程
		areahint.rename.RenameVisualController.register();

		// 初始化BoundViz功能
		initBoundViz();

		// 初始化i18n系统
		areahint.i18n.I18nManager.init();

		// 注册统一的X键处理器
		areahint.keyhandler.UnifiedKeyHandler.register();

		// 注册断开连接事件监听器
		registerDisconnectListener();

		// 注册客户端tick事件
		registerTickEvents();

		LOGGER.info("区域提示模组客户端初始化完成!");
	}
	
	/**
	 * 初始化EasyAdd功能
	 */
	private void initEasyAdd() {
		try {
			// 注册按键处理器
			areahint.easyadd.EasyAddKeyHandler.register();
			
			// 注册网络接收器
			areahint.easyadd.EasyAddNetworking.registerClientReceivers();

			// 注册 EasyAdd 图形流程 HUD 和启动超时处理。
			areahint.easyadd.EasyAddVisualController.register();
			areahint.easyadd.EasyAddRecordingHud.register();
			
			LOGGER.info("EasyAdd功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化EasyAdd功能时发生错误", e);
		}
	}
	
	/**
	 * 初始化ExpandArea功能
	 */
	private void initExpandArea() {
		try {
			// 注册按键处理器
			areahint.expandarea.ExpandAreaKeyHandler.register();
			
			// 注册网络接收器
			areahint.expandarea.ExpandAreaClientNetworking.registerClientNetworking();
			
			LOGGER.info("ExpandArea功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化ExpandArea功能时发生错误", e);
		}
	}
	
	/**
	 * 初始化ShrinkArea功能
	 */
	private void initShrinkArea() {
		try {
			// 注册按键处理器
			areahint.shrinkarea.ShrinkAreaKeyHandler.register();

			// 注册网络接收器
			areahint.shrinkarea.ShrinkAreaClientNetworking.registerClientNetworking();

			LOGGER.info("ShrinkArea功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化ShrinkArea功能时发生错误", e);
		}
	}

	/**
	 * 初始化AddHint功能
	 */
	private void initAddHint() {
		try {
			areahint.addhint.AddHintClientNetworking.registerClientReceivers();
			LOGGER.info("AddHint功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化AddHint功能时发生错误", e);
		}
	}

	/**
	 * 初始化DeleteHint功能
	 */
	private void initDeleteHint() {
		try {
			areahint.deletehint.DeleteHintClientNetworking.registerClientReceivers();
			LOGGER.info("DeleteHint功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化DeleteHint功能时发生错误", e);
		}
	}

	/**
	 * 初始化Signature功能
	 */
	private void initSignature() {
		try {
			areahint.signature.SignatureManager.init();
			areahint.signature.SignatureClientNetworking.registerClientReceivers();
			areahint.signature.AddSignatureVisualController.register();
			areahint.signature.DeleteSignatureVisualController.register();
			LOGGER.info("Signature功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化Signature功能时发生错误", e);
		}
	}

	/**
	 * 初始化Description功能
	 */
	private void initDescription() {
		try {
			areahint.description.DescriptionClientNetworking.registerClientReceivers();
			areahint.description.DescriptionKeyHandler.register();
			areahint.description.AddDescriptionVisualController.register();
			areahint.description.ReplaceDescriptionVisualController.register();
			areahint.description.DeleteDescriptionVisualController.register();
			areahint.description.AddDimensionalityDescriptionVisualController.register();
			areahint.description.ReplaceDimensionalityDescriptionVisualController.register();
			areahint.description.DeleteDimensionalityDescriptionVisualController.register();
			LOGGER.info("Description功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化Description功能时发生错误", e);
		}
	}

	/**
	 * 初始化Subtitle功能
	 */
	private void initSubtitle() {
		try {
			// 注册副字幕文本输入阶段的聊天拦截器，使 addsubtitle 的输入体验与 EasyAdd 类似。
			areahint.subtitle.SubtitleManager.init();
			areahint.subtitle.SubtitleNetworking.registerClientReceivers();
			areahint.subtitle.AddSubtitleVisualController.register();
			areahint.subtitle.ReplaceSubtitleVisualController.register();
			areahint.subtitle.DeleteSubtitleVisualController.register();
			areahint.subtitle.ReplaceSubtitleColorVisualController.register();
			areahint.subtitle.ReplaceSubtitleSizeVisualController.register();
			LOGGER.info("Subtitle功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化Subtitle功能时发生错误", e);
		}
	}

	/**
	 * 初始化DivideArea功能
	 */
	private void initDivideArea() {
		try {
			areahint.dividearea.DivideAreaClientNetworking.registerClientNetworking();
			LOGGER.info("DivideArea功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化DivideArea功能时发生错误", e);
		}
	}

	/**
	 * 初始化Delete功能
	 */
	private void initDelete() {
		try {
			// 注册网络接收器
			areahint.delete.DeleteNetworking.registerClientReceivers();
			areahint.delete.DeleteVisualController.register();

			LOGGER.info("Delete功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化Delete功能时发生错误", e);
		}
	}

	/**
	 * 初始化ReplaceButton功能
	 */
	private void initReplaceButton() {
		try {
			// 注册按键监听器
			areahint.replacebutton.ReplaceButtonKeyListener.register();

			// 注册网络接收器
			areahint.replacebutton.ReplaceButtonNetworking.registerClientReceivers();

			// 注册ReplaceButton图形流程的启动超时处理
			areahint.replacebutton.ReplaceButtonVisualController.register();

			LOGGER.info("ReplaceButton功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化ReplaceButton功能时发生错误", e);
		}
	}

	/**
	 * 初始化BoundViz功能
	 */
	private void initBoundViz() {
		try {
			// 初始化BoundViz管理器
			areahint.boundviz.BoundVizManager.getInstance();

			LOGGER.info("BoundViz功能初始化完成");
		} catch (Exception e) {
			LOGGER.error("初始化BoundViz功能时发生错误", e);
		}
	}

	/**
	 * 检查并创建外部配置目录
	 */
	private void initConfigDir() {
		try {
			// 使用FileManager获取配置目录
			Path configDirPath = FileManager.checkFolderExist();
			// 创建配置文件
			FileManager.createDefaultConfigFile(FileManager.getConfigFile(CONFIG_FILE));
			LOGGER.info("配置目录初始化完成：{}", configDirPath);
		} catch (IOException e) {
			LOGGER.error("创建配置文件失败: " + e.getMessage());
		}
	}
	
	/**
	 * 获取当前服务器地址，用于检测服务器变化
	 * @param client Minecraft客户端实例
	 * @return 当前服务器地址
	 */
	private static String getCurrentServerAddress(MinecraftClient client) {
		try {
			if (client.isIntegratedServerRunning()) {
				// 单人游戏
				return "localhost";
			}
			
			// 多人游戏
			ServerInfo serverInfo = client.getCurrentServerEntry();
			if (serverInfo != null && serverInfo.address != null) {
				// 移除端口号（如果有的话）
				String address = serverInfo.address;
				int colonIndex = address.lastIndexOf(':');
				if (colonIndex > 0) {
					address = address.substring(0, colonIndex);
				}
				return address;
			}
			
			// 备用
			return "unknown";
			
		} catch (Exception e) {
			LOGGER.warn("获取当前服务器地址失败", e);
			return "unknown";
		}
	}

	/**
	 * 注册断开连接事件监听器
	 * 在玩家退出世界/服务器时清理状态
	 */
	private void registerDisconnectListener() {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			LOGGER.info("检测到断开连接,清理世界文件夹管理器状态");

			// 重置ClientWorldFolderManager状态
			areahint.world.ClientWorldFolderManager.resetState();

			// 重置当前状态
			currentDimension = null;
			currentAreaName = null;
			currentServerAddress = null;
			wasPlayerNull = true;
			hasShownDimensionalName = false;
			pendingFirstNameDimId = null;
			firstNameTimeoutTicks = 0;

			// 重置区域追踪器、描述状态机和异步检测器
			areahint.log.AreaChangeTracker.reset();
			areahint.description.DescriptionManager.getInstance().reset();
			asyncAreaDetector.reset();

			LOGGER.info("世界文件夹管理器状态已清理");
		});
	}

	/**
	 * 注册客户端tick事件，用于检测玩家位置
	 */
	private void registerTickEvents() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientPlayerEntity player = client.player;
			
			// 检测玩家状态变化（从null变为非null = 刚进入世界）
			boolean justEnteredWorld = wasPlayerNull && (player != null && client.world != null);
			wasPlayerNull = (player == null || client.world == null);
			
			if (player == null || client.world == null) {
				return;
			}
			
			// 检查当前维度和服务器地址，如果改变则重新加载区域数据
			Identifier dimension = client.world.getDimensionKey().getValue();
			String serverAddress = getCurrentServerAddress(client);
			
			boolean dimensionChanged = currentDimension == null || !currentDimension.equals(dimension);
			boolean serverChanged = currentServerAddress == null || !currentServerAddress.equals(serverAddress);
			
			// 如果刚进入世界、维度变化或服务器变化，则执行检测和显示逻辑
			if (justEnteredWorld || dimensionChanged || serverChanged) {
				LOGGER.info("触发域名显示 - 刚进入世界: {}, 维度变化: {}, 服务器变化: {}",
					justEnteredWorld, dimensionChanged, serverChanged);

				// 如果刚进入世界，通知日志管理器
				if (justEnteredWorld) {
					areahint.log.AreaChangeTracker.onWorldEnter();
				}

				// 只有在维度或服务器变化时才重新加载数据和初始化
				if (dimensionChanged || serverChanged) {
					// 重置区域追踪器、描述交互状态和异步检测器
					areahint.log.AreaChangeTracker.reset();
					areahint.description.DescriptionManager.getInstance().reset();
					asyncAreaDetector.reset();

					// 如果服务器变化了，重置状态并重新初始化客户端世界文件夹
					if (serverChanged) {
						LOGGER.info("检测到服务器变化: '{}' -> '{}', 重新初始化世界文件夹", currentServerAddress, serverAddress);
						currentServerAddress = serverAddress;
						// 重置ClientWorldFolderManager状态
						areahint.world.ClientWorldFolderManager.resetState();
						areahint.world.ClientWorldFolderManager.initializeClientWorld();
					} else if (currentDimension == null) {
						// 如果是第一次进入世界，初始化客户端世界文件夹
						areahint.world.ClientWorldFolderManager.initializeClientWorld();
					}
					
					String dimensionFileName = getDimensionFileName(dimension);
					LOGGER.info("检测到维度变化为：{}，加载对应的区域文件：{}", dimension.toString(), dimensionFileName);
					areaDetector.loadAreaData(dimensionFileName);
				}
				
				// 更新当前状态
				currentDimension = dimension;
				
				// 每次触发时都重置状态并立即检测（包括刚进入世界的情况）
				currentAreaName = null;
				hasShownDimensionalName = false; // 重置维度域名显示标记
				pendingFirstNameDimId = null; // 重置首次命名状态
				firstNameTimeoutTicks = 0;

				// 检查是否为未命名维度（displayName == dimensionId）
				{
					String dimId = dimension.toString();
					String dimDisplayName = areahint.dimensional.ClientDimensionalNameManager.getDimensionalName(dimId);
					if (dimDisplayName.equals(dimId)) {
						pendingFirstNameDimId = dimId;
						firstNameTimeoutTicks = FIRST_NAME_TIMEOUT;
						areahint.dimensional.DimensionalNameUI.showFirstNamePrompt(dimId);
						LOGGER.info("检测到未命名维度: ，已显示命名提示", dimId);
					}
				}

				// 立即检测一次当前位置
				double playerX = player.getX();
				double playerY = player.getY();
				double playerZ = player.getZ();

				// 使用AreaChangeTracker检测区域变化并发送日志
				String areaName = null;
				if (ClientConfig.isEnabled()) {
					areaName = areahint.log.AreaChangeTracker.detectAndLogAreaChange(areaDetector, playerX, playerY, playerZ, currentDimension);
				}

				// 立即显示结果（仅在模组启用时）
				if (ClientConfig.isEnabled()) {
					if (areaName != null) {
						currentAreaName = areaName;
						areahint.data.AreaData detectedArea = areahint.log.AreaChangeTracker.getCurrentAreaData();
						String areaColor = detectedArea != null ? detectedArea.getColor() : "#FFFFFF";
						renderManager.showAreaTitle(areaName, areaColor, getAreaSubtitle(detectedArea), getAreaSubtitleColor(detectedArea));
						if (justEnteredWorld) {
							LOGGER.info("进入世界时位于区域：{}", areaName);
						} else {
							LOGGER.info("切换维度时位于区域：", areaName);
						}
					} else {
						// 如果不在区域内，显示维度域名
						String dimensionId = currentDimension.toString();
						String dimensionalName = areahint.dimensional.ClientDimensionalNameManager.getDimensionalName(dimensionId);
						String dimColor = areahint.dimensional.ClientDimensionalNameManager.getDimensionalColor(dimensionId);
						renderManager.showAreaTitle(dimensionalName, dimColor != null ? dimColor : "#FFFFFF");
						hasShownDimensionalName = true;
						if (justEnteredWorld) {
							LOGGER.info("进入世界时显示维度域名：{}", dimensionalName);
						} else {
							LOGGER.info("切换维度时显示维度域名：{}", dimensionalName);
						}
					}
				} else {
					if (justEnteredWorld) {
						LOGGER.info("进入世界时模组已禁用，不显示域名");
					} else {
						LOGGER.info("切换维度时模组已禁用，不显示域名");
					}
				}
			}

			// 首次维度命名超时处理
			if (pendingFirstNameDimId != null) {
				firstNameTimeoutTicks--;
				if (firstNameTimeoutTicks <= 0) {
					String dimPath = currentDimension != null ? currentDimension.getPath() : pendingFirstNameDimId;
					areahint.network.ClientDimNameNetworking.sendFirstDimName(pendingFirstNameDimId, dimPath);
					if (player != null) {
						player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.name_4") + dimPath), false);
					}
					pendingFirstNameDimId = null;
					firstNameTimeoutTicks = 0;
				}
			}

			// 异步检测：提交检测任务（移动阈值在AsyncAreaDetector内部判断）
			if (ClientConfig.isEnabled() && areaDetector.shouldDetect()) {
				asyncAreaDetector.submitDetection(player.getX(), player.getY(), player.getZ());
			}

			// 主线程消费异步检测结果
			if (ClientConfig.isEnabled()) {
				AsyncAreaDetector.DetectionResult result = asyncAreaDetector.pollResult();
				if (result != null) {
					String areaName = result.formattedName;

					// 在主线程处理区域变化追踪（网络消息必须在主线程发送）
					areahint.log.AreaChangeTracker.handlePrecomputedChange(
						result.areaData, currentDimension);

					LOGGER.debug("异步检测结果 - 区域：{}，之前区域：{}", areaName, currentAreaName);

					if ((areaName == null && currentAreaName != null) ||
						(areaName != null && !areaName.equals(currentAreaName))) {
						currentAreaName = areaName;
						if (areaName != null) {
							String areaColor = result.areaData != null ? result.areaData.getColor() : "#FFFFFF";
							renderManager.showAreaTitle(areaName, areaColor, getAreaSubtitle(result.areaData), getAreaSubtitleColor(result.areaData));
							hasShownDimensionalName = false;
						} else if (!hasShownDimensionalName) {
							String dimensionId = currentDimension != null
								? currentDimension.toString() : "minecraft:overworld";
							String dimensionalName = areahint.dimensional
								.ClientDimensionalNameManager.getDimensionalName(dimensionId);
							String dimColor = areahint.dimensional
								.ClientDimensionalNameManager.getDimensionalColor(dimensionId);
							renderManager.showAreaTitle(dimensionalName, dimColor != null ? dimColor : "#FFFFFF");
							hasShownDimensionalName = true;
						}
						}
					}
				} else {
					clearAreaDisplayState();
				}
			});
		}
	
	/**
	 * 获取外部配置目录路径
	 * @return 配置目录路径
	 */
	public static Path getConfigDir() {
		return FileManager.getConfigFolder();
	}
	
	/**
	 * 获取配置文件路径
	 * @return 配置文件路径
	 */
	public static Path getConfigFile() {
		return FileManager.getConfigFile(CONFIG_FILE);
	}
	
	/**
	 * 获取区域检测器实例
	 * @return 区域检测器
	 */
	public static AreaDetector getAreaDetector() {
		return areaDetector;
	}
	
	/**
	 * 获取渲染管理器实例
	 * @return 渲染管理器
	 */
	public static RenderManager getRenderManager() {
		return renderManager;
	}

	/**
	 * 获取域名副字幕文本。
	 * 只有域名文件里真实存在 subtitle 且内容非空时才显示副字幕，符合“没有字段则不显示副字幕”的规则。
	 */
	private static String getAreaSubtitle(AreaData area) {
		return area != null && area.hasSubtitle() ? area.getSubtitle() : null;
	}

	/**
	 * 获取域名副字幕颜色。
	 * 副字幕颜色只在副字幕存在时生效，避免缺少 subtitle 字段的旧域名被误显示空副字幕。
	 */
	private static String getAreaSubtitleColor(AreaData area) {
		return area != null && area.hasSubtitle() ? area.getSubtitleColor() : null;
	}

	/**
	 * 清理当前域名显示相关的运行状态。
	 * 模组关闭时只停止域名显示和检测，不清理已经接收并保存到本地的服务端文件。
	 */
	public static void clearAreaDisplayState() {
		currentAreaName = null;
		hasShownDimensionalName = false;
		areahint.log.AreaChangeTracker.reset();

		if (asyncAreaDetector != null) {
			asyncAreaDetector.reset();
		}

		if (renderManager != null) {
			renderManager.clearAreaTitle();
		}
	}
	
	/**
	 * 强制重新检测当前区域（在网络同步完成后调用）
	 */
	/**
	 * 清除首次维度命名的pending状态（命名成功后调用）
	 */
	public static void clearPendingFirstName() {
		pendingFirstNameDimId = null;
		firstNameTimeoutTicks = 0;
	}

	public static void forceRedetectCurrentArea() {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			ClientPlayerEntity player = client.player;
			
			if (player == null || client.world == null) {
				LOGGER.warn("无法进行强制重新检测：玩家或世界为null");
				return;
			}
			
			// 重新加载当前维度的区域数据
			if (currentDimension != null) {
				String dimensionFileName = getDimensionFileName(currentDimension);
				LOGGER.info("强制重新加载维度{}的区域文件：{}", currentDimension.toString(), dimensionFileName);
				areaDetector.loadAreaData(dimensionFileName);

				// 关闭状态下只重新加载同步文件，不进行域名计算，也不显示维度域名。
				if (!ClientConfig.isEnabled()) {
					clearAreaDisplayState();
					LOGGER.info("模组已关闭，网络同步后仅重新加载域名文件，不执行检测和显示");
					return;
				}
				
				// 立即检测当前位置
				double playerX = player.getX();
				double playerY = player.getY();
				double playerZ = player.getZ();
				AreaData detectedArea = areaDetector.findAreaRaw(playerX, playerY, playerZ);
				String areaName = detectedArea != null ? areaDetector.formatAreaNameFromData(detectedArea) : null;
				
				// 立即显示结果
				if (areaName != null) {
					currentAreaName = areaName;
					String areaColor = detectedArea != null ? detectedArea.getColor() : "#FFFFFF";
					renderManager.showAreaTitle(areaName, areaColor, getAreaSubtitle(detectedArea), getAreaSubtitleColor(detectedArea));
					LOGGER.info("网络同步后检测到位于区域：{}", areaName);
				} else {
					// 如果不在区域内，显示维度域名
					String dimensionId = currentDimension.toString();
					String dimensionalName = areahint.dimensional.ClientDimensionalNameManager.getDimensionalName(dimensionId);
					String dimColor = areahint.dimensional.ClientDimensionalNameManager.getDimensionalColor(dimensionId);
					renderManager.showAreaTitle(dimensionalName, dimColor != null ? dimColor : "#FFFFFF");
					hasShownDimensionalName = true;
					LOGGER.info("网络同步后显示维度域名：{}", dimensionalName);
				}
			}
			
		} catch (Exception e) {
			LOGGER.error("强制重新检测当前区域时发生错误", e);
		}
	}
	
	/**
	 * 从配置界面一次性应用客户端配置，并刷新依赖配置的客户端系统。
	 * @param draft 草稿配置
	 * @param recordKeyChanged 记录按键是否变化
	 */
	public static void applyClientConfig(ConfigData draft, boolean recordKeyChanged) {
		LOGGER.info("应用客户端配置界面修改...");
		ClientConfig.apply(draft);
		ClientConfig.correctHintRenderForEnvironment();
		I18nManager.loadLanguage(ClientConfig.getLanguage());
		areahint.network.ClientNetworking.sendLanguageToServer();

		if (recordKeyChanged) {
			areahint.keyhandler.UnifiedKeyHandler.reregisterKey();
		}

		if (renderManager != null) {
			renderManager.updateRenderMode();
			LOGGER.info("渲染管理器已更新");
		}

		areahint.boundviz.BoundVizManager.getInstance().applyConfigEnabled(ClientConfig.isBoundVizEnabled());
		LOGGER.info("边界可视化状态和数据已重新加载");

		reloadCurrentDimensionAreaData();
	}

	/**
	 * 重新加载配置和区域数据
	 */
	public static void reload() {
		LOGGER.info("重新加载区域提示模组配置和区域数据...");
		ClientConfig.load();
		ClientConfig.correctHintRenderForEnvironment();

		// 更新渲染管理器的渲染模式（这会重新读取域名标题大小配置）
		if (renderManager != null) {
			renderManager.updateRenderMode();
			LOGGER.info("渲染管理器已更新");
		}

		// 维度域名配置由服务端同步，不在此处重置
		LOGGER.info("维度域名配置保持服务端同步状态");

		// 重新加载边界可视化数据
		areahint.boundviz.BoundVizManager.getInstance().reload();
		LOGGER.info("边界可视化数据已重新加载");

		reloadCurrentDimensionAreaData();
	}

	private static void reloadCurrentDimensionAreaData() {
		if (currentDimension == null || areaDetector == null) {
			return;
		}

		String dimensionFileName = getDimensionFileName(currentDimension);
		LOGGER.info("重新加载维度{}的区域文件：{}", currentDimension.toString(), dimensionFileName);

		// 获取文件路径并检查是否存在
		Path areaFile = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(dimensionFileName);
		LOGGER.info("[调试] 重新加载区域文件路径: {}", areaFile.toAbsolutePath());
		if (java.nio.file.Files.exists(areaFile)) {
			try {
				String content = java.nio.file.Files.readString(areaFile);
				LOGGER.info("[调试] 区域文件大小: {} 字节", content.length());
			} catch (Exception e) {
				LOGGER.error("读取区域文件失败", e);
			}
		} else {
			LOGGER.warn("[调试] 区域文件不存在: {}", areaFile.toAbsolutePath());
		}

		areaDetector.loadAreaData(dimensionFileName);
	}
	
	/**
	 * 根据维度标识符获取对应的区域数据文件名（静态方法）
	 * @param dimension 维度标识符
	 * @return 区域数据文件名
	 */
	public static String getDimensionFileName(Identifier dimension) {
		if (dimension.getPath().equals("the_nether")) {
			return Areashint.NETHER_FILE;
		} else if (dimension.getPath().equals("the_end")) {
			return Areashint.END_FILE;
		} else {
			return Areashint.OVERWORLD_FILE;
		}
	}
}
