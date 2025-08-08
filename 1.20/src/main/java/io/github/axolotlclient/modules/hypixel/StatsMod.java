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
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class StatsMod implements AbstractHypixelMod {
	private interface Handler {
		void accept(FabricClientCommandSource ctx, String uuid, String username, PlayerData data);
	}

	private record Entry(String name, Handler handler) {
	}

	private static MutableText statText(String key, Object... args) {
		return Text.translatable(key, Arrays.stream(args).map(s -> {
			if (s instanceof Float f) {
				return Text.literal(String.format("%.2f", f)).formatted(Formatting.GREEN);
			} else {
				return Text.literal(s.toString()).formatted(Formatting.GREEN);
			}
		}).toArray());
	}

	private static final List<Formatting> RAINBOW = List.of(
		Formatting.RED, Formatting.GOLD, Formatting.YELLOW, Formatting.GREEN, Formatting.AQUA, Formatting.LIGHT_PURPLE, Formatting.DARK_PURPLE
	);

	private static Text formatBedwarsPrestige(int level) {
		String levelString = level + "☆";
		return switch (level / 100) {
			case 0 -> Text.literal(levelString).formatted(Formatting.GRAY);
			case 1 -> Text.literal(levelString).formatted(Formatting.WHITE);
			case 2 -> Text.literal(levelString).formatted(Formatting.GOLD);
			case 3 -> Text.literal(levelString).formatted(Formatting.AQUA);
			case 4 -> Text.literal(levelString).formatted(Formatting.DARK_GREEN);
			case 5 -> Text.literal(levelString).formatted(Formatting.DARK_AQUA);
			case 6 -> Text.literal(levelString).formatted(Formatting.DARK_RED);
			case 7 -> Text.literal(levelString).formatted(Formatting.LIGHT_PURPLE);
			case 8 -> Text.literal(levelString).formatted(Formatting.BLUE);
			case 9 -> Text.literal(levelString).formatted(Formatting.DARK_PURPLE);
			default -> IntStream.range(0, levelString.length())
				.mapToObj(x -> Text.literal(levelString.substring(x, x + 1)).formatted(RAINBOW.get(x % RAINBOW.size())))
				.reduce(Text.empty(), MutableText::append);
		};
	}

	private static Text buildBedwarsGameMode(String key, PlayerData.Bedwars.BedwarsGameData data) {
		final var text = statText(key);

		final var hover = Text.empty();
		hover.append(statText("playerstats.bedwars.kdr", data.kills(), data.deaths(), data.kdr()));
		hover.append("\n");
		hover.append(statText("playerstats.bedwars.fkdr", data.finalKills(), data.finalDeaths(), data.fkdr()));
		hover.append("\n");
		hover.append(statText("playerstats.bedwars.beds", data.bedsBroken(), data.bedsLost(), data.bblr()));
		hover.append("\n");
		hover.append(statText("playerstats.bedwars.summary_short", data.wins(), data.losses(), data.wlr()));

		text.setStyle(text.getStyle()
			.withFormatting(Formatting.GOLD)
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
		);
		return text;
	}

	private static Text buildBedwarsGameModesLine(PlayerData.Bedwars data) {
		final var text = Text.empty();

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

	private static Text buildSkywarsGameMode(String key, PlayerData.Skywars.GameData data) {
		final var text = statText(key);

		final var hover = Text.empty();
		hover.append(statText("playerstats.skywars.kdr", data.kills(), data.deaths(), data.kdr()));
		hover.append("\n");
		hover.append(statText("playerstats.skywars.summary", data.wins(), data.losses(), data.wlr()));

		text.setStyle(text.getStyle()
			.withFormatting(Formatting.GOLD)
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
		);

		return text;
	}

	private static Text buildSkywarsGameModesLine(PlayerData.Skywars data) {
		final var text = Text.empty();

		text.append(buildSkywarsGameMode("playerstats.skywars.solo", data.solo().normal()));
		text.append(" | ");
		text.append(buildSkywarsGameMode("playerstats.skywars.duos", data.team().normal()));
		text.append(" | ");
		text.append(buildSkywarsGameMode("playerstats.skywars.solo_insane", data.solo().insane()));
		text.append(" | ");
		text.append(buildSkywarsGameMode("playerstats.skywars.duos_insane", data.team().insane()));
		return text;
	}

	private static Text buildDuelsGameMode(Map.Entry<String, PlayerData.DuelsData.DuelsGameData> entry) {
		final var value = entry.getValue();
		final var hover = Text.translatable("playerstats.duels.mode_title",
			Text.translatable("playerstats.duels." + entry.getKey())
				.setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)));
		hover.append("\n");
		hover.append(statText("playerstats.duels.kdr", value.kills(), value.deaths(), value.kdr()));
		hover.append("\n");
		hover.append(statText("playerstats.duels.summary", value.wins(), value.losses(), value.wlr(), value.winstreak()));

		return Text.translatable("playerstats.duels." + entry.getKey())
			.setStyle(Style.EMPTY
				.withFormatting(Formatting.GOLD)
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
	}

	private static Text buildDuelsGameModesLine(PlayerData.DuelsData data) {
		final var text = Text.empty();
		boolean empty = true;
		for (Map.Entry<String, PlayerData.DuelsData.DuelsGameData> stringDuelsGameDataEntry : data.modes().entrySet()) {
			if (!empty) {
				text.append("\n");
			}
			Text buildDuelsGameMode = buildDuelsGameMode(stringDuelsGameDataEntry);
			text.append(Text.literal("» ").setStyle(Style.EMPTY.withFormatting(Formatting.RED)));
			text.append(buildDuelsGameMode);
			empty = false;
		}
		return text;
	}

	private static final List<Entry> HANDLERS = List.of(
		new Entry("bedwars", (c, uuid, username, data) -> {
			final var allStats = data.bedwars().all();
			List.of(
				Text.translatable("playerstats.bedwars.title", data.formattedName(), formatBedwarsPrestige(data.bedwars().level())),
				statText("playerstats.bedwars.kdr", allStats.kills(), allStats.deaths(), allStats.kdr()),
				statText("playerstats.bedwars.fkdr", allStats.finalKills(), allStats.finalDeaths(), allStats.fkdr()),
				statText("playerstats.bedwars.beds", allStats.bedsBroken(), allStats.bedsLost(), allStats.bblr()),
				statText("playerstats.bedwars.summary", allStats.wins(), allStats.losses(), allStats.wlr(), allStats.winstreak()),
				buildBedwarsGameModesLine(data.bedwars())
			).forEach(c::sendFeedback);
		}),
		new Entry("skywars", (c, uuid, username, data) -> {
			final var allStats = data.skywars().all();
			List.of(
				Text.translatable("playerstats.skywars.title", data.formattedName(), data.skywars().level()),
				statText("playerstats.skywars.kdr", allStats.kills(), allStats.deaths(), allStats.kdr()),
				statText("playerstats.skywars.summary", allStats.wins(), allStats.losses(), allStats.wlr()),
				buildSkywarsGameModesLine(data.skywars())
			).forEach(c::sendFeedback);
		}),
		new Entry("duels", (c, uuid, username, data) -> {
			List.of(
				Text.translatable("playerstats.duels.title", data.formattedName()),
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
						c.getSource().sendError(Text.translatable("playerstats.error.api_disabled").formatted(Formatting.RED));
						return -1;
					}
					if (!API.getInstance().isAuthenticated()) {
						c.getSource().sendError(Text.translatable("api.error.unauthenticated").formatted(Formatting.RED));
						return -1;
					}

					final var res = PlayerArgument.get(c, "player");

					res.uuid().whenCompleteAsync((s, ex) -> {
						if (s.isEmpty()) {
							c.getSource().sendFeedback(Text.translatable("playerstats.error.unknown_player").formatted(Formatting.RED));
						} else {
							HypixelAbstractionLayer.getInstance().getPlayerDataApi().getAsync(s.get()).whenCompleteAsync((playerData, throwable) -> {
								if (playerData.isEmpty()) {
									c.getSource().sendFeedback(Text.translatable("playerstats.error.failed_data"));
									return;
								}

								handler.handler().accept(c.getSource(), s.get(), res.playerName(), playerData.get());
							}, MinecraftClient.getInstance());
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
