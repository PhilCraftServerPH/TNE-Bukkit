package com.github.tnerevival.commands.money;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.tnerevival.TNE;
import com.github.tnerevival.commands.TNECommand;
import com.github.tnerevival.core.Message;
import com.github.tnerevival.utils.MISCUtils;

public class MoneyBalanceCommand extends TNECommand {

	public MoneyBalanceCommand(TNE plugin) {
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
		return "tne.money.balance";
	}

	@Override
	public boolean console() {
		return false;
	}
	
	@Override
	public boolean execute(CommandSender sender, String[] arguments) {
		Player player = getPlayer(sender);
		Message balance = new Message("Messages.Money.Balance");
		balance.addVariable("$amount",  MISCUtils.formatBalance(player.getWorld().getName(), plugin.api.getBalance(player)));
		player.sendMessage(balance.translate());
		return true;
	}

	@Override
	public void help(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "/money balance - find out how much money you have on you");
	}
	
}