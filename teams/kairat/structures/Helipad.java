package kairat.structures;

import kairat.Structure;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Helipad extends Structure {

    public Helipad(RobotController rc) throws GameActionException {
        super(rc);

        // Initialize channelID and increment total number of this RobotType
        channelStartWith = Channel_Helipad;
        initChannelNum();
    }

    public void execute() throws GameActionException {
        spawnUnit(RobotType.MINER);
    }

}
