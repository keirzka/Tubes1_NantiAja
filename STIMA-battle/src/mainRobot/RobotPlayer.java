package mainRobot;

import java.util.Random;

import javax.management.MalformedObjectNameException;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm alive");

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the UnitType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()){
                    case SOLDIER: runSoldier(rc); break; 
                    case MOPPER: runMopper(rc); break;
                    case SPLASHER: break; // Consider upgrading examplefuncsplayer to use splashers!
                    default: runTower(rc); break;
                    }
                }
             catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for towers.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static int robotMadeCount = 1;
    static MapLocation paintTowerLocation = null;
    public static void runTower(RobotController rc) throws GameActionException{
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
    
        MapLocation nextLoc = rc.getLocation().add(dir);
        // Bikin 2/3 robot soldier, 1/3 robot mopper
        if (robotMadeCount % 3 == 0 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)) {
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            System.out.println("BUILT A MOPPER");
            paintTowerLocation = rc.getLocation();
            robotMadeCount++;
        }
        else if (robotMadeCount % 3 != 0 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)) {
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            System.out.println("BUILT A SOLDIER");
            robotMadeCount++;
        }

        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }

        // TODO: can we attack other bots?
        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (nearbyEnemy != null) {
            // attack single target
            MapLocation lowestHPEnemyLocation = null;
            int curMax = 999999999;

            for (RobotInfo enemy : nearbyEnemy) {
                if (enemy.getHealth() < curMax) {
                    curMax = enemy.getHealth();
                    lowestHPEnemyLocation = enemy.getLocation();
                }
            }

            if (rc.canAttack(lowestHPEnemyLocation)) {
                rc.attack(lowestHPEnemyLocation);
            }

            // terus attack aoe
            if (rc.canAttack(null)) {
                rc.attack(null);
            }

        }
        
    }


    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runSoldier(RobotController rc) throws GameActionException{
        // jadiin ruin ato upgrade kalo bisa
        for (Direction dir : Direction.values()) {
            MapLocation adjacent = rc.getLocation().add(dir);
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, adjacent)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, adjacent);
                }
                else if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, adjacent)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, adjacent);
                }
                else if (rc.canUpgradeTower(adjacent)) {
                    rc.upgradeTower(adjacent);
                }
            }
        
        // cari tower musuh
        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation enemyTowerLocation = null;
        for (RobotInfo robo : nearbyEnemy) {
            if (robo.type != UnitType.SOLDIER && robo.type != UnitType.MOPPER && robo.type != UnitType.SPLASHER) {
                enemyTowerLocation = robo.getLocation();
                break;
            }
        }

        // cari mark ruin yang dah ada
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapInfo markRuin = null;
        for (MapInfo tile : nearbyTiles) {
            if (tile.getMark() != PaintType.EMPTY && tile.getMark() != tile.getPaint() && (tile.getPaint() != PaintType.ENEMY_PRIMARY && tile.getPaint() != PaintType.ENEMY_SECONDARY)) {
                markRuin = tile;
                break;
            }
        }


        // cari ruin baru
        RobotInfo[] alliedRobots = rc.senseNearbyRobots(-1, rc.getTeam()); 
        MapLocation targetRuinLocation = null;
        for (MapInfo tile : nearbyTiles) {
            if (tile.hasRuin()) {
                // cek ruin nya itu tower ally apa bukan
                boolean hasTower = false;
                for (RobotInfo robot : alliedRobots) {
                    if (robot.getLocation().equals(tile.getMapLocation()) && robot.type.isTowerType()) {
                        hasTower = true;
                        break;
                    }
                }

                // kalo baru, pilih jadi tempat ruin
                boolean boleh = true;

                if (rc.canSenseLocation(tile.getMapLocation().add(Direction.EAST))) {
                    boleh = rc.senseMapInfo(tile.getMapLocation().add(Direction.EAST)).getMark() == PaintType.EMPTY;
                }
                if (rc.canSenseLocation(tile.getMapLocation().add(Direction.SOUTH))) {
                    boleh = rc.senseMapInfo(tile.getMapLocation().add(Direction.SOUTH)).getMark() == PaintType.EMPTY;
                }
                if (rc.canSenseLocation(tile.getMapLocation().add(Direction.WEST))) {
                    boleh = rc.senseMapInfo(tile.getMapLocation().add(Direction.WEST)).getMark() == PaintType.EMPTY;
                }
                if (rc.canSenseLocation(tile.getMapLocation().add(Direction.NORTH))) {
                    boleh = rc.senseMapInfo(tile.getMapLocation().add(Direction.NORTH)).getMark() == PaintType.EMPTY;
                }

                if (!hasTower) {
                    if (boleh) {
                        targetRuinLocation = tile.getMapLocation();
                    }
                }
            }
        }

        if (enemyTowerLocation != null) {
            if (rc.canAttack(enemyTowerLocation)) {
                rc.attack(enemyTowerLocation);
            }
            else {
                Direction dir = rc.getLocation().directionTo(enemyTowerLocation);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
        else if (markRuin != null) {
            boolean useSecondaryColor = (markRuin.getMark() == PaintType.ALLY_SECONDARY);
            if (rc.canAttack(markRuin.getMapLocation())) {
                rc.attack(markRuin.getMapLocation(), useSecondaryColor);
            }
            else {
                Direction dir = rc.getLocation().directionTo(markRuin.getMapLocation());
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }

        }
        else if (targetRuinLocation != null) {
            if (rc.isActionReady()) {
                int val = rng.nextInt();
                if (rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetRuinLocation) && val % 5 >= 2) {
                    rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetRuinLocation);
                }
                else if (rc.canMarkTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetRuinLocation) && val % 5 <= 1) {
                    rc.markTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, targetRuinLocation);
                }
                else {
                    Direction dir = rc.getLocation().directionTo(targetRuinLocation);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
            }
            
        }
        else {
            if (rc.isActionReady()) {
                Direction dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)){
                    rc.move(dir);
                }   
            }           
        }

        // warnain terus sambil jalan
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())){
            rc.attack(rc.getLocation());
        }
    }


    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException{
        // jadiin tower kalo bisa
        for (Direction dir : Direction.values()) {
            MapLocation adjacent = rc.getLocation().add(dir);
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, adjacent)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, adjacent);
                }
                else if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, adjacent)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, adjacent);
                }
                else if (rc.canUpgradeTower(adjacent)) {
                    rc.upgradeTower(adjacent);
                }
        }

        // cari ruin di ruin itu, cari cat musuh yang bisa dihapus
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapInfo markRuin = null;
        for (MapInfo tile : nearbyTiles) {
            if (tile.getMark() != PaintType.EMPTY && (tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY)) {
                markRuin = tile;
                break;
            }
        }
        
        // deteksi musuh, kalo nemu gunakan mop swing
        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(2, rc.getTeam().opponent());
        MapLocation enemyRobotLocation = null;
        for (RobotInfo robo : nearbyEnemy) {
            if (robo.type == UnitType.SOLDIER && robo.type == UnitType.MOPPER && robo.type == UnitType.SPLASHER) {
                enemyRobotLocation = robo.getLocation();
                break;
            }
        }

        // apus cat musuh yang ngalangin ruin
        if (markRuin != null) {
            if (rc.isActionReady()) {
                if (rc.canAttack(markRuin.getMapLocation())) {
                    rc.attack(markRuin.getMapLocation());
                }
                else {
                    Direction dir = rc.getLocation().directionTo(markRuin.getMapLocation());
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
            }
        }
        // serang musuh (colong cet nya)
        else if (enemyRobotLocation != null) {
            if (enemyRobotLocation.y > rc.getLocation().y) {
                // serang utara
                if (rc.canMopSwing(Direction.NORTH)) {
                    rc.mopSwing(Direction.NORTH);
                }
            }
            else if (enemyRobotLocation.y < rc.getLocation().y) {
                // serang selatan
                if (rc.canMopSwing(Direction.SOUTH)) {
                    rc.mopSwing(Direction.SOUTH);
                }
            }
            else if (enemyRobotLocation.x > rc.getLocation().x) {
                // serang timur
                if (rc.canMopSwing(Direction.EAST)) {
                    rc.mopSwing(Direction.EAST);
                }
            }
            else {
                // serang barat
                if (rc.canMopSwing(Direction.WEST)) {
                    rc.mopSwing(Direction.WEST);
                }
            }
        }
        else {
            if (rc.isActionReady()) {
                Direction dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)){
                    rc.move(dir);
                }                
            }
        }

    }

}
