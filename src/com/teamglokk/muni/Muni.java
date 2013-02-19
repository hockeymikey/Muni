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

package com.teamglokk.muni;

import com.teamglokk.muni.commands.TownCommand;
import com.teamglokk.muni.commands.OfficerCommand;
import com.teamglokk.muni.commands.TownAdminCommand;
import com.teamglokk.muni.utilities.dbWrapper;
import com.teamglokk.muni.utilities.WGWrapper;
import com.teamglokk.muni.utilities.EconWrapper;
import com.teamglokk.muni.listeners.MuniLoginEvent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.util.HashMap;
import net.milkbowl.vault.economy.Economy;

import java.util.TreeMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import org.bukkit.command.CommandSender;

/**
 * Muni.java: Startup and shutdown for the Muni plugin
 * 
 * @author bobbshields
 */
public class Muni extends JavaPlugin {
    
    protected static WorldGuardPlugin wgp;
    public static WGWrapper wgwrapper = null;
            
    protected static Economy economy = null;
    public static EconWrapper econwrapper = null;
    
    public static dbWrapper dbwrapper = null;

    //Config file path  (Believe this is not needed, testing)
    //private static final String MUNI_DATA_FOLDER = "plugins" + File.separator + "Muni";
    //private static final String MUNI_CONFIG_PATH = MUNI_DATA_FOLDER + File.separator + "config.yml";
   
    //Global options to be pulled from config
    private static double CONFIG_VERSION = .01;
    private static boolean DEBUG = true;
    private static boolean SQL_DEBUG = true;
    private static boolean USE_OP = true;
    protected static boolean useMYSQL = false;
    
    private static String db_host = "jdbc:sqlite://localhost:3306/defaultdb";
    private static String db_database = "defaultdatabase";
    protected static String db_user = "defaultuser";
    protected static String db_pass = "defaultpass"; 
    protected static String db_prefix = "defaultpass"; 
    protected static String db_URL = null;
    public static String getDB_URL() { return db_URL;}
    public static String getDB_user() {return db_user;}
    public static String getDB_pass() { return db_pass;}
    public static String getDB_prefix() {return db_prefix; }
    
    protected static double maxTaxRate = 10000;
    protected static int rankupItemID = 19;
    protected static double maxTBbal = -1;
    protected static int totalTownRanks = 5;
    public static double getMaxTaxRate () { return maxTaxRate; }
    public static int getRankupItemID () { return rankupItemID; }
    public static double getMaxTBbal () { return maxTBbal; }
    public static int getTotalTownRanks () { return totalTownRanks; } 
    
    public static TownRank [] townRanks;
    
    //public static TreeSet<Town> towns = new TreeSet<Town>();
    public TreeMap<String, Town> towns = new TreeMap<String,Town>(String.CASE_INSENSITIVE_ORDER);
    public TreeMap<String,String> allCitizens = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Shut down sequence
     */
    @Override
    public void onDisable() {
        getLogger().info("Shutting Down");
        
        //Save Citizens and towns
        saveTowns();
        
        // Save the config to file
        this.saveConfig();
        
        getLogger().info("Shut Down sequence complete");
    }

    /**
     * Start up sequence
     */
    @Override
    public void onEnable() {
        getLogger().info("Starting Up");
        
        // Hooks in Vault Economy regions
        // Hooks in World Guard
        // Initializes Muni wrappers
        hookInDependencies();
        
        //Load the configuration file
        this.saveDefaultConfig(); // saves plugins/Muni/config.yml if !exists
        loadConfigSettings(); // parses the settings and loads into memory
        
        // Register Muni listener(s)
        getServer().getPluginManager().registerEvents(new MuniLoginEvent(this),this );

        // Register Muni commands
        getCommand("town"     ).setExecutor(new TownCommand     (this) );
        getCommand("deputy"   ).setExecutor(new OfficerCommand  (this) );
        getCommand("mayor"    ).setExecutor(new OfficerCommand  (this) );
        getCommand("townadmin").setExecutor(new TownAdminCommand(this) );
        
        //this.getLogger().info( Calendar.getInstance().getTime().toString() );
        
        boolean runDefault = false;
        if (runDefault){ // this is for testing purposes only, will be deleted  
            // Passing true drops the db first, normally false
            this.getLogger().warning("Dropping database!");
            dbwrapper.createDB(true);
            makeDefaultCitizens();
            makeTestTowns();
        }
        // Ensure the database is there but does not drop tables
        dbwrapper.createDB(false);
        
        this.getLogger().info ("Loading Towns from database");
        loadTowns();
        
        this.getLogger().info ("Loaded and Ready" );
    }// end: onEnable()
        
