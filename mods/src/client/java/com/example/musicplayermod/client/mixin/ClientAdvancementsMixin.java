package com.example.musicplayermod.client.mixin;

import com.example.musicplayermod.client.MusicPlayerModClient;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 监听客户端成就进度更新（ClientAdvancements#update 的尾部）。
 * 不受原版成就界面覆盖 listener 的影响，跨世界（新连接新实例）自动重置。
 * 进入世界首次同步只记录不播放，之后新达成的成就（任意成就/进度/挑战）播放一次音效。
 */
@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin {

	@Shadow
	@Final
	private Map<AdvancementHolder, AdvancementProgress> progress;

	/** 首次同步标志：进入世界后的第一次 update 只记录已完成成就，不播放 */
	@Unique
	private boolean musicPlayerModInitialSync = true;

	/** 本连接内已触发过音效的成就 id */
	@Unique
	private final Set<Identifier> musicPlayerModHandled = new HashSet<>();

	@Inject(method = "update", at = @At("TAIL"))
	private void musicPlayerModOnAdvancementsUpdated(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
		if (this.musicPlayerModInitialSync) {
			this.musicPlayerModInitialSync = false;
			for (Map.Entry<AdvancementHolder, AdvancementProgress> entry : this.progress.entrySet()) {
				if (entry.getValue().isDone()) {
					this.musicPlayerModHandled.add(entry.getKey().id());
				}
			}
			return;
		}

		for (Map.Entry<AdvancementHolder, AdvancementProgress> entry : this.progress.entrySet()) {
			if (entry.getValue().isDone() && this.musicPlayerModHandled.add(entry.getKey().id())) {
				MusicPlayerModClient.onAdvancementDone(entry.getKey().id());
			}
		}
	}
}
