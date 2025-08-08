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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.axolotlclient.api.API;
import io.github.axolotlclient.api.Request;
import io.github.axolotlclient.api.Response;
import io.github.axolotlclient.util.CachedAPI;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class HypixelAbstractionLayer {
	private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	private static final int MAX_ATTEMPTS = 5;

	@AllArgsConstructor
	@Getter
	private enum RequestDataType {
		NETWORK_LEVEL("network_level"),
		BEDWARS_LEVEL("bedwars_level"),
		SKYWARS_EXPERIENCE("skywars_experience"),
		BEDWARS_DATA("bedwars_data"),
		PLAYER_DATA("player_data");
		private final String id;
	}

	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	@Getter
	private static final HypixelAbstractionLayer instance = new HypixelAbstractionLayer();

	private void queueRetry(int attempts, String desc, Request request, CompletableFuture<Optional<Response>> future, long count, TimeUnit unit) {
		service.schedule(() -> queueRequest0(attempts - 1, desc, request, future), count, unit);
	}

	private void queueRequest0(int attempts, String desc, Request request, CompletableFuture<Optional<Response>> future) {
		if (attempts <= 0) {
			future.complete(Optional.empty());
			return;
		}

		API.getInstance().get(request).whenComplete((response, throwable) -> {
			if (response == null) {
				API.getInstance().getLogger().warn("Failed to process request {}: ", desc, throwable);
				return;
			}

			if (response.getStatus() == 429) {
				API.getInstance().getLogger().warn("Failed to process request {}: rate limited", desc, throwable);

				queueRetry(
					attempts, desc, request, future,
					response.firstHeader("RateLimit-Reset").map(Long::parseLong).orElse(2L),
					TimeUnit.SECONDS
				);
			} else if (response.getStatus() != 200 || response.isError()) {
				API.getInstance().getLogger().warn("Failed to process request {} ({}): {}", desc, response.getStatus(), response.getBody());
			} else {
				future.complete(Optional.of(response));
			}
		});
	}

	private CompletableFuture<Optional<Response>> queueRequest(String desc, Request request) {
		CompletableFuture<Optional<Response>> future = new CompletableFuture<>();
		queueRequest0(MAX_ATTEMPTS, desc, request, future);
		return future;
	}

	private <V> CachedAPI<String, V> create(RequestDataType type, Function<Response, V> app) {
		return new CachedAPI<>(uuid -> {
			Request request = Request.Route.HYPIXEL
				.builder()
				.field("request_type", type.getId())
				.field("target_player", uuid)
				.build();

			return queueRequest("[%s, %s]".formatted(type.getId(), uuid), request).thenApply(opt -> opt.flatMap(res -> {
				try {
					return Optional.ofNullable(app.apply(res));
				} catch (Throwable e) {
					API.getInstance().getLogger().warn("Failed to parse request for {} (uuid={})", type.getId(), uuid);
					return Optional.empty();
				}
			}));
		}, 128, true);
	}

	private CachedAPI<String, Integer> createLevel(RequestDataType type) {
		return create(type, res -> {
			var level = res.<Number>getBody(type.getId()).intValue();
			return level == -1 ? null : level;
		});
	}

	private void freePlayerData(String uuid) {
		bedwarsDataApi.invalidate(uuid);
		networkLevelApi.invalidate(uuid);
		bedwarsLevelApi.invalidate(uuid);
		skywarsExpApi.invalidate(uuid);
	}

	@Getter
	private final CachedAPI<String, BedwarsData> bedwarsDataApi = create(RequestDataType.BEDWARS_DATA,
		res -> new BedwarsData(
			res.<Number>getBody("final_kills_bedwars").intValue(),
			res.<Number>getBody("final_deaths_bedwars").intValue(),
			res.<Number>getBody("beds_broken_bedwars").intValue(),
			res.<Number>getBody("deaths_bedwars").intValue(),
			res.<Number>getBody("kills_bedwars").intValue(),
			res.<Number>getBody("losses_bedwars").intValue(),
			res.<Number>getBody("wins_bedwars").intValue(),
			res.<Number>getBody("winstreak").intValue()
		)
	);

	@Getter
	private final CachedAPI<String, Integer> networkLevelApi = createLevel(RequestDataType.NETWORK_LEVEL);

	@Getter
	private final CachedAPI<String, Integer> bedwarsLevelApi = createLevel(RequestDataType.BEDWARS_LEVEL);

	@Getter
	private final CachedAPI<String, Integer> skywarsExpApi = create(RequestDataType.SKYWARS_EXPERIENCE,
		res -> {
			var exp = res.<Number>getBody(RequestDataType.SKYWARS_EXPERIENCE.getId()).intValue();
			if (exp == -1) {
				return null;
			}
			return Math.round(ExpCalculator.getLevelForExp(exp));
		});

	@Getter
	private final CachedAPI<String, PlayerData> playerDataApi = create(RequestDataType.PLAYER_DATA,
		response -> GSON.fromJson(response.getPlainBody(), PlayerData.class)
	);

	public void clearPlayerData() {
		bedwarsDataApi.invalidate();
		networkLevelApi.invalidate();
		bedwarsLevelApi.invalidate();
		skywarsExpApi.invalidate();
	}

	public void handleDisconnectEvents(UUID uuid) {
		freePlayerData(uuid.toString());
	}
}
