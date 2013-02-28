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
package com.teamglokk.muni.utilities;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import org.bukkit.util.Vector;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.databases.RegionDBUtil;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion.CircularInheritanceException;
import com.teamglokk.muni.Muni;
import com.teamglokk.muni.Town;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandException;
/**
 * Makes the World Guard Commands easier to work with
 * @author BobbShields
 */
public class WGWrapper extends Muni {
    private final Muni plugin;
    private WorldGuardPlugin wg = null;
    private RegionManager rm = null;
        
    /**
     * Default constructor
     * @param instance 
     */
    public WGWrapper(Muni instance) {
        plugin = instance;
        wg =  plugin.wgp; 
    }
    /**
     * Reminder to make this function in the future
     * @param worldName
     * @param regionName
     * @return 
     */
    public boolean createSchematic (String worldName, String regionName){
        
        return true;
    }
    /**
     * Deletes a region 
     * @param regionName
     * @return 
     */
    public boolean deleteRegion (String worldName, String regionName) {
        World world = plugin.getServer().getWorld(worldName);
        RegionManager mgr = wg.getGlobalRegionManager().get( world );
        
        if (!mgr.hasRegion(regionName)) {
            return false;
        }
        
        mgr.removeRegion(regionName);

        try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            plugin.getLogger().warning("Failed to delete region: "  + e.getMessage() );
        }
        return true; 
    }
    
    /**
     * This will be used to check a square area for other regions
     * @param player
     * @param radius
     * @param excludedRegions
     * @return 
     */
    public boolean checkSquareArea (Player player, int radius, List<String> excludedRegions){
        
        return true; 
    }
    
    
    /**
     * Expands the specified region in one direction by the rules defined in config
     * @return 
     */
    public int expandRegion ( String worldName, String regionName,
            String dir, int expansion) {
        
        if (expansion <= 0 ) { return -1; }
        
        World world = plugin.getServer().getWorld( worldName );
        RegionManager mgr = wg.getGlobalRegionManager().get( world );
        final ProtectedRegion existing = mgr.getRegion( regionName );
        ProtectedRegion newRegion;
        
        BlockVector min = existing.getMinimumPoint();
        BlockVector max = existing.getMaximumPoint();
        int x1 = min.getBlockX();
        int y1 = min.getBlockY();
        int z1 = min.getBlockZ();
        int x2 = max.getBlockX();
        int y2 = max.getBlockY();
        int z2 = max.getBlockZ();
        plugin.getLogger().info(min.toString() );
        plugin.getLogger().info(max.toString() );
        
        BlockVector newMin = new BlockVector(min);
        BlockVector newMax = new BlockVector(max);
        
        if ( dir.equalsIgnoreCase("n") || dir.equalsIgnoreCase("north") ){
            newMin.setZ( z2 - expansion );
        } else if ( dir.equalsIgnoreCase("s") || dir.equalsIgnoreCase("south") ){
            newMax.setZ( min.getBlockZ() + expansion ); 
        } else if ( dir.equalsIgnoreCase("e") || dir.equalsIgnoreCase("east") ){
            newMax.add(expansion,0,0); 
        } else if ( dir.equalsIgnoreCase("w") || dir.equalsIgnoreCase("west") ){
            newMin.add(-expansion,0,0); 
        } else if ( dir.equalsIgnoreCase("u") || dir.equalsIgnoreCase("up") ){
            newMax.add(0,expansion,0); 
        } else if ( dir.equalsIgnoreCase("d") || dir.equalsIgnoreCase("down") ){
            newMin.add(0,-expansion,0); 
        } else { return -1; }
        
        plugin.getLogger().info(newMin.toString() );
        plugin.getLogger().info(newMax.toString() );
        
        int area = (x2 - x1) * (z2 - z1);
        
        /*
        final CuboidSelection selection = new CuboidSelection(world, min, max);
        
        Location loc = player.getLocation();
        Vector shift = new Vector (halfSize,height,halfSize);
        Location loc1 = loc.add(shift);
        shift = new Vector (-halfSize,-5,-halfSize);
        Location loc2 = loc.add(shift);
        
        CuboidSelection sel = new CuboidSelection(world,loc1,loc2);
        BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
        BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
        * */
        newRegion = new ProtectedCuboidRegion(regionName, newMin, newMax);
            
        newRegion.setMembers(existing.getMembers());
        newRegion.setOwners(existing.getOwners());
        newRegion.setFlags(existing.getFlags());
        newRegion.setPriority(existing.getPriority());
        try {
            newRegion.setParent(existing.getParent());
        } catch (CircularInheritanceException ignore) {
        }

        mgr.addRegion(newRegion);
        
       try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            plugin.getLogger().warning("Failed to write region: "  + e.getMessage() );
        }
        return area;
    } 
    /**
     * Makes a square region of a specified size 
     * @param town
     * @param player
     * @param subRegionName
     * @return 
     */
    public ProtectedRegion makeSubRegion (Town town, Player player, String subRegionName, 
            int halfSize, int height ){
        World world = plugin.getServer().getWorld( town.getWorld() );
        RegionManager mgr = wg.getGlobalRegionManager().get( world );
        ProtectedRegion parent = mgr.getRegion( town.getName() );
        ProtectedRegion child = null;
            
        if ( makeRegion (player.getLocation(),subRegionName,halfSize,height,false) > 0 ){
            mgr = wg.getGlobalRegionManager().get( world );
            child = mgr.getRegion( subRegionName );
            setParent(parent,child);
        }
        try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            plugin.getLogger().warning("Failed to write region: "  + e.getMessage() );
        }
        return child;
    }
    
    
    /**
     * Makes a 25x(vert)x25 region centered on the player
     * @param player
     * @param regionName
     * @return 
     */
    public int makeTownBorder ( Player player, String regionName ) {
         RegionManager mgr = wg.getGlobalRegionManager().get(player.getWorld());
         if (!ProtectedRegion.isValidId(regionName ) ) {
            player.sendMessage("Region cannot be named: " +regionName+" (INVALID)" ) ;
            return -1; 
        }
        if (regionName.equalsIgnoreCase( "__global__" )){
            player.sendMessage("The region cannot be named __global__" ) ;
            return -1; 
        }
        if (mgr.hasRegion(regionName)) {
            player.sendMessage( "There is already a region by that name" );
            return -1;
        }
        return makeRegionVert (player.getLocation(), regionName, 12);
    }
    
    /**
     * Future planning: highlight with wool or glowstone along the ground at the border
     * may only replace grass, dirt, and stone along that path
     * needs to be a temporary function (client side if possible)
     * 
     * @param town
     * @return 
     */
    public boolean highlightTownBorder( Town town) {
        
        return true; 
    }
    
    /**
     * Makes a region that centered on the player
     * @param player
     * @param regionName
     * @return 
     */
    public int makeRegionVert ( Location loc, String regionName, int halfSize ) {
        if (!ProtectedRegion.isValidId( regionName ) ) {
            return -1; 
        }
        if (regionName.equalsIgnoreCase( "__global__" )){
            return -1; 
        }
        
        /*
        //Prepare the first corner
        Vector shift = new Vector (halfSize,0,halfSize);
        Location loc1 = loc.clone();
        loc1.add(shift);
        loc1.setY(loc.getWorld().getMaxHeight() );
        
        //Prepare the second corner
        shift = new Vector (-halfSize,0,-halfSize);
        Location loc2 = loc.clone();
        loc2.add(shift);
        loc2.setY(0);
        plugin.getLogger().info(loc1.toString());
        plugin.getLogger().info(loc2.toString());
        
        ProtectedRegion newRegion;
        CuboidSelection sel = new CuboidSelection(loc.getWorld(),loc1,loc2);
        BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
        BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
        newRegion = new ProtectedCuboidRegion(regionName, min, max);
            
        RegionManager mgr = wg.getGlobalRegionManager().get(loc.getWorld());
        
        if (mgr.hasRegion(regionName)) {
            return false;
        }
        mgr.addRegion(newRegion);

        try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            plugin.getLogger().warning("Failed to write region: "  + e.getMessage() );
        }
        return true; 
        */
        return makeRegion(loc, regionName,halfSize,0,true);
    }
    
    
    public int makeRegion ( Location loc, String regionName, int halfSize, int height , boolean vert) {
        if (!ProtectedRegion.isValidId( regionName ) ) {
            return -1; 
        }
        if (regionName.equalsIgnoreCase( "__global__" )){
            return -1; 
        }
        
        //Prepare the first corner
        Vector shift = new Vector (halfSize,0,halfSize);
        Location loc1 = loc.clone();
        loc1.add(shift);
        
        //Prepare the second corner
        shift = new Vector (-halfSize,0,-halfSize);
        Location loc2 = loc.clone();
        loc2.add(shift);
        
        if (vert){
            loc1.setY(loc.getWorld().getMaxHeight() );
            loc2.setY(0);
        } else {
            loc1.add( 0, height, 0 );
            loc2.add( 0, -12, 0 );
        }
        
        
        
        ProtectedRegion newRegion;
        CuboidSelection sel = new CuboidSelection(loc.getWorld(),loc1,loc2);
        BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
        BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
        newRegion = new ProtectedCuboidRegion(regionName, min, max);
            
        RegionManager mgr = wg.getGlobalRegionManager().get(loc.getWorld());
        
        if (mgr.hasRegion(regionName)) {
            return -1;
        }
        mgr.addRegion(newRegion);
        try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            plugin.getLogger().warning("Failed to write region: "  + e.getMessage() );
        }
        return sel.getArea(); 
    }
    
    /**
     * Makes a PVP-safe zone outside of any border that has the town as its primary region
     * @param town
     * @param player
     * @param subRegionName
     * @param SRdisplayName
     * @return 
     */
    public boolean makeOutpost (Town town, Player player, String subRegionName, String SRdisplayName) {
        boolean rtn = true;
        ProtectedRegion region = makeSubRegion(town,player,subRegionName,12,40);
        
        // require outside of any protections 
        
        return rtn;
    }
    
    /**
     * Makes a food regen region only inside of the town's border
     * @param town
     * @param player
     * @param subRegionName
     * @param SRdisplayName
     * @return 
     */
    public boolean makeRestaurant (Town town, Player player, String subRegionName, String SRdisplayName) {
        boolean rtn = true;
        ProtectedRegion region = makeSubRegion(town,player,subRegionName,4,20);
        
        // check inside main town
        // set food regen
        
        return rtn;
    }
    
    /**
     * Makes a health regen region only inside of the town's border
     * @param town
     * @param player
     * @param subRegionName
     * @param SRdisplayName
     * @return 
     */
    public boolean makeHospital (Town town, Player player, String subRegionName, String SRdisplayName) {
        boolean rtn = true;
        ProtectedRegion region = makeSubRegion(town,player,subRegionName,6, 5);
        
        // check inside main town
        // set heath regen
        
        return rtn;
    }
    
    /**
     * Makes a region inside a foreign town protection that is only buildable by the original town's members
     * @param town
     * @param player
     * @param subRegionName
     * @param SRdisplayName
     * @return 
     */
    public boolean makeEmbassy (Town town, Player player, String subRegionName, String SRdisplayName) {
        boolean rtn = true;
        ProtectedRegion region = makeSubRegion(town,player,subRegionName,10, 100);
        
        // require inside of another town's main protection (no embassy in an outpost)
        
        return rtn;
    }
    
    /**
     * Makes a PVP region only inside of the town's border
     * @param town
     * @param player
     * @param subRegionName
     * @param SRdisplayName
     * @return 
     */
    public boolean makeArena (Town town, Player player, String subRegionName, String SRdisplayName) {
        boolean rtn = true;
        ProtectedRegion region = makeSubRegion(town,player,subRegionName,74/2,150);
        
        // check inside main town
        // set PVP
        
        return rtn;
    }
    /**
     * Takes a list of players and ensures they are members of a region
     * @param worldName
     * @param regionName
     * @param players
     * @return 
     */
    public boolean makeMembers(String worldName, String regionName, List<String> players){
        if (players.size() == 0 ) {return false; } 
        
        World world = plugin.getServer().getWorld(worldName);
        RegionManager mgr = wg.getGlobalRegionManager().get( world );
        ProtectedRegion region = mgr.getRegion(regionName);

        if (region == null) {
            plugin.getLogger().info("Could not find a region called: "+regionName);
        }

        String [] newMembers = new String[players.size()];
        int i = 0;
        
        for (String localplayer : players ){
            if (!region.isMember(localplayer) ){
                newMembers[i++] = localplayer;
            }        
        }
        
        if (newMembers.length != 0) {
            RegionDBUtil.addToDomain(region.getMembers(), newMembers, 0);
        } else { return true; } //all listed members are already members
        
        try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            throw new CommandException("Failed to write regions: "
                    + e.getMessage());
        }
        return true; 
    }
    
    /**
     * Takes a list of players and ensures they are owners of a region
     * @param worldName
     * @param regionName
     * @param players
     * @return 
     */
    public boolean makeOwners(String worldName, String regionName, List<String>players){
        if (players.size() == 0 ) {return false; } 
        
        World world = plugin.getServer().getWorld(worldName);
        RegionManager mgr = wg.getGlobalRegionManager().get( world );
        ProtectedRegion region = mgr.getRegion(regionName);

        if (region == null) {
            plugin.getLogger().info("Could not find a region called: "+regionName);
        }

        String [] newOwners = new String[players.size()];
        int i = 0;
        
        for (String localplayer : players ){
            if (!region.isOwner(localplayer) ) {
                newOwners[i++] = localplayer;
            }
        }
        if (newOwners.length != 0) {
            RegionDBUtil.addToDomain(region.getOwners(), newOwners, 0);
        } else {return true;} // all listed owners are already owners
        
        try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            throw new CommandException("Failed to write regions: "
                    + e.getMessage());
        }
        return true; 
    }
    
    /**
     * Establishes the parent / child relationship
     * @param child
     * @param parent
     * @return 
     */
    public boolean setParent (ProtectedRegion parent, ProtectedRegion child){
        try{
            child.setParent(parent);
            return true;
        } catch (CircularInheritanceException e){
            plugin.getLogger().severe("Could not set "+parent.getId()+" as parent region for " +child.getId() );
            plugin.getLogger().severe(e.getMessage() );
            return false; 
        }
    }
    
    public String getParent (ProtectedRegion child){
        return child.getParent().getId();
    }
    
    /**
     * Returns the valid region from the world guard manager
     * @param worldName
     * @param regionName
     * @return 
     */
    public ProtectedRegion getRegion (String worldName, String regionName){
        ProtectedRegion rtn = null;
        World world = plugin.getServer().getWorld(worldName);
        RegionManager mgr = wg.getGlobalRegionManager().get( world );
        
        rtn = mgr.getRegion(regionName);
        
        return rtn;
    }
    /**
     * Gets a list of regions at the player's current location 
     * @param player
     * @return 
     */
    public String getRegions (Player player)
    {
        //return this.wgp.getRegionManager(null).
         //       canBuild(player,
          //  player.getLocation().getBlock().getRelative(0, -1, 0));
        return "nothing";
    }
    
    
    /**
     * Checks the World Guard build permission at the player's current location
     * @param player
     * @return 
     */
    public boolean checkBuildPerms (Player player)
    {
        return this.wgp.canBuild(player,
            player.getLocation().getBlock().getRelative(0, -1, 0));
    }
    
   public ApplicableRegionSet getARS(Player player) {
        return plugin.wgp.getRegionManager( player.getWorld() )
                .getApplicableRegions(player.getLocation() );
    }

   public void setGreeting (ProtectedRegion region, String msg ){
       setFlag(region, DefaultFlag.GREET_MESSAGE, msg);
   }
   /**
    * Gets whether the player can use the specified flag at his/her location
    * @param player
    * @param flag
    * @return 
    */
    public boolean isFlagged (Player player, StateFlag flag ){
        //region.getFlag(flag, flag.parseInput(plugin.wgp, player, flag));
        return getARS(player).allows(flag);
    }
    
    /**
     * Sets the flag for the region 
     * @param region
     * @param flag
     * @param value
     * @return 
     */
    public void setFlag (ProtectedRegion region, Flag flag, String value){
            //Check that the user has ownership
            region.setFlag(flag,value );
        
    }
    
    /**
     * Sets the boolean flag for the region 
     * @param region
     * @param flag
     * @param value
     * @return 
     */
    public boolean setflag (ProtectedRegion region, Flag flag, boolean value){
        region.setFlag(flag, value);
        return true;
    }
    
    /**
     * Sets the Entry message for the region 
     * @param region
     * @param message
     * @return 
     */
    public boolean setMSG_Entry(ProtectedRegion region,String message){
        
        return true;
    }
    
    /**
     * Sets the Exit message for the region 
     * @param region
     * @param message
     * @return 
     */
    public boolean setMSG_Exit(ProtectedRegion region, String message) {
        
        return true; 
    }
    
    /**
     * Sets the PVP flag for the region to the value of the toggle parameter
     * @param region
     * @param toggle
     * @return 
     */
    public boolean setPVP(ProtectedRegion region, boolean toggle){
        
        return true; 
    }
    
    /**
     * Sets the health regeneration of the region to a predetermined value 
     * @param region
     * @return 
     */
    public boolean setRegen_Health(ProtectedRegion region){
        
        return true; 
    }
    
    /**
     * Sets the food regeneration of the region to a predetermined value 
     * @param region
     * @return 
     */
    public boolean setRegen_Food(ProtectedRegion region){
        
        return true; 
    }
    
    /**
     * Sets all the flags to a basic state
     * @param region
     * @return 
     */
    public boolean resetToStandardFlags(ProtectedRegion region){
        
        return true; 
    }

}
