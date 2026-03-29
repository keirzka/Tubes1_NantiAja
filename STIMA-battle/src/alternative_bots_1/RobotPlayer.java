package alternative_bots_1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


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
    static MapLocation lastLocation = null;

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
                    case SPLASHER: runSplasher(rc); break; // Consider upgrading examplefuncsplayer to use splashers!
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
    // counter robot production
    static int splasherCount = 0;
    static int soldierCount = 0;
    static int mopperCount = 0;

    // RUN TOWER
    public static void runTower(RobotController rc) throws GameActionException{
        // Tower Attack : nearby robots
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        if (enemies.length > 0 && rc.isActionReady()) {
            RobotInfo best = enemies[0];
            for(RobotInfo e : enemies){
                if(e.health < best.health){
                    best = e;
                }
            }
            rc.attack(best.location);
        }

        // Transfer paint to allies
        RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());
        for(RobotInfo r : allies){
            if(r.paintAmount < 50){
                if(rc.canTransferPaint(r.location, 40)){
                    rc.transferPaint(r.location, 40);
                    return;
                }
            }
        }

        // perhitungan counter robot production: 70% splasher, 30% soldier, 20% mopper
        int total = splasherCount + soldierCount + mopperCount;

        double splasherRatio = total == 0 ? 0 : (double)splasherCount / total;
        double soldierRatio  = total == 0 ? 0 : (double)soldierCount / total;
        double mopperRatio   = total == 0 ? 0 : (double)mopperCount / total;

        for(Direction dir : directions){
            MapLocation spawnLoc = rc.getLocation().add(dir);

            if(splasherRatio < 0.7 && rc.canBuildRobot(UnitType.SPLASHER, spawnLoc)){
                rc.buildRobot(UnitType.SPLASHER, spawnLoc);
                splasherCount++;
                return;
            }

            if(soldierRatio < 0.2 && rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)){
                rc.buildRobot(UnitType.SOLDIER, spawnLoc);
                soldierCount++;
                return;
            }

            if(mopperRatio < 0.1 && rc.canBuildRobot(UnitType.MOPPER, spawnLoc)){
                rc.buildRobot(UnitType.MOPPER, spawnLoc);
                mopperCount++;
                return;
            }
        }
        
        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }
    }


    /**
     * Run a single turn for a Splasher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    // RUN SPLASER
    public static void runSplasher(RobotController rc) throws GameActionException{
        // Refill ke tower jika cadangan sudah menipis
        if(rc.getPaint() < 20){
            RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());
            for(RobotInfo r : allies){
                if(r.getType().isTowerType() && r.paintAmount > 50){
                    if(rc.canTransferPaint(r.location, -40)){
                        rc.transferPaint(r.location, -40);
                        return;
                    }
                }
            }
        }

        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation bestTarget = null;
        int bestScore = -999;

        for (MapInfo tile : nearbyTiles) {
            MapLocation loc = tile.getMapLocation();
            if (!rc.canAttack(loc)) continue;
            int score = evaluateSplash(rc, loc);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = loc;
            }
        }

        // attack : painting
        if (bestTarget != null && rc.isActionReady() && bestScore > 2) {
            rc.attack(bestTarget);
        }

        // move to priority direction
        if (rc.isMovementReady()) {
            Direction move = bestMoveSplasher(rc);
            if (move != null) {
                MapLocation current = rc.getLocation();
                rc.move(move);
                lastLocation = current;
            }
        }

    }

    // Evaluate tindakan splasher
    public static int evaluateSplash(RobotController rc, MapLocation center) throws GameActionException {
        int score = 0;
        MapInfo[] area = rc.senseNearbyMapInfos(center, 4);
        for (MapInfo tile : area) {
            // prioritas 1 : wilayah kosong yang sudah ditandai (agar bisa segera membangun tower)
            if(tile.getMark() != PaintType.EMPTY && tile.getPaint() != tile.getMark())
                score += 6;
            // prioritas 2 : wilayah musuh
            else if (tile.getPaint().isEnemy())
                score += 5;
            // prioritas 3 : wilayah kosong
            else if (tile.getPaint() == PaintType.EMPTY)
                score += 3;
            // bukan prioritas : wilayah sekutu
            else if (tile.getPaint().isAlly())
                score += 1;
        }
        return score;
    }

    // Pertimbangan movement splasher
    public static int evaluateMoveSplasher(RobotController rc, MapLocation loc) throws GameActionException {
        int score = 0;
        if (adjacentEnemyPaint(rc, loc))
            score += 6;

        MapInfo[] nearby = rc.senseNearbyMapInfos(loc, 4);
        for (MapInfo tile : nearby) {
            // Prioritas 1 : menuju ruin
            if (tile.hasRuin())
                score += 5;
            // Prioritas 2 : menuju wilayah musuh
            else if (tile.getPaint().isEnemy())
                score += 4;
            // Prioritas 3 : wilayah kosong
            else if (tile.getPaint() == PaintType.EMPTY)
                score += 3;
            // Prioritas 4 : menuju sekutu
            else if (tile.getPaint().isAlly())
                score += 1;
        }

        // penalti mendekati tower -> menghindari wilayah sekitar tower (jika cadangan cat masing banyak)
        if(rc.getPaint() > 20){
            RobotInfo[] allies = rc.senseNearbyRobots(3, rc.getTeam());
            for(RobotInfo r : allies){
                if(r.getType().isTowerType()){
                    score -= 3;
                }
            }
        }

        // penalti jika kembali ke lokasi sebelumnya --> agar menyebar
        if (lastLocation != null && loc.equals(lastLocation)) {
            score -= 5;
        }

        // penalti mendekati tembok
        nearby = rc.senseNearbyMapInfos(loc, 2);
        int wallCount = 0;

        for(MapInfo tile : nearby){
            if(tile.isWall()){
                wallCount++;
            }
        }

        score -= wallCount * 2;

        // dorongan eksplorasi wilayah
        score += loc.distanceSquaredTo(rc.getLocation()) / 2;

        // randomize
        score += rng.nextInt(3);

        return score;
    }

    // pencarian direction move terbaik splasher
    public static Direction bestMoveSplasher(RobotController rc) throws GameActionException {
        Direction bestDir = null;

        int bestScore = -999;
        for (Direction dir : directions) {
            if (!rc.canMove(dir)) continue;

            MapLocation next = rc.getLocation().add(dir);
            int score = evaluateMoveSplasher(rc, next);

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        return bestDir;
    }

    // boolean deteksi posisi berdekatan dengan wilayah musuh
    public static boolean adjacentEnemyPaint(RobotController rc, MapLocation loc) throws GameActionException{
        MapInfo[] nearby = rc.senseNearbyMapInfos(loc,2);
        for(MapInfo tile : nearby){
            if(tile.getPaint().isEnemy()){
                return true;
            }
        }

        return false;
    }

    // RUN SOLDIER
    public static void runSoldier(RobotController rc) throws GameActionException { 

        // Refill paint ke tower jika cadangan sudah menipis
        if(rc.getPaint() < 20){
            RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());
            for(RobotInfo r : allies){
                if(r.getType().isTowerType() && r.paintAmount > 50){
                    if(rc.canTransferPaint(r.location, -40)){
                        rc.transferPaint(r.location, -40);
                        return;
                    }
                }
            }
        }
        
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapInfo curRuin = null;
        int bestScore = -999;

        // Find best ruin to visit
        for (MapInfo tile : nearbyTiles) {
            if (tile.hasRuin()) {
                MapLocation ruinLoc = tile.getMapLocation();
                if (rc.canSenseLocation(ruinLoc)) {
                    RobotInfo robot = rc.senseRobotAtLocation(ruinLoc);
                    if (robot != null && robot.getTeam() == rc.getTeam()) continue;
                }

                int score = evaluateRuin(rc, ruinLoc);
                int dist = rc.getLocation().distanceSquaredTo(ruinLoc);
                score -= dist; 

                if (score > bestScore) {
                    bestScore = score;
                    curRuin = tile;
                }
            }
        }

        // Ruin found
        if (curRuin != null) {
            MapLocation targetLoc = curRuin.getMapLocation();
            Direction toRuin = rc.getLocation().directionTo(targetLoc);

            // Prioritas 1: Complete tower
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)) {
                rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
                rc.setTimelineMarker("Tower built", 0, 255, 0);
                return;
            }

            // Prioritas 2: Paint pattern
            if (rc.isActionReady()) {
                for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)) {
                    if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
                        boolean useSecondary = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                        if (rc.canAttack(patternTile.getMapLocation())) {
                            rc.attack(patternTile.getMapLocation(), useSecondary);
                            break; 
                        }
                    }
                }
            }

            // Prioritas 3 : Mark pattern (jika dalam jangkauan)
            if (toRuin != Direction.CENTER && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)) {
                rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
            }

            // Prioritas 4 : Bergerak mendekati ruin jika belum sampai
            if (rc.isMovementReady()) {
                if (rc.canMove(toRuin)) {
                    rc.move(toRuin);
                } else {
                    Direction alt = bestMoveSoldier(rc);
                    if (alt != null && rc.canMove(alt)) rc.move(alt);
                }
            }

            // Update arah setelah bergerak
            toRuin = rc.getLocation().directionTo(targetLoc);

            // Prioritas 5 : SRP
            MapLocation cur = rc.getLocation();
            if(rc.canMarkResourcePattern(cur)){
                rc.markResourcePattern(cur);
                if(rc.canCompleteResourcePattern(cur)){
                    rc.completeResourcePattern(cur);
                }
            }
        } 

        // ruin not found 
        else {
            // attack : painting
            MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
            if (rc.isActionReady() && !currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
                rc.attack(rc.getLocation());
            } 
            
            // eksplorasi
            if (rc.isMovementReady()) {
                Direction move = bestMoveSoldier(rc);
                if (move != null && rc.canMove(move)) {
                    rc.move(move);
                }
            }
        }
    }

    // Best Move Soldier
    public static Direction bestMoveSoldier(RobotController rc) throws GameActionException {
        Direction bestDir = null;
        int bestScore = -9999;

        for (Direction dir : directions){
            if (!rc.canMove(dir)) continue;
            MapLocation next = rc.getLocation().add(dir);
            MapInfo info = rc.senseMapInfo(next);

            int score = 0;

            // Prioritas utama : menuju ruin untuk membangun tower
            if (info.hasRuin())
                score += 40;

            // Prioritas 1 : mendekati wilayah musuh
            if (info.getPaint().isEnemy())
                score += 8;
            // Prioritas 2 : menuju wilayah kosong
            else if (info.getPaint() == PaintType.EMPTY)
                score += 6;
            // Prioritas 3 : menuju wilayah sekutu
            else if (info.getPaint().isAlly())
                score += 1;

            // wilayah sekitar ruin
            MapInfo[] nearby = rc.senseNearbyMapInfos(next, 4);
            for (MapInfo tile : nearby){
                if (tile.hasRuin()){
                    score += 10;
                }
            }

            // penalti mendekati tower -> menghindari wilayah sekitar tower (jika cadangan cat masing banyak)
            if(rc.getPaint() > 20){
                RobotInfo[] allies = rc.senseNearbyRobots(3, rc.getTeam());
                for(RobotInfo r : allies){
                    if(r.getType().isTowerType()){
                        score -= 3;
                    }
                }
            }

            // penalti mendekati tembok
            if (info.isWall()) score -= 100;

            // randomize
            score += rng.nextInt(2);

            // mendorong eksplorasi
            score += rc.getLocation().distanceSquaredTo(next) / 4;

            if (score > bestScore){
                bestScore = score;
                bestDir = dir;
            }
        }

        return bestDir;
    }

    // evaluate keberadaan ruin
    public static int evaluateRuin(RobotController rc, MapLocation ruinLoc) throws GameActionException {
        int score = 0;

        MapInfo[] area = rc.senseNearbyMapInfos(ruinLoc, 8);

        int ally = 0;
        int empty = 0;
        int enemy = 0;

        for (MapInfo tile : area){
            if (tile.getPaint().isAlly())
                ally++;
            else if (tile.getPaint() == PaintType.EMPTY)
                empty++;
            else if (tile.getPaint().isEnemy())
                enemy++;
        }

        // Wilayah yang sudah dibantu splasher
        if (ally > 3) score += 20;

        // wilayah kosong
        if (empty > 0) score += 10;

        // penalti mendekati area musuh
        score -= enemy * 2;

        return score;
    }

    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    // RUN MOPPER
    public static void runMopper(RobotController rc) throws GameActionException{
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();

        MapLocation bestEnemyPaint = null;
        int bestDist = 999;

        // cari wilayah musuh terdekat
        for (MapInfo tile : nearbyTiles) {
            if (tile.getPaint().isEnemy()) {
                MapLocation loc = tile.getMapLocation();
                int dist = rc.getLocation().distanceSquaredTo(loc);

                if (dist < bestDist) {
                    bestDist = dist;
                    bestEnemyPaint = loc;
                }
            }
        }

        // Prioritas 1 : bersihkan enemy paint
        if (bestEnemyPaint != null) {
            if (rc.canAttack(bestEnemyPaint)) {
                rc.attack(bestEnemyPaint);
                return;
            }
            Direction dir = rc.getLocation().directionTo(bestEnemyPaint);

            if (rc.canMove(dir)) {
                rc.move(dir);
                return;
            }
        }

        // Prioritas 2 : Transfer Paint ke Sekutu
        // Refill cadangan paint robot sekutu
        RobotInfo[] allies = rc.senseNearbyRobots(8, rc.getTeam());
        for (RobotInfo ally : allies) {
            if (ally.paintAmount < 20) {
                if (rc.canTransferPaint(ally.location, 20)) {
                    rc.transferPaint(ally.location, 20);
                    return;
                }
            }
        }

        // Refill cadangan paint tower sekutu
        MapLocation myLoc = rc.getLocation();
        for (Direction d : directions) {
            MapLocation adj = myLoc.add(d);
            if(rc.canSenseLocation(adj)){
                RobotInfo r = rc.senseRobotAtLocation(adj);
                if (r != null && r.getTeam() == rc.getTeam() && r.getType().isTowerType()) {
                    if (rc.canTransferPaint(adj, 30)) {
                        rc.transferPaint(adj, 30);
                        return;
                    }
                }
            }
        }

        // Prioritas 2 : mop swing jika ada musuh
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        if (enemies.length > 0) {
            Direction dir = rc.getLocation().directionTo(enemies[0].getLocation());
            if (rc.canMopSwing(dir)) {
                rc.mopSwing(dir);
                return;
            }
        }

        // Prioritas 3: eksplorasi
        Direction dir = bestMoveSoldier(rc);
        if (dir != null && rc.canMove(dir)) {
            rc.move(dir);
        }

        updateEnemyRobots(rc);
    }

    // Update robot musuh
    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically 
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for possible future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            // Occasionally try to tell nearby allies how many enemy robots we see.
            if (rc.getRoundNum() % 20 == 0){
                for (RobotInfo ally : allyRobots){
                    if (rc.canSendMessage(ally.location, enemyRobots.length)){
                        rc.sendMessage(ally.location, enemyRobots.length);
                    }
                }
            }
        }
    }
}