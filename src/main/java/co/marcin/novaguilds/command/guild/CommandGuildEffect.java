/*
 *     NovaGuilds - Bukkit plugin
 *     Copyright (C) 2015 Marcin (CTRL) Wieczorek
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package co.marcin.novaguilds.command.guild;

import co.marcin.novaguilds.basic.NovaPlayer;
import co.marcin.novaguilds.enums.Command;
import co.marcin.novaguilds.enums.Config;
import co.marcin.novaguilds.enums.GuildPermission;
import co.marcin.novaguilds.enums.Message;
import co.marcin.novaguilds.interfaces.Executor;
import co.marcin.novaguilds.util.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandGuildEffect implements Executor {
	private final Command command = Command.GUILD_EFFECT;

	public CommandGuildEffect() {
		plugin.getCommandManager().registerExecutor(command, this);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!command.hasPermission(sender)) {
			Message.CHAT_NOPERMISSIONS.send(sender);
			return;
		}

		if(!command.allowedSender(sender)) {
			Message.CHAT_CMDFROMCONSOLE.send(sender);
			return;
		}
		NovaPlayer nPlayer = plugin.getPlayerManager().getPlayer(sender);

		if(!nPlayer.hasGuild()) {
			Message.CHAT_GUILD_NOTINGUILD.send(sender);
			return;
		}

		if(!nPlayer.hasPermission(GuildPermission.EFFECT)) {
			Message.CHAT_GUILD_NOGUILDPERM.send(sender);
			return;
		}

		double price = plugin.getGroupManager().getGroup(sender).getGuildEffectPrice();

		if(nPlayer.getGuild().getMoney() < price) {
			Message.CHAT_GUILD_NOTENOUGHMONEY.send(sender);
			return;
		}

		//TODO: configurable duration
		int duration = Config.GUILD_EFFECT_DURATION.getInt();

		List<PotionEffectType> potionEffects = plugin.getConfigManager().getGuildEffects();

		int rand = NumberUtils.randInt(0, potionEffects.size() - 1);
		PotionEffectType effectType = potionEffects.get(rand);

		PotionEffect effect = effectType.createEffect(duration, 1);
		Player player = (Player)sender;

		//add effect
		if(player.hasPotionEffect(effectType)) {
			player.removePotionEffect(effectType);
		}

		for(Player gPlayer : nPlayer.getGuild().getOnlinePlayers()) {
			gPlayer.addPotionEffect(effect);
		}

		//remove money
		nPlayer.getGuild().takeMoney(price);

		//message
		Map<String, String> vars = new HashMap<>();
		vars.put("EFFECTTYPE",effectType.getName());

		Message.CHAT_GUILD_EFFECT_SUCCESS.vars(vars).send(sender);
	}
}
