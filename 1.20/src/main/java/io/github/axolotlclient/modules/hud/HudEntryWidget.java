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

package io.github.axolotlclient.modules.hud;

import com.mojang.blaze3d.platform.InputUtil;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.ConfigStyles;
import io.github.axolotlclient.modules.hud.gui.component.HudEntry;
import io.github.axolotlclient.modules.hud.util.DrawUtil;
import io.github.axolotlclient.util.ClientColors;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.GuiNavigationEvent;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenArea;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class HudEntryWidget implements Drawable, Element, Selectable {

	private final HudEntry entry;
	private boolean focused, moving;

	@Override
	public void setFocused(boolean focused) {
		this.focused = focused;
		if (!focused) {
			moving = false;
		}
	}

	@Override
	public boolean isFocused() {
		return entry.isEnabled() && focused;
	}

	@Override
	public SelectionType getType() {
		return isFocused() ? SelectionType.FOCUSED : (entry.isHovered() ? SelectionType.HOVERED : SelectionType.NONE);
	}

	@Override
	public void appendNarrations(NarrationMessageBuilder narrationElementOutput) {
		narrationElementOutput.put(NarrationPart.TITLE, Text.translatable("hud.entry.name", entry.getName()));
		if (moving) {
			narrationElementOutput.put(NarrationPart.USAGE, Text.translatable("hud.entry.usage.move"));
		} else {
			narrationElementOutput.put(NarrationPart.USAGE, Text.translatable("hud.entry.usage"));
		}
	}

	@Override
	public @NotNull ScreenArea getArea() {
		if (!entry.isEnabled()) {
			return ScreenArea.empty();
		}
		return new ScreenArea(entry.getTrueX(), entry.getTrueY(), entry.getTrueWidth(), entry.getTrueHeight());
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return entry.isEnabled() && entry.isHovered();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		if (isFocused()) {
			var bounds = entry.getTrueBounds();
			DrawUtil.outlineRect(guiGraphics, bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, moving ? ClientColors.SELECTOR_RED.toInt() : -1);
		}
	}

	@Nullable
	@Override
	public ElementPath nextFocusPath(GuiNavigationEvent event) {
		if (!entry.isEnabled()) {
			return null;
		} else {
			return !this.isFocused() ? ElementPath.createLeaf(this) : null;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		var mc = MinecraftClient.getInstance();
		boolean consume = false;
		if (moving) {
			consume = true;
			int step = Screen.hasControlDown() ? 10 : 1;
			if (keyCode == InputUtil.KEY_ESCAPE_CODE || keyCode == InputUtil.KEY_SPACE_CODE) {
				moving = false;
			} else if (keyCode == InputUtil.KEY_UP_CODE) {
				entry.setY(entry.getRawTrueY() - step + entry.offsetTrueHeight());
			} else if (keyCode == InputUtil.KEY_DOWN_CODE) {
				entry.setY(entry.getRawTrueY() + step + entry.offsetTrueHeight());
			} else if (keyCode == InputUtil.KEY_LEFT_CODE) {
				entry.setX(entry.getRawTrueX() - step + entry.offsetTrueWidth());
			} else if (keyCode == InputUtil.KEY_RIGHT_CODE) {
				entry.setX(entry.getRawTrueX() + step + entry.offsetTrueWidth());
			} else {
				consume = false;
			}
		}
		if (consume) {
			return true;
		}
		consume = true;
		if (keyCode == InputUtil.KEY_SPACE_CODE) {
			moving = true;
		} else if (keyCode == InputUtil.KEY_ENTER_CODE || keyCode == InputUtil.KEY_NUMPAD_ENTER_CODE) {
			mc.setScreen(ConfigStyles.createScreen(mc.currentScreen, entry.getCategory()));
		} else {
			consume = false;
		}
		return consume;
	}
}
