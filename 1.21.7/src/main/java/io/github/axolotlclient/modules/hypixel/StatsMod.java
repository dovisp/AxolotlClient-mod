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

package io.github.axolotlclient.modules.hypixel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.api.API;
import io.github.axolotlclient.commands.PlayerArgument;
import lombok.Getter;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class StatsMod implements AbstractHypixelMod {
	private interface Handler {
		void accept(FabricClientCommandSource ctx, String uuid, String username, PlayerData data);
	}

	private record Entry(String name, Handler handler) {
	}


	private static MutableComponent statComponent(String key, Object... args) {
		return Component.translatable(key, Arrays.stream(args).map(s -> {
			if (s instanceof Float f) {
				return Component.literal(String.format("%.2f", f)).withStyle(ChatFormatting.GREEN);
			} else {
				return Component.literal(s.toString()).withStyle(ChatFormatting.GREEN);
			}
		}).toArray());
	}

	private static final List<ChatFormatting> RAINBOW = List.of(
		ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE
	);

	private static Component formatBedwarsPrestige(int level) {
		String levelString = level + "☆";
		return switch (level / 100) {
			case 0 -> Component.literal(levelString).withStyle(ChatFormatting.GRAY);
			case 1 -> Component.literal(levelString).withStyle(ChatFormatting.WHITE);
			case 2 -> Component.literal(levelString).withStyle(ChatFormatting.GOLD);
			case 3 -> Component.literal(levelString).withStyle(ChatFormatting.AQUA);
			case 4 -> Component.literal(levelString).withStyle(ChatFormatting.DARK_GREEN);
			case 5 -> Component.literal(levelString).withStyle(ChatFormatting.DARK_AQUA);
			case 6 -> Component.literal(levelString).withStyle(ChatFormatting.DARK_RED);
			case 7 -> Component.literal(levelString).withStyle(ChatFormatting.LIGHT_PURPLE);
			case 8 -> Component.literal(levelString).withStyle(ChatFormatting.BLUE);
			case 9 -> Component.literal(levelString).withStyle(ChatFormatting.DARK_PURPLE);
			default -> IntStream.range(0, levelString.length())
				.mapToObj(x -> Component.literal(levelString.substring(x, x + 1)).withStyle(RAINBOW.get(x % RAINBOW.size())))
				.reduce(Component.empty(), MutableComponent::append);
		};
	}

	private static Component buildBedwarsGameMode(String key, PlayerData.Bedwars.BedwarsGameData data) {
		final var text = statComponent(key);

		final var hover = Component.empty();
		hover.append(statComponent("playerstats.bedwars.kdr", data.kills(), data.deaths(), data.kdr()));
		hover.append("\n");
		hover.append(statComponent("playerstats.bedwars.fkdr", data.finalKills(), data.finalDeaths(), data.fkdr()));
		hover.append("\n");
		hover.append(statComponent("playerstats.bedwars.beds", data.bedsBroken(), data.bedsLost(), data.bblr()));
		hover.append("\n");
		hover.append(statComponent("playerstats.bedwars.summary_short", data.wins(), data.losses(), data.wlr()));

		text.setStyle(text.getStyle()
			.applyFormat(ChatFormatting.GOLD)
			.withHoverEvent(new HoverEvent.ShowText(hover))
		);
		return text;
	}

	private static Component buildBedwarsGameModesLine(PlayerData.Bedwars data) {
		final var text = Component.empty();

		text.append(buildBedwarsGameMode("playerstats.bedwars.solo", data.solo()));
		text.append(" | ");
		text.append(buildBedwarsGameMode("playerstats.bedwars.duos", data.doubles()));
		text.append(" | ");
		text.append(buildBedwarsGameMode("playerstats.bedwars.fours", data.fours()));
		text.append(" | ");
		text.append(buildBedwarsGameMode("playerstats.bedwars.core", data.core()));
		text.append(" | ");
		text.append(buildBedwarsGameMode("playerstats.bedwars.dreams", data.dreams()));
		return text;
	}

	private static Component buildSkywarsGameMode(String key, PlayerData.Skywars.GameData data) {
		final var text = statComponent(key);

		final var hover = Component.empty();
		hover.append(statComponent("playerstats.skywars.kdr", data.kills(), data.deaths(), data.kdr()));
		hover.append("\n");
		hover.append(statComponent("playerstats.skywars.summary", data.wins(), data.losses(), data.wlr()));

		text.setStyle(text.getStyle()
			.applyFormat(ChatFormatting.GOLD)
			.withHoverEvent(new HoverEvent.ShowText(hover))
		);

		return text;
	}

	private static Component buildSkywarsGameModesLine(PlayerData.Skywars data) {
		final var text = Component.empty();

		text.append(buildSkywarsGameMode("playerstats.skywars.solo", data.solo().normal()));
		text.append(" | ");
		text.append(buildSkywarsGameMode("playerstats.skywars.duos", data.team().normal()));
		text.append(" | ");
		text.append(buildSkywarsGameMode("playerstats.skywars.solo_insane", data.solo().insane()));
		text.append(" | ");
		text.append(buildSkywarsGameMode("playerstats.skywars.duos_insane", data.team().insane()));
		return text;
	}

	private static Component buildDuelsGameMode(Map.Entry<String, PlayerData.DuelsData.DuelsGameData> entry) {
		final var value = entry.getValue();
		final var hover = Component.translatable("playerstats.duels.mode_title",
			Component.translatable("playerstats.duels." + entry.getKey())
				.setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
		hover.append("\n");
		hover.append(statComponent("playerstats.duels.kdr", value.kills(), value.deaths(), value.kdr()));
		hover.append("\n");
		hover.append(statComponent("playerstats.duels.summary", value.wins(), value.losses(), value.wlr(), value.winstreak()));

		return Component.translatable("playerstats.duels." + entry.getKey())
			.setStyle(Style.EMPTY
				.withColor(ChatFormatting.GOLD)
				.withHoverEvent(new HoverEvent.ShowText(hover)));
	}

	private static Component buildDuelsGameModesLine(PlayerData.DuelsData data) {
		final var text = Component.empty();
		boolean empty = true;
		for (Map.Entry<String, PlayerData.DuelsData.DuelsGameData> stringDuelsGameDataEntry : data.modes().entrySet()) {
			if (!empty) {
				text.append("\n");
			}
			Component buildDuelsGameMode = buildDuelsGameMode(stringDuelsGameDataEntry);
			text.append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
			text.append(buildDuelsGameMode);
			empty = false;
		}
		return text;
	}

	private static final List<Entry> HANDLERS = List.of(
		new Entry("bedwars", (c, uuid, username, data) -> {
			final var allStats = data.bedwars().all();
			List.of(
				Component.translatable("playerstats.bedwars.title", data.formattedName(), formatBedwarsPrestige(data.bedwars().level())),
				statComponent("playerstats.bedwars.kdr", allStats.kills(), allStats.deaths(), allStats.kdr()),
				statComponent("playerstats.bedwars.fkdr", allStats.finalKills(), allStats.finalDeaths(), allStats.fkdr()),
				statComponent("playerstats.bedwars.beds", allStats.bedsBroken(), allStats.bedsLost(), allStats.bblr()),
				statComponent("playerstats.bedwars.summary", allStats.wins(), allStats.losses(), allStats.wlr(), allStats.winstreak()),
				buildBedwarsGameModesLine(data.bedwars())
			).forEach(c::sendFeedback);
		}),
		new Entry("skywars", (c, uuid, username, data) -> {
			final var allStats = data.skywars().all();
			List.of(
				Component.translatable("playerstats.skywars.title", data.formattedName(), data.skywars().level()),
				statComponent("playerstats.skywars.kdr", allStats.kills(), allStats.deaths(), allStats.kdr()),
				statComponent("playerstats.skywars.summary", allStats.wins(), allStats.losses(), allStats.wlr()),
				buildSkywarsGameModesLine(data.skywars())
			).forEach(c::sendFeedback);
		}),
		new Entry("duels", (c, uuid, username, data) -> {
			List.of(
				Component.translatable("playerstats.duels.title", data.formattedName()),
				buildDuelsGameModesLine(data.duels())
			).forEach(c::sendFeedback);
		})
	);
	@Getter
	private static final StatsMod instance = new StatsMod();

	private final OptionCategory playerstats = OptionCategory.create("playerstats");

	@Override
	public void init() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> {
			final var command = literal("playerstats");

			for (Entry handler : HANDLERS) {
				command.then(literal(handler.name()).then(argument("player", PlayerArgument.player()).executes(c -> {
					if (!API.getInstance().getApiOptions().enabled.get()) {
						c.getSource().sendError(Component.translatable("playerstats.error.api_disabled").withStyle(ChatFormatting.RED));
						return -1;
					}
					if (!API.getInstance().isAuthenticated()) {
						c.getSource().sendError(Component.translatable("api.error.unauthenticated").withStyle(ChatFormatting.RED));
						return -1;
					}

					final var res = PlayerArgument.get(c, "player");

					res.uuid().whenCompleteAsync((s, ex) -> {
						if (s.isEmpty()) {
							c.getSource().sendFeedback(Component.translatable("playerstats.error.unknown_player").withStyle(ChatFormatting.RED));
						} else {
							HypixelAbstractionLayer.getInstance().getPlayerDataApi().getAsync(s.get()).whenCompleteAsync((playerData, throwable) -> {
								if (playerData.isEmpty()) {
									c.getSource().sendFeedback(Component.translatable("playerstats.error.failed_data"));
									return;
								}

								handler.handler().accept(c.getSource(), s.get(), res.playerName(), playerData.get());
							}, Minecraft.getInstance());
						}
					});

					return 0;
				})));
			}

			final var node = dispatcher.register(command);
			dispatcher.register(literal("pstats").redirect(node));
		});
	}

	@Override
	public OptionCategory getCategory() {
		return playerstats;
	}
}
