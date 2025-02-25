package com.github.tnerevival.commands.bank;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.tnerevival.TNE;
import com.github.tnerevival.commands.TNECommand;
import com.github.tnerevival.core.Message;
import com.github.tnerevival.utils.BankUtils;
import com.github.tnerevival.utils.MISCUtils;

public class BankPriceCommand extends TNECommand {
	
	public BankPriceCommand(TNE plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "price";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getNode() {
		return "tne.bank.price";
	}

	@Override
	public boolean console() {
		return false;
	}
	
	@Override
	public boolean execute(CommandSender sender, String[] arguments) {
		Player player = getPlayer(sender);
		Message cost = new Message("Messages.Bank.Cost");
		cost.addVariable("$amount",  MISCUtils.formatBalance(player.getWorld().getName(), BankUtils.cost(player.getWorld().getName())));
		player.sendMessage(cost.translate());
		return true;
	}

	@Override
	public void help(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "/bank price - Displays the price of a bank.");
	}
	
}