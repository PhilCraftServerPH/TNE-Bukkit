package com.github.tnerevival.commands.admin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.github.tnerevival.TNE;
import com.github.tnerevival.commands.TNECommand;
import com.github.tnerevival.utils.MISCUtils;

public class AdminReloadCommand extends TNECommand {
	
	public AdminReloadCommand(TNE plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getNode() {
		return "tne.admin.reload";
	}

	@Override
	public boolean console() {
		return true;
	}
	
	@Override
	public boolean execute(CommandSender sender, String[] arguments) {
		if(arguments.length < 2) {
			if(arguments.length == 0) {
				MISCUtils.reloadConfigurations("config");
				sender.sendMessage(ChatColor.WHITE + "Configurations reloaded!");
				return true;
			} else if(arguments.length == 1) {
				if(arguments[0].equalsIgnoreCase("all") || arguments[0].equalsIgnoreCase("config") || arguments[0].equalsIgnoreCase("messages") || arguments[0].equalsIgnoreCase("mobs") || arguments[0].equalsIgnoreCase("worlds")) {
					MISCUtils.reloadConfigurations(arguments[0]);
					String message = (arguments[0].equalsIgnoreCase("all"))? " All configurations reloaded." : arguments[0] + ".yml reloaded.";
					sender.sendMessage(ChatColor.WHITE + message);
					return true;
				}
			}
		}
		help(sender);
		return false;
	}

	@Override
	public void help(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "/theneweconomy reload <all/config/mobs/worlds> - reload the TNE configurations or reload the specified file");
	}
	
}