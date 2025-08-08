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

package io.github.axolotlclient.modules.hud.gui.keystrokes;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import io.github.axolotlclient.AxolotlClientConfig.api.util.Colors;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.IntegerOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.ui.vanilla.widgets.IntegerWidget;
import io.github.axolotlclient.modules.hud.gui.hud.KeystrokeHud;
import io.github.axolotlclient.modules.hud.gui.layout.Justification;
import io.github.axolotlclient.modules.hud.util.DrawUtil;
import lombok.Getter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class ConfigureKeyBindScreen extends io.github.axolotlclient.AxolotlClientConfig.impl.ui.Screen {

	private final Screen parent;
	private final KeystrokeHud hud;
	public final KeystrokeHud.Keystroke stroke;
	private final IntegerOption width;
	private final IntegerOption height;
	private final boolean isAddScreen;

	public ConfigureKeyBindScreen(Screen parent, KeystrokeHud hud, KeystrokeHud.Keystroke stroke, boolean isAddScreen) {
		super("keystrokes.stroke.configure_stroke");
		this.parent = parent;
		this.hud = hud;
		this.stroke = stroke;

		width = new IntegerOption("", stroke.getBounds().width(), v -> stroke.getBounds().width(v), 7, 100);
		height = new IntegerOption("", stroke.getBounds().height(), v -> stroke.getBounds().height(v), 7, 100);
		this.isAddScreen = isAddScreen;
	}

	@Override
	public void init() {
		super.init();

		int leftColX = super.width / 2 - 4 - 150;
		int leftColY = 36 + 5;
		int rightColX = super.width / 2 + 4;
		int rightColY = 36;

		addDrawableChild(new AbstractButtonWidget(super.width / 2 - 100, rightColY, 200, 40, LiteralText.EMPTY) {
			@Override
			public void renderButton(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
				var rect = stroke.getRenderPosition();
				guiGraphics.push();
				guiGraphics.translate(x, y, 0);
				float scale = Math.min((float) getHeight() / rect.height(), (float) getWidth() / rect.width());
				guiGraphics.translate(getWidth() / 2f - (rect.width() * scale) / 2f, 0, 0);
				guiGraphics.scale(scale, scale, 1);
				guiGraphics.translate(-rect.x(), -rect.y(), 0);
				DrawUtil.fillRect(guiGraphics, rect, Colors.WHITE.withAlpha(128));
				stroke.render(guiGraphics);
				guiGraphics.pop();
			}
		}).active = false;
		leftColY += 48;
		rightColY += 48;

		AbstractButtonWidget currentKey = addDrawableChild(textWidget(0, rightColY, super.width, 9, LiteralText.EMPTY, textRenderer));
		if (stroke.getKey() != null) {
			currentKey.setMessage(new TranslatableText("keystrokes.stroke.key", stroke.getKey().getBoundKeyLocalizedText(), new TranslatableText(stroke.getKey().getTranslationKey())));
		} else {
			currentKey.setMessage(LiteralText.EMPTY);
		}
		leftColY += 9 + 8;
		rightColY += 9 + 8;

		if (stroke.isLabelEditable()) {
			addDrawableChild(textWidget(leftColX, leftColY, 150, 20, new TranslatableText("keystrokes.stroke.label"), textRenderer));
			leftColY += 28;
			boolean supportsSynchronization = stroke instanceof KeystrokeHud.LabelKeystroke;

			var label = addDrawableChild(new TextFieldWidget(textRenderer, rightColX, rightColY, supportsSynchronization ? 30 : 150, 20, LiteralText.EMPTY));

			label.setText(stroke.getLabel());
			label.setChangedListener(stroke::setLabel);
			if (supportsSynchronization) {
				var s = (KeystrokeHud.LabelKeystroke) stroke;
				ButtonWidget synchronizeButton = addDrawableChild(new ButtonWidget(rightColX + 30 + 4, rightColY, 58, 20,
					new TranslatableText("keystrokes.stroke.label.synchronize_with_key", s.isSynchronizeLabel() ? ScreenTexts.ON : ScreenTexts.OFF), b -> {
					s.setSynchronizeLabel(!s.isSynchronizeLabel());
					b.setMessage(new TranslatableText("keystrokes.stroke.label.synchronize_with_key", s.isSynchronizeLabel() ? ScreenTexts.ON : ScreenTexts.OFF));
					label.setEditable(!s.isSynchronizeLabel());
					if (s.isSynchronizeLabel()) {
						label.setText(stroke.getLabel());
					}
				}));
				synchronizeButton.active = s.getKey() != null;
				label.setEditable(!s.isSynchronizeLabel());
				addDrawableChild(CyclingButtonWidget.<Justification>builder(j -> new TranslatableText(j.toString())).values(Justification.values())
					.initially(s.getJustification()).build(rightColX + 30 + 4 + 58 + 4, rightColY, 58, 20,
						new TranslatableText("justification"), (btn, val) -> s.setJustification(val)));
			}
			rightColY += 28;
		}
		addDrawableChild(textWidget(leftColX, leftColY, 150, 20, new TranslatableText("keystrokes.stroke.width"), textRenderer));
		leftColY += 28;
		addDrawableChild(new IntegerWidget(rightColX, rightColY, 150, 20, width));
		rightColY += 28;
		addDrawableChild(textWidget(leftColX, leftColY, 150, 20, new TranslatableText("keystrokes.stroke.height"), textRenderer));
		leftColY += 28;
		addDrawableChild(new IntegerWidget(rightColX, rightColY, 150, 20, height));

		rightColY += 28;

		addDrawableChild(new ButtonWidget(super.width / 2 - 150 - 4, rightColY, 150, 20,
			new TranslatableText("keystrokes.stroke.configure_key"), b -> {
			client.openScreen(new KeyBindSelectionScreen(this, stroke));
		}));
		addDrawableChild(new ButtonWidget(super.width / 2 + 4, rightColY, 150, 20,
			new TranslatableText("keystrokes.stroke.configure_position"), b -> {
			client.openScreen(new KeystrokePositioningScreen(this, hud, stroke));
		}));


		if (isAddScreen) {
			ButtonWidget addButton = addDrawableChild(new ButtonWidget(super.width / 2 - 150 - 4, super.height - 33 / 2 - 10, 150, 20,
				new TranslatableText("keystrokes.stroke.add"), b -> {
				hud.keystrokes.add(stroke);
				onClose();
			}));
			addButton.active = stroke.getKey() != null;
		}
		addDrawableChild(new ButtonWidget(isAddScreen ? super.width / 2 + 4 : super.width / 2 - 75, super.height - 33 / 2 - 10, 150, 20,
			isAddScreen ? ScreenTexts.CANCEL : ScreenTexts.BACK, b -> onClose()));
	}

	@Override
	public void render(MatrixStack graphics, int mouseX, int mouseY, float delta) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, delta);
		drawCenteredText(graphics, textRenderer, getTitle(), super.width / 2, 33 / 2 - textRenderer.fontHeight / 2, -1);
	}

	@Override
	public void onClose() {
		client.openScreen(parent);
		hud.saveKeystrokes();
	}

	private static AbstractButtonWidget textWidget(int x, int y, int width, int height, Text text, TextRenderer renderer) {
		var widget = new AbstractButtonWidget(x, y, width, height, text) {
			@Override
			public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
				drawCenteredText(matrices, renderer, getMessage(), x + width / 2, y, -1);
			}
		};
		widget.active = false;
		return widget;
	}
}

