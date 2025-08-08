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

package io.github.axolotlclient.modules.hypixel.autoboop;

import java.util.List;

import io.github.axolotlclient.AxolotlClientCommon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.CommonTexts;
import net.minecraft.text.Text;

public class FilterListConfigurationScreen extends Screen {
	final List<String> filters;
	private FiltersList filtersList;
	private final Screen parent;

	public FilterListConfigurationScreen(List<String> filters, Screen parent) {
		super(Text.translatable("autoboop.filters.configure"));
		this.filters = filters;
		this.parent = parent;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredShadowedText(textRenderer, getTitle(), width / 2, 33 / 2 - textRenderer.fontHeight / 2, -1);
	}

	@Override
	protected void init() {
		this.filtersList = addDrawableChild(new FiltersList(this));
		ButtonWidget resetButton = ButtonWidget.builder(Text.translatable("autoboop.filters.clear"), button -> {
			filters.clear();
			filtersList.reload();
			AxolotlClientCommon.getInstance().saveConfig();
		}).positionAndSize(width / 2 - 150 - 4, height - 33 / 2 - 10, 150, 20).build();
		addDrawableChild(resetButton);
		addDrawableChild(ButtonWidget.builder(CommonTexts.DONE, button -> this.closeScreen())
			.positionAndSize(width / 2 + 4, height - 33 / 2 - 10, 150, 20).build());
	}

	@Override
	public void closeScreen() {
		this.client.setScreen(this.parent);
		filtersList.apply();
		AxolotlClientCommon.getInstance().saveConfig();
	}
}
