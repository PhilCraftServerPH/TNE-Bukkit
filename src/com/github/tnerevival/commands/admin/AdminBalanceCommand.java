package com.github.tnerevival.commands.admin;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.github.tnerevival.TNE;
import com.github.tnerevival.account.Account;
import com.github.tnerevival.commands.TNECommand;
import com.github.tnerevival.core.Message;
import com.github.tnerevival.core.api.MojangAPI;
import com.github.tnerevival.utils.AccountUtils;
import com.github.tnerevival.utils.MISCUtils;

public class AdminBalanceCommand extends TNECommand {
	
	public AdminBalanceCommand(TNE plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "balance";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getNode() {
		return "tne.admin.balance";
	}

	@Override
	public boolean console() {
		return true;
	}
	
	@Override
	public boolean execute(CommandSender sender, String[] arguments) {
		if(arguments.length == 1 || arguments.length == 2) {
			String world = (arguments.length == 2) ? arguments[1] : TNE.instance.defaultWorld;
			UUID playerID = MojangAPI.getPlayerUUID(arguments[0]);
			if(playerID != null && TNE.instance.manager.accounts.containsKey(playerID)) {
				Account acc = AccountUtils.getAccount(playerID);
				if(acc.getBalances().containsKey(world)) {
					Message balance = new Message("Messages.Admin.Balance");
					balance.addVariable("$player", arguments[0]);
					balance.addVariable("$world", world);
					balance.addVariable("$amount", MISCUtils.formatBalance(world, plugin.api.getBalance(Bukkit.getOfflinePlayer(playerID), world)));
					sender.sendMessage(balance.translate());
					return true;
				}
				Message noBalance = new Message("Messages.Admin.NoBalance");
				noBalance.addVariable("$player", arguments[0]);
				noBalance.addVariable("$world", world);
				sender.sendMessage(noBalance.translate());
				return false;
			}
			Message noPlayer = new Message("Messages.General.NoPlayer");
			noPlayer.addVariable("$player", arguments[0]);
			sender.sendMessage(noPlayer.translate());
			return false;
		}
		help(sender);
		return false;
	}

	@Override
	public void help(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "/theneweconomy balance <player> [world] - Check the specified player's balance for [world]");
	}
}