    /**
     * Queries DB for town names then town constructor loads towns individually 
     * 
     */
    public void loadTowns(){
        Town copyTown = new Town (this);
        try{
            if ( isDebug() ) { this.getLogger().info("Towns Loading. " ); }
            
            for (String curr : dbwrapper.getSingleCol("towns", "townName") ){
                if ( isDebug() ) { this.getLogger().info("Loading town: " + curr); }
                //Town copyTown = new Town (this);
                copyTown.loadFromDB( curr );
                towns.put(copyTown.getName(), new Town ( copyTown ) );
                copyTown = new Town (this); // trying to fix a bug with this
            }
        } catch (NullPointerException ex){
            this.getLogger().severe("Loading towns: "+ex.getMessage() );
            this.getLogger().info(copyTown.info());
        } finally {
            if ( isDebug() ) { this.getLogger().info("Finshed loading Towns"); }
        }
        // Now we'll iterate the towns once to load its citizens from the db
        for (Town t: towns.values() ){
            t.loadFromDB(t.getName() );
        }
    }
    
    /**
     * Returns the use_op config option, which allows/denies players with OP and no perms
     * @return 
     */
    public boolean useOP(){ return USE_OP; }
    public boolean isSQLdebug(){ return SQL_DEBUG; }
    public boolean useMysql() { return useMYSQL; } 
    
    /**
     * For testing only, will be deleted closer to the beta
     */
    public void makeTestTowns(){
        this.getLogger().info ("Making test towns");
        Town maker = new Town(this);
        maker = new Town (this,"TestTown","bobbshields",1,1005.0,100.0);
        //maker.setMaxDeputies(5); maker.setRank(0); Removed from the town class
        maker.setTaxRate(105.5);
        
        maker.saveToDB();
        
        maker = new Town (this,"SecondTest","astickynote",2,1000,100);
        maker.saveToDB();
        
        maker.loadFromDB("TestTown");
        this.getLogger().warning("Loaded from db: "+maker.toDB_UpdateRowVals() );
        
        maker = new Town (this,"SecondTest");
        this.getLogger().warning("Loaded from db: "+maker.toDB_UpdateRowVals() );
    }
    
    public void makeDefaultCitizens(){
        this.getLogger().info ("Making test citizens");
        Citizen maker = new Citizen(this);
        maker = new Citizen(this,"TestTown","bobbshields","mayor",null);
        maker.saveToDB();
        maker = new Citizen(this,"TestTown","tlunarrune","deputy",null);
        maker.saveToDB();
        maker = new Citizen(this,"TestTown","themoltenangel","invitee","bobbshields");
        maker.saveToDB();
        maker = new Citizen(this,"TestTown","efofex","citizen","bobbshields");
        maker.saveToDB();
        maker = new Citizen(this,"TestTown","clawson","applicant",null);
        maker.saveToDB();
        maker = new Citizen(this,"SecondTest","astickynote", "mayor", null);
        maker.saveToDB();
        maker = new Citizen(this,"SecondTest","astickynote", "mayor", null);
        maker.saveToDB();
        maker = new Citizen(this,"SecondTest","pharoahrhames", "deputy", null);
        maker.saveToDB();
        
    } 
    
    /**
     * Saves all towns to the database
     */
    public void saveTowns() {
        for (Town curr: towns.values()) {
            curr.saveToDB();
        }
    }
    
    /**
     * Adds a town to the collection
     * @param addition 
     */
    public void addTown( Town addition ) {
        towns.put(addition.getName(),addition);
    }
    
