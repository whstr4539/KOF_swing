package Client.Character;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//动作类，实现人物行走等基本功能
public class Dir {
    private Image currentMovement;//设置当前动作
    private boolean currentDir;//保存目标当前方向
    private Map<String,Image> moveMap = new HashMap<String, Image>();//动作对应地图

    public boolean LF;//往右前进
    public boolean LS;//往右站立
    public boolean LU;//右上
    public boolean LD;//右下

    public boolean RF;//往左前进
    public boolean RS;//朝左站立
    public boolean RU;//左上
    public boolean RD;//左下

    public boolean A;//攻击
    public boolean KICK;
    public boolean DEFEND;//防御
    public boolean FALL;//被击倒
    public boolean JUMPING;
    public boolean JUMP_UP;
    public boolean JUMP_DOWN;
    public boolean DASH_LEFT; // 快速向左移动
    public boolean DASH_RIGHT; // 快速向右移动
    
    // 受击倒状态开始的时间，用于做闪烁效果等
    private long fallStartTime;


    public Dir(boolean isLeft) {//初始状态是面向左还是面向右
        LF = false;
        LU = false;
        LD = false;

        RF = false;
        RU = false;
        RD = false;

        LS = isLeft;
        RS = !isLeft;

        //攻击和击倒
        A = false;
        FALL = false;
        KICK = false;
        JUMPING = false;
        JUMP_UP = false;
        JUMP_DOWN = false;
        DASH_LEFT = false;
        DASH_RIGHT = false;
    }



    public void createMap(ArrayList<Image> movements) {
        //将movement做成maps
        //Left Forward, Left Stand, ...往右走，往右停，往左走，往左停,左攻击，右攻击，左倒，右倒
        //添加踢腿相关键名
        String[] keys = {"LF","LS","RF","RS","LA","RA","LH","RH","LK","RK","LDEF","RDEF","LJ","RJ","L_DASH","R_DASH"};
        for(int i = 0; i < movements.size(); i++) {
            moveMap.put(keys[i],movements.get(i));
        }
        if(getCurrentDir()) setCurrentMovement(moveMap.get("LS"));
        else setCurrentMovement(moveMap.get("RS"));
    }//创建动作图对应maps

    public void locateDirection() {
        if(!LF && !RF && !RU && !LU && !RD && !LD && !A && !FALL && !KICK && !DEFEND && !JUMPING && !JUMP_UP && !JUMP_DOWN && !DASH_LEFT && !DASH_RIGHT) {//如果没有其他动作，就站立
            if (getCurrentDir() == Character.LEFT) {
                LS = true;
                setCurrentMovement(getMoveMap().get("LS"));
            } else {
                RS = true;
                setCurrentMovement(getMoveMap().get("RS"));
            }
        } else if(FALL) {//如果检测被击倒
            if(getCurrentDir() == Character.LEFT) setCurrentMovement(getMoveMap().get("LS"));
            else  setCurrentMovement(getMoveMap().get("RS"));
        } else if(DASH_LEFT) {
            // 快速向左移动时显示专门的快速移动动作
            setCurrentMovement(getMoveMap().get("L_DASH"));
        } else if(DASH_RIGHT) {
            // 快速向右移动时显示专门的快速移动动作
            setCurrentMovement(getMoveMap().get("R_DASH"));
        } else if(A) {//若检测到攻击键按下
            if(getCurrentDir() == Character.LEFT) setCurrentMovement(getMoveMap().get("LA"));//如果LA为真，则触发攻击动作
            else  setCurrentMovement(getMoveMap().get("RA"));
        } else if(KICK){
            if(getCurrentDir() == Character.LEFT) setCurrentMovement(getMoveMap().get("LK"));//如果LA为真，则触发攻击动作
            else  setCurrentMovement(getMoveMap().get("RK"));
        } else if(DEFEND){
            if(getCurrentDir() == Character.LEFT) setCurrentMovement(getMoveMap().get("LDEF"));//如果左防为真，则触发左防动作
            else  setCurrentMovement(getMoveMap().get("RDEF"));
        } else if(JUMPING){
            if (getCurrentDir() == Character.LEFT) setCurrentMovement(getMoveMap().get("LJ"));
            else  setCurrentMovement(getMoveMap().get("RJ"));
        } else {
            if (LF || LU || LD) {
                setCurrentMovement(getMoveMap().get("LF"));//否则前进
            } else if (RF || RU || RD){
                setCurrentMovement(getMoveMap().get("RF"));
            }
        }
    }//确定方向

    public int limitLocation(int position, int speed, int min, int max,boolean isPlus) {
        int tmp = 0;
        if(isPlus) {
            tmp = position + speed;
        } else tmp = position - speed;
        if(tmp >= min && tmp < max) return tmp;
        else return position;
    }//限制角色走出范围

