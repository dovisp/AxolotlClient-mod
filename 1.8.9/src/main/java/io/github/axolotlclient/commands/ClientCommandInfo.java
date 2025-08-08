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

package io.github.axolotlclient.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

@Getter
@AllArgsConstructor
public final class ClientCommandInfo {
	private final CommandSource origin;
	private final Minecraft minecraft;

	@With
	private final ClientWorld world;

	@With
	private final Entity entity;

	@With
	private final Vec3d pos;

	@With
	private final float yaw;

	@With
	private final float pitch;

	public void sendMessageAsync(Iterable<Text> more) {
		if (!minecraft.isOnSameThread()) {
			minecraft.submit(() -> more.forEach(this::sendMessage));
		} else {
			more.forEach(this::sendMessage);
		}
	}

	public void sendMessage(String... messages) {
		for (String message : messages) {
			sendMessage(new LiteralText(message));
		}
	}

	public void sendMessage(Text... messages) {
		for (Text message : messages) {
			origin.sendMessage(message);
		}
	}
}
