package iveel.structures;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import iveel.Structure;

public class MinerFactory extends Structure {

    public MinerFactory(RobotController rc) throws GameActionException {
        super(rc);    
       
        //Initialize channelID and increment total number of this RobotType
        channelStartWith = Channel_MinerFactory;
        initChannelNum();  

    }

    public void execute() throws GameActionException {
        spawnUnit(RobotType.MINER);
    }

}
