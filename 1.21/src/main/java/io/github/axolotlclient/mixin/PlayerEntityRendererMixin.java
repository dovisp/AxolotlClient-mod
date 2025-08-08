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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.modules.hypixel.nickhider.NickHider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

	@WrapOperation(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V"))
	private void axolotlclient$modifiyName(PlayerEntityRenderer instance, Entity entity, Text text, MatrixStack stack, VertexConsumerProvider vertexConsumerProvider, int i, float v, Operation<Void> original, @Local(argsOnly = true) AbstractClientPlayerEntity player) {
		if (AxolotlClient.CONFIG != null) {
			if (player.getUuid() == MinecraftClient.getInstance().player.getUuid()
				&& NickHider.getInstance().hideOwnName.get()) {
				text = NickHider.getInstance().editComponent(text, player.getName().getString(), NickHider.getInstance().hiddenNameSelf.get());
			} else if (player.getUuid() != MinecraftClient.getInstance().player.getUuid()
				&& NickHider.getInstance().hideOtherNames.get()) {
				text = NickHider.getInstance().editComponent(text, player.getName().getString(), NickHider.getInstance().hiddenNameOthers.get());
			}
		}
		original.call(instance, entity, text, stack, vertexConsumerProvider, i, v);
	}
}
