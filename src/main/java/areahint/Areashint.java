package areahint;

import areahint.command.ServerCommands;
import areahint.file.FileManager;
import areahint.i18n.ServerI18nManager;
import areahint.map.BlueMapCompat;
import areahint.network.ServerNetworking;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.permission.LuckPermsCompat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

/**
 * 区域提示模组 - 服务端主类
 * 负责服务端的初始化、命令注册和文件管理
 */
public class Areashint implements ModInitializer {
	public static final String MOD_ID = "areas-hint";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	// 各维度区域数据文件名
	public static final String OVERWORLD_FILE = "overworld.json";
	public static final String NETHER_FILE = "the_nether.json";
	public static final String END_FILE = "the_end.json";
	
	// 服务器实例引用
	private static MinecraftServer server;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("区域提示模组服务端初始化中...");

		// 初始化服务端国际化
		ServerI18nManager.init();

		// 初始化权限兼容层
		LuckPermsCompat.initialize();

		// 初始化 BlueMap 兼容层
		BlueMapCompat.init();

		// 初始化文件管理
		initConfigDir();

		// 初始化服务端日志管理器
		areahint.log.ServerLogManager.init();

		// 初始化服务端日志网络处理
		areahint.log.ServerLogNetworking.init();
		
		// 初始化维度域名管理器
		areahint.dimensional.DimensionalNameManager.init();
		
		// 初始化维度域名网络处理
		areahint.network.DimensionalNameNetworking.init();
		
		// 初始化服务端世界网络处理
		areahint.network.ServerWorldNetworking.init();
		
		// 注册服务器启动事件监听器
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
		// 注册服务器停止事件监听器
		ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);
		ServerTickEvents.END_SERVER_TICK.register(server -> BlueMapCompat.onServerTick(System.currentTimeMillis()));

		// 初始化服务端网络处理
		ServerNetworking.init();

		// 初始化 Xaero 世界地图域名管理能力查询
		areahint.management.AreaManagementServerNetworking.register();
		
		// 初始化EasyAdd服务端网络处理
		areahint.easyadd.EasyAddServerNetworking.registerServerReceivers();

		// 初始化Rename服务端网络处理
		areahint.command.RenameAreaCommand.registerServerReceivers();

		// 初始化ExpandArea服务端网络处理
		areahint.expandarea.ExpandAreaServerNetworking.registerServerNetworking();
		
		// 初始化ShrinkArea服务端网络处理
		areahint.shrinkarea.ShrinkAreaServerNetworking.registerServerNetworking();

		// 初始化AddHint服务端网络处理
		areahint.addhint.AddHintServerNetworking.registerServerReceivers();

		// 初始化DeleteHint服务端网络处理
		areahint.deletehint.DeleteHintServerNetworking.registerServerReceivers();

		// 初始化Signature服务端网络处理
		areahint.signature.SignatureServerNetworking.registerServerReceivers();

		// 初始化Description服务端网络处理
		areahint.description.DescriptionServerNetworking.registerServerReceivers();

		// 初始化Subtitle服务端网络处理
		areahint.subtitle.SubtitleCommand.registerServerReceivers();

		// 初始化DivideArea服务端网络处理
		areahint.dividearea.DivideAreaServerNetworking.registerServerNetworking();
		
		// 注册维度变更事件 - 首次进入未命名维度时提示OP命名
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			String dimId = destination.getRegistryKey().getValue().toString();
			if (PermissionService.hasCommandPermission(player, PermissionNodes.DIMENSIONALITY_NAME, 2)
				&& !areahint.dimensional.DimensionalNameManager.hasDimensionalName(dimId)) {
				// 自动注册该维度（使用维度ID作为默认名称）
				areahint.dimensional.DimensionalNameManager.setDimensionalName(
					dimId, dimId, player.getGameProfile().getName());
				areahint.dimensional.DimensionalNameManager.saveDimensionalNames();
				// 提示OP玩家命名
				MutableText msg = Text.translatable("message.message.dimension_3").append(Text.literal(dimId));
				player.sendMessage(msg, false);
				MutableText nameBtn = Text.translatable("message.button.dimension_3")
					.setStyle(Style.EMPTY
						.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
							"/areahint dimensionalityname select \"" + dimId + "\""))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							Text.translatable("message.hover.dimension.setname").append(Text.literal(dimId)))));
				player.sendMessage(nameBtn, false);
				// 同步给客户端
				areahint.network.DimensionalNameNetworking.sendDimensionalNamesToClient(player);
			}
		});

		// 注册命令
		ServerCommands.register();
		
		LOGGER.info("区域提示模组服务端初始化完成!");
	}
	
	/**
	 * 检查并创建外部配置目录
	 */
	private void initConfigDir() {
		try {
			// 使用FileManager获取配置目录
			Path configDirPath = FileManager.checkFolderExist();
			LOGGER.info("配置目录初始化完成: {}", configDirPath);
			
			// 注意：默认区域文件现在将在世界文件夹初始化时创建
			
		} catch (Exception e) {
			LOGGER.error("初始化配置目录失败: " + e.getMessage());
		}
	}
	
	/**
	 * 服务器启动事件处理
	 * @param minecraftServer 服务器实例
	 */
	private void onServerStarting(MinecraftServer minecraftServer) {
		server = minecraftServer;
		LOGGER.info("区域提示模组: 服务器启动中");
		
		// 初始化世界文件夹管理器
		areahint.world.WorldFolderManager.initializeServerWorld(minecraftServer);

		// 世界文件夹就绪后重新加载维度域名（init时世界路径尚未初始化）
		areahint.dimensional.DimensionalNameManager.loadDimensionalNames();

		// 初始化 BlueMap 服务端同步
		BlueMapCompat.onServerStarted(minecraftServer);
	}
	
	/**
	 * 服务器停止事件处理
	 * @param minecraftServer 服务器实例
	 */
	private void onServerStopped(MinecraftServer minecraftServer) {
		BlueMapCompat.onServerStopped();
		server = null;
		LOGGER.info("区域提示模组: 服务器已停止");

		// 关闭服务端日志管理器
		areahint.log.ServerLogManager.shutdown();
	}
	
	/**
	 * 获取服务器实例
	 * @return 当前服务器实例
	 */
	public static MinecraftServer getServer() {
		return server;
	}
	
	/**
	 * 获取外部配置目录路径
	 * @return 配置目录路径
	 */
	public static Path getConfigDir() {
		return FileManager.getConfigFolder();
	}
}
