/*
 * Copyright © 2021-2023 moehreag <moehreag@gmail.com> & Contributors
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

package io.github.axolotlclient.util;

import java.util.function.Supplier;

import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;

public class FeatureDisabler {
	public static void init() {
		return;
	}

	private static void setServers(BooleanOption option, Supplier<Boolean> condition, String... servers) {
		return;
	}

	public static void onServerJoin(String address) {
		return;
	}

	public static void clear() {
		return;
	}

	public static void update() {
		return;
	}

	private static void disableOption(BooleanOption option, String[] servers, String currentServer) {
		return;
	}
}