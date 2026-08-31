package com.example.musicplayermod.client;

import com.example.musicplayermod.ModConfig;
import com.example.musicplayermod.MusicPlayerMod;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 模组 HUD 元素（通过 26.1 的 HudElementRegistry 注册）：
 * <ul>
 *   <li>空闲提示：「这个玩家睡着了」在屏幕中央显示 2 秒（文字跟随设置的语言）</li>
 *   <li>杀猫黑白效果：全屏灰色覆盖层，音效播完自动恢复</li>
 * </ul>
 */
public class ModHud implements HudElement {

	/** 空闲提示剩余显示 tick（>0 时显示，由客户端主类每 tick 递减） */
	public static int idleMessageTicks = 0;

	/** 杀猫黑白效果激活标志（由客户端主类控制） */
	public static boolean catEffectActive = false;

	public static void register() {
		HudElementRegistry.addLast(MusicPlayerMod.id("mod_hud"), new ModHud());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		// 空闲提示：屏幕中央
		if (idleMessageTicks > 0) {
			boolean chinese = "zh".equals(ModConfig.get().uiLanguage);
			String message = chinese ? "这个玩家睡着了" : "This player fell asleep";
			int x = (width - font.width(message)) / 2;
			int y = height / 2 - 10;
			graphics.text(font, message, x, y, 0xFFFFFFFF, true);
		}

		// 杀猫黑白效果：全屏灰色覆盖（近似黑白画面）
		if (catEffectActive) {
			graphics.fill(0, 0, width, height, 0x99000000);
		}
	}
}
