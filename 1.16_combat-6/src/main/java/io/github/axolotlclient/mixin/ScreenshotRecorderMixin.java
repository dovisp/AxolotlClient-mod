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

package io.github.axolotlclient.mixin;

import java.io.File;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.axolotlclient.modules.screenshotUtils.ScreenshotUtils;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.util.ScreenshotUtils.class)
public abstract class ScreenshotRecorderMixin {

	// for some reason @WrapOperation doesn't like injecting at <init>
	@Redirect(method = "method_1661", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0))
	private static void axolotlclient$onScreenshotSaveSuccess(Consumer<MutableText> instance, Object t, @Local(argsOnly = true) File target) {
		instance.accept(ScreenshotUtils.getInstance().onScreenshotTaken((MutableText) t, target));
	}
}