class CyclingButtonWidget<T> extends ButtonWidget {
	private final Text optionText;
	private int index;
	@Getter
	private T value;
	private final CyclingButtonWidget.Values<T> values;
	private final Function<T, Text> valueToText;
	private final Function<CyclingButtonWidget<T>, MutableText> narrationMessageFactory;
	private final CyclingButtonWidget.UpdateCallback<T> callback;

	CyclingButtonWidget(int x, int y, int width, int height, Text message, Text optionText, int index, T value,
						CyclingButtonWidget.Values<T> values, Function<T, Text> valueToText,
						Function<CyclingButtonWidget<T>, MutableText> narrationMessageFactory,
						CyclingButtonWidget.UpdateCallback<T> callback) {
		super(x, y, width, height, message, btn -> {
		});
		this.optionText = optionText;
		this.index = index;
		this.value = value;
		this.values = values;
		this.valueToText = valueToText;
		this.narrationMessageFactory = narrationMessageFactory;
		this.callback = callback;
	}

	@Override
	public void onPress() {
		if (Screen.hasShiftDown()) {
			this.cycle(-1);
		} else {
			this.cycle(1);
		}
	}

	private void cycle(int amount) {
		List<T> list = this.values.getCurrent();
		this.index = MathHelper.floorMod(this.index + amount, list.size());
		T object = list.get(this.index);
		this.internalSetValue(object);
		this.callback.onValueChange(this, object);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (amount > 0.0) {
			this.cycle(-1);
		} else if (amount < 0.0) {
			this.cycle(1);
		}

		return true;
	}

