package com.example.musicplayermod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模组配置：保存到 config/music_player_mod.json。
 * 包含总音量与各音效的独立开关。
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static ModConfig instance = new ModConfig();

	/** 模组音效总音量，0.0 ~ 1.0，对所有音效生效 */
	public float volume = 1.0F;

	/** 飞行音效开关（中国人能飞） */
	public boolean enableFlySound = true;

	/** 死亡音效开关（Tuco Get Out） */
	public boolean enableDeathSound = true;

	/** W+S 组合音效开关（Du Bist Gut Genug） */
	public boolean enableWsSound = true;

	/** C 键音效开关（Vine Boom） */
	public boolean enableVineBoomSound = true;

	/** 10 分钟无操作音效开关（含屏幕提示） */
	public boolean enableIdleSound = true;

	/** 成就达成音效开关（Oh My God） */
	public boolean enableAdvancementSound = true;

	/** 杀死猫音效开关（Sad Meow + 黑白画面） */
	public boolean enableCatSound = true;

	/** 吃东西音效开关（Gogogogogogo，吃时循环、停止/吃完即停） */
	public boolean enableEatSound = true;

	/** 模组界面语言："zh" 中文 / "en" 英文 */
	public String uiLanguage = "zh";

	public static ModConfig get() {
		return instance;
	}

	/** 从配置文件加载（不存在则使用默认值） */
	public static void load() {
		Path path = getPath();
		if (Files.exists(path)) {
			try {
				ModConfig loaded = GSON.fromJson(Files.readString(path), ModConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				MusicPlayerMod.LOGGER.error("[MusicPlayerMod] Failed to load config, using defaults", e);
			}
		}
	}

	/** 保存配置到文件 */
	public static void save() {
		try {
			Path path = getPath();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(instance));
		} catch (IOException e) {
			MusicPlayerMod.LOGGER.error("[MusicPlayerMod] Failed to save config", e);
		}
	}

	private static Path getPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("music_player_mod.json");
	}
}
