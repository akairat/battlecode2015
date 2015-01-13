package team105.units;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import team105.Unit;
import team105.units.Drone.RobotHealthComparator;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class Drone extends Unit {

    //Few drones would be used for exploring the map. Essential variables for those. 
    //destination  null if it is not an explorer drone!

    //Path exploring variables
    public boolean searchAlongY = true;
    public int searchCoord;
    public boolean exploredDeadLock = false;
    public boolean freePath = false; //if it is true, it guarantees that there is a way to reach its destination.
    public int pathTo;
    public String path; //just for debugging!
    public MapLocation oreAreaPoint = null;
    public double maxOreArea = 0;
    public int channel_maxOreX; //channelID +1
    public int channel_maxOreY; //channelID +2
    public int channel_maxOreAmount; //channelID +3

    public HashSet<MapLocation> reachableSpots = new HashSet<MapLocation>();

    public Drone(RobotController rc) throws GameActionException {
        super(rc);

        // Initialize channelID and increment total number of this RobotType
        channelStartWith = Channel_Drone;
        initChannelNum(); 

        //differences coordX and coordY.
        if (Math.abs(myHQ.x - theirHQ.x) > Math.abs(myHQ.y - theirHQ.y) ){
            searchAlongY = false;
            searchCoord = rc.getLocation().x;

            //add free spots along x axis
            int y = rc.getLocation().y-3;
            for (int i = 0; i < 7; i++ ){
                TerrainTile t = rc.senseTerrainTile(new MapLocation(searchCoord, y));
                if (t == TerrainTile.NORMAL){
                    reachableSpots.add(new MapLocation(searchCoord, y));
                }
            }
        }else{
            searchCoord = rc.getLocation().y;
            //add free spots along y axis
            int x = rc.getLocation().x-3;
            for (int i = 0; i < 7; i++ ){
                TerrainTile t = rc.senseTerrainTile(new MapLocation(x, searchCoord));
                if (t == TerrainTile.NORMAL){
                    reachableSpots.add(new MapLocation(x, searchCoord));
                }
            }

        }
    }

    /**
     * Initialize channelNum AA BBB
     * 
     * Increment total number of this robot type.
     * 
     * @throws GameActionException
     */
    public void initChannelNum() throws GameActionException{
        int spawnedOrder = rc.readBroadcast(channelStartWith) + 1;
        rc.broadcast(channelStartWith, spawnedOrder);
        channelID = channelStartWith + spawnedOrder*10;
        channel_maxOreX = channelID + 1;
        channel_maxOreY = channelID + 2;
        channel_maxOreAmount = channelID + 3;
        //first three drones are going to explore map.

        pathTo = spawnedOrder %5; //would be used for broadcasting! BE CAREFUL
        // System.out.println("spawned order: " + spawnedOrder + " type: " + type);


        if( pathTo ==1  ){
            //ourHQ - > theirHQ
            destination = theirHQ;
            path = "centerOfMap";
        }else if( pathTo ==2 ){
            destination = endCorner2;
            path = "endCorner";
        }else if( pathTo ==3 ){
            destination = endCorner1;
            path = "endCorner";
        }else if( pathTo ==4 ){
            destination = middle1;
            path = "middle bwtn end and center";
        }else if( pathTo == 0){
            destination = middle2;
            pathTo = 5;
            path = "middle bwtn end and center";
        }

    }


    /**
     * Should be called only on explorer drones! If this drone has reached its
     * final destination, it should become non explorer ??
     * 
     * @throws GameActionException
     */

    public void explore() throws GameActionException{
        //System.out.println("destination -- " + exploreToDest);
        if (destination != null){
            //check if it has reached its destination
            MapLocation currentDest = rc.getLocation();

            int diff = currentDest.distanceSquaredTo(destination);
            //            System.out.println("difference -----" + Math.abs(diff));

            if  ( Math.abs(diff) < 6){
                destination = theirHQ;
                //                System.out.println("here we seee-------" + exploredDeadLock);

                if (!exploredDeadLock && channelID < 40051){
                    freePath = true;
                    broadcastExporation(1);
                    //                    rc.broadcast(Channel_FreePathFound, pathTo); 
                    // System.out.println("FOUND FREE PATH TO------ " + pathTo + " " + path + "\n channelID: " + channelID);
                }
                broadcastExporation(2);

            }

            harassToLocation(destination);
            extendExploration();
            //            System.out.println("here we seee-------");
        }

    }


    private void broadcastExporation(int status) throws GameActionException {
        int type = (channelID /10) %5;
        if( type ==1  ){
            //ourHQ - > theirHQ
            //            destination = theirHQ;
            rc.broadcast(Channel_PathCenter, status);
        }else if( type ==2 ){
            //            destination = endCorner2;
            rc.broadcast(Channel_PathCorner2, status);
        }else if( type ==3 ){
            //            destination = endCorner1;
            rc.broadcast(Channel_PathCorner1, status);
        }else if( type ==4 ){
            //            destination = middle1;
            rc.broadcast(Channel_PathMiddle1, status);
        }else if( type == 0){
            //            destination = middle2;
            rc.broadcast(Channel_PathMiddle2, status);


        }
    }

    public void execute() throws GameActionException {
        hugoPlan();
        rc.yield();
    }
    
    public void hugoPlan(){
        try{
            int roundNum = Clock.getRoundNum();
            if(roundNum < 1700){
                if(roundNum < 400) // start exploring
                    explore();
                else if(roundNum < 1300){
                    if((roundNum / 40) % 5 == 0){
                        // retreat
                        MapLocation loc = rc.getLocation();
                        harassToLocation(loc.add(loc.directionTo(theirHQ).opposite()));
                    }
                    else { // advance
                        explore();
                    }
                }
                else  // advance
                    explore();
            }
                
            else{ // after round 1800
                startAttackingTowersAndHQ();
            }
        }
        catch(GameActionException e){
            e.printStackTrace();
        }
        rc.yield();
    }
    
    public void startAttackingTowersAndHQ() throws GameActionException{
        MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
        
        MapLocation nearestAttackableTowerSafeFromHQ = nearestAttackableTowerSafeFromHQ(enemyTowers);
        
        if(nearestAttackableTowerSafeFromHQ != null){
            attackTower();
            moveToLocationSafeFromHQ(nearestAttackableTowerSafeFromHQ);
        }
        else if(enemyTowers.length > 0){
            attackTower();
            moveToLocationNotSafe(enemyTowers[0]);
        }
        else{
            attackTower();
            moveToLocationNotSafe(theirHQ);
        }
    }
    
    public MapLocation nearestAttackableTowerSafeFromHQ(MapLocation[] enemyTowers){
        MapLocation towerLocation = null;
        int distance = Integer.MAX_VALUE;
        MapLocation droneLocation = rc.getLocation();
        
        for(MapLocation location : enemyTowers){
            int tempDistance = droneLocation.distanceSquaredTo(location);
            if(tempDistance < distance && safelyAttackableFromHQ(location)){
                distance = tempDistance;
                towerLocation = location;
            }
        }
        
        return towerLocation;
    }
    
    public boolean safelyAttackableFromHQ(MapLocation location){
        return location.distanceSquaredTo(theirHQ) > 1;
    }
    
    public void harassStrategy(MapLocation ml) throws GameActionException {
        harassToLocation(ml);
    }

    public void player6() throws GameActionException {
        attackTower();
        moveAround();
    }

    public void swarmPot() throws GameActionException {
        //        attackLeastHealthEnemy();

        if (rc.isCoreReady()) {
            int rallyX = rc.readBroadcast(0);
            int rallyY = rc.readBroadcast(1);
            MapLocation rallyPoint = new MapLocation(rallyX, rallyY);

            Direction newDir = getMoveDir(rallyPoint);

            if (newDir != null) {
                rc.move(newDir);
            }
        }
    }

    
    /**
     * Check if it has found greater ore area than it had found before.
     * If so , broadcast it. (since that spot is reachable).
     * @throws GameActionException
     */
    public void checkOreArea() throws GameActionException{ 
        double tempMax = 0;
        Integer coordX = null;
        Integer coordY = null;
        for (MapLocation loc: reachableSpots){
            double sensedOre = rc.senseOre(loc);
            if (sensedOre > tempMax){
                tempMax = sensedOre;
                coordX = loc.x;
                coordY = loc.y;
            }
        }
        
        if (maxOreArea < tempMax){
        maxOreArea = tempMax;
        rc.broadcast(channel_maxOreAmount, (int) Math.ceil(maxOreArea));
        rc.broadcast(channel_maxOreX, coordX);
        rc.broadcast(channel_maxOreY, coordY);
        }
    }
    //    }

    public void extendExploration() throws GameActionException{
        //only first 5 drones should explore path!
        if (!exploredDeadLock && channelID < 40051){ 
            checkOreArea();
            //have not moved along y coord
            if (searchCoord == rc.getLocation().y){
                int x = rc.getLocation().x-3;
                for (int i = 0; i < 7; i++ ){
                    TerrainTile t = rc.senseTerrainTile(new MapLocation(x, searchCoord));
                    if (t == TerrainTile.NORMAL){
                        reachableSpots.add(new MapLocation(x, searchCoord));
                    }
                }
                //moved and has to level up reachableSpots  (Assuming drones not going back)  
            }else{
                HashSet<MapLocation> newReachableSpots = new HashSet<MapLocation>();
                for (MapLocation loc: reachableSpots){
                    for(int i = -1; i<=1; i++){
                        TerrainTile t = rc.senseTerrainTile(new MapLocation(loc.x + i, searchCoord));
                        if (t == TerrainTile.NORMAL){
                            newReachableSpots.add(new MapLocation(loc.x + i, searchCoord));
                        }
                    }
                }
                if (newReachableSpots.size() == 0){
                    exploredDeadLock = true;
                }
                reachableSpots = newReachableSpots;
            }
            searchCoord = rc.getLocation().y;
            //add free spots along y axis
            int x = rc.getLocation().x-3;
            for (int i = 0; i < 7; i++ ){
                TerrainTile t = rc.senseTerrainTile(new MapLocation(x, searchCoord));
                if (t == TerrainTile.NORMAL){
                    reachableSpots.add(new MapLocation(x, searchCoord));
                }
            }
        }
    }

    /**
     * Attack towers if it sees towers, otherwise attack enemy with lowest
     * health
     * 
     * @throws GameActionException
     */
    private void attackTower() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),
                rc.getType().attackRadiusSquared, rc.getTeam().opponent());

        int numberOfEnemies = nearbyEnemies.length;
        if (numberOfEnemies > 0) {
            MapLocation attackBuildingLocation = null;
            for (RobotInfo enemy : nearbyEnemies) {
                if (enemy.type == RobotType.TOWER) {
                    attackBuildingLocation = enemy.location;
                }
            }

            if (attackBuildingLocation != null) {
                if (rc.isWeaponReady()
                        && rc.canAttackLocation(attackBuildingLocation)) {
                    rc.attackLocation(attackBuildingLocation);
                }
            } else {
                Arrays.sort(nearbyEnemies, new RobotHealthComparator());
                if (rc.isWeaponReady()
                        && rc.canAttackLocation(nearbyEnemies[numberOfEnemies - 1].location)) {
                    rc.attackLocation(nearbyEnemies[numberOfEnemies - 1].location);
                }
            }
        }
    }

    /**
     * Comparator for the hit points of health of two different robots
     * (Ascending order)
     */
    static class RobotHealthComparator implements Comparator<RobotInfo> {

        // @Override
        public int compare(RobotInfo o1, RobotInfo o2) {
            if (o1.health > o2.health) {
                return 1;
            } else if (o1.health < o2.health) {
                return -1;
            } else {
                return 0;
            }
        }
    }



}