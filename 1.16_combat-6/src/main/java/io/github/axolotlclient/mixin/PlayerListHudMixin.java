/*
 * Copyright © 2024 moehreag <moehreag@gmail.com> & Contributors
 *
 * This file is part of AxolotlClient.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, see the LICENSE file.
 */

package io.github.axolotlclient.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.api.requests.UserRequest;
import io.github.axolotlclient.api.util.UUIDHelper;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsMod;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayer;
import io.github.axolotlclient.modules.hypixel.levelhead.LevelHead;
import io.github.axolotlclient.modules.hypixel.levelhead.LevelHeadMode;
import io.github.axolotlclient.modules.hypixel.nickhider.NickHider;
import io.github.axolotlclient.modules.tablist.Tablist;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

	@Shadow
	private Text header;
	@Shadow
	private Text footer;

	@Unique
	private GameProfile axolotlclient$profile;
	@Shadow
	@Final
	private MinecraftClient client;

	@WrapMethod(method = "getPlayerName")
	private Text nickHider(PlayerListEntry entry, Operation<Text> original) {
		var orig = original.call(entry);
		if (client.player == null) {
			return orig;
		}
		if (entry.getProfile().equals(client.player.getGameProfile()) && NickHider.getInstance().hideOwnName.get()) {
			return NickHider.getInstance().editComponent(orig, entry.getProfile().getName(), NickHider.getInstance().hiddenNameSelf.get());
		} else if (!entry.getProfile().equals(client.player.getGameProfile()) &&
			NickHider.getInstance().hideOtherNames.get()) {
			return NickHider.getInstance().editComponent(orig, entry.getProfile().getName(), NickHider.getInstance().hiddenNameOthers.get());
		}
		return orig;
	}

	@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;"))
	public PlayerListEntry axolotlclient$getPlayer(PlayerListEntry playerEntry) {
		axolotlclient$profile = playerEntry.getProfile();
		return playerEntry;
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Lnet/minecraft/text/StringVisitable;)I"))
	public int axolotlclient$moveName(TextRenderer instance, StringVisitable text) {
		if (axolotlclient$profile != null && AxolotlClient.CONFIG.showBadges.get() && UserRequest.getOnline(axolotlclient$profile.getId().toString()))
			return instance.getWidth(text) + 10;
		return instance.getWidth(text);
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;FFI)I"))
	public int axolotlclient$moveName2(TextRenderer instance, MatrixStack matrices, Text text, float x, float y, int color) {
		if (axolotlclient$profile != null && AxolotlClient.CONFIG.showBadges.get() && UserRequest.getOnline(axolotlclient$profile.getId().toString())) {
			MinecraftClient.getInstance().getTextureManager().bindTexture(AxolotlClient.badgeIcon);
			RenderSystem.color4f(1, 1, 1, 1);

			DrawableHelper.drawTexture(matrices, (int) x, (int) y, 8, 8, 0, 0, 8, 8, 8, 8);

			x += 9;
		}
		axolotlclient$profile = null;
		return instance.drawWithShadow(matrices, text, x, y, color);
	}

	@Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
	private void axolotlclient$numericalPing(MatrixStack matrices, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
		if (BedwarsMod.getInstance().isEnabled() && BedwarsMod.getInstance().blockLatencyIcon() && (BedwarsMod.getInstance().isWaiting() || BedwarsMod.getInstance().inGame())) {
			ci.cancel();
		} else if (Tablist.getInstance().renderNumericPing(matrices, width, x, y, entry)) {
			ci.cancel();
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isInSingleplayer()Z"))
	private boolean showPlayerHeads$1(MinecraftClient instance) {
		if (Tablist.getInstance().showPlayerHeads.get()) {
			return instance.isInSingleplayer();
		}
		return false;
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isEncrypted()Z"))
	private boolean axolotlclient$showPlayerHeads$1(ClientConnection instance) {
		if (Tablist.getInstance().showPlayerHeads.get()) {
			return instance.isEncrypted();
		}
		return false;
	}

	@Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;header:Lnet/minecraft/text/Text;"))
	private void axolotlclient$setRenderHeaderFooter(MatrixStack matrices, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
		if (!Tablist.getInstance().showHeader.get()) {
			header = null;
		}
		if (!Tablist.getInstance().showFooter.get()) {
			footer = null;
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isPartVisible(Lnet/minecraft/client/render/entity/PlayerModelPart;)Z", ordinal = 1))
	private boolean axolotlclient$alwaysShowHeadLayer(PlayerEntity instance, PlayerModelPart modelPart) {
		return Tablist.getInstance().alwaysShowHeadLayer.get() || instance.isPartVisible(modelPart);
	}

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/PlayerListHud;renderLatencyIcon(Lnet/minecraft/client/util/math/MatrixStack;IIILnet/minecraft/client/network/PlayerListEntry;)V"
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void axolotlclient$renderWithoutObjective(
		MatrixStack matrixStack, int argY, Scoreboard scoreboard, ScoreboardObjective scoreboardObjective, CallbackInfo ci,
		ClientPlayNetworkHandler clientPlayNetworkHandler, List list, int i, int j, int l, int m, int k,
		boolean bl, int n, int o, int p, int q, int r, List list2, int t, int u, int s, int v, int y, int z, PlayerListEntry playerListEntry2
	) {
		if (!BedwarsMod.getInstance().isEnabled() || !BedwarsMod.getInstance().isWaiting()) {
			return;
		}
		int startX = v + i + 1;
		int endX = startX + n;
		String render;
		try {
			if (playerListEntry2.getProfile().getName().contains(Formatting.OBFUSCATED.toString())) {
				return;
			}

			String uuid = UUIDHelper.toUndashed(playerListEntry2.getProfile().getId());
			render = LevelHead.getDisplayString(LevelHeadMode.BEDWARS, uuid);
		} catch (Exception e) {
			return;
		}
		this.client.textRenderer.drawWithShadow(matrixStack,
			render,
			(float) (endX - this.client.textRenderer.getWidth(render)) + 20,
			(float) y,
			-1
		);
	}

	@Inject(
		method = "renderScoreboardObjective",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Ljava/lang/String;FFI)I",
			ordinal = 1
		),
		cancellable = true
	)
	private void axolotlclient$renderCustomScoreboardObjective(
		ScoreboardObjective objective, int y, String player, int startX, int endX,
		PlayerListEntry playerEntry, MatrixStack matrices, CallbackInfo ci
	) {
		if (!BedwarsMod.getInstance().isEnabled()) {
			return;
		}

		BedwarsGame game = BedwarsMod.getInstance().getGame().orElse(null);
		if (game == null) {
			return;
		}

		game.renderCustomScoreboardObjective(matrices, playerEntry.getProfile().getName(), objective, y, endX);

		ci.cancel();
	}

	@ModifyVariable(
		method = "render",
		at = @At(
			value = "STORE"
		),
		ordinal = 7
	)
	public int axolotlclient$changeWidth(int value) {
		if (BedwarsMod.getInstance().isEnabled() && BedwarsMod.getInstance().blockLatencyIcon() && (BedwarsMod.getInstance().isWaiting() || BedwarsMod.getInstance().inGame())) {
			value -= 9;
		}
		if (BedwarsMod.getInstance().isEnabled() && BedwarsMod.getInstance().isWaiting()) {
			value += 20;
		}
		return value;
	}

	@Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
	public void axolotlclient$getPlayerName(PlayerListEntry playerEntry, CallbackInfoReturnable<Text> cir) {
		if (!BedwarsMod.getInstance().isEnabled()) {
			return;
		}
		BedwarsGame game = BedwarsMod.getInstance().getGame().orElse(null);
		if (game == null || !game.isStarted()) {
			return;
		}
		BedwarsPlayer player = game.getPlayer(playerEntry.getProfile().getName()).orElse(null);
		if (player == null) {
			return;
		}
		cir.setReturnValue(Text.of(player.getTabListDisplay()));
	}

	@ModifyVariable(method = "render", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/Ordering;sortedCopy(Ljava/lang/Iterable;)Ljava/util/List;", remap = false))
	public List<PlayerListEntry> axolotlclient$overrideSortedPlayers(List<PlayerListEntry> original) {
		if (!BedwarsMod.getInstance().inGame()) {
			return original;
		}
		List<PlayerListEntry> players = BedwarsMod.getInstance().getGame().get().getTabPlayerList(original);
		if (players == null) {
			return original;
		}
		return players;
	}

	@Inject(method = "setHeader", at = @At("HEAD"), cancellable = true)
	public void axolotlclient$changeHeader(Text header, CallbackInfo ci) {
		if (!BedwarsMod.getInstance().inGame()) {
			return;
		}
		this.header = BedwarsMod.getInstance().getGame().get().getTopBarText();
		ci.cancel();
	}

	@Inject(method = "setFooter", at = @At("HEAD"), cancellable = true)
	public void axolotlclient$changeFooter(Text footer, CallbackInfo ci) {
		if (!BedwarsMod.getInstance().inGame()) {
			return;
		}
		this.footer = BedwarsMod.getInstance().getGame().get().getBottomBarText();
		ci.cancel();
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/GameOptions;getTextBackgroundColor(I)I")))
	private void modifyBackground(MatrixStack stack, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
		var tablist = Tablist.getInstance();
		if (!tablist.backgroundEnabled.get()) {
			return;
		}
		if (tablist.customBackgroundColor.get()) {
			original.call(stack, x1, y1, x2, y2, tablist.backgroundColor.get().toInt());
			return;
		}
		original.call(stack, x1, y1, x2, y2, color);
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;renderLatencyIcon(Lnet/minecraft/client/util/math/MatrixStack;IIILnet/minecraft/client/network/PlayerListEntry;)V")))
	private void modifyBackground$2(MatrixStack stack, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
		modifyBackground(stack, x1, y1, x2, y2, color, original);
	}
}
