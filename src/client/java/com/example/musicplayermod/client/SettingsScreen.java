package com.example.musicplayermod.client;

import com.example.musicplayermod.ModConfig;
import com.example.musicplayermod.MusicPlayerMod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 模组设置界面（默认按 K 键打开），支持中文/英文切换：
 * <ul>
 *   <li>音量滑条（0% ~ 100%，作用于所有音效）</li>
 *   <li>语言选择（中文 / English）</li>
 *   <li>各音效独立开关（飞行 / 死亡 / W+S / C 键 / 空闲 / 成就 / 杀猫 / 吃）</li>
 *   <li>模组版本号显示、按键说明、作者主页链接</li>
 * </ul>
 * 点「保存并关闭」保存配置并关闭。
 */
public class SettingsScreen extends Screen {

	private VolumeSlider volumeSlider;

	/** 当前是否为中文界面 */
	private final boolean chinese;

	public SettingsScreen() {
		super(Component.literal(""));
		this.chinese = "zh".equals(ModConfig.get().uiLanguage);
	}

	/** 根据当前语言返回文本 */
	private String t(String zh, String en) {
		return this.chinese ? zh : en;
	}

	private Component tc(String zh, String en) {
		return Component.literal(t(zh, en));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;

		// 音量滑条
		this.volumeSlider = new VolumeSlider(cx - 100, 60, 200, 20, ModConfig.get().volume);
		this.addRenderableWidget(this.volumeSlider);

		// 语言选择按钮：点击切换并重建界面
		this.addRenderableWidget(Button.builder(tc("语言: 中文", "Language: English"), button -> {
			ModConfig.get().uiLanguage = this.chinese ? "en" : "zh";
			ModConfig.save();
			if (this.minecraft != null) {
				this.minecraft.setScreen(new SettingsScreen());
			}
		}).bounds(cx - 100, 90, 200, 20).build());

		// 各音效开关（两列）
		int colX1 = cx - 165;
		int colX2 = cx + 10;
		int y = 118;
		y = addToggle(colX1, y, t("飞行", "Fly"), () -> ModConfig.get().enableFlySound, v -> ModConfig.get().enableFlySound = v);
		y = addToggle(colX2, y - 22, t("挂机", "Idle"), () -> ModConfig.get().enableIdleSound, v -> ModConfig.get().enableIdleSound = v);
		y = addToggle(colX1, y, t("死亡", "Death"), () -> ModConfig.get().enableDeathSound, v -> ModConfig.get().enableDeathSound = v);
		y = addToggle(colX2, y - 22, t("成就", "Advance"), () -> ModConfig.get().enableAdvancementSound, v -> ModConfig.get().enableAdvancementSound = v);
		y = addToggle(colX1, y, t("W+S", "W+S"), () -> ModConfig.get().enableWsSound, v -> ModConfig.get().enableWsSound = v);
		y = addToggle(colX2, y - 22, t("杀猫", "Cat"), () -> ModConfig.get().enableCatSound, v -> ModConfig.get().enableCatSound = v);
		y = addToggle(colX1, y, t("C 键", "C Key"), () -> ModConfig.get().enableVineBoomSound, v -> ModConfig.get().enableVineBoomSound = v);
		y = addToggle(colX2, y - 22, t("吃", "Eat"), () -> ModConfig.get().enableEatSound, v -> ModConfig.get().enableEatSound = v);

		// 保存并关闭
		this.addRenderableWidget(Button.builder(tc("保存并关闭", "Save & Close"), button -> {
			ModConfig.get().volume = this.volumeSlider.getVolume();
			ModConfig.save();
			this.onClose();
		}).bounds(cx - 100, y + 6, 200, 20).build());

		// 重置所有设置
		this.addRenderableWidget(Button.builder(tc("重置全部", "Reset All"), button -> {
			ModConfig.get().volume = 1.0F;
			ModConfig.get().enableFlySound = true;
			ModConfig.get().enableDeathSound = true;
			ModConfig.get().enableWsSound = true;
			ModConfig.get().enableVineBoomSound = true;
			ModConfig.get().enableIdleSound = true;
			ModConfig.get().enableAdvancementSound = true;
			ModConfig.get().enableCatSound = true;
			ModConfig.get().enableEatSound = true;
			ModConfig.save();
			if (this.minecraft != null) {
				this.minecraft.setScreen(new SettingsScreen());
			}
		}).bounds(cx - 100, y + 30, 200, 20).build());

		// 作者主页链接（打开浏览器）
		this.addRenderableWidget(Button.builder(tc("作者主页", "Author Home"), button -> {
			Util.getPlatform().openUri("https://space.bilibili.com/1396925881?spm_id_from=333.337.0.0");
		}).bounds(cx - 100, y + 54, 200, 20).build());

		this.infoY = y + 80;
	}

	/** 版本号/按键说明文本的 Y 坐标（由 init 计算） */
	private int infoY = 250;

	/** 创建一个开关按钮，点击切换配置值并更新按钮文字 */
	private int addToggle(int x, int y, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
		Button button = Button.builder(Component.literal(label + ": " + (getter.getAsBoolean() ? (this.chinese ? "开" : "ON") : (this.chinese ? "关" : "OFF"))), b -> {
			boolean newValue = !getter.getAsBoolean();
			setter.accept(newValue);
			b.setMessage(Component.literal(label + ": " + (newValue ? (this.chinese ? "开" : "ON") : (this.chinese ? "关" : "OFF"))));
		}).bounds(x, y, 155, 20).build();
		this.addRenderableWidget(button);
		return y + 22;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int cx = this.width / 2;

		graphics.text(this.font, t("音量: ", "Volume: ") + Math.round(this.volumeSlider.getVolume() * 100) + "%", cx - 100, 40, 0xFFFFFFFF, true);
		graphics.text(this.font, t("模组版本: ", "Mod Version: ") + MusicPlayerMod.getVersion(), cx - 100, this.infoY, 0xFFFFFFFF, true);
		graphics.text(this.font, t("C: Vine Boom | K: 设置 | W+S: Du Bist Gut Genug", "C: Vine Boom | K: Settings | W+S: Du Bist Gut Genug"), cx - 100, this.infoY + 20, 0xFFFFFFFF, true);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(null);
		}
	}

	/** 音量滑条：0.0 ~ 1.0 */
	private static class VolumeSlider extends AbstractSliderButton {

		VolumeSlider(int x, int y, int width, int height, double value) {
			super(x, y, width, height, Component.empty(), clamp(value));
		}

		float getVolume() {
			return (float) this.value;
		}

		void setVolumeValue(double value) {
			this.value = clamp(value);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal(Math.round(this.value * 100) + "%"));
		}

		@Override
		protected void applyValue() {
			// 值在保存时才写入配置
		}

		private static double clamp(double value) {
			return Math.max(0.0D, Math.min(1.0D, value));
		}
	}
}
