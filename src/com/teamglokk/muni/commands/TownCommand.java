/* 
 * Muni 
 * Copyright (C) 2013 bobbshields <https://github.com/xiebozhi/Muni> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Binary releases are available freely at <http://dev.bukkit.org/server-mods/muni/>.
*/
package com.teamglokk.muni.commands;

import com.teamglokk.muni.Citizen;
import com.teamglokk.muni.Muni;
import com.teamglokk.muni.Town;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;

import java.util.Iterator;
/**
 * Handler for the /town command.
 * @author BobbShields
 */
public class TownCommand implements CommandExecutor {
    private Muni plugin;
    private Player player;
    private boolean console = false;
    
    public TownCommand (Muni instance){
        plugin = instance;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
        if (split.length == 0){
            displayHelp(sender);
            return false;
        } else if (split[0].equalsIgnoreCase("list")) { //tested and working - 18 Feb 13
            if (split.length != 1) {
                plugin.out(sender,"/town list (no parameters allowed)");
                return false;
            }
            plugin.out(sender,"List of towns:");
            // iteration will be required here
            Iterator<Town> itr = plugin.towns.values().iterator();
            if (!itr.hasNext() ){
                plugin.out(sender,"No towns to check");
                return false;
            }
            while (itr.hasNext() ){
                Town current = itr.next();
                plugin.out(sender,current.getName() ) ;
            }
            return true;
        } else if (split[0].equalsIgnoreCase("info")) { //tested and mostly working - 18 Feb 13
            if(split.length>2){
                plugin.out(sender,"/town info <town_Name> "+ChatColor.RED+"OR" +ChatColor.WHITE+" /town info (this is for your own town",ChatColor.WHITE); 
                return false;
            } else if (split.length==1){
                if (sender instanceof Player && plugin.isCitizen(sender.getName() ) ) {
                    plugin.getTown(plugin.getTownName( sender.getName() ) ).info(sender);
                } else { plugin.out(sender, "You must specify a town"); }
            }else if (split.length == 2) { 
                // if ( plugin.isTown() ) // error occurs when invalid town name is entered
                plugin.getTown( split[1] ).info(sender);
            }
            return true;
        } else if (split[0].equalsIgnoreCase("help") ) { //tested and working - 18 Feb 13
            displayHelp(sender);
            return true;
        }  
        //End of console commands
        
        if (!(sender instanceof Player)) {
            console = true;
            sender.sendMessage("You cannot send that command from the console");
            return true;
        } else { player = (Player) sender; }
        
        if (split[0].equalsIgnoreCase("payTaxes")) { //tested and working - 18 Feb 13 
            Town temp = plugin.getTown( plugin.getTownName( player.getName() ) );
            if (split.length == 2 ) {
                Double amount = Double.parseDouble(split[1]);
                return temp.payTaxes(player, amount );
                 
            } else if ( split.length == 1 ){
                return temp.payTaxes(player);
                
            } else { return false; }
        } else if (split[0].equalsIgnoreCase("apply")) { //denies existing citizens, more testing needed - 28 Feb 13
            if (split.length != 2) {
                player.sendMessage("Incorrect number of parameters");
                return false;
            }
            if (!plugin.isCitizen(player.getName()) ){
                Town temp = plugin.getTown(split[1] );
                temp.apply ( player );
                player.sendMessage("Application to "+temp.getName()+" was sent.");
                return true;
            } else { 
                player.sendMessage("You are already engaged with "+plugin.allCitizens.get(player.getName() ) );
                player.sendMessage("To clear your status, do /town leave");
                return true;
            }
        } else if (split[0].equalsIgnoreCase("accept")) { //untested - 18 Feb
            if (split.length != 1) {
                player.sendMessage("/town accept (no parameters, do /town viewInvite)");
                return false;
            }
            Town temp = plugin.getTown( plugin.getTownName( player.getName() ) );
            temp.acceptInvite(player);
            
            return true;
        } else if (split[0].equalsIgnoreCase("viewInvite")) { //untested - 18 Feb
            if (split.length != 1) {
                player.sendMessage("/town viewInvite (no parameters)");
                return false;
            }
            if (!plugin.isCitizen(player) ) {
                plugin.out(player, "You are not engaged with any town.");
                return true;
            }
            Town temp = plugin.getTown( plugin.getTownName( player.getName() ) );
            if (temp.isInvited(player) ){
                plugin.out(player,"You are invited to "+temp.getName() ); 
            } else { 
                plugin.out(player,"You are not an invitee of " + temp.getName() );
            }
            return true;
        } else if (split[0].equalsIgnoreCase("leave")) { //infinite loop! - 18 Feb 
            Town temp = plugin.getTown( plugin.getTownName( player.getName() ) );
            temp.leave(player);
            return true;
        }else if (split[0].equalsIgnoreCase("sethome")) {
            player.sendMessage("Sethome not yet added.");
            return true;
        }else if (split[0].equalsIgnoreCase("vote")) {
            player.sendMessage("Voting not yet added.");
            return true;
        } else if (split[0].equalsIgnoreCase("bank")) { //tested and working - 28 Feb 13
            Town temp = plugin.getTown( plugin.getTownName( player.getName() ) );
            player.sendMessage(temp.getName()+" has bank balance of "+temp.getBankBal());
            return true;
        }  else if (split[0].equalsIgnoreCase("signCharter")) {
            player.sendMessage("Charters not yet enabled ");
            return true;
        } else {
            displayHelp(player);
            return false;
        }
    }
    private void displayHelp(CommandSender player){
        plugin.out( player,ChatColor.LIGHT_PURPLE+"Muni Help.  You can do these commands:");
        plugin.out( player, "/town list");
        plugin.out( player, "/town info <optional:townName");
        plugin.out( player, "/town apply <townName>");
        plugin.out( player, "/town viewInvite");
        plugin.out( player, "/town accept");
        plugin.out( player, "/town leave");
        //plugin.out( player,"/town sethome");
        //plugin.out( player,"/town signCharter");
        plugin.out( player, "/town payTaxes <optional: amount>");
        plugin.out( player, "/town bank (check the town bank balance)");
        plugin.out( player,"Future: /town vote");
    }
   
}
