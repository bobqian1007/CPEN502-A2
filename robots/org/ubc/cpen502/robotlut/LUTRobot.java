package org.ubc.cpen502.robotlut;

import robocode.*;

import java.awt.*;
import java.io.File;

public class LUTRobot extends AdvancedRobot {
    //Enum type
    public enum HP {low, medium, high};
    public enum Distance {close, medium, far};
    public enum Action {fire, left, right, forward, back};
    public enum operaMode {onScan, onAction};

    // Initialization: Current State
    private HP curMyHP = HP.high;
    private HP curEneHP = HP.high;
    private Distance curMyDistance = Distance.close; // distance between myRobot and the enemy
    private Distance curWaDistance = Distance.far; // distance between myRobot and the wall
    private Action curAction = Action.forward;

    // Initialization: Previous State
    private HP preMyHP = HP.high;
    private HP preEneHP = HP.high;
    private Distance preMyDistance = Distance.close;
    private Distance preWaDistance = Distance.far;
    private Action preAction = Action.forward;

    // Initialization: operationMode
    private operaMode myOperationMode= operaMode.onScan;

    // My X position
    public double myX = 0.0;
    // My Y position
    public double myY = 0.0;
    // My HP
    public double myHP = 100;
    // Enemy's HP
    public double enemyHP = 100;
    // Distance between enemy and me
    public double dis = 0.0;

    // Whether take immediate rewards
    public static boolean takeImmediate = true;

    // Whether take on-policy algorithm
    public static boolean onPolicy = true;

    // Discount factor
    private double gamma = 0.9;
    // Learning rate
    private double alpha = 0.1;
    // Random number for epsilon-greedy policy
    private double epsilon = 0.0;
    // Q
    private double Q = 0.0;
    // Reward
    private double reward = 0.0;

    /* Bonus and Penalty */
    private final double immediateBonus = 0.5;
    private final double terminalBonus = 1.0;
    private final double immediatePenalty = -0.1;
    private final double terminalPenalty = -0.2;

    // Whether take greedy method
    public static int curActionIndex;
    public static double enemyBearing;

    // static numbers for winning rounds
    public static int totalRound = 0;
    public static int round = 0;
    public static int winRound = 0;
    //    public static double[] winPercentage = new double[351];
    public static double winPercentage = 0.0;
    public static String fileToSaveName = LUTRobot.class.getSimpleName() + "-"  + "winningRate"+ ".log";
    public static String fileToSaveLUT = LUTRobot.class.getSimpleName() + "-"  + "LUT";
    static LogFile log = new LogFile();

    public static LookUpTable lut = new LookUpTable(HP.values().length,
            HP.values().length,
            Distance.values().length,
            Distance.values().length,
            Action.values().length);

     // private LookUpTable lut;

    // Get the level of HP
    public HP getHPLevel(double hp) {
        HP level = null;
        if(hp < 0) {
            return level;
        } else if(hp <= 33) {
            level = HP.low;
        } else if(hp <= 67) {
            level = HP.medium;
        } else {
            level = HP.high;
        }
        return level;
    }

    // Get the distance
    public Distance getDistanceFromWallLevel(double x1, double y1) {
        Distance disWLevel = null;
        double width = getBattleFieldWidth();
        double height = getBattleFieldHeight();
        double dist = y1;
        double disb = height - y1;
        double disl = x1;
        double disr = width - x1;
        if(dist < 30 || disb < 30 || disl < 30 || disr < 30) {
            disWLevel = Distance.close;
        } else if(dist < 80 || disb < 80 || disl < 80 || disr < 80) {
            disWLevel = Distance.medium;
        } else {
            disWLevel = Distance.far;
        }
        return disWLevel;
    }

    //
    // Get the distance level
    public Distance getDistanceLevel(double dis) {
        Distance level = null;
        if(dis < 0) {
            return level;
        } else if(dis < 300) {
            level = Distance.close;
        } else if(dis < 600) {
            level = Distance.medium;
        } else {
            level = Distance.far;
        }
        return level;
    }

