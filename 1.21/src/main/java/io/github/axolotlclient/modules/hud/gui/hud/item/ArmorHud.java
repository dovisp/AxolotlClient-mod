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

package io.github.axolotlclient.modules.hud.gui.hud.item;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import io.github.axolotlclient.AxolotlClientConfig.api.options.Option;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.ColorOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.EnumOption;
import io.github.axolotlclient.modules.hud.gui.component.DynamicallyPositionable;
import io.github.axolotlclient.modules.hud.gui.entry.TextHudEntry;
import io.github.axolotlclient.modules.hud.gui.layout.AnchorPoint;
import io.github.axolotlclient.modules.hud.util.DrawPosition;
import io.github.axolotlclient.modules.hud.util.ItemUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * This implementation of Hud modules is based on KronHUD.
 * <a href="https://github.com/DarkKronicle/KronHUD">Github Link.</a>
 *
 * @license GPL-3.0
 */

public class ArmorHud extends TextHudEntry implements DynamicallyPositionable {

	public static final Identifier ID = Identifier.of("kronhud", "armorhud");

	protected final BooleanOption showProtLvl = new BooleanOption("showProtectionLevel", false);
	private final ItemStack[] placeholderStacks = new ItemStack[]{new ItemStack(Items.IRON_BOOTS),
		new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_HELMET),
		new ItemStack(Items.IRON_SWORD)};
	private final BooleanOption showDurabilityNumber = new BooleanOption("show_durability_num", false);
	private final BooleanOption showMaxDurabilityNumber = new BooleanOption("show_max_durability_num", false);
	private final BooleanOption customDurabilityNumColor = new BooleanOption("armorhud.custom_durability_num_color", false);
	private final ColorOption durabilityNumColor = new ColorOption("armorhud.durability_num_color", Colors.WHITE);
	private final EnumOption<MainHandItemPosition> mainHandItemPosition = new EnumOption<>("armorhud.main_hand_item_position", MainHandItemPosition.class, MainHandItemPosition.BOTTOM);

	private final EnumOption<AnchorPoint> anchor = new EnumOption<>("anchorpoint", AnchorPoint.class,
		AnchorPoint.TOP_RIGHT);

	public ArmorHud() {
		super(20, 100, true);
	}

	@Override
	public void renderComponent(GuiGraphics graphics, float delta) {
		int width = 20;
		int height = 100;
		boolean boundsChanged = false;
		boolean showDurability = showDurabilityNumber.get();
		boolean showMaxDurability = showMaxDurabilityNumber.get();
		int labelWidth = showDurability || showMaxDurability ? Stream.concat(Stream.of(mainHandItemPosition.get() == MainHandItemPosition.DISABLED ? null :
				client.player.getInventory().getMainHandStack()), client.player.getInventory().armor.stream())
			.filter(Objects::nonNull)
			.map(stack -> showDurability && showMaxDurability ? (stack.getMaxDamage() - stack.getDamage()) + "/" + stack.getMaxDamage() : String.valueOf((showDurability ? stack.getMaxDamage() - stack.getDamage() : stack.getMaxDamage())))
			.mapToInt(text -> client.textRenderer.getWidth(text) + 2).max().orElse(0) : 0;
		width += labelWidth;
		if (width != getWidth()) {
			setWidth(width);
			boundsChanged = true;
		}
		DrawPosition pos = getPos();
		MainHandItemPosition mainHandItemTop = mainHandItemPosition.get();
		if (mainHandItemTop == MainHandItemPosition.DISABLED) {
			height -= 20;
		}
		if (height != getHeight()) {
			setHeight(height);
			boundsChanged = true;
		}
		if (boundsChanged) {
			onBoundsUpdate();
		}
		int lastY = 2 + (height-20);
		if (mainHandItemTop == MainHandItemPosition.BOTTOM) {
			renderMainItem(graphics, client.player.getInventory().getMainHandStack(), pos.x() + 2, pos.y() + lastY, labelWidth);
			lastY = lastY - 20;
		}
		for (int i = 0; i <= 3; i++) {
			ItemStack stack = client.player.getInventory().getArmorStack(i).copy();
			String label = null;
			if (showProtLvl.get() && stack.hasEnchantments()) {
				ItemEnchantmentsComponent nbtList = stack.getEnchantments();
				if (nbtList != null) {
					var protection = client.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
						.getHolder(Enchantments.PROTECTION);
					if (protection.isPresent()) {
						label = String.valueOf(EnchantmentHelper.getLevel(protection.get(), stack));
					}
				}
			}
			renderItem(graphics, stack, pos.x() + 2, lastY + pos.y(), labelWidth, label);
			lastY = lastY - 20;
		}
		if (mainHandItemTop == MainHandItemPosition.TOP) {
			renderMainItem(graphics, client.player.getInventory().getMainHandStack(), pos.x() + 2, pos.y() + lastY, labelWidth);
		}
	}

	public void renderMainItem(GuiGraphics graphics, ItemStack stack, int x, int y, int offset) {
		renderDurabilityNumber(graphics, stack, x, y);
		x += offset;
		String total = String.valueOf(ItemUtil.getTotal(client, stack));
		if (total.equals("1")) {
			total = null;
		}
		graphics.drawItem(stack, x, y);
		graphics.drawItemInSlot(client.textRenderer, stack, x, y, total);
	}

	public void renderItem(GuiGraphics graphics, ItemStack stack, int x, int y, int offset, String labelOverride) {
		renderDurabilityNumber(graphics, stack, x, y);
		x += offset;
		graphics.drawItem(stack, x, y);
		graphics.drawItemInSlot(client.textRenderer, stack, x, y, labelOverride);
	}

	private void renderDurabilityNumber(GuiGraphics graphics, ItemStack stack, int x, int y) {
		boolean showDurability = showDurabilityNumber.get();
		boolean showMaxDurability = showMaxDurabilityNumber.get();
		if (stack.isEmpty() || !(showMaxDurability || showDurability) || stack.getMaxDamage() == 0) {
			return;
		}
		String text = showDurability && showMaxDurability ? (stack.getMaxDamage() - stack.getDamage()) + "/" + stack.getMaxDamage() : String.valueOf((showDurability ? stack.getMaxDamage() - stack.getDamage() : stack.getMaxDamage()));
		int textY = y + 10 - client.textRenderer.fontHeight / 2;
		graphics.drawShadowedText(client.textRenderer, text, x, textY, customDurabilityNumColor.get() ? durabilityNumColor.get().toInt() : stack.getItemBarColor());
	}

	@Override
	public void renderPlaceholderComponent(GuiGraphics graphics, float delta) {
		int width = 20;
		int height = 100;
		boolean boundsChanged = false;
		boolean showDurability = showDurabilityNumber.get();
		boolean showMaxDurability = showMaxDurabilityNumber.get();
		int labelWidth = showDurability || showMaxDurability ? Arrays.stream(placeholderStacks)
			.map(stack -> showDurability && showMaxDurability ? (stack.getMaxDamage() - stack.getDamage()) + "/" + stack.getMaxDamage() : String.valueOf((showDurability ? stack.getMaxDamage() - stack.getDamage() : stack.getMaxDamage())))
			.mapToInt(text -> client.textRenderer.getWidth(text) + 2).max().orElse(0) : 0;
		width += labelWidth;
		if (width != getWidth()) {
			setWidth(width);
			boundsChanged = true;
		}
		DrawPosition pos = getPos();
		MainHandItemPosition mainHandItemTop = mainHandItemPosition.get();
		if (mainHandItemTop == MainHandItemPosition.DISABLED) {
			height -= 20;
		}
		if (height != getHeight()) {
			setHeight(height);
			boundsChanged = true;
		}
		if (boundsChanged) {
			onBoundsUpdate();
		}
		int lastY = 2 + (height-20);
		if (mainHandItemTop == MainHandItemPosition.BOTTOM) {
			renderItem(graphics, placeholderStacks[4], pos.x() + 2, pos.y() + lastY, labelWidth, null);
			lastY = lastY - 20;
		}
		for (int i = 0; i <= 3; i++) {
			ItemStack item = placeholderStacks[i];
			renderItem(graphics, item, pos.x() + 2, lastY + pos.y(), labelWidth, null);
			lastY = lastY - 20;
		}
		if (mainHandItemTop == MainHandItemPosition.TOP) {
			renderItem(graphics, placeholderStacks[4], pos.x() + 2, pos.y() + lastY, labelWidth, null);
		}
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public List<Option<?>> getConfigurationOptions() {
		List<Option<?>> options = super.getConfigurationOptions();
		options.add(showProtLvl);
		options.add(showDurabilityNumber);
		options.add(showMaxDurabilityNumber);
		options.add(customDurabilityNumColor);
		options.add(durabilityNumColor);
		options.add(anchor);
		options.add(mainHandItemPosition);
		return options;
	}

	public AnchorPoint getAnchor() {
		return anchor.get();
	}

	private enum MainHandItemPosition {
		BOTTOM,
		TOP,
		DISABLED,
		;

		@Override
		public String toString() {
			return "armorhud.main_hand_item_position."+super.toString().toLowerCase(Locale.ROOT);
		}
	}
}
