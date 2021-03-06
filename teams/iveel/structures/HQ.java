package iveel.structures;

import java.util.HashMap;

import battlecode.common.*;
import iveel.Structure;



/*BaseBot represents Unit and Structure.
 * General:
 * 
 * Starts with 500 ore, and each team automatically receives 5 ore per turn before any mining income
 * 
 * 
 * 
 * USE OF  CHANNELS.
 * 1. 
 * 2. Number of spawned beavers.
 * 3. 
 * 4. Path explorer with right preference
 * 5. Path explorer with left preference
 * 6.
 * 7. Barrack   4; 200-700
 * 8. Miner factory 3; 0-300
 * 9. HandwashStation 2; 1000-1300
 * 10. Helipad 2; 500-1000
 * 11. Tank factory 4; 700-1200
 * 12. Aerospace lab 2; 1000-1700
 * 13.
 * 
 *  == HQ Channel:
 *   A BBB 
 *   A:  Always 1 (making it different from structures).
 *   BBB:  up this stucture's management. 
 *   Handling producing armies (swarm pots)
 *   
 *  ARMY MODE CHANNEL: 1000 (all barraks listen to this channel to decide to build armies).
 *   If it broadcasts 0, then not building any army.
 *   Otherwise, it is new army's channel number.
 *   For each army channel 2___:
 *       2__0: broadcasting total number of that army units.
 *       2__1: x coordinate of destination.
 *       2__2: y coordinate of destination.
 *       2__3: AA BBB ->  AA = quantity limit (<65) and BBB = time limit (< 1000)
 *       
 *       2__5:  1 if each army unit should work on its own. 
 *              0 if units should go to specified destination.
 *       
 *   
 *   
 * 
 */
public class HQ extends Structure{
    
   //Keep track all info about armies and their last dest.
   //Each army unit listens its army channel which is unique.
    public int MAX_NUM_ARMIES = 99;
    public HashMap<Integer, MapLocation> armies = new HashMap<Integer, MapLocation>();
    public int timeLimetBuildingArmy = 0;

    
    public MapLocation centerOfMap;
//    public HashMap

    public HQ(RobotController rc) throws GameActionException {
        super(rc);
        channelStartWith = 10;

        centerOfMap = new MapLocation((this.myHQ.x + this.theirHQ.x) / 2,
                (this.myHQ.y + this.theirHQ.y) / 2);
        
        //set number of structures to 0
//        * 7. Barrack   4; 200-700
//        * 8. Miner factory 3; 0-300
//        * 9. HandwashStation 2; 1000-1300
//        * 10. Helipad 2; 500-1000
//        * 11. Tank factory 4; 700-1200
//        * 12. Aerospace lab 2; 1000-1700
  

    }
    
    
    /**
     * Order building armies and broadcast it.
     * To stop building that particular army, one should call stopBuildArmy()
     * @param dest
     * @throws GameActionException
     */
    public void startBuildArmy(MapLocation dest) throws GameActionException{
        if (armies.size() <  MAX_NUM_ARMIES){
            int armyChannel = newArmyGetChannelID();
            armies.put(armyChannel, dest);
            rc.broadcast(Channel_ArmyMode, armyChannel);
            giveDestinationToArmy(armyChannel, dest);
        }
    }
    
    public void giveDestinationToArmy(int armyChannel, MapLocation dest) throws GameActionException{
        rc.broadcast(armyChannel +1 , dest.x); 
        rc.broadcast(armyChannel +2, dest.y); 
        
    }
    
    
    /**
     * If building any army, stops that process.
     * @throws GameActionException
     */
    public void stopBuildArmy() throws GameActionException{
        rc.broadcast(Channel_ArmyMode, 0);
    }
    
