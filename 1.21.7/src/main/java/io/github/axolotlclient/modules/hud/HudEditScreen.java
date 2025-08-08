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

package io.github.axolotlclient.modules.hud;

import java.util.List;
import java.util.Optional;

import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.ConfigStyles;
import io.github.axolotlclient.modules.hud.gui.component.HudEntry;
import io.github.axolotlclient.modules.hud.snapping.SnappingHelper;
import io.github.axolotlclient.modules.hud.util.DrawPosition;
import io.github.axolotlclient.modules.hud.util.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * This implementation of Hud modules is based on KronHUD.
 * <a href="https://github.com/DarkKronicle/KronHUD">Github Link.</a>
 *
 * @license GPL-3.0
 */

public class HudEditScreen extends Screen {

	private static final BooleanOption snapping = new BooleanOption("snapping", true);
	private static final OptionCategory hudEditScreenCategory = OptionCategory.create("hudEditScreen");
	private static final int GRAB_TOLERANCE = 5;
	private static final long MOVE_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR);
	private static final long DEFAULT_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
	private static final long NWSE_RESIZE_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR),
		NESW_RESIZE_CURSOR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR);

	public static boolean isSnappingEnabled() {
		return snapping.get();
	}

	public static void toggleSnapping() {
		snapping.toggle();
	}

	static {
		hudEditScreenCategory.add(snapping);
		AxolotlClient.config.add(hudEditScreenCategory);
	}

	private final Screen parent;
	private HudEntry current;
	private DrawPosition offset = null;
	private boolean mouseDown;
	private SnappingHelper snap;
	private long currentCursor;
	private ModificationMode mode = ModificationMode.NONE;

	public HudEditScreen() {
		this(null);
	}

	public HudEditScreen(Screen parent) {
		super(Component.empty());
		updateSnapState();
		mouseDown = false;
		this.parent = parent;
	}

	private void updateSnapState() {
		if (snapping.get() && current != null) {
			List<Rectangle> bounds = HudManager.getInstance().getAllBounds();
			bounds.remove(current.getTrueBounds());
			snap = new SnappingHelper(bounds, current.getTrueBounds());
		} else if (snap != null) {
			snap = null;
		}
	}

	private void setCursor(long cursor) {
		if (cursor > 0 && cursor != currentCursor) {
			currentCursor = cursor;
			GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), cursor);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);

		Optional<HudEntry> entry;
		if (current != null && mode != ModificationMode.NONE) {
			current.setHovered(true);
			entry = Optional.of(current);
		} else {
			entry = HudManager.getInstance().getEntryXY(mouseX, mouseY);
			entry.ifPresent(abstractHudEntry -> abstractHudEntry.setHovered(true));
		}
		HudManager.getInstance().renderPlaceholder(graphics, delta);
		if (entry.isPresent()) {
			var bounds = entry.get().getTrueBounds();
			if (mode == ModificationMode.NONE && bounds.isMouseOver(mouseX, mouseY)) {
				var xBound = Math.max(0, mouseX - bounds.x());
				var yBound = Math.max(0, mouseY - bounds.y());
				var tolerance = GRAB_TOLERANCE;
				if (xBound < tolerance && yBound < tolerance) {
					// top-left
					setCursor(NWSE_RESIZE_CURSOR);
				} else if (Math.abs(xBound - bounds.width()) < tolerance && Math.abs(yBound - bounds.height()) < tolerance) {
					// bottom-right
					setCursor(NWSE_RESIZE_CURSOR);
				} else if (xBound < tolerance && Math.abs(yBound - bounds.height()) < tolerance) {
					// bottom-left
					setCursor(NESW_RESIZE_CURSOR);
				} else if (yBound < tolerance && Math.abs(xBound - bounds.width()) < tolerance) {
					// top-right
					setCursor(NESW_RESIZE_CURSOR);
				} else {
					setCursor(MOVE_CURSOR);
				}
			}
		} else if (current == null) {
			setCursor(DEFAULT_CURSOR);
			mode = ModificationMode.NONE;
		}
		if (mouseDown && snap != null) {
			snap.renderSnaps(graphics);
		}
	}

	@Override
	public void removed() {
		setCursor(DEFAULT_CURSOR);
		mode = ModificationMode.NONE;
		super.removed();
	}

	@Override
	public void init() {
		mode = ModificationMode.NONE;

		HudManager.getInstance().getMoveableEntries().forEach(e -> addRenderableWidget(new HudEntryWidget(e)));

		this.addRenderableWidget(Button.builder(Component.translatable("hud.snapping").append(": ")
				.append(Component.translatable(snapping.get() ? "options.on" : "options.off")),
			buttonWidget -> {
				snapping.toggle();
				buttonWidget.setMessage(Component.translatable("hud.snapping").append(": ")
					.append(Component.translatable(snapping.get() ? "options.on" : "options.off")));
				AxolotlClient.configManager.save();
			}).bounds(width / 2 - 50, height / 2 + 12, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("hud.clientOptions"),
			buttonWidget -> {
				Screen screen = ConfigStyles.createScreen(this, AxolotlClient.configManager.getRoot());
				minecraft.setScreen(screen);
			}).bounds(width / 2 - 75, height / 2 - 10, 150, 20).build());

		if (parent != null)
			addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, buttonWidget -> minecraft.setScreen(parent))
				.bounds(width / 2 - 75, height - 50 + 22, 150, 20).build());
		else
			addRenderableWidget(Button.builder(Component.translatable("close"),
					buttonWidget -> minecraft.setScreen(null))
				.bounds(width / 2 - 75, height - 50 + 22, 150, 20).build());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean value = super.mouseClicked(mouseX, mouseY, button);
		Optional<HudEntry> entry = HudManager.getInstance().getEntryXY((int) Math.round(mouseX),
			(int) Math.round(mouseY));
		if (button == 0) {
			mouseDown = true;
			if (entry.isPresent()) {
				current = entry.get();
				offset = new DrawPosition((int) Math.round(mouseX - current.getTruePos().x()),
					(int) Math.round(mouseY - current.getTruePos().y()));
				var bounds = entry.get().getTrueBounds();
				var xBound = Math.max(0, mouseX - bounds.x());
				var yBound = Math.max(0, mouseY - bounds.y());
				if (currentCursor == NWSE_RESIZE_CURSOR) {
					if (xBound < bounds.width() / 2f && yBound < bounds.height() / 2f) {
						// top-left corner
						mode = ModificationMode.TOP_LEFT;
					} else if (xBound - bounds.width() / 2f > 0 && yBound - bounds.height() / 2f > 0) {
						// bottom-right corner
						mode = ModificationMode.BOTTOM_RIGHT;
					}
				} else if (currentCursor == NESW_RESIZE_CURSOR) {
					if (xBound < bounds.width() / 2f && yBound - bounds.height() / 2f > 0) {
						// bottom-left corner
						mode = ModificationMode.BOTTOM_LEFT;
					} else if (xBound - bounds.width() / 2f > 0 && yBound < bounds.height() / 2f) {
						// top-right corner
						mode = ModificationMode.TOP_RIGHT;
					}
				} else if (currentCursor == MOVE_CURSOR) {
					updateSnapState();
					mode = ModificationMode.MOVE;
				}
				return true;
			} else {
				mode = ModificationMode.NONE;
				current = null;
			}
		} else if (button == 1) {
			entry.ifPresent(abstractHudEntry -> {
				Screen screen = ConfigStyles.createScreen(this, abstractHudEntry.getCategory());
				minecraft.setScreen(screen);
			});
		}
		return value;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (current != null) {
			AxolotlClient.configManager.save();
		}
		current = null;
		snap = null;
		mouseDown = false;
		mode = ModificationMode.NONE;
		setCursor(DEFAULT_CURSOR);
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (current != null) {
			if (mode == ModificationMode.MOVE) {
				current.setX((int) mouseX - offset.x() + current.offsetTrueWidth());
				current.setY((int) mouseY - offset.y() + current.offsetTrueHeight());
				if (snap != null) {
					Integer snapX, snapY;
					snap.setCurrent(current.getTrueBounds());
					if ((snapX = snap.getCurrentXSnap()) != null) {
						current.setX(snapX + current.offsetTrueWidth());
					}
					if ((snapY = snap.getCurrentYSnap()) != null) {
						current.setY(snapY + current.offsetTrueHeight());
					}
				}
			} else {
				var bounds = current.getTrueBounds();
				double newWidth, newHeight;
				if (mode == ModificationMode.TOP_LEFT) {
					// top-left corner
					newWidth = mouseX - bounds.xEnd();
					newHeight = mouseY - bounds.yEnd();
				} else if (mode == ModificationMode.BOTTOM_LEFT) {
					// bottom-left corner
					newWidth = mouseX - bounds.xEnd();
					newHeight = mouseY - bounds.y();
				} else if (mode == ModificationMode.TOP_RIGHT) {
					// top-right corner
					newWidth = mouseX - bounds.x();
					newHeight = mouseY - bounds.yEnd();
				} else if (mode == ModificationMode.BOTTOM_RIGHT) {
					// bottom-right corner
					newWidth = mouseX - bounds.x();
					newHeight = mouseY - bounds.y();
				} else {
					newWidth = bounds.width();
					newHeight = bounds.height();
				}
				float newScale = current.getScale() * Math.max((float) Math.abs(newWidth) / bounds.width(), (float) Math.abs(newHeight) / bounds.height());
				current.setScale(Math.max(0.1f, newScale));
				if (mode == ModificationMode.TOP_LEFT) {
					// top-left corner
					current.setX(bounds.xEnd() - current.getTrueWidth() + current.offsetTrueWidth());
					current.setY(bounds.yEnd() - current.getTrueHeight() + current.offsetTrueHeight());
				} else if (mode == ModificationMode.BOTTOM_LEFT) {
					// bottom-left corner
					current.setX(bounds.xEnd() - current.getTrueWidth() + current.offsetTrueWidth());
				} else if (mode == ModificationMode.TOP_RIGHT) {
					// top-right corner
					current.setY(bounds.yEnd() - current.getTrueHeight() + current.offsetTrueHeight());
				}
			}
			if (current.tickable()) {
				current.tick();
			}
			return true;
		}
		return false;
	}

	private enum ModificationMode {
		NONE,
		MOVE,
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT
	}
}
