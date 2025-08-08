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

import io.github.axolotlclient.modules.hud.HudManager;
import io.github.axolotlclient.modules.hud.gui.hud.simple.ComboHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.effect.StatusEffect;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

	@Shadow
	public abstract float getHealth();

	@Shadow
	public abstract boolean hasStatusEffect(StatusEffect statusEffect);

	@Shadow
	public int defaultMaxHealth;

	public LivingEntityMixin(World world) {
		super(world);
	}

	@Inject(method = "damage", at = @At(value = "HEAD"))
	private void axolotlclient$onDamage(DamageSource source, float damage, CallbackInfoReturnable<Boolean> ci) {
		if (this.isInvulnerable(source)) {
			return;
		}
		if (getHealth() <= 0.0F) {
			return;
		} else if (source.isFire() && hasStatusEffect(StatusEffect.FIRE_RESISTANCE)) {
			return;
		}
		if (this.maxHealth > this.defaultMaxHealth / 2.0F) {
			return;
		}

		// The client doesn't really get any sort of information about why a person is damaged
		// Kinda sucks since that means combos can't be guaranteed (i.e. fall damage, or other person hits)
		// Possible fixes: Could wait for swing animation from a player to be played. Could then track eyes to see if hit, give or take
		// 2 ticks or so? Definitely not perfect tho
		if (source.getAttacker() instanceof PlayerEntity) {
			ComboHud comboHud = (ComboHud) HudManager.getInstance().get(ComboHud.ID);
			if (comboHud != null) {
				comboHud.onEntityDamage(this);
			}
		}
	}
}
