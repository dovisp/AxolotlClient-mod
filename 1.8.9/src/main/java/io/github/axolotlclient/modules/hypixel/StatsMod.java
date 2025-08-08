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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.api.API;
import io.github.axolotlclient.commands.ClientCommandInfo;
import io.github.axolotlclient.commands.ClientCommands;
import io.github.axolotlclient.commands.PlayerArgument;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.*;

import static io.github.axolotlclient.commands.ClientCommands.argument;
import static io.github.axolotlclient.commands.ClientCommands.literal;

public class StatsMod implements AbstractHypixelMod {
	private interface Handler {
		void accept(ClientCommandInfo ctx, String uuid, String username, PlayerData data);
	}

	private record Entry(String name, Handler handler) {
	}

	private static Text statText(String key, Object... args) {
		return new TranslatableText(key, Arrays.stream(args).map(s -> {
			if (s instanceof Float f) {
				return Formatting.GREEN + String.format("%.2f", f) + Formatting.RESET;
			} else {
				return Formatting.GREEN + s.toString() + Formatting.RESET;
			}
		}).toArray());
	}

	private static final List<Formatting> RAINBOW = List.of(
		Formatting.RED, Formatting.GOLD, Formatting.YELLOW, Formatting.GREEN, Formatting.AQUA, Formatting.LIGHT_PURPLE, Formatting.DARK_PURPLE
	);

	private static String formatBedwarsPrestige(int level) {
		String levelString = level + "☆";
		return switch (level / 100) {
			case 0 -> Formatting.GRAY + levelString;
			case 1 -> Formatting.WHITE + levelString;
			case 2 -> Formatting.GOLD + levelString;
			case 3 -> Formatting.AQUA + levelString;
			case 4 -> Formatting.DARK_GREEN + levelString;
			case 5 -> Formatting.DARK_AQUA + levelString;
			case 6 -> Formatting.DARK_RED + levelString;
			case 7 -> Formatting.LIGHT_PURPLE + levelString;
			case 8 -> Formatting.BLUE + levelString;
			case 9 -> Formatting.DARK_PURPLE + levelString;
			default -> IntStream.range(0, levelString.length())
				.mapToObj(x -> RAINBOW.get(x % RAINBOW.size()) + levelString.substring(x, x + 1))
				.collect(Collectors.joining());
		} + Formatting.RESET;
	}

	private static Text buildBedwarsGameMode(String key, PlayerData.Bedwars.BedwarsGameData data) {
		final var text = statText(key);

		final var hover = new LiteralText("");
		hover.append(statText("playerstats.bedwars.kdr", data.kills(), data.deaths(), data.kdr()));
		hover.append("\n");
		hover.append(statText("playerstats.bedwars.fkdr", data.finalKills(), data.finalDeaths(), data.fkdr()));
		hover.append("\n");
		hover.append(statText("playerstats.bedwars.beds", data.bedsBroken(), data.bedsLost(), data.bblr()));
		hover.append("\n");
		hover.append(statText("playerstats.bedwars.summary_short", data.wins(), data.losses(), data.wlr()));

		final var style = new Style();
		style.setColor(Formatting.GOLD);
		style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
		text.setStyle(style);
		return text;
	}

	private static Text buildBedwarsGameModesLine(PlayerData.Bedwars data) {
		final var text = new LiteralText("");

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

		final var hover = new LiteralText("");
		hover.append(statText("playerstats.skywars.kdr", data.kills(), data.deaths(), data.kdr()));
		hover.append("\n");
		hover.append(statText("playerstats.skywars.summary", data.wins(), data.losses(), data.wlr()));

		final var style = new Style();
		style.setColor(Formatting.GOLD);
		style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
		text.setStyle(style);
		return text;
	}

