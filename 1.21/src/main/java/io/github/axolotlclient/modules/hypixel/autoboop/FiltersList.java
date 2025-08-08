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
import net.minecraft.client.gui.ElementPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.GuiNavigationEvent;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.ButtonWidget;
import net.minecraft.client.gui.widget.list.ElementListWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class FiltersList extends ElementListWidget<FiltersList.Entry> {
	final FilterListConfigurationScreen screen;

	public FiltersList(FilterListConfigurationScreen screen) {
		super(MinecraftClient.getInstance(), screen.width, screen.layout.getContentsHeight(), screen.layout.getHeaderHeight(), 24);
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
		public List<? extends Selectable> selectableChildren() {
			return List.of();
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {

		}

		@Override
		public List<? extends Element> children() {
			return List.of();
		}

		@Override
		public @Nullable ElementPath nextFocusPath(GuiNavigationEvent event) {
			return null;
		}
	}

	@Environment(EnvType.CLIENT)
	public class FilterEntry extends Entry {
		private static final Text REMOVE_BUTTON_TITLE = Text.translatable("autoboop.filters.remove");
		private final TextFieldWidget editBox;
		private final ButtonWidget removeButton;

		FilterEntry(String filter) {
			this.editBox = new TextFieldWidget(client.textRenderer, 0, 0, 200, 20, Text.translatable("autoboop.filters.edit"));
			editBox.setText(filter);
			editBox.setMaxLength(16);
			this.removeButton = ButtonWidget.builder(REMOVE_BUTTON_TITLE, b -> {
					removeEntry(this);
					apply();
					refreshScrollAmount();
				}).positionAndSize(0, 0, 50, 20)
				.build();
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			int i = getScrollbarPositionX() - removeButton.getWidth() - 10;
			int j = top - 2;
			this.removeButton.setPosition(i, j);
			this.removeButton.render(guiGraphics, mouseX, mouseY, partialTick);

			this.editBox.setPosition(left, j);
			this.editBox.setWidth(i - left - 4);
			this.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends Element> children() {
			return ImmutableList.of(this.editBox, removeButton);
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return ImmutableList.of(this.editBox, removeButton);
		}
	}

	public class NewEntry extends Entry {

		private final ButtonWidget addButton;

		public NewEntry() {
			this.addButton = ButtonWidget.builder(Text.translatable("autoboop.filters.add"), button -> {
					int i = FiltersList.this.children().indexOf(this);
					FiltersList.this.children().add(Math.max(i - 1, 0), new FilterEntry(""));
					apply();
					setScrollAmount(getMaxScroll());
				}).positionAndSize(0, 0, 150, 20)
				.build();
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return List.of(addButton);
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			int i = getScrollbarPositionX() - width / 2 - 10 - addButton.getWidth() / 2;
			int j = top - 2;
			this.addButton.setPosition(i, j);
			this.addButton.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends Element> children() {
			return List.of(addButton);
		}
	}
}
