package com.example.musicplayermod.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * 音乐声音实例。
 * 由外部控制生命周期：start 后播放（可循环），调用 SoundManager#stop 立即停止。
 * 音量在构造时指定（受模组设置的总音量控制）。
 */
public class PlayerMusicSoundInstance extends AbstractTickableSoundInstance {

	public PlayerMusicSoundInstance(SoundEvent soundEvent, SoundSource source, float volume) {
		this(soundEvent, source, volume, true);
	}

	public PlayerMusicSoundInstance(SoundEvent soundEvent, SoundSource source, float volume, boolean looping) {
		super(soundEvent, source, SoundInstance.createUnseededRandom());
		this.volume = volume;
		this.pitch = 1.0F;
		this.looping = looping;
		this.relative = true;     // 相对于玩家，不受距离衰减
		this.attenuation = SoundInstance.Attenuation.NONE;
	}

	@Override
	public void tick() {
		// 无需逐刻更新，由调用方通过 SoundManager#stop 控制结束
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
