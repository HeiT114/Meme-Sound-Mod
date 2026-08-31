package com.example.musicplayermod.client;

import com.example.musicplayermod.ModConfig;
import com.example.musicplayermod.MusicPlayerMod;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.feline.Cat;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端入口：
 * <ul>
 *   <li>开始飞行（创造/鞘翅）→ 循环播放《中国人能飞》；停止飞行 → 立即停止</li>
 *   <li>死亡 → 播放《Tuco Get Out》（只播一次）；点重生 → 立即停止</li>
 *   <li>同时按住 W+S → 循环播放《Du Bist Gut Genug》；松开任意键 → 立即停止</li>
 *   <li>按 C 键 → 播放 vine boom（按住不重复触发）</li>
 *   <li>按 K 键 → 打开模组设置界面</li>
 *   <li>10 分钟无操作 → 播放一次《A Few Moments Later》并在屏幕中央显示「这个玩家睡着了」2 秒</li>
 *   <li>达成任意成就/进度/挑战 → 播放《Oh My God》（由 mixin 调用 {@link #onAdvancementDone}）</li>
 *   <li>杀死猫 → 播放《Sad Meow》并让画面变为黑白，播放完恢复</li>
 * </ul>
 * 所有音效可分别在设置界面中开关，音量受总音量设置控制。
 */
public class MusicPlayerModClient implements ClientModInitializer {

	/** C 键：播放 vine boom */
	private static final KeyMapping VINE_BOOM_KEY = new KeyMapping(
			"key.music_player_mod.vine_boom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KeyMapping.Category.MISC);

	/** K 键：打开设置界面 */
	private static final KeyMapping SETTINGS_KEY = new KeyMapping(
			"key.music_player_mod.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC);

	/** 10 分钟 = 600 秒 = 12000 tick */
	private static final int IDLE_THRESHOLD_TICKS = 20 * 600;

	/** 空闲提示显示时长：2 秒 = 40 tick */
	private static final int IDLE_MESSAGE_TICKS = 40;

	/** 杀猫黑白效果时长：sad_meow.ogg 约 25.04 秒，26 秒后恢复画面 */
	private static final int CAT_EFFECT_TICKS = 26 * 20;

	/** 上一刻的飞行状态 */
	private boolean wasFlying = false;

	/** 上一刻的死亡状态 */
	private boolean wasDead = false;

	/** 上一刻 C 键是否按下（边沿检测，防止长按 repeat 重复触发） */
	private boolean wasVineBoomDown = false;

	/** 上一刻 K 键是否按下（边沿检测） */
	private boolean wasSettingsDown = false;

	/** 上一刻 W+S 是否同时按下（边沿检测） */
	private boolean wasWsPressed = false;

	/** 当前正在循环播放的飞行音乐实例 */
	private PlayerMusicSoundInstance flyMusic;

	/** 当前正在播放的死亡音乐实例（一次性） */
	private PlayerMusicSoundInstance deathMusic;

	/** 当前正在循环播放的 W+S 音乐实例 */
	private PlayerMusicSoundInstance wsMusic;

	/** 当前正在播放的杀猫音乐实例（一次性） */
	private PlayerMusicSoundInstance catMusic;

	/** 当前正在循环播放的吃东西音乐实例 */
	private PlayerMusicSoundInstance eatMusic;

	/** 上一刻是否在吃东西（边沿检测） */
	private boolean wasEating = false;

	/** 杀猫黑白效果已持续的 tick 数 */
	private int catEffectTicks = 0;

	// —— 空闲检测 ——
	private int idleTicks = 0;
	private double lastMouseX = Double.NaN;
	private double lastMouseY = Double.NaN;

	// —— 杀猫检测 ——
	private int catScanCooldown = 0;
	private final Map<Integer, Boolean> catDeadStates = new HashMap<>();

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ModHud.register();
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(Minecraft client) {
		// —— C 键播放 vine boom（isDown 边沿：按下瞬间一次，按住不重复） ——
		boolean vineBoomDown = VINE_BOOM_KEY.isDown();
		if (vineBoomDown && !wasVineBoomDown && ModConfig.get().enableVineBoomSound) {
			playOneShot(MusicPlayerMod.VINE_BOOM_SOUND.event());
			MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Played vine boom (key C)");
		}
		wasVineBoomDown = vineBoomDown;

		// —— K 键打开设置界面（isDown 边沿） ——
		boolean settingsDown = SETTINGS_KEY.isDown();
		if (settingsDown && !wasSettingsDown && client.screen == null) {
			client.setScreen(new SettingsScreen());
			MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Opened settings screen");
		}
		wasSettingsDown = settingsDown;

		// —— 同时按住 W+S：循环播放，松开任意键立即停止 ——
		boolean wsPressed = client.options.keyUp.isDown() && client.options.keyDown.isDown();
		if (wsPressed && !wasWsPressed) {
			if (ModConfig.get().enableWsSound) {
				wsMusic = startMusic(MusicPlayerMod.DU_BIST_GUT_GENUG_SOUND.event(), true);
				MusicPlayerMod.LOGGER.info("[MusicPlayerMod] W+S pressed, playing du_bist_gut_genug (loop)");
			}
		} else if (!wsPressed && wasWsPressed) {
			stopMusic(wsMusic);
			wsMusic = null;
			MusicPlayerMod.LOGGER.info("[MusicPlayerMod] W+S released, stopping music");
		}
		wasWsPressed = wsPressed;

		// —— 10 分钟无操作检测 ——
		if (client.player != null) {
			tickIdleDetection(client);
		}

		// —— 杀猫检测 ——
		tickCatDetection(client);

		// —— 杀猫黑白效果：固定时长（音频 25 秒）后恢复画面 ——
		if (ModHud.catEffectActive) {
			catEffectTicks++;
			if (catEffectTicks >= CAT_EFFECT_TICKS) {
				ModHud.catEffectActive = false;
				catEffectTicks = 0;
				stopMusic(catMusic);
				catMusic = null;
				MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Sad meow finished, screen restored");
			}
		}

		// —— 空闲提示计时 ——
		if (ModHud.idleMessageTicks > 0) {
			ModHud.idleMessageTicks--;
		}

		LocalPlayer player = client.player;
		if (player == null) {
			// 未进入世界 / 断线：停止所有音乐并重置状态
			stopMusic(flyMusic);
			stopMusic(deathMusic);
			stopMusic(wsMusic);
			stopMusic(catMusic);
			stopMusic(eatMusic);
			flyMusic = null;
			deathMusic = null;
			wsMusic = null;
			catMusic = null;
			eatMusic = null;
			wasEating = false;
			ModHud.catEffectActive = false;
			catEffectTicks = 0;
			wasFlying = false;
			wasDead = false;
			return;
		}

		boolean dead = player.isDeadOrDying();

		if (dead) {
			// —— 死亡状态：停止飞行/W+S/吃东西音乐，播放死亡音效（只播一次） ——
			if (!wasDead) {
				stopMusic(flyMusic);
				stopMusic(wsMusic);
				stopMusic(eatMusic);
				flyMusic = null;
				wsMusic = null;
				eatMusic = null;
				wasEating = false;
				if (ModConfig.get().enableDeathSound) {
					deathMusic = startMusic(MusicPlayerMod.DEATH_SOUND.event(), false);
					MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Player died, playing tuco_get_out (once)");
				}
			}
		} else {
			// —— 吃东西检测（正在食用食物：循环播放，停止/吃完立即停止） ——
			boolean eating = player.isUsingItem() && player.getUseItem().has(DataComponents.FOOD);
			if (eating && !wasEating) {
				if (ModConfig.get().enableEatSound) {
					eatMusic = startMusic(MusicPlayerMod.EAT_SOUND.event(), true);
					MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Player eating, playing gogogogogogo (loop)");
				}
			} else if (!eating && wasEating) {
				stopMusic(eatMusic);
				eatMusic = null;
				MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Player stopped eating, stopping music");
			}
			wasEating = eating;

			// —— 飞行检测（创造模式飞行 或 鞘翅滑翔） ——
			boolean flying = player.getAbilities().flying || player.isFallFlying();
			if (flying && !wasFlying) {
				if (ModConfig.get().enableFlySound) {
					flyMusic = startMusic(MusicPlayerMod.FLY_SOUND.event(), true);
					MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Player started flying, playing chinese_can_fly (loop)");
				}
			} else if (!flying && wasFlying) {
				stopMusic(flyMusic);
				flyMusic = null;
				MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Player stopped flying, stopping music");
			}
			wasFlying = flying;

			// —— 重生检测：点确认死亡并重生后立即停止死亡音乐 ——
			if (wasDead) {
				stopMusic(deathMusic);
				deathMusic = null;
				wasFlying = false; // 允许重生后仍处于飞行状态时重新触发
				MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Player respawned, stopping music");
			}
		}

		wasDead = dead;
	}

	/** 空闲检测：任何键盘/鼠标输入都会重置计时，满 10 分钟触发一次 */
	private void tickIdleDetection(Minecraft client) {
		boolean inputActive = false;
		com.mojang.blaze3d.platform.Window window = client.getWindow();
		if (window != null) {
			for (int key = 32; key <= 348; key++) {
				if (InputConstants.isKeyDown(window, key)) {
					inputActive = true;
					break;
				}
			}
		}

		double mouseX = client.mouseHandler.xpos();
		double mouseY = client.mouseHandler.ypos();
		boolean mouseMoved = !Double.isNaN(lastMouseX) && (mouseX != lastMouseX || mouseY != lastMouseY);
		boolean mouseClicked = client.mouseHandler.isLeftPressed()
				|| client.mouseHandler.isRightPressed()
				|| client.mouseHandler.isMiddlePressed();
		lastMouseX = mouseX;
		lastMouseY = mouseY;

		if (inputActive || mouseMoved || mouseClicked) {
			idleTicks = 0;
		} else {
			idleTicks++;
			if (ModConfig.get().enableIdleSound && idleTicks >= IDLE_THRESHOLD_TICKS) {
				idleTicks = 0;
				playOneShot(MusicPlayerMod.IDLE_SOUND.event());
				ModHud.idleMessageTicks = IDLE_MESSAGE_TICKS;
				MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Player idle 10 minutes, playing a_few_moments_later");
			}
		}
	}

	/** 杀猫检测：扫描附近猫的死亡状态，刚死亡则播放音效并开启黑白效果 */
	private void tickCatDetection(Minecraft client) {
		if (--catScanCooldown > 0) {
			return;
		}
		catScanCooldown = 5;
		if (client.level == null) {
			return;
		}

		Set<Integer> seen = new HashSet<>();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof Cat cat) {
				int id = cat.getId();
				seen.add(id);
				boolean dead = cat.isDeadOrDying();
				Boolean previous = catDeadStates.get(id);
				if (dead && (previous == null || !previous)) {
					// 猫刚死亡
					if (ModConfig.get().enableCatSound) {
						catMusic = startMusic(MusicPlayerMod.CAT_SOUND.event(), false);
						ModHud.catEffectActive = true;
						catEffectTicks = 0;
						MusicPlayerMod.LOGGER.info("[MusicPlayerMod] A cat died, playing sad_meow + grayscale");
					}
				}
				catDeadStates.put(id, dead);
			}
		}
		catDeadStates.keySet().retainAll(seen);
	}

	/**
	 * 成就/进度/挑战达成回调（由 ClientAdvancementsMixin 调用）。
	 * 任意新达成的成就都会播放一次《Oh My God》。
	 */
	public static void onAdvancementDone(Identifier id) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.getSoundManager() != null && ModConfig.get().enableAdvancementSound) {
			minecraft.getSoundManager().play(
					SimpleSoundInstance.forUI(MusicPlayerMod.ADVANCEMENT_SOUND.event(), 1.0F, ModConfig.get().volume));
			MusicPlayerMod.LOGGER.info("[MusicPlayerMod] Advancement done: {}", id);
		}
	}

	/** 开始播放一段音乐（可循环），音量取模组设置 */
	private PlayerMusicSoundInstance startMusic(SoundEvent soundEvent, boolean looping) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getSoundManager() == null) {
			return null;
		}
		PlayerMusicSoundInstance instance = new PlayerMusicSoundInstance(soundEvent, SoundSource.MASTER, ModConfig.get().volume, looping);
		minecraft.getSoundManager().play(instance);
		return instance;
	}

	/** 立即停止正在播放的音乐 */
	private void stopMusic(PlayerMusicSoundInstance instance) {
		if (instance != null) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft != null && minecraft.getSoundManager() != null) {
				minecraft.getSoundManager().stop(instance);
			}
		}
	}

	/**
	 * 播放一次音效（不循环，音量取模组设置）。
	 * 注意：26.1 的 {@code SimpleSoundInstance.forUI(SoundEvent, float, float)} 参数顺序为 (sound, pitch, volume)。
	 */
	private void playOneShot(SoundEvent soundEvent) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.getSoundManager() != null) {
			minecraft.getSoundManager().play(
					SimpleSoundInstance.forUI(soundEvent, 1.0F, ModConfig.get().volume));
		}
	}
}
