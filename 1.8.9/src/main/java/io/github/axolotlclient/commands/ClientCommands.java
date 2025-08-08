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

package io.github.axolotlclient.commands;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientCommands {
	@Getter
	private static final CommandDispatcher<ClientCommandInfo> DISPATCHER = new CommandDispatcher<>();
	private static final Logger LOGGER = LogManager.getLogger("ClientCommandHandler");

	private static boolean isIgnoredException(CommandExceptionType type) {
		return type == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand() ||
			type == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException();
	}

	private static Text getErrorMessage(CommandSyntaxException e) {
		Text message = new LiteralText(e.getMessage());
		String context = e.getContext();
		return context != null ?
			new TranslatableText("lcu.command.parse_error", message, e.getCursor(), context) : message;
	}

	private static ClientCommandInfo buildClientSource(Minecraft client) {
		Preconditions.checkState(client.player != null);

		return new ClientCommandInfo(
			client.player,
			client,
			Objects.requireNonNull(client.world),
			client.player,
			client.player.getSourcePos(),
			client.player.yaw,
			client.player.pitch
		);
	}

	public static boolean dispatchClient(String command) {
		if (!command.startsWith("/")) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		ClientCommandInfo source = buildClientSource(client);
		// cancel if present
		command = command.trim().substring(1);

		try {
			DISPATCHER.execute(command, source);
			return true;
		} catch (CommandSyntaxException e) {
			if (ClientCommands.isIgnoredException(e.getType())) {
				return false;
			}

			ClientCommands.LOGGER.warn("Syntax exception for command '{}'", command, e);
			source.getOrigin().sendMessage(ClientCommands.getErrorMessage(e));
			return true;
		} catch (Exception e) {
			ClientCommands.LOGGER.warn("Error while executing command '{}'", command, e);
			source.getOrigin().sendMessage(new LiteralText(e.getMessage() == null ? "" : e.getMessage()));
			return true;
		}
	}

	public static CompletableFuture<List<String>> getCompletionsClient(String command) {
		Minecraft client = Minecraft.getInstance();

		String command0 = command.startsWith("/") ? command.substring(1) : command;
		return DISPATCHER.getCompletionSuggestions(DISPATCHER.parse(command0, buildClientSource(client)))
			.thenApply(suggestions -> suggestions.getList()
				.stream()
				.map(x -> command0.contains(" ") ? x.getText() : "/" + x.getText())
				.toList()
			);
	}

	public static LiteralArgumentBuilder<ClientCommandInfo> literal(String arg) {
		return LiteralArgumentBuilder.literal(arg);
	}

	public static <T> RequiredArgumentBuilder<ClientCommandInfo, T> argument(String arg, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(arg, type);
	}
}