    public void move(Graphics g, Character character) {//更新图片
        // 如果处于防御状态，禁止角色移动，只更新当前防御动作的绘制
        if (DEFEND) {
            character.drawCurrentMovement(g);
            // 防止角色与对手重叠（即使防御也要保持不穿模）
            if (otherCharacter != null) {
                Character.preventOverlap(character, otherCharacter);
            }
            return;
        }

        // 处理跳跃逻辑
        if (character.isJumping()) {
            // 更新跳跃速度（应用重力）
            character.updateJumpVelocity();

            // 更新Y位置
            character.getPosition().y += character.getJumpVelocity();

            // 水平边界限制（防止跳出屏幕左右两侧）
            int leftBound = 0;      // 左边界
            int rightBound = 730;   // 右边界（与原有limitLocation方法保持一致）
            if (character.getPosition().x < leftBound) {
                character.getPosition().x = leftBound;
            } else if (character.getPosition().x > rightBound) {
                character.getPosition().x = rightBound;
            }

            // 根据跳跃模式决定落地逻辑
            boolean shouldEndJump = false;

            // 模式1：原地跳跃模式（落回起跳位置）
            if (character.isJumpingAtSameSpot() && character.getJumpVelocity() > 0) {
                // 当跳跃高度开始回落，且回到初始位置时完成跳跃
                if (character.getPosition().y >= character.getJumpStartPosition().y) {
                    // 落回起跳位置
                    character.getPosition().setLocation(character.getJumpStartPosition());
                    shouldEndJump = true;
                }
            }
            if (character.getJumpVelocity() > 0 && character.getPosition().y >= character.getJumpStartPosition().y) {
                // 保持水平位置不变，只将Y位置设置为起跳时的Y坐标
                character.getPosition().y = character.getJumpStartPosition().y;
                shouldEndJump = true;
            }

            // 统一处理跳跃结束逻辑
            if (shouldEndJump) {
                character.setJumping(false);
                character.setOnGround(true);
                character.setJumpVelocity(0);
                JUMP_UP = false;
                JUMP_DOWN = false;
            }

            // 更新跳跃状态
            if (character.isJumping()) {
                if (character.getJumpVelocity() < 0) {
                    // 上升阶段
                    JUMP_UP = true;
                    JUMP_DOWN = false;
                } else {
                    // 下降阶段
                    JUMP_UP = false;
                    JUMP_DOWN = true;
                }
            }
        }

        // 处理水平移动（无论是否跳跃都允许）
        if(!FALL) {
            // 快速移动优先级高于普通移动
            if (DASH_RIGHT) {
                character.setDashing(true);
                character.setDashDirection(Character.RIGHT);
                character.getPosition().x = limitLocation(character.getPosition().x, character.getDASH_SPEED(), 0, 730, true);
            } else if (DASH_LEFT) {
                character.setDashing(true);
                character.setDashDirection(Character.LEFT);
                character.getPosition().x = limitLocation(character.getPosition().x, character.getDASH_SPEED(), 0, 730, false);
            } else if(!LS && !RS) {
                character.setDashing(false);
                if (LF) {//往右走
                    character.getPosition().x = limitLocation(character.getPosition().x, character.getSPEED(), 0, 730, true);
                }
                if (RF) {//往左走
                    character.getPosition().x = limitLocation(character.getPosition().x, character.getSPEED(), 0, 730, false);
                }
            }
            
            // 只有不在跳跃时才允许垂直移动（地面移动）
            if (!character.isJumping()) {
                if (RU || LU) {//往上走
                    character.getPosition().y = limitLocation(character.getPosition().y, character.getSPEED(), 120, 260, false);
                }
                if (RD || LD) {//往下走
                    character.getPosition().y = limitLocation(character.getPosition().y, character.getSPEED(), 120, 260, true);
                }
            }
        }

        character.drawCurrentMovement(g);
        
        // 防止角色重叠
        if (otherCharacter != null) {
            Character.preventOverlap(character, otherCharacter);
        }
    }//实现上下左右行走

    //修正服务器控制角色的位置，缓解两个客户端同一个角色出现的位置错位情况
    public static void refineMovement(Character character, Point position) {
        character.getPosition().setLocation(position);
    }
    
    /**
     * 设置另一个角色，用于防止重叠
     * @param otherCharacter 另一个角色
     */
    public void setOtherCharacter(Character otherCharacter) {
        this.otherCharacter = otherCharacter;
    }
    
    private Character otherCharacter; // 另一个角色，用于防止重叠

    //触发角色被击倒画面
    public void fighterFall() {
        Runnable task1 = new Runnable() {
            @Override
            public void run() {
                // 记录开始被击倒的时间
                fallStartTime = System.currentTimeMillis();
                FALL = true;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                FALL = false;
            }
        };
        Thread thread1 = new Thread(task1);
        thread1.start();
    }
    
    /**
     * 获取被击倒状态开始的时间，用于闪烁等效果
     */
    public long getFallStartTime() {
        return fallStartTime;
    }

    public Image getCurrentMovement() {
        return currentMovement;
    }
    public void setCurrentDir(boolean currentDir) {
        this.currentDir = currentDir;
    }
    public boolean getCurrentDir() {
        return currentDir;
    }
    public Map<String, Image> getMoveMap() {
        return moveMap;
    }
    public void setCurrentMovement(Image currentMovement) {
        this.currentMovement = currentMovement;
    }
    
    /**
     * 获取当前动作的键名
     * @return 当前动作的键名，如"LS", "RS", "LDEF", "RDEF"等
     */
    public String getCurrentAction() {
        if (FALL) {
            return getCurrentDir() == Character.LEFT ? "LH" : "RH";
        } else if (A) {
            return getCurrentDir() == Character.LEFT ? "LA" : "RA";
        } else if (KICK) {
            return getCurrentDir() == Character.LEFT ? "LK" : "RK";
        } else if (DEFEND) {
            return getCurrentDir() == Character.LEFT ? "LDEF" : "RDEF";
        } else if(JUMPING){
            return getCurrentDir() == Character.LEFT ? "LJ" : "RJ";
        } else if (DASH_LEFT) {
            return "L_DASH"; // 快速向左移动时返回左快速移动动作键名
        } else if (DASH_RIGHT) {
            return "R_DASH"; // 快速向右移动时返回右快速移动动作键名
        } else if (LF || LU || LD) {
            return "LF";
        } else if (RF || RU || RD) {
            return "RF";
        } else {
            return getCurrentDir() == Character.LEFT ? "LS" : "RS";
        }
    }
}
