package team105;

import java.util.Arrays;
import java.util.List;

import team105.BaseBot;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class Structure extends BaseBot {

    private static List<Direction> listOfDirections = Arrays.asList(
            Direction.EAST, Direction.NORTH, Direction.NORTH_EAST,
            Direction.NORTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST,
            Direction.SOUTH_WEST, Direction.WEST);

    public Structure(RobotController rc) {
        super(rc);
        supplyUpkeep = 0;
    }

    /**
     * 
     * @param type
     * @return
     */
    public Direction getSpawnDirection(RobotType type) {
        Direction[] dirs = getDirectionsToward(theirHQ);
        for (Direction d : dirs) {
            if (rc.canSpawn(d, type)) {
                return d;
            }
        }
        
        for (Direction d : dirs) {
            if (rc.canSpawn(d.opposite(), type)) {
                return d.opposite();
            }
        }
        
        return null;
    }

    public void spawnUnit(RobotType type) throws GameActionException {
        Direction randomDir = getRandomDirection();
        if (rc.isCoreReady() && rc.canSpawn(randomDir, type)) {
            rc.spawn(randomDir, type);
        }
    }
    


    /**
     * Method that will spawn a robot in the direction that has most amount of
     * ore around the building
     * 
     * @param type
     *            type of the robot to be generated
     * @throws GameActionException
     */
    private void spawnUnitOreCollector(RobotType type)
            throws GameActionException {

        MapLocation currentLocation = rc.getLocation();
        Direction richDirection = Direction.EAST;
        double oreAmount = rc.senseOre(currentLocation.add(richDirection));
        for (Direction d : listOfDirections) {
            if ((rc.senseOre(currentLocation.add(d)) > oreAmount)
                    && rc.canSpawn(richDirection, type)) {
                richDirection = d;
            }
        }

        if (rc.isCoreReady()) {
            rc.spawn(richDirection, type);
        }
    }
    

    /**
     * 
     * @param type
     * @return
     */
    public Direction getSpawnDirection(RobotType type, MapLocation dest) {
        Direction[] dirs = getDirectionsToward(dest);
        for (Direction d : dirs) {
            if (rc.canSpawn(d, type)) {
                return d;
            }
        }
        return null;
    }
}
