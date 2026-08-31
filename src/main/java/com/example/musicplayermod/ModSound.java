package com.example.musicplayermod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * 自定义声音的包装记录，注册后保存 SoundEvent 供客户端播放。
 */
public record ModSound(Identifier id, SoundEvent event) {

	/**
	 * 将 assets/music_player_mod/sounds.json 中定义的声音注册进游戏。
	 *
	 * @param path sounds.json 中的声音 key（同时也是资源文件名，如 chinese_can_fly.mp3）
	 * @return 已注册的 ModSound
	 */
	public static ModSound registerSound(String path) {
		Identifier identifier = MusicPlayerMod.id(path);
		SoundEvent event = Registry.register(
				BuiltInRegistries.SOUND_EVENT,
				identifier,
				SoundEvent.createVariableRangeEvent(identifier));
		return new ModSound(identifier, event);
	}
}
