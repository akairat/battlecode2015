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
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class Drone extends Unit {

    //Few drones would be used for exploring the map. Essential variables for those. 
    //destination  null if it is not an explorer drone!

    //Path exploring variables
    public int mode = 0; //0= exploring destination on diagonal then theirHQ, 1 = spreading supply
    public boolean onDutyForSupply = false;
    public boolean searchedPath = false; //try to find path to its current destination;
    public boolean freePath = false; //if it is true, it guarantees that there is a way to reach its destination.
    public int pathTo;
    public String path; //just for debugging!
    public MapLocation oreAreaPoint = null;
    public double maxOreArea = 0;
    public int channel_maxOreX; //channelID +1
    public int channel_maxOreY; //channelID +2
    public int channel_maxOreAmount; //channelID +3
    public int channel_callOfSupply;
    public int channel_callOfSupplyX;
    public int channel_callOfSupplyY;


    public Direction[] allDirs = new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH,
            Direction.NORTH_EAST, Direction.NORTH_WEST, Direction.SOUTH_EAST, Direction.SOUTH_WEST};

    public HashSet<MapLocation> reachableSpots = new HashSet<MapLocation>();
    public ArrayList<MapLocation> recentPathRecord = new ArrayList<MapLocation>();

    public Drone(RobotController rc) throws GameActionException {
        super(rc);

        // Initialize channelID and increment total number of this RobotType
        channelStartWith = Channel_Drone;
        initChannelNum(); 
        supplyUpkeep = 10;
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

        channel_callOfSupply = channelID + 5;
        channel_callOfSupplyX =  channelID + 6;
        channel_callOfSupplyY = channelID + 7;

        pathTo = spawnedOrder %3; //would be used for broadcasting! BE CAREFUL

        if( pathTo ==1 || spawnedOrder >3 ){
            //ourHQ - > theirHQ
            destination = centerOfMap;
            path = "centerOfMap";
        }else if( pathTo ==2 ){
            destination = endCorner2;

            path = "endCorner";
        }else if( pathTo ==0 ){
            destination = endCorner1;
            path = "endCorner";
        }

        System.out.println("Destination: " + destination.x + " " + destination.y);

    }


    /**
     * Should be called only on explorer drones! If this drone has reached its
     * final destination, it should become non explorer ??
     * 
     * @throws GameActionException
     */



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
        int roundNum = Clock.getRoundNum();
        goAroungWithSpecialMode(destination);
        rc.yield();
    }

    //    public void hugoPlan(){
    //        try{
    //            int roundNum = Clock.getRoundNum();
    //            if(roundNum < 1700){
    //                if(roundNum < 400) // start exploring
    //                    explore();
    //                else if(roundNum < 1300){
    //                    if((roundNum / 40) % 5 == 0){
    //                        // retreat
    //                        MapLocation loc = rc.getLocation();
    //                        harassToLocation(loc.add(loc.directionTo(theirHQ).opposite()));
    //                    }
    //                    else { // advance
    //                        explore();
    //                    }
    //                }
    //                else  // advance
    //                    explore();
    //            }
    //                
    //            else{ // after round 1800
    //                startAttackingTowersAndHQ();
    //            }
    //        }
    //        catch(GameActionException e){
    //            e.printStackTrace();
    //        }
    //        rc.yield();
    //    }

    public void goAroungWithSpecialMode(MapLocation ml) throws GameActionException{
        RobotInfo nearestEnemy = senseNearestEnemy(rc.getType()) ;
        if (nearestEnemy != null) {
            int distanceToEnemy = rc.getLocation().distanceSquaredTo(
                    nearestEnemy.location);
            if (distanceToEnemy <= rc.getType().attackRadiusSquared) {
                attack();
                // attackRobot(nearestEnemy.location);
                avoid(nearestEnemy);
            } else {
                if (nearestEnemy.type == RobotType.DRONE) {
                    if (shouldStand) {
                        shouldStand = false; // waited once
                    } else {
                        moveToLocation(nearestEnemy.location);
                        shouldStand = true;
                    }
                } else {
                    avoid(nearestEnemy);
                }
            }
        } else {
            if (mode == 0){
                //expanding map range and exploring path.
                moveToLocationExtandingRange();
                System.out.println("Mode 0" + searchedPath);
            }else if( !searchedPath){
                System.out.println("SearchPATH" + Clock.getRoundNum());
                    findShortestPathAstar( myHQ, 10000 );
                    searchedPath = true;
                    System.out.println("searched!" + Clock.getRoundNum());
                
                //transfering supply
//                provideSupply();
            }
        }
    }


    public boolean locked(){
        int repetition = 0;
        for (int i =0; i< recentPathRecord.size(); i++){
            if (recentPathRecord.get(i).equals(rc.getLocation())){
                repetition +=1;
            }
        }
        if (repetition > 3){
            System.out.println("locked!") ;return true;}
        return false;
    }

    public void recordMovement(){
        recentPathRecord.add(rc.getLocation());
        if ( recentPathRecord.size() > 10){
            recentPathRecord.remove(0);
        }

    }

    public int numNormalsdAround(MapLocation ml){
        int numNormals = 0;

        TerrainTile loc = rc.senseTerrainTile(ml);
        if (loc.equals(TerrainTile.NORMAL)){
            numNormals +=1;
        }
        for (Direction dir: allDirs){
            loc = rc.senseTerrainTile(ml.add(dir));
            if (loc.equals(TerrainTile.NORMAL)){
                numNormals +=1;
            }
        }
        return numNormals;


    }

    public void provideSupply() throws GameActionException{
        
        if (rc.getSupplyLevel() < 100){
            destination = myHQ;
            moveToLocation(destination);
            onDutyForSupply = false;
        }else{
            if (onDutyForSupply){
                if (rc.getLocation().distanceSquaredTo(destination) < 10){
                    //structures are always 0 then never call drones.
                    RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),
                            GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
                    double lowestSupply = rc.getSupplyLevel();
                    double transferAmount = 0;
                    MapLocation suppliesToThisLocation = null;
                    for (RobotInfo ri : nearbyAllies) {
                        if (ri.supplyLevel < lowestSupply) {
                            lowestSupply = ri.supplyLevel;
                            transferAmount = (rc.getSupplyLevel() - ri.supplyLevel) / 2;
                            suppliesToThisLocation = ri.location;
                        }
                    }
                    if (suppliesToThisLocation != null) {
                        rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
                    }
                }
                //after transfer
                if (aroundAverageSupply() >500 || rc.getSupplyLevel() < 100 ){
                    destination = myHQ;
                    moveToLocation(destination);
                    onDutyForSupply = false;
                }
            }else{
                if (rc.readBroadcast(channel_callOfSupply) !=0 ){
                    int x = rc.readBroadcast(channel_callOfSupplyX);
                    int y = rc.readBroadcast(channel_callOfSupplyY);
                    destination = new MapLocation(x,y);
                    moveToLocation(destination);
                    rc.broadcast(channel_callOfSupply, 0);
                    onDutyForSupply = true;
                }
            }
        }

    }

    // move to location (Safe!)
    public void moveToLocation(MapLocation location) throws GameActionException {
        if (rc.isCoreReady()) {
            Direction dirs[] = getDirectionsTowardAndNext(location);

            for (Direction newDir : dirs) {
                if (rc.canMove(newDir)) {
                    if (!safeToMove2(rc.getLocation().add(newDir))
                            || !safeFromShortShooters(rc.getLocation().add(
                                    newDir))) {
                        continue;
                    } else if (rc.canMove(newDir)) {
                        if( !locked()){
                            rc.move(newDir);
                            recordMovement();
                        }
                    }
                }
            }
        }
    }



    // move to destination, avoiding enemies ------ MODE 0, 1;
    /**
     * Move to destination, inclined to direction where more normal terrainTiles exist.
     * If it was circulating somewhere (locked), change its destination. 
     * @throws GameActionException
     */
    public void moveToLocationExtandingRange() throws GameActionException {
        if (rc.isCoreReady()) {
            //Directions where normal exist
            MapLocation currentLoc = rc.getLocation();
            Direction[] dirs;
            Direction towardDest = currentLoc.directionTo(destination);
            MapLocation rightLoc = currentLoc.add(towardDest.rotateRight().rotateRight(), 2);
            MapLocation leftLoc = currentLoc.add(towardDest.rotateLeft().rotateLeft(), 2);
            MapLocation forward = currentLoc.add(towardDest, 2);
            int leftNormals = numNormalsdAround(leftLoc);
            int rightNormals = numNormalsdAround( rightLoc);
            int forwardNormals = numNormalsdAround(forward);
            int maxNormals = Math.max(Math.max(leftNormals, rightNormals), forwardNormals);

            if (currentLoc.distanceSquaredTo(destination) < 5 || locked()){
                if (destination.equals(theirHQ)){
                    mode = 1; //stop this execution! 
//                    System.out.println("change mode to" + mode);
                }else{
                    destination = theirHQ;
//                    System.out.println("to their HQ");
                }
            }else{
                if (forwardNormals == maxNormals || leftNormals == rightNormals ){
                    dirs = new Direction[]{ towardDest, towardDest.rotateLeft(), towardDest.rotateRight()};
                }else if ( leftNormals == maxNormals){
                    dirs = new Direction[]{towardDest.rotateLeft(),towardDest, towardDest.rotateRight()};
                }else{
                    dirs = new Direction[]{  towardDest.rotateRight(), towardDest, towardDest.rotateLeft()};
                }

                for (Direction newDir : dirs) {
                    if (rc.canMove(newDir)) {
                        if (!safeToMove2(rc.getLocation().add(newDir))
                                || !safeFromShortShooters(rc.getLocation().add(
                                        newDir))) {
                            continue;
                        } else if (rc.canMove(newDir)) {
                            rc.move(newDir);
                            return;
                        }
                    }
                }
                
                recordMovement(); //record where it ends.
            }
        }
    }


    // return the nearest enemy robot within radius
    public RobotInfo senseNearestEnemyWithin(int radiusSquared) {
        RobotInfo[] enemies = rc.senseNearbyRobots( radiusSquared, theirTeam);

        if (enemies.length > 0) {
            RobotInfo nearestRobot = null;
            int nearestDistance = Integer.MAX_VALUE;
            for (RobotInfo robot : enemies) {
                int distance = rc.getLocation().distanceSquaredTo(
                        robot.location);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestRobot = robot;
                }
            }

            if (nearestDistance > radiusSquared){
                return null;
            }
            return nearestRobot;
        }
        return null;
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