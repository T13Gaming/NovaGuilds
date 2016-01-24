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

package co.marcin.novaguilds.manager;

import co.marcin.novaguilds.NovaGuilds;
import co.marcin.novaguilds.basic.NovaGuild;
import co.marcin.novaguilds.enums.Config;
import co.marcin.novaguilds.enums.Lang;
import co.marcin.novaguilds.enums.Message;
import co.marcin.novaguilds.enums.Permission;
import co.marcin.novaguilds.util.LoggerUtils;
import co.marcin.novaguilds.util.StringUtils;
import co.marcin.novaguilds.util.Title;
import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageManager {
	private final NovaGuilds plugin = NovaGuilds.getInstance();
	private FileConfiguration messages = null;
	public String prefix;
	public ChatColor prefixColor = ChatColor.WHITE;
	public static MessageManager instance;
	private File messagesFile;

	/**
	 * The constructor
	 */
	public MessageManager() {
		instance = this;
	}

	/**
	 * Detects the language basing on Essentials and config
	 * @return false if detecting/creating new file failed
	 */
	public boolean detectLanguage() {
		detectEssentialsLocale();
		String lang = Config.LANG_NAME.getString();
		messagesFile = new File(plugin.getDataFolder() + "/lang", lang + ".yml");

		if(!messagesFile.exists()) {
			if(plugin.getResource("lang/" + lang + ".yml") != null) {
				plugin.saveResource("lang/" + lang + ".yml", false);
				LoggerUtils.info("New messages file created: " + lang + ".yml");
			}
			else {
				LoggerUtils.info("Couldn't find language file: " + lang + ".yml");
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if the messages file exists
	 * @return true if the file exists
	 */
	public boolean existsFile() {
		return messagesFile.exists();
	}

	/**
	 * Loads messages
	 * @return true if success
	 */
	public boolean load() {
		setupDirectories();
		
		if(!detectLanguage()) {
			return false;
		}

		try {
			messages = Lang.loadConfiguration(messagesFile);
		}
		catch(ScannerException | IOException e) {
			LoggerUtils.exception(e);
		}

		prefix = Message.CHAT_PREFIX.get();
		prefixColor = ChatColor.getByChar(ChatColor.getLastColors(prefix).charAt(1));

		return true;
	}

	/**
	 * Setups directories
	 */
	private void setupDirectories() {
		File langsDir = new File(plugin.getDataFolder(), "lang/");

		if(!langsDir.exists()) {
			if(langsDir.mkdir()) {
				LoggerUtils.info("Language dir created");
			}
		}
	}

	/**
	 * Detects Essentials' Locale
	 */
	public static void detectEssentialsLocale() {
		Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

		if(essentials != null && !Config.LANG_OVERRIDEESSENTIALS.getBoolean()) {
			if(essentials.getSettings() == null) {
				return;
			}

			String locale = essentials.getSettings().getLocale();
			if(locale.isEmpty()) {
				locale = "en";
			}

			if(ConfigManager.essentialsLocale.containsKey(locale)) {
				Config.LANG_NAME.set(ConfigManager.essentialsLocale.get(locale));
			}

			LoggerUtils.info("Changed lang to Essentials' locale: " + Config.LANG_NAME.getString());
		}
	}

	/**
	 * Gets message string from configuration
	 * @param message Message enum
	 * @return message string
	 */
	public static String getMessagesString(Message message) {
		String msg = StringUtils.fixColors(getMessages().getString(message.getPath()));

		return msg == null ? message.getPath() : msg;
	}

	/**
	 * Gets messages FileConfiguration
	 * @return Messages' FileConfiguration
	 */
	public static FileConfiguration getMessages() {
		return instance.messages;
	}

	/**
	 * Sends prefixed message to a player
	 * @param sender receiver
	 * @param msg message string
	 */
	public static void sendPrefixMessage(CommandSender sender, String msg) {
		if(!msg.equals("none")) {
			sender.sendMessage(StringUtils.fixColors(instance.prefix + msg));
		}
	}

	/**
	 * Sends a message without prefix to a player
	 * @param sender receiver
	 * @param msg message string
	 */
	public static void sendMessage(CommandSender sender, String msg) {
		if(!msg.equals("none")) {
			sender.sendMessage(StringUtils.fixColors(msg));
		}
	}

	/**
	 * Sends a list of messages to a player
	 * @param sender receiver
	 * @param message Message enum
	 */
	public static void sendMessagesList(CommandSender sender, Message message) {
		List<String> list = getMessages().getStringList(message.getPath());
		Map<String, String> vars = message.getVars();
		boolean prefix = message.isPrefix();

		if(list != null) {
			for(String msg : list) {
				if(vars != null) {
					msg = replaceMap(msg, vars);
				}

				if(prefix) {
					sendPrefixMessage(sender, msg);
				}
				else {
					sendMessage(sender, msg);
				}
			}
		}
	}

	/**
	 * Sends a message to a player
	 * @param sender receiver
	 * @param message Message enum
	 */
	public static void sendMessagesMsg(CommandSender sender, Message message) {
		String msg = getMessagesString(message);
		msg = replaceMap(msg, message.getVars());
		boolean title = message.getTitle();

		if(Config.USETITLES.getBoolean() && title && sender instanceof Player) {
			sendTitle((Player) sender, msg);
		}
		else {
			if(message.isPrefix()) {
				sendPrefixMessage(sender, msg);
			}
			else {
				sendMessage(sender, msg);
			}
		}
	}

	/**
	 * Send a Title to the player
	 * @param player Player instance
	 * @param msg message string
	 */
	public static void sendTitle(Player player, String msg) {
		Title title = new Title("");
		title.setSubtitleColor(instance.prefixColor);
		title.setSubtitle(StringUtils.fixColors(msg));
		title.send(player);
	}

	/**
	 * Broadcasts Message to players
	 * @param playerList List of Players
	 * @param message Message enum
	 * @param permission Permission enum (null for none)
	 */
	public static void broadcast(List<Player> playerList, Message message, Permission permission) {
		for(Player player : playerList) {
			if(permission == null || permission.has(player)) {
				message.send(player);
			}
		}
	}

	/**
	 * Broadcasts message from file to all players with permission
	 * @param message Message enum
	 * @param permission Permission enum
	 */
	public static void broadcast(Message message, Permission permission) {
		broadcast(new ArrayList<>(Bukkit.getOnlinePlayers()), message, permission);
	}

	/**
	 * Broadcasts message to all players
	 * @param message Message enum
	 */
	public static void broadcast(Message message) {
		broadcast(message, null);
	}

	/**
	 * Broadcasts message to guild members
	 * @param guild Guild instance
	 * @param message Message enum
	 */
	public static void broadcast(NovaGuild guild, Message message) {
		broadcast(guild.getOnlinePlayers(), message, null);
	}

	/**
	 * Replaces a map of vars preserving the prefix color
	 * @param msg message string
	 * @param vars Map<String, String> of variables
	 * @return String
	 */
	public static String replaceMap(String msg, Map<String, String> vars) {
		for(Map.Entry<String, String> entry : vars.entrySet()) {
			vars.put(entry.getKey(), entry.getValue() + NovaGuilds.getInstance().getMessageManager().prefixColor);
		}

		return StringUtils.replaceMap(msg, vars);
	}

	public void setMessages(YamlConfiguration messages) {
		this.messages = messages;
	}
}