	public void setValue(T value) {
		List<T> list = this.values.getCurrent();
		int i = list.indexOf(value);
		if (i != -1) {
			this.index = i;
		}

		this.internalSetValue(value);
	}

	private void internalSetValue(T value) {
		Text text = this.composeText(value);
		this.setMessage(text);
		this.value = value;
	}

	private Text composeText(T value) {
		return this.composeGenericOptionText(value);
	}

	private MutableText composeGenericOptionText(T value) {
		return this.optionText.copy().append(": ").append(this.valueToText.apply(value));
	}

	@Override
	protected MutableText getNarrationMessage() {
		return this.narrationMessageFactory.apply(this);
	}

	public MutableText getGenericNarrationMessage() {
		return new TranslatableText("gui.narrate.button", this.getMessage());
	}

	static <T> CyclingButtonWidget.Builder<T> builder(Function<T, Text> valueToText) {
		return new CyclingButtonWidget.Builder<>(valueToText);
	}

	static class Builder<T> {
		private int initialIndex;
		@Nullable
		private T value;
		private final Function<T, Text> valueToText;
		private CyclingButtonWidget.Values<T> values = CyclingButtonWidget.Values.of(ImmutableList.<T>of());

		public Builder(Function<T, Text> valueToText) {
			this.valueToText = valueToText;
		}

		public CyclingButtonWidget.Builder<T> values(Collection<T> values) {
			return this.values(CyclingButtonWidget.Values.of(values));
		}

		@SafeVarargs
		public final CyclingButtonWidget.Builder<T> values(T... values) {
			return this.values(ImmutableList.copyOf(values));
		}

		public CyclingButtonWidget.Builder<T> values(CyclingButtonWidget.Values<T> values) {
			this.values = values;
			return this;
		}

		public CyclingButtonWidget.Builder<T> initially(T value) {
			this.value = value;
			int i = this.values.getDefaults().indexOf(value);
			if (i != -1) {
				this.initialIndex = i;
			}

			return this;
		}

		public CyclingButtonWidget<T> build(int x, int y, int width, int height, Text optionText, CyclingButtonWidget.UpdateCallback<T> callback) {
			List<T> list = this.values.getDefaults();
			if (list.isEmpty()) {
				throw new IllegalStateException("No values for cycle button");
			} else {
				T object = this.value != null ? this.value : list.get(this.initialIndex);
				Text text = this.valueToText.apply(object);
				Text text2 = optionText.copy().append(": ").append(text);
				return new CyclingButtonWidget<>(
					x,
					y,
					width,
					height,
					text2,
					optionText,
					this.initialIndex,
					object,
					this.values,
					this.valueToText,
					CyclingButtonWidget::getGenericNarrationMessage,
					callback
				);
			}
		}
	}

	interface UpdateCallback<T> {
		void onValueChange(CyclingButtonWidget<T> cyclingButtonWidget, T object);
	}

	interface Values<T> {
		List<T> getCurrent();

		List<T> getDefaults();

		static <T> CyclingButtonWidget.Values<T> of(Collection<T> values) {
			final List<T> list = ImmutableList.copyOf(values);
			return new CyclingButtonWidget.Values<T>() {
				@Override
				public List<T> getCurrent() {
					return list;
				}

				@Override
				public List<T> getDefaults() {
					return list;
				}
			};
		}

		static <T> CyclingButtonWidget.Values<T> of(BooleanSupplier alternativeToggle, List<T> defaults, List<T> alternatives) {
			final List<T> list = ImmutableList.copyOf(defaults);
			final List<T> list2 = ImmutableList.copyOf(alternatives);
			return new CyclingButtonWidget.Values<T>() {
				@Override
				public List<T> getCurrent() {
					return alternativeToggle.getAsBoolean() ? list2 : list;
				}

				@Override
				public List<T> getDefaults() {
					return list;
				}
			};
		}
	}
}
