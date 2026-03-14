package alternativebots2;

import java.util.Random;

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

public class RobotPlayer {
    static int turnCount = 0;
    static final boolean DEBUG = false;
    static void log(RobotController rc, String msg) {
        if (DEBUG) {
            System.out.println("(" + rc.getType() + " " + rc.getID() + " " + rc.getRoundNum() + " " + msg + ")");
        }
    }
    static final Random rng = new Random(6147);
    static final Direction[] ALL_DIRS = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,
    };
    static final Direction[] CARDINAL = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
    };
    static final double PAINT_DANGER = 0.10;
    static final double PAINT_REFILL = 0.25;
    static final double PAINT_SAFE   = 0.50;
    static final int PHASE_MID  = 300;
    static final int PHASE_LATE = 800;
    static MapLocation exploreTarget = null;
    static int cornerIdx = 0;
    static MapLocation msgRuinTarget = null;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            turnCount++;
            try {
                UnitType t = rc.getType();
                if      (t.isTowerType())       runTower(rc);
                else if (t == UnitType.SOLDIER)  runSoldier(rc);
                else if (t == UnitType.MOPPER)   runMopper(rc);
                else if (t == UnitType.SPLASHER) runSplasher(rc);
            } catch (GameActionException e) {
                System.out.println("Game Action Error:" + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error:" + e.getMessage());
            } finally {
                Clock.yield();
            }
        }
    }

    static void runTower(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        attackNearest(rc);
        MapLocation myloc = rc.getLocation();
        if (rc.getType().canUpgradeType() && rc.canUpgradeTower(myloc)) {
            if (rc.getChips() > 2000) {
                rc.upgradeTower(myloc);
                log(rc, "Upgrade Tower!");
            }
        }
        for (MapLocation ruin : rc.senseNearbyRuins(-1)) {
            if (!rc.isLocationOccupied(ruin)) {
                if (rc.canBroadcastMessage()) rc.broadcastMessage(encLoc(ruin));
                log(rc, "Broadcast ruin target " + ruin);
                break;
            }
        }
        if (!rc.isActionReady()) return;
        Direction spawnDir = freeDir(rc);
        if (spawnDir == null) return;
        MapLocation sp = rc.getLocation().add(spawnDir);
        boolean lowPaint  = rc.getPaint() < 200;
        boolean hasRuin   = rc.senseNearbyRuins(-1).length > 0;
        int chips = rc.getChips();

        if (round < PHASE_MID) {
            if (lowPaint && rc.canBuildRobot(UnitType.MOPPER, sp)) {
                rc.buildRobot(UnitType.MOPPER, sp);
                log(rc, "Build Mopper at " + sp);
            }
            else if (rc.canBuildRobot(UnitType.SOLDIER, sp)) {
                rc.buildRobot(UnitType.SOLDIER, sp);
                log(rc, "Build Soldier at " + sp);
            }
            else if (rc.canBuildRobot(UnitType.MOPPER, sp)) {
                rc.buildRobot(UnitType.MOPPER, sp);
                log(rc, "Build Mopper at " + sp);
            }

        } else if (round < PHASE_LATE) {
            if (lowPaint && rc.canBuildRobot(UnitType.MOPPER, sp)) {
                rc.buildRobot(UnitType.MOPPER, sp);
                log(rc, "Build Mopper at " + sp);
            }
            else if (hasRuin && rc.canBuildRobot(UnitType.SOLDIER, sp)) {
                rc.buildRobot(UnitType.SOLDIER, sp);
                log(rc, "Build Soldier at " + sp);
            }
            else if (round % 5 == 0 && chips > 800 && rc.canBuildRobot(UnitType.SPLASHER, sp)) {
                rc.buildRobot(UnitType.SPLASHER, sp);
                log(rc, "Build Splasher at " + sp);
            }
                
            else if (rc.canBuildRobot(UnitType.SOLDIER, sp)){
                rc.buildRobot(UnitType.SOLDIER, sp);
                log(rc, "Build Soldier at " + sp);
            }

        } else {
            if (round % 3 == 0 && chips > 600 && rc.canBuildRobot(UnitType.SPLASHER, sp)) {
                rc.buildRobot(UnitType.SPLASHER, sp);
                log(rc, "Build Splasher at " + sp);
            }
            else if (lowPaint && rc.canBuildRobot(UnitType.MOPPER, sp)) {
                rc.buildRobot(UnitType.MOPPER, sp);
                log(rc, "Build Mopper at " + sp);
            }
            else if (rc.canBuildRobot(UnitType.SOLDIER, sp)) {
                rc.buildRobot(UnitType.SOLDIER, sp);
                log(rc, "Build Soldier at " + sp);
            }
        }
    }

    static void runSoldier(RobotController rc) throws GameActionException {
        int paint = rc.getPaint(), cap = rc.getType().paintCapacity;
        MapLocation me = rc.getLocation();
        int round = rc.getRoundNum();
        readMsg(rc);
        if (paint < cap * PAINT_DANGER) {
            log(rc, "Mode refill (DANGER),  paint = " + paint + "/" + cap);
            MapLocation tw = nearestTower(rc);
            if (tw != null) {
                if (me.isAdjacentTo(tw)) refill(rc, tw);
                else moveToward(rc, tw);
            }
            rc.setIndicatorString("DANGER paint=" + paint);
            return;
        }
        if (paint < cap * PAINT_REFILL) {
            log(rc, "Mode refill,  paint = " + paint + "/" + cap);
            MapLocation tw = nearestTower(rc);
            if (tw != null) {
                if (me.isAdjacentTo(tw)) refill(rc, tw);
                else { moveToward(rc, tw); paintTile(rc); }
                return;
            }
        }
        MapLocation activeRuin = ruinWithPattern(rc);
        MapLocation nearestFree = nearestFreeRuin(rc);
        if (activeRuin != null && nearestFree != null) {
            int distActive = me.distanceSquaredTo(activeRuin);
            int distFree   = me.distanceSquaredTo(nearestFree);
            if (distActive > distFree * 2) {
                log(rc, "Skip active ruin " + activeRuin);
                activeRuin = null;
            }
        }
        if (activeRuin != null) {
            log(rc, "Mode active ruin, target = " + activeRuin);
            UnitType tt = towerType(rc, activeRuin);
            boolean filled = fillMark(rc, activeRuin);
            if (rc.canCompleteTowerPattern(tt, activeRuin)) {
                rc.completeTowerPattern(tt, activeRuin);
                log(rc, "Completed tower: " + tt + " at " + activeRuin);
                rc.setIndicatorString("Tower built!");
                exploreTarget = null;
            } else if (!filled) {
                MapLocation pending = pendingTileLocation(rc, activeRuin);
                moveToward(rc, pending != null ? pending : activeRuin);
            }
            paintTile(rc);
            return;
        }
        MapLocation ruin = (nearestFree != null) ? nearestFree : nearestFreeRuin(rc);
        if (ruin == null && msgRuinTarget != null) ruin = msgRuinTarget;
        if (ruin != null && allyCloserToRuin(rc, ruin)) {
            MapLocation altRuin = null;
            for (MapLocation r : rc.senseNearbyRuins(-1)) {
                if (rc.isLocationOccupied(r)) continue;
                if (!allyCloserToRuin(rc, r)) { altRuin = r; break; }
            }
            if (altRuin != null) {
                log(rc, "Redirect dari " + ruin + " ke ruin alternatif " + altRuin);
                ruin = altRuin;
            }
        }
        if (ruin != null) {
            log(rc, "Mode build target, target : " + ruin + " towerType : " + towerType(rc, ruin));
            UnitType tt = towerType(rc, ruin);
            if (rc.canMarkTowerPattern(tt, ruin)) {
                rc.markTowerPattern(tt, ruin);
                log(rc, "Marked tower pattern " + tt + " at " + ruin);
            }
            boolean filled = fillMark(rc, ruin);
            if (rc.canCompleteTowerPattern(tt, ruin)) {
                rc.completeTowerPattern(tt, ruin);
                log(rc, "Completed tower: " + tt + " at " + ruin);
                rc.setIndicatorString("Tower built!");
                msgRuinTarget = null;
            } else if (!filled) {
                MapLocation pending = pendingTileLocation(rc, ruin);
                moveToward(rc, pending != null ? pending : ruin);
            }
            paintTile(rc);
            return;
        }
        MapLocation srpDone = completableSRP(rc);
        if (srpDone != null) {
            log(rc, "Target SRP : " + srpDone);
            if (rc.canCompleteResourcePattern(srpDone)) {
                rc.completeResourcePattern(srpDone);
                log(rc, "Completed SRP at " + srpDone);
                rc.setIndicatorString("SRP activated!");
            } else moveToward(rc, srpDone);
            paintTile(rc);
            return;
        }
        if (round >= PHASE_MID) {
            MapLocation srpSpot = safeSRPSpot(rc);
            if (srpSpot != null) {
                log(rc, "Mark SRP target : " + srpSpot);
                if (rc.canMarkResourcePattern(srpSpot)) {
                    rc.markResourcePattern(srpSpot);
                    log(rc, "Marked SRP at " + srpSpot);
                    rc.setIndicatorString("SRP marked!");
                } else moveToward(rc, srpSpot);
                paintTile(rc);
                return;
            }
        }
        MapLocation threat = enemyNearPattern(rc);
        if (threat != null) {
            log(rc, "Target reclaim : " + threat);
            if (rc.isActionReady() && rc.canAttack(threat)) {
                rc.attack(threat, false);
                rc.setIndicatorString("Reclaim tile");
                return;
            }
            moveToward(rc, threat);
            paintTile(rc);
            return;
        }
        exploreAndPaint(rc);
    }

    static void runMopper(RobotController rc) throws GameActionException {
        int paint = rc.getPaint(), cap = rc.getType().paintCapacity;
        MapLocation me = rc.getLocation();
        if (paint < cap * PAINT_DANGER) {
            log(rc, "Mode refill (DANGER), paint : " + paint + "/" + cap);
            MapLocation tw = nearestTower(rc);
            if (tw != null) {
                if (me.isWithinDistanceSquared(tw, 2)) refill(rc, tw);
                else moveToward(rc, tw);
            }
            return;
        }
        MapLocation srp = nearestSRP(rc);
        if (srp != null) {
            MapLocation srpThreat = enemyInRadius(rc, srp, 12);
            if (srpThreat != null) {
                log(rc, "SRP defend, center : " + srp + " threat : " + srpThreat);
                if (rc.isActionReady() && rc.canAttack(srpThreat)) {
                    rc.attack(srpThreat);
                    rc.setIndicatorString("DEFEND SRP!");
                    return;
                }
                moveToward(rc, srpThreat);
                return;
            }
        }
        MapLocation twThreat = enemyNearActiveRuin(rc);
        if (twThreat != null) {
            log(rc, "Clean pattern, threat : " + twThreat);
            if (rc.isActionReady() && rc.canAttack(twThreat)) {
                rc.attack(twThreat);
                rc.setIndicatorString("Clean tower");
                return;
            }
            moveToward(rc, twThreat);
            return;
        }
        Direction swing = bestSwing(rc);
        if (swing != null && rc.canMopSwing(swing)) {
            rc.mopSwing(swing);
            log(rc, "Mop swing!");
            rc.setIndicatorString("Mop swing!");
            return;
        }
        RobotInfo urgent = urgentAlly(rc);
        if (urgent != null && paint > cap * 0.5
                && me.isWithinDistanceSquared(urgent.location, 8)) {
            if (me.isWithinDistanceSquared(urgent.location, 2)) {
                int give = Math.min((int)(paint - cap * 0.3),
                    urgent.type.paintCapacity - urgent.paintAmount);
                if (give > 0 && rc.canTransferPaint(urgent.location, give)) {
                    rc.transferPaint(urgent.location, give);
                    log(rc, "Emergency feed to " + urgent.location + " amount : " + give);
                    rc.setIndicatorString("Emergency feed!");
                    return;
                }
            } else { moveToward(rc, urgent.location); return; }
        }
        if (paint < cap * PAINT_REFILL) {
            MapLocation tw = nearestTower(rc);
            if (tw != null) {
                if (me.isWithinDistanceSquared(tw, 2)) refill(rc, tw);
                else moveToward(rc, tw);
                return;
            }
        }
        RobotInfo needy = needyAlly(rc);
        if (needy != null && paint > cap * 0.6
                && me.isWithinDistanceSquared(needy.location, 8)) {
            if (me.isWithinDistanceSquared(needy.location, 2)) {
                int give = Math.min((int)(paint - cap * 0.5),
                    needy.type.paintCapacity - needy.paintAmount);
                if (give > 0 && rc.canTransferPaint(needy.location, give)) {
                    rc.transferPaint(needy.location, give);
                    log(rc, "Feed ally at " + needy.location + " amount : " + give);
                    rc.setIndicatorString("Feed ally");
                    return;
                }
            } else { moveToward(rc, needy.location); return; }
        }
        MapLocation tw   = nearestTower(rc);
        MapLocation area = ruinWithPattern(rc);
        if (area == null) area = nearestSRP(rc);
        if (tw != null && area != null) {
            MapLocation mid = new MapLocation((tw.x + area.x)/2, (tw.y + area.y)/2);
            if (!me.isWithinDistanceSquared(mid, 4)) moveToward(rc, mid);
            else { Direction d = ALL_DIRS[turnCount % 8]; if (rc.canMove(d)) rc.move(d); }
        } else if (tw != null) {
            if (!me.isWithinDistanceSquared(tw, 9)) moveToward(rc, tw);
            else { Direction d = ALL_DIRS[turnCount % 8]; if (rc.canMove(d)) rc.move(d); }
        } else {
            Direction d = ALL_DIRS[turnCount % 8]; if (rc.canMove(d)) rc.move(d);
        }
    }

    static void runSplasher(RobotController rc) throws GameActionException {
        int paint = rc.getPaint(), cap = rc.getType().paintCapacity;
        MapLocation me = rc.getLocation();
        if (paint < cap * PAINT_REFILL) {
            log(rc, "Refill splasher, paint : " + paint + "/" + cap);
            MapLocation tw = nearestTower(rc);
            if (tw != null) {
                if (me.isAdjacentTo(tw)) refill(rc, tw);
                else moveToward(rc, tw);
            }
            return;
        }
        MapLocation strategic = splashTarget(rc, true);
        if (strategic != null) {
            if (rc.isActionReady() && rc.canAttack(strategic)) {
                rc.attack(strategic);
                rc.setIndicatorString("Strategic splash");
                return;
            }
            moveToward(rc, strategic);
            return;
        }
        if (paint > cap * PAINT_SAFE) {
            MapLocation coverage = splashTarget(rc, false);
            if (coverage != null) {
                if (rc.isActionReady() && rc.canAttack(coverage)) {
                    rc.attack(coverage);
                    rc.setIndicatorString("Coverage splash");
                    return;
                }
                moveToward(rc, coverage);
                return;
            }
        }
        exploreAndPaint(rc);
    }

    static MapLocation nearestTower(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation(); 
        MapLocation best = null; 
        int bd = Integer.MAX_VALUE;
        for (RobotInfo a : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (!a.type.isTowerType()) continue;
            int d = me.distanceSquaredTo(a.location);
            if (d < bd) { 
                bd = d; 
                best = a.location; 
            }
        }
        return best;
    }
    static void refill(RobotController rc, MapLocation tower) throws GameActionException {
        int need = rc.getType().paintCapacity - rc.getPaint();
        if (need > 0 && rc.canTransferPaint(tower, -need))  {
            rc.transferPaint(tower, -need);
            log(rc, "Refilled from tower " + tower + " amount : " + need);
        }
    }
    static MapLocation nearestFreeRuin(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation(); 
        MapLocation best = null; 
        int bd = Integer.MAX_VALUE;
        for (MapLocation r : rc.senseNearbyRuins(-1)) {
            if (rc.isLocationOccupied(r)) continue;
            int d = me.distanceSquaredTo(r);
            if (d < bd) { 
                bd = d; 
                best = r; 
            }
        }
        return best;
    }
    static MapLocation ruinWithPattern(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation(); 
        MapLocation best = null; 
        int bd = Integer.MAX_VALUE;
        for (MapLocation r : rc.senseNearbyRuins(-1)) {
            if (rc.isLocationOccupied(r)) continue;
            for (MapInfo t : rc.senseNearbyMapInfos(r, 8)) {
                if (t.getMark() != PaintType.EMPTY) {
                    int d = me.distanceSquaredTo(r);
                    if (d < bd) { 
                        bd = d; 
                        best = r; 
                        break; }
                }
            }
        }
        return best;
    }
    static boolean fillMark(RobotController rc, MapLocation ruin) throws GameActionException {
        if (!rc.isActionReady()) return false;
        for (MapInfo t : rc.senseNearbyMapInfos(ruin, 8)) {
            PaintType mark = t.getMark();
            if (mark == PaintType.EMPTY || mark == t.getPaint()) continue;
            MapLocation loc = t.getMapLocation();
            if (rc.canAttack(loc)) { 
                rc.attack(loc, mark == PaintType.ALLY_SECONDARY); 
                return true; 
            }
        }
        return false;
    }
    static UnitType towerType(RobotController rc, MapLocation ruin) throws GameActionException {
        boolean hasAnyMark = false;
        for (MapInfo t : rc.senseNearbyMapInfos(ruin, 8)) {
            if (t.getMark() != PaintType.EMPTY) { 
                hasAnyMark = true; 
                break; 
            }
        }

        if (hasAnyMark) {
            UnitType[] candidates = {
                UnitType.LEVEL_ONE_PAINT_TOWER,
                UnitType.LEVEL_ONE_MONEY_TOWER,
                UnitType.LEVEL_ONE_DEFENSE_TOWER
            };
            for (UnitType candidate : candidates) {
                if (rc.canCompleteTowerPattern(candidate, ruin)) return candidate;
            }
            for (UnitType candidate : candidates) {
                if (rc.canMarkTowerPattern(candidate, ruin)) return candidate;
            }
        }
        int mod = rc.getNumberTowers() % 4;
        if (mod == 0) return UnitType.LEVEL_ONE_PAINT_TOWER;
        if (mod == 1) return UnitType.LEVEL_ONE_MONEY_TOWER;
        if (mod == 2) return UnitType.LEVEL_ONE_PAINT_TOWER;
        return UnitType.LEVEL_ONE_DEFENSE_TOWER;
    }
    static MapLocation nearestSRP(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation(); 
        MapLocation best = null; 
        int bd = Integer.MAX_VALUE;
        for (MapInfo t : rc.senseNearbyMapInfos()) {
            if (!t.isResourcePatternCenter()) continue;
            int d = me.distanceSquaredTo(t.getMapLocation());
            if (d < bd) { 
                bd = d; 
                best = t.getMapLocation(); 
            }
        }
        return best;
    }
    static MapLocation completableSRP(RobotController rc) throws GameActionException {
        for (MapInfo t : rc.senseNearbyMapInfos()) {
            if (!t.isResourcePatternCenter()) continue;
            if (rc.canCompleteResourcePattern(t.getMapLocation())) return t.getMapLocation();
        }
        return null;
    }
    static MapLocation safeSRPSpot(RobotController rc) throws GameActionException {
        for (MapInfo t : rc.senseNearbyMapInfos()) {
            MapLocation loc = t.getMapLocation();
            if (!t.isPassable() || t.hasRuin()) continue;
            if (!rc.canMarkResourcePattern(loc)) continue;
            if (rc.senseNearbyRobots(loc, 9, rc.getTeam().opponent()).length == 0) return loc;
        }
        return null;
    }
    static MapLocation enemyNearPattern(RobotController rc) throws GameActionException {
        for (MapInfo t : rc.senseNearbyMapInfos()) {
            if (!t.getPaint().isEnemy()) continue;
            for (MapInfo near : rc.senseNearbyMapInfos(t.getMapLocation(), 4))
                if (near.hasRuin() || near.isResourcePatternCenter()) return t.getMapLocation();
        }
        return null;
    }
    static MapLocation enemyInRadius(RobotController rc, MapLocation center, int rSq) throws GameActionException {
        for (MapInfo t : rc.senseNearbyMapInfos(center, rSq))
            if (t.getPaint().isEnemy()) return t.getMapLocation();
        return null;
    }
    static MapLocation enemyNearActiveRuin(RobotController rc) throws GameActionException {
        for (MapLocation r : rc.senseNearbyRuins(-1)) {
            if (rc.isLocationOccupied(r)) continue;
            boolean hasMark = false;
            for (MapInfo t : rc.senseNearbyMapInfos(r, 8))
                if (t.getMark() != PaintType.EMPTY) { hasMark = true; break; }
            if (!hasMark) continue;
            for (MapInfo t : rc.senseNearbyMapInfos(r, 8))
                if (t.getPaint().isEnemy()) return t.getMapLocation();
        }
        return null;
    }
    static RobotInfo urgentAlly(RobotController rc) throws GameActionException {
        RobotInfo best = null; int min = Integer.MAX_VALUE;
        for (RobotInfo a : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (!a.type.isRobotType() || a.type == UnitType.MOPPER) continue;
            int thr = (int)(a.type.paintCapacity * PAINT_DANGER);
            if (a.paintAmount < thr && a.paintAmount < min) { min = a.paintAmount; best = a; }
        }
        return best;
    }
    static RobotInfo needyAlly(RobotController rc) throws GameActionException {
        RobotInfo best = null; int min = Integer.MAX_VALUE;
        for (RobotInfo a : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (!a.type.isRobotType() || a.type == UnitType.MOPPER) continue;
            int thr = (int)(a.type.paintCapacity * PAINT_REFILL);
            if (a.paintAmount < thr && a.paintAmount < min) { min = a.paintAmount; best = a; }
        }
        return best;
    }
    static Direction bestSwing(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation(); Direction best = null; int bc = 0;
        for (Direction dir : CARDINAL) {
            if (!rc.canMopSwing(dir)) continue;
            int count = 0;
            MapLocation s1 = me.add(dir), s2 = s1.add(dir);
            for (MapLocation check : new MapLocation[]{s1, s2}) {
                if (!rc.onTheMap(check)) continue;
                if (rc.canSenseRobotAtLocation(check)) {
                    RobotInfo r = rc.senseRobotAtLocation(check);
                    if (r != null && r.team != rc.getTeam()) count++;
                }
                for (Direction side : new Direction[]{dir.rotateLeft(), dir.rotateRight()}) {
                    MapLocation sl = check.add(side);
                    if (!rc.onTheMap(sl) || !rc.canSenseRobotAtLocation(sl)) continue;
                    RobotInfo r = rc.senseRobotAtLocation(sl);
                    if (r != null && r.team != rc.getTeam()) count++;
                }
            }
            if (count > bc) { bc = count; best = dir; }
        }
        return (bc > 0) ? best : null;
    }
    static MapLocation splashTarget(RobotController rc, boolean strategicOnly) throws GameActionException {
        MapLocation me = rc.getLocation();
        MapLocation best = null; int bs = strategicOnly ? 10 : 5;
        for (MapLocation c : rc.getAllLocationsWithinRadiusSquared(me, 4)) {
            if (!rc.canAttack(c)) continue;
            int score = 0;
            for (MapInfo t : rc.senseNearbyMapInfos(c, 4)) {
                if (t.getPaint().isEnemy()) score += 3;
                else if (t.getPaint() == PaintType.EMPTY) score += 1;
            }
            for (MapInfo t : rc.senseNearbyMapInfos(c, 9)) if (t.hasRuin()) { score += 10; break; }
            for (MapInfo t : rc.senseNearbyMapInfos(c, 9)) if (t.isResourcePatternCenter()) { score += 15; break; }
            if (strategicOnly && score < 20) continue;
            if (score > bs) { bs = score; best = c; }
        }
        return best;
    }

    static void moveToward(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return;
        MapLocation me = rc.getLocation();
        if (me.equals(target)) return;
        Direction main = me.directionTo(target);
        Direction[] cands = { main, main.rotateLeft(), main.rotateRight(), main.rotateLeft().rotateLeft(), main.rotateRight().rotateRight() };
        Direction best = null; int bs = Integer.MIN_VALUE;
        for (Direction d : cands) {
            if (!rc.canMove(d)) continue;
            MapLocation nx = me.add(d);
            int s = -nx.distanceSquaredTo(target);
            try {
                PaintType p = rc.senseMapInfo(nx).getPaint();
                if (p.isAlly()) s += 3;
                else if (p == PaintType.EMPTY) s += 1;
            } catch (GameActionException ignored) {}
            if (s > bs) { bs = s; best = d; }
        }
        if (best != null) rc.move(best);
    }
    static void paintTile(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        MapLocation me = rc.getLocation();
        MapInfo t = rc.senseMapInfo(me);
        if (!t.getPaint().isAlly() && rc.canAttack(me)) rc.attack(me, false);
    }
    static void attackNearest(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation(); int bd = Integer.MAX_VALUE; MapLocation tgt = null;
        for (RobotInfo e : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
            int d = me.distanceSquaredTo(e.location);
            if (d < bd) { bd = d; tgt = e.location; }
        }
        if (tgt != null && rc.canAttack(tgt)) {
            rc.attack(tgt);
            log(rc, "Tower attacked enemy at " + tgt);
        }
    }
    static void exploreAndPaint(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        paintTile(rc);
        if (exploreTarget == null || me.distanceSquaredTo(exploreTarget) <= 4) exploreTarget = nextCorner(rc);
        moveToward(rc, exploreTarget);
        if (!rc.isActionReady()) return;
        int rad = rc.getType().actionRadiusSquared;
        MapLocation bp = null; int bs = 0;
        for (MapInfo t : rc.senseNearbyMapInfos(rad)) {
            if (!t.isPassable()) continue;
            PaintType p = t.getPaint();
            int s = p.isEnemy() ? 3 : (p == PaintType.EMPTY ? 1 : 0);
            if (s > bs && rc.canAttack(t.getMapLocation())) { bs = s; bp = t.getMapLocation(); }
        }
        if (bp != null) rc.attack(bp, false);
    }
    static MapLocation nextCorner(RobotController rc) throws GameActionException {
        int w = rc.getMapWidth(), h = rc.getMapHeight();
        MapLocation[] c = {
            new MapLocation(2, 2), new MapLocation(w-3, 2),
            new MapLocation(w-3, h-3), new MapLocation(2, h-3),
            new MapLocation(w/2, h/2),
        };
        cornerIdx = (cornerIdx + 1) % c.length;
        return c[cornerIdx];
    }
    static Direction freeDir(RobotController rc) throws GameActionException {
        Direction[] sh = ALL_DIRS.clone();
        for (int i = sh.length-1; i > 0; i--) {
            int j = rng.nextInt(i+1); Direction tmp = sh[i]; sh[i] = sh[j]; sh[j] = tmp;
        }
        for (Direction d : sh) {
            MapLocation loc = rc.getLocation().add(d);
            if (rc.onTheMap(loc) && !rc.isLocationOccupied(loc)) return d;
        }
        return null;
    }

    static boolean allyCloserToRuin(RobotController rc, MapLocation ruin)
            throws GameActionException {
        int myDist = rc.getLocation().distanceSquaredTo(ruin);
        for (RobotInfo a : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (a.type != UnitType.SOLDIER) continue;
            if (a.location.distanceSquaredTo(ruin) < myDist) return true;
        }
        return false;
    }

    static MapLocation pendingTileLocation(RobotController rc, MapLocation ruin)
            throws GameActionException {
        for (MapInfo t : rc.senseNearbyMapInfos(ruin, 8)) {
            PaintType mark = t.getMark();
            if (mark == PaintType.EMPTY || mark == t.getPaint()) continue;
            return t.getMapLocation();
        }
        return null;
    }

    static int encLoc(MapLocation loc) { return loc.x * 100 + loc.y; }
    static MapLocation decLoc(int v) { return new MapLocation(v / 100, v % 100); }
    static void readMsg(RobotController rc) throws GameActionException {
        for (Message m : rc.readMessages(rc.getRoundNum() - 1)) {
            int data = m.getBytes();
            if (data <= 0) continue;
            MapLocation loc = decLoc(data);
            if (loc.x >= 0 && loc.x < rc.getMapWidth() && loc.y >= 0 && loc.y < rc.getMapHeight()) {
                msgRuinTarget = loc; 
                log(rc, "Received ruin message target : " + loc + " from sender : " + m.getSenderID());
                break;
            }
        }
    }
}