    /**
     * If the game is army mode, then HQ stops any kind of army building 
     * if specified time or quantity of specified army units is exceeded. 
     * @param untilTurn
     * @param quantityLimit
     * @throws GameActionException 
     * @return true if stopBuildArmy is called.(Does not necessarily mean there was army building).
     */
    public boolean stopArmyBuildingRestrictedTo(int armyChannel, int untilTurn, int quantityLimit) throws GameActionException{
        if (Clock.getRoundNum() > untilTurn){
            stopBuildArmy();
            return true;
        }else if (rc.readBroadcast(armyChannel) > quantityLimit){
            stopBuildArmy();
            return true;
        }
        return false;
        
    }
    
    
    /**
     * Send all armies to a targeted destination.
     * @param dest
     * @throws GameActionException 
     */
    public void sendAllArmiesToDest(MapLocation dest) throws GameActionException{
        for( int armyChannel: armies.keySet()){
            rc.broadcast(armyChannel +1, dest.x);
            rc.broadcast(armyChannel +2, dest.y);
        }
    }
    
    
    public void freeArmyUnits(int armyChannel){
        
    }
    
    public void formBackArmyUnits( int armyChannel){
        
    }
    
    
    

    public void execute() throws GameActionException {

//        armyMode(); // Example of building 3 swarmPot armies!!
        
//      swarmPot();
//        buildArmy(10, RobotType.BEAVER, centerOfMap, 1000);
    }

    /**
     * Player 6 example
     * 
     * @throws GameActionException
     */
    public void player6() throws GameActionException {
        attackEnemyZero();
        spawnUnit(RobotType.BEAVER);

        transferSupplies();

    }

    /**
     * Swarm pot example
     * 
     * @throws GameActionException
     */
    public void swarmPot() throws GameActionException {
        int numBeavers = rc.readBroadcast(2);

        if (rc.isCoreReady() && rc.getTeamOre() > 100 && numBeavers < 10) {
            Direction newDir = getSpawnDirection(RobotType.BEAVER, theirHQ);
            if (newDir != null) {
                rc.spawn(newDir, RobotType.BEAVER);
                rc.broadcast(2, numBeavers + 1);
            }
        }
        MapLocation rallyPoint;
        if (Clock.getRoundNum() < 1400) {
            rallyPoint = centerOfMap;
        } else {
            rallyPoint = this.theirHQ;
        }
        rc.broadcast(0, rallyPoint.x);
        rc.broadcast(1, rallyPoint.y);
    }
    
    
    /**
     * Example of building 3 swarmPot armies!!
     * @throws GameActionException
     */
    public void armyMode() throws GameActionException {
        int numBeavers = rc.readBroadcast(2);

        if (rc.isCoreReady() && rc.getTeamOre() > 100 && numBeavers < 10) {
            Direction newDir = getSpawnDirection(RobotType.BEAVER, theirHQ);
            if (newDir != null) {
                rc.spawn(newDir, RobotType.BEAVER);
                rc.broadcast(2, numBeavers + 1);
            }
        }
        
//        stopBuildArmy();
//        if(Clock.getRoundNum() == 100){startBuildArmy(centerOfMap);}
        
        int currentTurn = Clock.getRoundNum();
        if(currentTurn == 100 || currentTurn == 900 ){
            stopBuildArmy();
            startBuildArmy(centerOfMap);
            
        }else if (currentTurn == 700 ){
            stopBuildArmy();
            int x = centerOfMap.x - 10;
            int y = centerOfMap.y + 10;
                    
            MapLocation dest = new MapLocation(x,y);
            startBuildArmy(dest);
            
//        }else if (currentTurn == 700  ){
//            stopBuildArmy();
//            int x = centerOfMap.x + 10 ;
//            int y = centerOfMap.y - 10;  
//            MapLocation dest = new MapLocation(x,y);
//            startBuildArmy(dest);
        }else if (currentTurn == 1100){
            stopBuildArmy();
            startBuildArmy(centerOfMap);
            sendAllArmiesToDest(theirHQ);
        }
        
        
    }


}
