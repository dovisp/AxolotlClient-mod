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

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Environment(EnvType.CLIENT)
public class FiltersList extends ElementListWidget<FiltersList.Entry> {
	final FilterListConfigurationScreen screen;

	public FiltersList(FilterListConfigurationScreen screen) {
		super(MinecraftClient.getInstance(), screen.width, screen.height, 33, screen.height - 33, 24);
		this.screen = screen;

		reload();
	}

	public void reload() {
		clearEntries();
		for (String entry : screen.filters) {
			this.addEntry(new FilterEntry(entry));
		}

		addEntry(new SpacerEntry());
		addEntry(new NewEntry());
	}

	@Override
	public int getRowWidth() {
		return 340;
	}

	public void apply() {
		screen.filters.clear();
		screen.filters.addAll(children().stream().filter(e -> e instanceof FilterEntry)
			.map(e -> (FilterEntry) e)
			.map(e -> e.editBox.getText()).toList());
	}

	@Environment(EnvType.CLIENT)
	public abstract static class Entry extends ElementListWidget.Entry<Entry> {

	}

	public static class SpacerEntry extends Entry {
		@Override
		public void render(MatrixStack guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {

		}

		@Override
		public List<? extends Element> children() {
			return List.of();
		}
	}

	@Environment(EnvType.CLIENT)
	public class FilterEntry extends Entry {
		private static final Text REMOVE_BUTTON_TITLE = new TranslatableText("autoboop.filters.remove");
		private final TextFieldWidget editBox;
		private final ButtonWidget removeButton;

		FilterEntry(String filter) {
			this.editBox = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, new TranslatableText("autoboop.filters.edit"));
			editBox.setText(filter);
			editBox.setMaxLength(16);
			this.removeButton = new ButtonWidget(0, 0, 50, 20, REMOVE_BUTTON_TITLE, b -> {
				removeEntry(this);
				apply();
				setScrollAmount(getScrollAmount());
			});
		}

		@Override
		public void render(MatrixStack guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			int i = getScrollbarPositionX() - removeButton.getWidth() - 10;
			int j = top - 2;
			this.removeButton.x = i;
			this.removeButton.y = j;
			this.removeButton.render(guiGraphics, mouseX, mouseY, partialTick);

			this.editBox.x = left;
			this.editBox.y = j;
			this.editBox.setWidth(i - left - 4);
			this.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends Element> children() {
			return ImmutableList.of(this.editBox, removeButton);
		}
	}

	public class NewEntry extends Entry {

		private final ButtonWidget addButton;

		public NewEntry() {
			this.addButton = new ButtonWidget(0, 0, 150, 20, new TranslatableText("autoboop.filters.add"), button -> {
				int i = FiltersList.this.children().indexOf(this);
				FiltersList.this.children().add(Math.max(i - 1, 0), new FilterEntry(""));
				apply();
				setScrollAmount(Math.max(0, getMaxPosition() - (bottom - top - 4)));
			});
		}

		@Override
		public void render(MatrixStack guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			int i = getScrollbarPositionX() - width / 2 - 10 - addButton.getWidth() / 2;
			int j = top - 2;
			this.addButton.x = i;
			this.addButton.y = j;
			this.addButton.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends Element> children() {
			return List.of(addButton);
		}
	}
}
