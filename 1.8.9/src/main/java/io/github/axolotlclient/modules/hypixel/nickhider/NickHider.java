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

import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.StringOption;
import io.github.axolotlclient.api.util.BiContainer;
import io.github.axolotlclient.modules.hypixel.AbstractHypixelMod;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class NickHider implements AbstractHypixelMod {

	@Getter
	private static final NickHider Instance = new NickHider();
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
	}

	@Override
	public OptionCategory getCategory() {
		return category;
	}

	public Text editMessage(Text message) {
		if (hideOwnName.get() || hideOtherNames.get()) {
			String msg = message.getString();

			List<BiContainer<String, String>> replacements = new ArrayList<>();
			if (Minecraft.getInstance().player != null) {
				String playerName = Minecraft.getInstance().player.getName();
				if (hideOwnName.get() && msg.contains(playerName)) {
					replacements.add(BiContainer.of(playerName, hiddenNameSelf.get()));
				}
			}

			if (hideOtherNames.get() && Minecraft.getInstance().world != null) {
				for (PlayerEntity player : Minecraft.getInstance().world.players) {
					if (player == Minecraft.getInstance().player) {
						continue;
					}
					if (msg.contains(player.getName())) {
						replacements.add(BiContainer.of(player.getName(), hiddenNameOthers.get()));
					}
				}
			}

			if (!replacements.isEmpty()) {
				BaseText editedMessage = new LiteralText("");
				editComponent(message, replacements, editedMessage);
				return editedMessage;
			}
		}
		return message;
	}

	public Text editComponent(Text c, String find, String replace) {
		BaseText edited = new LiteralText("");
		c.iterator().forEachRemaining(text -> {
			edited.append(new LiteralText(text.getContent().replace(find, replace)).setStyle(text.getStyle()));
		});
		return edited;
	}

	private void editComponent(Text component, List<BiContainer<String, String>> replacements, BaseText edited) {
		component.iterator().forEachRemaining(text -> {
			String edit = text.getContent();
			for (var entry : replacements) {
				edit = edit.replace(entry.getLeft(), entry.getRight());
			}
			edited.append(new LiteralText(edit).setStyle(text.getStyle()));
		});
	}
}
