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

package io.github.axolotlclient.modules.hypixel.bedwars.upgrades;


import java.util.regex.Pattern;

import io.github.axolotlclient.AxolotlClientConfig.api.util.Color;
import io.github.axolotlclient.util.ClientColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * @author DarkKronicle
 */

public class BedwarsTeamUpgrades {

	public final TrapUpgrade trap = new TrapUpgrade();

	public final TeamUpgrade sharpness =
		new BinaryUpgrade("sharp", Pattern.compile("^\\b[A-Za-z0-9_§]{3,16}\\b purchased Sharpened Swords"), 8, 4,
			(graphics, x, y, width, height, upgradeLevel) -> {
				if (upgradeLevel == 0) {
					graphics.renderItem(new ItemStack(Items.STONE_SWORD), x, y);
				} else {
					graphics.renderItem(new ItemStack(Items.DIAMOND_SWORD), x, y);
				}
			}
		);

	public final TeamUpgrade healPool =
		new BinaryUpgrade("healpool", Pattern.compile("^\\b[A-Za-z0-9_§]{3,16}\\b purchased Heal Pool\\s*$"), 3, 1,
			(graphics, x, y, width, height, upgradeLevel) -> {
				int color = -1;
				if (upgradeLevel == 0) {
					color = ClientColors.DARK_GRAY.toInt();
				}
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(MobEffects.HEALTH_BOOST), x, y, width, height, color);
			}
		);

	public final TeamUpgrade protection =
		new TieredUpgrade("prot", Pattern.compile("^\\b[A-Za-z0-9_§]{3,16}\\b purchased Reinforced Armor .{1,3}\\s*$"),
			new int[]{5, 10, 20, 30}, new int[]{2, 4, 8, 16},
			(graphics, x, y, width, height, upgradeLevel) -> {
				switch (upgradeLevel) {
					case 1 -> {
						graphics.renderItem(new ItemStack(Items.IRON_CHESTPLATE), x, y);
						graphics.enableScissor(x, y + height / 2, x + width / 2, y + height);
						graphics.renderItem(new ItemStack(Items.DIAMOND_CHESTPLATE), x, y);
						graphics.disableScissor();
					}
					case 2 -> {
						graphics.renderItem(new ItemStack(Items.IRON_CHESTPLATE), x, y);
						graphics.enableScissor(x, y, x + width / 2, y + height);
						graphics.renderItem(new ItemStack(Items.DIAMOND_CHESTPLATE), x, y);
						graphics.disableScissor();
					}
					case 3 -> {
						graphics.renderItem(new ItemStack(Items.DIAMOND_CHESTPLATE), x, y);
						graphics.enableScissor(x + width / 2, y + height / 2, x + width, y + height);
						graphics.renderItem(new ItemStack(Items.IRON_CHESTPLATE), x, y);
						graphics.disableScissor();
					}
					case 4 -> graphics.renderItem(new ItemStack(Items.DIAMOND_CHESTPLATE), x, y);
					default -> graphics.renderItem(new ItemStack(Items.IRON_CHESTPLATE), x, y);
				}
			}
		);

	public final TeamUpgrade maniacMiner =
		new TieredUpgrade("haste", Pattern.compile("^\\b[A-Za-z0-9_§]{3,16}\\b purchased Maniac Miner .{1,3}\\s*$"),
			new int[]{2, 4}, new int[]{4, 6}, (graphics, x, y, width, height, upgradeLevel) -> {
			int color = -1;
			if (upgradeLevel == 1) {
				color = ClientColors.GRAY.toInt();
			} else if (upgradeLevel == 0) {
				color = ClientColors.DARK_GRAY.toInt();
			}
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
				Gui.getMobEffectSprite(MobEffects.HASTE), x, y,
				width, height, color
			);
		}
		);

	public final TeamUpgrade forge = new TieredUpgrade("forge", Pattern.compile(
		"^\\b[A-Za-z0-9_§]{3,16}\\b purchased (?:Iron|Golden|Emerald|Molten) Forge\\s*$"), new int[]{2, 4},
		new int[]{4, 6},
		(graphics, x, y, width, height, upgradeLevel) -> {
			if (upgradeLevel == 0) {
				graphics.blit(RenderPipelines.GUI_TEXTURED,
					ResourceLocation.withDefaultNamespace(
						"textures/block/furnace_front.png"), x,
					y, 0, 0, width, height, width, height
				);
			} else {
				int color = -1;
				if (upgradeLevel == 2) {
					color = Color.parse("#FFFF00").toInt();
				} else if (upgradeLevel == 3) {
					color = Color.parse("#00FF00").toInt();
				} else if (upgradeLevel == 4) {
					color = Color.parse("#FF0000").toInt();
				}
				graphics.blit(RenderPipelines.GUI_TEXTURED,
					ResourceLocation.withDefaultNamespace(
						"textures/block/furnace_front_on.png"),
					x, y, 0, 0, width, height, width, height, color
				);
				graphics.drawString(Minecraft.getInstance().font,
					String.valueOf(upgradeLevel),
					x + width - 4, y + height - 6, -1
				);
			}
		}
	);

	public final TeamUpgrade featherFalling = new TieredUpgrade("feather_falling", Pattern.compile("^\\b[A-Za-z0-9_§]{3,16}\\b purchased Cushioned Boots .{1,2}\\s*$"),
		new int[]{2, 4}, new int[]{1, 2}, (graphics, x, y, width, height, upgradeLevel) -> {
		if (upgradeLevel == 1) {
			graphics.renderItem(new ItemStack(Items.IRON_BOOTS), x, y);
		} else {
			graphics.renderItem(new ItemStack(Items.DIAMOND_BOOTS), x, y);
		}
	});

	public final TeamUpgrade[] upgrades = {trap, sharpness, healPool, protection, maniacMiner, forge, featherFalling};

	public BedwarsTeamUpgrades() {

	}

	public void onMessage(String rawMessage) {
		for (TeamUpgrade upgrade : upgrades) {
			if (upgrade.match(rawMessage)) {
				return;
			}
		}
	}

}
