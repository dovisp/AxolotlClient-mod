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

package io.github.axolotlclient.modules.hypixel.nickhider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.StringOption;
import io.github.axolotlclient.api.util.BiContainer;
import io.github.axolotlclient.modules.hypixel.AbstractHypixelMod;
import io.github.axolotlclient.util.events.Events;
import io.github.axolotlclient.util.events.impl.ReceiveChatMessageEvent;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class NickHider implements AbstractHypixelMod {

	@Getter
	private final static NickHider Instance = new NickHider();
	public final StringOption hiddenNameSelf = new StringOption("hiddenNameSelf", "You");
	public final StringOption hiddenNameOthers = new StringOption("hiddenNameOthers", "Player");
	public final BooleanOption hideOwnName = new BooleanOption("hideOwnName", false);
	public final BooleanOption hideOtherNames = new BooleanOption("hideOtherNames", false);
	public final BooleanOption hideOwnSkin = new BooleanOption("hideOwnSkin", false);
	public final BooleanOption hideOtherSkins = new BooleanOption("hideOtherSkins", false);
	private final OptionCategory category = OptionCategory.create("nickhider");

	@Override
	public void init() {
		category.add(hiddenNameSelf);
		category.add(hiddenNameOthers);
		category.add(hideOwnName);
		category.add(hideOtherNames);
		category.add(hideOwnSkin);
		category.add(hideOtherSkins);
		Events.RECEIVE_CHAT_MESSAGE_EVENT.register(this::editMessage);
	}

	@Override
	public OptionCategory getCategory() {
		return category;
	}

	private void editMessage(ReceiveChatMessageEvent event) {
		if (hideOwnName.get() || hideOtherNames.get()) {
			String msg = event.getOriginalMessage();
			Component message = event.getFormattedMessage();

			List<BiContainer<String, String>> replacements = new ArrayList<>();
			if (Minecraft.getInstance().player != null) {
				String playerName = Minecraft.getInstance().player.getName().getString();
				if (hideOwnName.get() && msg.contains(playerName)) {
					replacements.add(BiContainer.of(playerName, hiddenNameSelf.get()));
				}
			}

			if (hideOtherNames.get() && Minecraft.getInstance().level != null) {
				for (AbstractClientPlayer player : Minecraft.getInstance().level.players()) {
					if (player == Minecraft.getInstance().player) {
						continue;
					}
					if (msg.contains(player.getName().getString())) {
						replacements.add(BiContainer.of(player.getName().getString(), hiddenNameOthers.get()));
					}
				}
			}
			if (!replacements.isEmpty()) {
				MutableComponent editedMessage = Component.empty();
				editComponent(message, replacements, editedMessage);
				event.setNewMessage(editedMessage);
			}
		}
	}

	public Component editComponent(Component c, String find, String replace) {
		MutableComponent edited = Component.empty();
		c.visit((style, string) -> {
			edited.append(Component.literal(string.replace(find, replace)).withStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return edited;
	}

	private void editComponent(Component component, List<BiContainer<String, String>> replacements, MutableComponent edited) {
		component.visit((style, string) -> {
			String edit = string;
			for (var entry : replacements) {
				edit = edit.replace(entry.getLeft(), entry.getRight());
			}
			edited.append(Component.literal(edit).setStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
	}
}