	private static Text buildSkywarsGameModesLine(PlayerData.Skywars data) {
		final var text = new LiteralText("");

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
		final var hover = new TranslatableText("playerstats.duels.mode_title",
			new TranslatableText("playerstats.duels." + entry.getKey())
				.setStyle(new Style().setColor(Formatting.GOLD)));
		hover.append("\n");
		hover.append(statText("playerstats.duels.kdr", value.kills(), value.deaths(), value.kdr()));
		hover.append("\n");
		hover.append(statText("playerstats.duels.summary", value.wins(), value.losses(), value.wlr(), value.winstreak()));

		return new TranslatableText("playerstats.duels." + entry.getKey())
			.setStyle(new Style()
				.setColor(Formatting.GOLD)
				.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
	}

	private static Text buildDuelsGameModesLine(PlayerData.DuelsData data) {
		final var text = new LiteralText("");
		boolean empty = true;
		for (Map.Entry<String, PlayerData.DuelsData.DuelsGameData> stringDuelsGameDataEntry : data.modes().entrySet()) {
			if (!empty) {
				text.append("\n");
			}
			Text buildDuelsGameMode = buildDuelsGameMode(stringDuelsGameDataEntry);
			text.append(new LiteralText("» ").setStyle(new Style().setColor(Formatting.RED)));
			text.append(buildDuelsGameMode);
			empty = false;
		}
		return text;
	}

	private static final List<Entry> HANDLERS = List.of(
		new Entry("bedwars", (c, uuid, username, data) -> {
			final var allStats = data.bedwars().all();
			c.sendMessage(
				new TranslatableText("playerstats.bedwars.title", data.formattedName(), formatBedwarsPrestige(data.bedwars().level())),
				statText("playerstats.bedwars.kdr", allStats.kills(), allStats.deaths(), allStats.kdr()),
				statText("playerstats.bedwars.fkdr", allStats.finalKills(), allStats.finalDeaths(), allStats.fkdr()),
				statText("playerstats.bedwars.beds", allStats.bedsBroken(), allStats.bedsLost(), allStats.bblr()),
				statText("playerstats.bedwars.summary", allStats.wins(), allStats.losses(), allStats.wlr(), allStats.winstreak()),
				buildBedwarsGameModesLine(data.bedwars())
			);
		}),
		new Entry("skywars", (c, uuid, username, data) -> {
			final var allStats = data.skywars().all();
			c.sendMessage(
				new TranslatableText("playerstats.skywars.title", data.formattedName(), data.skywars().level()),
				statText("playerstats.skywars.kdr", allStats.kills(), allStats.deaths(), allStats.kdr()),
				statText("playerstats.skywars.summary", allStats.wins(), allStats.losses(), allStats.wlr()),
				buildSkywarsGameModesLine(data.skywars())
			);
		}),
		new Entry("duels", (c, uuid, username, data) -> {
			c.sendMessage(
				new TranslatableText("playerstats.duels.title", data.formattedName()),
				buildDuelsGameModesLine(data.duels())
			);
		})
	);

	@Getter
	private static final StatsMod instance = new StatsMod();

	private final OptionCategory playerstats = OptionCategory.create("playerstats");

	@Override
	public void init() {
		final var command = literal("playerstats");

		for (Entry handler : HANDLERS) {
			command.then(literal(handler.name()).then(argument("player", PlayerArgument.player()).executes(c -> {
				if (!API.getInstance().getApiOptions().enabled.get()) {
					c.getSource().sendMessage(Formatting.RED + I18n.translate("playerstats.error.api_disabled"));
					return -1;
				}
				if (!API.getInstance().isAuthenticated()) {
					c.getSource().sendMessage(Formatting.RED + I18n.translate("api.error.unauthenticated"));
					return -1;
				}

				final var res = PlayerArgument.get(c, "player");

				res.uuid().whenCompleteAsync((uuid, ex) -> {
					if (uuid.isEmpty()) {
						c.getSource().sendMessageAsync(new LiteralText(Formatting.RED + I18n.translate("playerstats.error.unknown_player")));
					} else {
						HypixelAbstractionLayer.getInstance().getPlayerDataApi().getAsync(uuid.get()).whenCompleteAsync((playerData, throwable) -> {
							if (playerData.isEmpty()) {
								c.getSource().sendMessage(Formatting.RED + I18n.translate("playerstats.error.failed_data"));
								return;
							}

							handler.handler().accept(c.getSource(), uuid.get(), res.playerName(), playerData.get());
						}, Minecraft.getInstance()::submit);
					}
				});

				return 0;
			})));
		}

		final var node = ClientCommands.getDISPATCHER().register(command);
		ClientCommands.getDISPATCHER().register(literal("pstats").redirect(node));
	}

	@Override
	public OptionCategory getCategory() {
		return playerstats;
	}
}