    /**
     * Removes a town from the collection
     * @param town_Name 
     */
    public void removeTown(String town_Name){
        Town temp = new Town (this,town_Name);
        if (towns.containsKey(temp.getName() ) ){
            towns.remove(temp);
        }
        // if in database, remove from database
    }
    /**
     * Searches for town by name
     * @param town_Name
     * @return  the Town if found, null if not
     */
    public Town getTown(String town_Name){
        Town temp = null;
        if (town_Name == null) {this.getLogger().info("getTown received a null string") ;}
        
        if (towns.containsKey(town_Name) ){
            temp = towns.get(town_Name); 
        } else { this.getLogger().info("Town search result: " +town_Name+" not found"  ); }
        
        return temp;
        
    }
    
    /**
     * Returns the town to which the player belongs
     * @param player
     * @return the town name where the player is a citizen
     */
    public String getTownName (String player){
        String temp ="";
        if ( allCitizens.containsKey( player ) ){
            temp = allCitizens.get( player );
        } else { this.getLogger().info("allCitizen: "+player+" not found"); }
        return temp;
    }
    
    /**
     * Gets whether the player is a citizen of any town
     * @param player
     * @return 
     */
    public boolean isCitizen (Player player){
        if (allCitizens.containsKey(player.getName() ) ){
            return true;
        } else { return false; } 
    }
    
    /**
     * Returns whether the player is a citizen of any town
     * @param player
     * @return 
     */
    public boolean isCitizen ( String player ){
        return allCitizens.containsKey(player);
    }
    
    /**
     * Gives a comma separated list of towns
     * @return 
     */
    public String getAllTowns(){
        String temp = "";
        for (Town curr: towns.values() ) {
            temp = temp + curr.getName() +", ";
        }
        return temp;
    }

    /**
     * Hooks into World Guard, Vault, and loads custom wrappers
     */
    private void hookInDependencies() {
        try {
            wgp = (WorldGuardPlugin) this.getServer().getPluginManager().getPlugin("WorldGuard");
            wgwrapper = new WGWrapper(this);
        } catch (Exception e) {
            getLogger().severe( "Error occurred in hooking in to WorldGuard. Are both WorldGuard and WorldEdit installed?");
            getLogger().severe( "!!!!!NOTICE!!!!! MUNI WILL NOW BE DISABLED.  !!!!!NOTICE!!!!!");
            this.getPluginLoader().disablePlugin(this);
        }

        try {
            boolean Econ_success = setupEconomy();
            if (!Econ_success) {
                getLogger().severe( "Muni: Unable to hook-in to Vault (Econ)!");
            }
        } catch (Exception e) {
            getLogger().severe( "Unable to hook-in to Vault: "+e.getMessage());
            getLogger().severe("!!!!!NOTICE!!!!! MUNI WILL NOW BE DISABLED.  !!!!!NOTICE!!!!!");
            this.getPluginLoader().disablePlugin(this);
        }
        dbwrapper = new dbWrapper(this);
        if ( isDebug() ) { getLogger().info( "Dependancies Hooked"); }
    }
    
