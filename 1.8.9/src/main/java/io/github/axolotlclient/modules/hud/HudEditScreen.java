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

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.util.ConfigStyles;
import io.github.axolotlclient.modules.hud.gui.component.HudEntry;
import io.github.axolotlclient.modules.hud.snapping.SnappingHelper;
import io.github.axolotlclient.modules.hud.util.DrawPosition;
import io.github.axolotlclient.modules.hud.util.Rectangle;
import io.github.axolotlclient.util.GLFWUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
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
		super();
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
			GLFWUtil.runUsingGlfwHandle(ctx -> GLFW.glfwSetCursor(ctx, cursor));
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		if (Minecraft.getInstance().world != null)
			fillGradient(0, 0, width, height, 0xB0100E0E,
				0x46212020);
		else {
			renderBackground(0);
		}

		super.render(mouseX, mouseY, delta);
		GlStateManager.enableTexture();

		Optional<HudEntry> entry;
		if (current != null && mode != ModificationMode.NONE) {
			current.setHovered(true);
			entry = Optional.of(current);
		} else {
			entry = HudManager.getInstance().getEntryXY(mouseX, mouseY);
			entry.ifPresent(abstractHudEntry -> abstractHudEntry.setHovered(true));
		}
		HudManager.getInstance().renderPlaceholder(delta);
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
			snap.renderSnaps();
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		Optional<HudEntry> entry = HudManager.getInstance().getEntryXY(mouseX, mouseY);
		if (button == 0) {
			mouseDown = true;
			if (entry.isPresent()) {
				current = entry.get();
				offset = new DrawPosition(mouseX - current.getTruePos().x(),
					mouseY - current.getTruePos().y());
				var bounds = entry.get().getTrueBounds();
				var xBound = Math.max(0, mouseX - bounds.x());
				var yBound = Math.max(0, mouseY - bounds.y());
				if (currentCursor == NWSE_RESIZE_CURSOR) {
					if (xBound < bounds.width() / 2 && yBound < bounds.height() / 2) {
						// top-left corner
						mode = ModificationMode.TOP_LEFT;
					} else if (xBound - bounds.width() / 2 > 0 && yBound - bounds.height() / 2 > 0) {
						// bottom-right corner
						mode = ModificationMode.BOTTOM_RIGHT;
					}
				} else if (currentCursor == NESW_RESIZE_CURSOR) {
					if (xBound < bounds.width() / 2 && yBound - bounds.height() / 2 > 0) {
						// bottom-left corner
						mode = ModificationMode.BOTTOM_LEFT;
					} else if (xBound - bounds.width() / 2 > 0 && yBound < bounds.height() / 2) {
						// top-right corner
						mode = ModificationMode.TOP_RIGHT;
					}
				} else if (currentCursor == MOVE_CURSOR) {
					updateSnapState();
					mode = ModificationMode.MOVE;
				}
			} else {
				mode = ModificationMode.NONE;
				current = null;
			}
		} else if (button == 1) {
			entry.ifPresent(hudEntry -> {
				Screen screen = ConfigStyles.createScreen(this, hudEntry.getCategory());
				mode = ModificationMode.NONE;
				setCursor(DEFAULT_CURSOR);
				Minecraft.getInstance().openScreen(screen);
			});
		}
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int button) {
		if (current != null) {
			AxolotlClient.configManager.save();
		}
		current = null;
		snap = null;
		mouseDown = false;
		mode = ModificationMode.NONE;
		setCursor(DEFAULT_CURSOR);
		super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long mouseLastClicked) {
		if (current != null) {
			if (mode == ModificationMode.MOVE) {
				current.setX((mouseX - offset.x()) + current.offsetTrueWidth());
				current.setY(mouseY - offset.y() + current.offsetTrueHeight());
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
				int newWidth, newHeight;
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
					current.setX(bounds.xEnd() - current.getTrueWidth());
					current.setY(bounds.yEnd() - current.getTrueHeight());
				} else if (mode == ModificationMode.BOTTOM_LEFT) {
					// bottom-left corner
					current.setX(bounds.xEnd() - current.getTrueWidth());
				} else if (mode == ModificationMode.TOP_RIGHT) {
					// top-right corner
					current.setY(bounds.yEnd() - current.getTrueHeight());
				}
			}
			if (current.tickable()) {
				current.tick();
			}
		}
	}

	@Override
	public void removed() {
		super.removed();
		setCursor(DEFAULT_CURSOR);
		mode = ModificationMode.NONE;
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		switch (button.id) {
			case 3:
				snapping.toggle();
				button.message = I18n.translate("hud.snapping") + ": "
					+ I18n.translate(snapping.get() ? "options.on" : "options.off");
				AxolotlClient.configManager.save();
				break;
			case 1:
				Screen screen = ConfigStyles.createScreen(this, AxolotlClient.configManager.getRoot());
				Minecraft.getInstance().openScreen(screen);
				break;
			case 0:
				Minecraft.getInstance().openScreen(parent);
				break;
			case 2:
				Minecraft.getInstance().openScreen(null);
				break;
		}
	}

	@Override
	public void init() {
		mode = ModificationMode.NONE;
		this.buttons.add(new ButtonWidget(3, width / 2 - 50, height / 2 + 12, 100, 20,
			I18n.translate("hud.snapping") + ": " + I18n.translate(snapping.get() ? "options.on" : "options.off")));

		this.buttons.add(
			new ButtonWidget(1, width / 2 - 75, height / 2 - 10, 150, 20, I18n.translate("hud.clientOptions")));

		if (parent != null)
			buttons.add(new ButtonWidget(0, width / 2 - 75, height - 50 + 22, 150, 20, I18n.translate("back")));
		else
			buttons.add(new ButtonWidget(2, width / 2 - 75, height - 50 + 22, 150, 20, I18n.translate("close")));
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
