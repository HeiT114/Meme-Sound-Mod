package com.example.musicplayermod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主入口。
 * 在服务端/客户端通用环境中注册自定义声音事件。
 */
public class MusicPlayerMod implements ModInitializer {
	public static final String MOD_ID = "music_player_mod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** 飞行时循环播放的声音（中国人能飞） */
	public static final ModSound FLY_SOUND = ModSound.registerSound("chinese_can_fly");

	/** 死亡时播放的声音（Tuco Get Out，只播一次） */
	public static final ModSound DEATH_SOUND = ModSound.registerSound("tuco_get_out");

	/** 按下 C 键时播放的声音（vine boom） */
	public static final ModSound VINE_BOOM_SOUND = ModSound.registerSound("vine_boom");

	/** 同时按住 W+S 时循环播放的声音（Du Bist Gut Genug） */
	public static final ModSound DU_BIST_GUT_GENUG_SOUND = ModSound.registerSound("du_bist_gut_genug");

	/** 10 分钟无操作时播放的声音（A Few Moments Later） */
	public static final ModSound IDLE_SOUND = ModSound.registerSound("a_few_moments_later");

	/** 达成成就/挑战时播放的声音（Oh My God） */
	public static final ModSound ADVANCEMENT_SOUND = ModSound.registerSound("oh_my_god");

	/** 杀死猫时播放的声音（Sad Meow） */
	public static final ModSound CAT_SOUND = ModSound.registerSound("sad_meow");

	/** 吃东西时循环播放的声音（Gogogogogogo） */
	public static final ModSound EAT_SOUND = ModSound.registerSound("gogogogogogo");

	@Override
	public void onInitialize() {
		// 静态字段初始化即完成声音注册，这里仅输出日志
		LOGGER.info("[MusicPlayerMod] initialized, version {}", getVersion());
	}

	/** 从 fabric.mod.json 读取当前模组版本号（如 1.0.5） */
	public static String getVersion() {
		return FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}

	/** 便捷方法：生成属于本模组的 Identifier */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