    /**
     * Called by hookInDependancies(), Loads Vault and econwrapper
     * @return false if there was a problem
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        econwrapper = new EconWrapper(this);
        return (economy != null );
    }

    /**
     * Loads the config settings from config.yml in plugins/muni
     */
    protected void loadConfigSettings(){
        if (CONFIG_VERSION != this.getConfig().getDouble("config_version") ){
            getLogger().warning("Config version does not match software requirements.");
        }
        DEBUG = this.getConfig().getBoolean("debug");
        SQL_DEBUG = this.getConfig().getBoolean("sql_debug");
        USE_OP = this.getConfig().getBoolean("use_op");
        
        useMYSQL = this.getConfig().getBoolean("database.use-mysql");
        db_host = this.getConfig().getString("database.host");
        db_database = this.getConfig().getString("database.database");
        db_user = this.getConfig().getString("database.user");
        db_pass = this.getConfig().getString("database.password");
        db_prefix = this.getConfig().getString("database.prefix");
        
        // Format the URL from the private variables
        db_URL = useMysql() ? "jdbc:mysql"+"://"+ db_host +":3306/"+db_database 
                : "jdbc:sqlite:plugins/Muni/"+db_database+".db";
        
        if ( isDebug() ) {getLogger().info("dbURL = " + db_URL); }
                    
        maxTaxRate = this.getConfig().getDouble("townsGlobal.maxTaxRate"); 
        rankupItemID = this.getConfig().getInt("townsGlobal.rankupItemID");    
        maxTBbal = this.getConfig().getDouble("townsGlobal.maxTownBankBalance");  
        totalTownRanks = this.getConfig().getInt("townsGlobal.maxRanks"); 

        if ( isDebug() ) {getLogger().info( maxTaxRate + " " + rankupItemID +
                " " + maxTBbal + " " + totalTownRanks ); }
        
        townRanks = new TownRank [totalTownRanks+1];
        for ( int i=1; i <= totalTownRanks; i++ ){
            townRanks[i] = new TownRank( i,
                    this.getConfig().getString("townRanks."+(i)+".title"),
                    this.getConfig().getInt   ("townRanks."+(i)+".maxDeputies"),
                    this.getConfig().getInt   ("townRanks."+(i)+".minCitizens"),
                    this.getConfig().getInt   ("townRanks."+(i)+".maxCitizens"),
                    this.getConfig().getDouble("townRanks."+(i)+".moneyCost"),
                    this.getConfig().getInt   ("townRanks."+(i)+".itemCost") );
                    if ( isDebug() ) { getLogger().info( townRanks[i].getName()+
                            " config settings were loaded"); }
        }
        if ( isDebug() ) {getLogger().info("Config settings loaded"); }
        
   }
   
    /**
     * Whether the plugin should output verbose debugging info to the log
     * @return 
     */
   public boolean isDebug() { return DEBUG; }
   
   /**
    * Set the debug value about whether to output verbose to the log
    * @param value 
    */
   public void setDebug(boolean value){ 
       DEBUG = value; 
       this.getLogger().info("Debug changed to: " + String.valueOf(value) );
   }
    
    /**
     * Defaulted Override: debug=true color=White
     * @param player
     * @param msg 
     */
    public void out (CommandSender sender, String msg){
       out (sender,msg,true,ChatColor.WHITE);
    }
    /**
     * Defaulted Override: debug=true and color is passed. Whole message is given color
     * @param player
     * @param msg
     * @param color 
     */
    public void out (CommandSender sender, String msg, ChatColor color){
       out (sender,msg,true,color);
    }
    /**
     * Defaulted method: color=white and debug is passed.
     * @param player
     * @param msg
     * @param useConsole 
     */
    public void out (CommandSender sender, String msg, boolean useConsole){
       out (sender,msg,useConsole,ChatColor.WHITE);
    }
    /**
     * Real Work 
     * @param player
     * @param msg
     * @param useConsole
     * @param color
     * @return 
     */
    public boolean out (CommandSender sender, String msg, boolean useConsole, ChatColor color){
        boolean console = false;
        if (!(sender instanceof Player)) {
            console = true;
        }
        if (console && useConsole){
            sender.sendMessage(msg);
            return true;
        } else { 
            Player player = (Player) sender;
            player.sendMessage(color+msg);
            return true;
        }
    }
    /**
     * Defaulted Override: debug=true color=White
     * @param player
     * @param msg 
     */
    public void out (Player player, String msg){
       out (player,msg,true,ChatColor.WHITE);
    }
    /**
     * Defaulted Override: debug=true and color is passed. Whole message is given color
     * @param player
     * @param msg
     * @param color 
     */
    public void out (Player player, String msg, ChatColor color){
       out (player,msg,true,color);
    }
    /**
     * Defaulted method: color=white and debug is passed.
     * @param player
     * @param msg
     * @param useConsole 
     */
    public void out (Player player, String msg, boolean useConsole){
       out (player,msg,useConsole,ChatColor.WHITE);
    }
    /**
     * Real Work 
     * @param player
     * @param msg
     * @param useConsole
     * @param color
     * @return 
     */
    public boolean out (Player player, String msg, boolean useConsole, ChatColor color){
        if (player==null ) {return false; }
        if ( !player.isOnline() ) {
            if (useConsole) {
                this.getLogger().info(msg);
                return true;
            } else{ return false; }
        } else { 
            player.sendMessage(color+msg);
            return true;
        }
    }
}
