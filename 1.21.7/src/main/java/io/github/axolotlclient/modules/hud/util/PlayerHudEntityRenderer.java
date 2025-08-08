/*
 * Copyright © 2025 moehreag <moehreag@gmail.com> & Contributors
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

package io.github.axolotlclient.modules.hud.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PlayerHudEntityRenderer extends PictureInPictureRenderer<PlayerHudEntityRenderState> {
	private final EntityRenderDispatcher entityRenderDispatcher;

	public PlayerHudEntityRenderer(MultiBufferSource.BufferSource bufferSource, EntityRenderDispatcher entityRenderDispatcher) {
		super(bufferSource);
		this.entityRenderDispatcher = entityRenderDispatcher;
	}

	@Override
	public Class<PlayerHudEntityRenderState> getRenderStateClass() {
		return PlayerHudEntityRenderState.class;
	}

	@Override
	protected String getTextureLabel() {
		return "axolotlclient:playerhud";
	}

	protected void renderToTexture(PlayerHudEntityRenderState guiEntityRenderState, PoseStack poseStack) {
		Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
		Vector3f vector3f = guiEntityRenderState.translation();
		poseStack.translate(vector3f.x, vector3f.y, vector3f.z);
		poseStack.mulPose(guiEntityRenderState.rotation());
		Quaternionf quaternionf = guiEntityRenderState.overrideCameraAngle();
		if (quaternionf != null) {
			this.entityRenderDispatcher.overrideCameraOrientation(quaternionf.conjugate(new Quaternionf()).rotateY((float) Math.PI));
		}

		this.entityRenderDispatcher.setRenderShadow(false);
		this.entityRenderDispatcher.render(guiEntityRenderState.renderState(), 0.0, 0.0, 0.0, poseStack, this.bufferSource, 15728880);
		this.entityRenderDispatcher.setRenderShadow(true);
	}

	@Override
	protected float getTranslateY(int i, int j) {
		return i / 2.0F;
	}
}