    // Compute the Q
    public double calQ(double reward, boolean onPolicy) {
        double previousQ = lut.getQValue(
                preMyHP.ordinal(),
                preEneHP.ordinal(),
                preMyDistance.ordinal(),
                preWaDistance.ordinal(),
                preAction.ordinal()
        );

        double curQ = lut.getQValue(
                curMyHP.ordinal(),
                curEneHP.ordinal(),
                curMyDistance.ordinal(),
                curWaDistance.ordinal(),
                curAction.ordinal()
        );

        int bestActionIndex = lut.getBestAction(
                curMyHP.ordinal(),
                curEneHP.ordinal(),
                curMyDistance.ordinal(),
                curWaDistance.ordinal()
        );

        // Get the maximum Q ( Off-policy )
        double maxQ = lut.getQValue(
                curMyHP.ordinal(),
                curEneHP.ordinal(),
                curMyDistance.ordinal(),
                curWaDistance.ordinal(),
                bestActionIndex
        );

        // onPolicy : Sarsa
        // offPolicy : Q-Learning
        double res = onPolicy ?
                previousQ + alpha * (reward + gamma * curQ - previousQ) :
                previousQ + alpha * (reward + gamma * maxQ - previousQ);

        return res;
    }
    @Override
    public void run() {
        super.run();
        /* Customize the robot tank */
        setBulletColor(Color.red);
        setGunColor(Color.darkGray);
        setBodyColor(Color.blue);
        setRadarColor(Color.white);
        curMyHP = HP.high;
        // loadTable();
        while (true) {
            switch (myOperationMode) {
                case onScan: {
                    reward = 0.0;
                    turnRadarLeft(90);
                    break;
                }
                case onAction: {
                    curMyDistance = getDistanceFromWallLevel(myX, myY);

                    curActionIndex = (Math.random() <= epsilon)
                            ? lut.getRandomAction() // explore a random action
                            : lut.getBestAction(
                            getHPLevel(myHP).ordinal(),
                            getHPLevel(enemyHP).ordinal(),
                            getDistanceLevel(dis).ordinal(),
                            curMyDistance.ordinal()); // select greedy action

//                     System.out.println(curActionIndex);
                    curAction = Action.values()[curActionIndex];
//                    turnLeft(90);
                    switch (curAction) {
                        case fire: {
                            turnGunRight(getHeading() - getGunHeading() + enemyBearing);
                            fire(3);
                            break;
                        }

                        case left: {
                            setTurnLeft(30);
                            execute();
                            break;
                        }

                        case right: {
                            setTurnRight(30);
                            execute();
                            break;
                        }

                        case forward: {
                            setAhead(100);
                            execute();
                            break;
                        }
                        case back: {
                            setBack(100);
                            execute();
                            break;
                        }
                    }
                    int[] indexes = new int[]{
                            preMyHP.ordinal(),
                            preEneHP.ordinal(),
                            preMyDistance.ordinal(),
                            preWaDistance.ordinal(),
                            preAction.ordinal()
                    };
                    Q = calQ(reward, onPolicy);
                    lut.setQValue(indexes, Q);
                    myOperationMode = operaMode.onScan;
                }

            }
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        super.onScannedRobot(e);
        enemyBearing = e.getBearing();

        myX = getX();
        myY = getY();
        myHP = getEnergy();
        enemyHP = e.getEnergy();
        dis = e.getDistance();

        preMyHP = curMyHP;
        preEneHP = curEneHP;
        preMyDistance = curMyDistance;
        preWaDistance = curWaDistance;
        preAction = curAction;

        curMyHP = getHPLevel(myHP);
        curEneHP = getHPLevel(enemyHP);
        curMyDistance = getDistanceLevel(dis);
        curWaDistance = getDistanceFromWallLevel(myX, myY);
        myOperationMode = operaMode.onAction;

    }

    @Override
    public void onHitByBullet(HitByBulletEvent e){
        if(takeImmediate) {
            reward += immediatePenalty;
        }
    }

    @Override
    public void onBulletHit(BulletHitEvent e){
        if(takeImmediate) {
            reward += immediateBonus;
        }
    }

    @Override
    public void onBulletMissed(BulletMissedEvent e){
        if(takeImmediate) {
            reward += immediatePenalty;
        }
    }

    @Override
    public void onHitWall(HitWallEvent e){
        if(takeImmediate) {
            reward += immediatePenalty;
        }
        avidObstacle();
    }
    public void avidObstacle() {
        setBack(200);
        setTurnRight(60);
        execute();
    }
    @Override
    public void onHitRobot(HitRobotEvent e) {
        if(takeImmediate) {
            reward += immediatePenalty;
        }
        avidObstacle();
    }
    @Override
    public void onWin(WinEvent e){

        reward = terminalBonus;
        // why int instead double?
        int[] indexes = new int []{
                preMyHP.ordinal(),
                preEneHP.ordinal(),
                preMyDistance.ordinal(),
                preWaDistance.ordinal(),
                preAction.ordinal()};
        Q = calQ(reward, onPolicy);
        lut.setQValue(indexes, Q);
        winRound++;
        //saveTable();
        totalRound++;
        if((totalRound % 100 == 0) && (totalRound != 0)){
            winPercentage = (double) winRound / 100;
            System.out.println(String.format("%d, %.3f",++round, winPercentage));
            File folderDst1 = getDataFile(fileToSaveName);
            log.writeToFile(folderDst1, winPercentage, round);
            winRound = 0;
            //saveTable();
        }

    }

    @Override
    public void onDeath(DeathEvent e){

        reward = terminalPenalty;
        // why int instead of double?
        int[] indexes = new int []{
                preMyHP.ordinal(),
                preEneHP.ordinal(),
                preMyDistance.ordinal(),
                preWaDistance.ordinal(),
                preAction.ordinal()};
        Q = calQ(reward, onPolicy);
        lut.setQValue(indexes, Q);
        //saveTable();
        totalRound++;
        if((totalRound % 100 == 0) && (totalRound != 0)){
            winPercentage = (double) winRound / 100;
            System.out.println(String.format("%d, %.3f",++round, winPercentage));
            File folderDst1 = getDataFile(fileToSaveName);
            log.writeToFile(folderDst1, winPercentage, round);
            winRound = 0;
            //saveTable();
        }

    }
    public void saveTable() {
        try {
            lut.save(fileToSaveLUT);
        } catch (Exception e) {
            System.out.println("Save Error!" + e);
        }
    }

    public void loadTable() {
        try {
            lut.load(fileToSaveLUT);
        } catch (Exception e) {
            System.out.println("Save Error!" + e);
            lut = new LookUpTable(HP.values().length,
            HP.values().length,
            Distance.values().length,
            Distance.values().length,
            Action.values().length);
        }
    }
}
