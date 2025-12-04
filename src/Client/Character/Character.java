package Client.Character;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.util.ArrayList;


public class Character {
    public static final boolean LEFT = true;
    public static final boolean RIGHT = false;

    private String name;//玩家名字

    private int HP = 5;//血量

    private  ArrayList<Image> movements;//人物角色的动作：左右停

    private int SPEED = 5;//当前移动速度（放大人物后适当提高速度）
    private int DASH_SPEED = 10;//快速移动速度（普通速度的2倍）

    private Point position;//当前位置

    private Dir dir;//动作按钮类
    
    private Hitbox hitbox; // 碰撞箱
    private AttackBox leftAttackBox; // 向左攻击箱
    private AttackBox rightAttackBox;// 向右攻击箱
    private AttackBox leftKickBox;
    private AttackBox rightKickBox;
    
    private long lastAttackTime = 0;//上次攻击时间
    private long lastKickTime = 0;
    private static final long ATTACK_COOLDOWN = 600;//攻击冷却时间（毫秒）
    private static final long KICK_COOLDOWN = 600;
    private static final long JUMP_COOLDOWN = 900; // 跳跃冷却时间（毫秒）
    private boolean isOnGround = true; // 角色是否在地面
    private double jumpVelocity = 0;  // 跳跃速度
    private final double GRAVITY = 0.5; // 重力加速度
    private final double JUMP_FORCE = -12.0; // 跳跃力度
    private Point jumpStartPosition = new Point(0, 0); // 起跳位置
    private boolean isJumpingAtSameSpot = false; // 是否在同一位置跳跃
    private long lastJumpTime = 0; // 上次跳跃时间
    private boolean isDashing = false; // 是否正在快速移动
    private boolean dashDirection = Character.RIGHT; // 快速移动方向
    
    private ArrayList<Dimension> movementSizes; // 新增：保存每个动作图片的实际尺寸
    
    // 左侧跳跃关键帧（上升/下降）
    private Image leftJumpUpFrame;
    private Image leftJumpDownFrame;
    private Dimension leftJumpUpSize;
    private Dimension leftJumpDownSize;
    
    // 右侧跳跃关键帧（上升/下降）
    private Image rightJumpUpFrame;
    private Image rightJumpDownFrame;
    private Dimension rightJumpUpSize;
    private Dimension rightJumpDownSize;
    
    public Character(String name, boolean leftOrRight,String rootDir,int x, int y ) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        movements = new ArrayList<Image>();
        movementSizes = new ArrayList<Dimension>(); // 初始化尺寸列表
        position = new Point(x,y);


        for(int i = 1; i <= 16; i++) {
            // 添加资源路径检查和调试信息
            String resourcePath = rootDir + "/" + i + ".gif";
            java.net.URL resourceUrl = Character.class.getClassLoader().getResource(resourcePath);

            Image image = null;
            Dimension size = null;
            if (resourceUrl != null) {
                // 使用ImageIcon预加载图片
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(resourceUrl);
                image = icon.getImage();
                size = new Dimension(icon.getIconWidth(), icon.getIconHeight());
                System.out.println("成功加载图片: " + resourcePath + ", 实际尺寸: " +
                        size.width + "x" + size.height);
            } else {

                // 如果资源不存在，添加null到列表并输出警告
                image = null;
                size = null;
                System.err.println("警告: 无法找到图片资源: " );
            }
            movements.add(image);
            movementSizes.add(size);
        }
        dir = new Dir(leftOrRight);//监控动作
        dir.setCurrentDir(leftOrRight);
        dir.createMap(movements);//创建对应动作maps
        
        // 将防御动作的 GIF 固定为静态帧，避免一直播放导致视觉怪异
        freezeDefendFrame("LDEF");
        freezeDefendFrame("RDEF");
        // 使用你导出的关键帧 PNG 覆盖左右防御动作
        // 假设资源目录结构为 images/<角色名>/LD 或 RD
        setDefendImageFromPng("LDEF", rootDir + "/LD/frame_2.png");
        setDefendImageFromPng("RDEF", rootDir + "/RD/frame_2.png");
        
        // 加载左侧跳跃的上升/下降关键帧（LJ_up.png / LJ_down.png）
        loadLeftJumpFrames(rootDir);
        // 加载右侧跳跃的上升/下降关键帧（RJ_up.png / RJ_down.png）
        loadRightJumpFrames(rootDir);
        
        // 初始化碰撞箱（使用边距参数）
        // 这些值需要根据实际图片调整，暂时使用示例值
        this.hitbox = new Hitbox(this, 10, 100, 10, 150);
        
        // 初始化攻击箱
        this.leftAttackBox = new AttackBox(this, "LA");
        this.rightAttackBox = new AttackBox(this, "RA");
        this.leftKickBox = new AttackBox(this,"LK");
        this.rightKickBox = new AttackBox(this,"RK");

    }//将图片载入缓存区, 并且做好索引

    /**
     * 将某个动作键对应的 GIF 转换为静态图片（当前帧），用于防御等需要保持姿势的状态
     * @param key 动作键名，如"LDEF"、"RDEF"
     */
    private void freezeDefendFrame(String key) {
        if (dir == null || dir.getMoveMap() == null) {
            return;
        }
        Image original = dir.getMoveMap().get(key);
        if (original == null) {
            return;
        }

        // 构造一个与原图尺寸相同的 BufferedImage，只绘制一次得到静态帧
        int width = original.getWidth(null);
        int height = original.getHeight(null);
        if (width <= 0 || height <= 0) {
            return;
        }
        BufferedImage staticImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = staticImg.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        // 替换 Dir 中的动作图片
        dir.getMoveMap().put(key, staticImg);

        // 同步替换 movements 列表中的引用，保证尺寸查询仍然有效
        for (int i = 0; i < movements.size(); i++) {
            if (movements.get(i) == original) {
                movements.set(i, staticImg);
                // 若之前尺寸为空，则补上
                if (movementSizes != null) {
                    Dimension size = movementSizes.get(i);
                    if (size == null) {
                        movementSizes.set(i, new Dimension(width, height));
                    }
                }
                break;
            }
        }
    }

    /**
     * 使用导出的 PNG 静态帧替换某个防御动作图片
     * @param key          动作键名，如"LDEF"
     * @param resourcePath 类路径下的资源路径，例如 "images/cao/LD/frame_2.png"
     */
    private void setDefendImageFromPng(String key, String resourcePath) {
        if (dir == null || dir.getMoveMap() == null) {
            return;
        }
        java.net.URL url = Character.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            System.err.println("找不到防御帧资源: " + resourcePath);
            return;
        }

        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(url);
        Image newImg = icon.getImage();
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();

        if (width <= 0 || height <= 0) {
            System.err.println("防御帧尺寸异常: " + resourcePath);
            return;
        }

        // 旧图，用于在 movements 里做替换
        Image old = dir.getMoveMap().get(key);

        // 替换 Dir 中的动作图片
        dir.getMoveMap().put(key, newImg);

        // 同步替换 movements 列表中的引用，保证尺寸查询仍然有效
        if (old != null) {
            for (int i = 0; i < movements.size(); i++) {
                if (movements.get(i) == old) {
                    movements.set(i, newImg);
                    if (movementSizes != null) {
                        if (i < movementSizes.size()) {
                            movementSizes.set(i, new Dimension(width, height));
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * 加载左侧跳跃关键帧（上升 / 下降），文件名约定为：
     * rootDir + "/LJ_up.png" 和 rootDir + "/LJ_down.png"
     */
    private void loadLeftJumpFrames(String rootDir) {
        // 上升帧
        java.net.URL upUrl = Character.class.getClassLoader().getResource(rootDir + "/LJ_up.png");
        if (upUrl != null) {
            javax.swing.ImageIcon upIcon = new javax.swing.ImageIcon(upUrl);
            leftJumpUpFrame = upIcon.getImage();
            leftJumpUpSize = new Dimension(upIcon.getIconWidth(), upIcon.getIconHeight());
        } else {
            System.err.println("未找到左跳跃上升帧: " + rootDir + "/LJ_up.png");
        }

        // 下降帧
        java.net.URL downUrl = Character.class.getClassLoader().getResource(rootDir + "/LJ_down.png");
        if (downUrl != null) {
            javax.swing.ImageIcon downIcon = new javax.swing.ImageIcon(downUrl);
            leftJumpDownFrame = downIcon.getImage();
            leftJumpDownSize = new Dimension(downIcon.getIconWidth(), downIcon.getIconHeight());
        } else {
            System.err.println("未找到左跳跃下降帧: " + rootDir + "/LJ_down.png");
        }
    }

    /**
     * 加载右侧跳跃关键帧（上升 / 下降），文件名约定为：
     * rootDir + "/RJ_up.png" 和 rootDir + "/RJ_down.png"
     */
    private void loadRightJumpFrames(String rootDir) {
        // 上升帧
        java.net.URL upUrl = Character.class.getClassLoader().getResource(rootDir + "/RJ_up.png");
        if (upUrl != null) {
            javax.swing.ImageIcon upIcon = new javax.swing.ImageIcon(upUrl);
            rightJumpUpFrame = upIcon.getImage();
            rightJumpUpSize = new Dimension(upIcon.getIconWidth(), upIcon.getIconHeight());
        } else {
            System.err.println("未找到右跳跃上升帧: " + rootDir + "/RJ_up.png");
        }

        // 下降帧
        java.net.URL downUrl = Character.class.getClassLoader().getResource(rootDir + "/RJ_down.png");
        if (downUrl != null) {
            javax.swing.ImageIcon downIcon = new javax.swing.ImageIcon(downUrl);
            rightJumpDownFrame = downIcon.getImage();
            rightJumpDownSize = new Dimension(downIcon.getIconWidth(), downIcon.getIconHeight());
        } else {
            System.err.println("未找到右跳跃下降帧: " + rootDir + "/RJ_down.png");
        }
    }


    //将当前动作画上面板（可以重写调整每一帧动作的图片位置）
    protected void drawCurrentMovement(Graphics g) {
        // 先更新方向再获取当前动作
        dir.locateDirection();//更新目前的动作
        Image currentMovement = dir.getCurrentMovement();
        String currentAction = dir.getCurrentAction();
        
        // 若处于被击倒状态，做一个简单的闪烁效果（例如 100ms 一次开关）
        // 不额外导入动图，通过“画 / 不画”制造闪烁两下的感觉
        if (dir.FALL) {
            long elapsed = System.currentTimeMillis() - dir.getFallStartTime();
            int interval = 100; // 闪烁间隔（毫秒），可根据手感微调
            // 这里简单让角色在偶数段不绘制，从而产生闪烁
            if ((elapsed / interval) % 2 == 0) {
                return;
            }
        }
        
        // 跳跃动作：对于左右跳跃，使用你提供的上升/下降关键帧替代原 GIF
        if ("LJ".equals(currentAction) && dir.getCurrentDir() == Character.LEFT) {
            if (getJumpVelocity() < 0 && leftJumpUpFrame != null) {
                currentMovement = leftJumpUpFrame;
            } else if (getJumpVelocity() >= 0 && leftJumpDownFrame != null) {
                currentMovement = leftJumpDownFrame;
            }
        } else if ("RJ".equals(currentAction) && dir.getCurrentDir() == Character.RIGHT) {
            if (getJumpVelocity() < 0 && rightJumpUpFrame != null) {
                currentMovement = rightJumpUpFrame;
            } else if (getJumpVelocity() >= 0 && rightJumpDownFrame != null) {
                currentMovement = rightJumpDownFrame;
            }
        }

        // 添加null检查避免空指针异常
        if (currentMovement != null) {
            // 更新碰撞箱位置
            hitbox.updatePosition();
            
            // 更新攻击箱位置
            leftAttackBox.updatePosition();
            rightAttackBox.updatePosition();
            leftKickBox.updatePosition();
            rightKickBox.updatePosition();

            
            // 获取绘制尺寸：左右跳跃关键帧单独使用自身尺寸，其它动作用预加载尺寸
            Dimension actualSize = null;
            if ("LJ".equals(currentAction) && dir.getCurrentDir() == Character.LEFT) {
                if (currentMovement == leftJumpUpFrame && leftJumpUpSize != null) {
                    actualSize = leftJumpUpSize;
                } else if (currentMovement == leftJumpDownFrame && leftJumpDownSize != null) {
                    actualSize = leftJumpDownSize;
                }
            } else if ("RJ".equals(currentAction) && dir.getCurrentDir() == Character.RIGHT) {
                if (currentMovement == rightJumpUpFrame && rightJumpUpSize != null) {
                    actualSize = rightJumpUpSize;
                } else if (currentMovement == rightJumpDownFrame && rightJumpDownSize != null) {
                    actualSize = rightJumpDownSize;
                }
            }
            if (actualSize == null) {
                actualSize = getMovementSize(currentMovement);
            }
            if (actualSize != null) {
                // 使用准确尺寸进行缩放
                int drawWidth = actualSize.width * 2;
                int drawHeight = actualSize.height * 2;
                int originalWidth = currentMovement.getWidth(null);
                int originalHeight = currentMovement.getHeight(null);
                // 绘制图片，使用准确尺寸避免缩放问题
                g.drawImage(currentMovement,
                        (int)position.getX(),
                        (int)position.getY(),
                        drawWidth,
                        drawHeight,
                        //originalWidth * 2,
                        //originalHeight * 2,
                        null);
                //if(currentAction.equals("LDEF"))System.out.println(drawWidth+","+drawHeight);
            } else {

                System.out.println("图片未加载完成，跳过绘制");

            }
        }

    }
    
    /**
     * 根据图片对象获取预加载时保存的准确尺寸
     * @param movement 动作图片
     * @return 图片尺寸，如果未找到或尺寸数组为空，返回默认尺寸
     */
    private Dimension getMovementSize(Image movement) {
        // 检查movementSizes数组是否为空
        if (movementSizes == null || movementSizes.isEmpty()) {
            // 返回默认尺寸以避免异常
            return new Dimension(30, 60);
        }
        
        for (int i = 0; i < movements.size(); i++) {
            if (movements.get(i) == movement) {
                // 检查索引是否在有效范围内
                if (i < movementSizes.size()) {
                    return movementSizes.get(i);
                } else {
                    // 索引超出范围时返回默认尺寸
                    return new Dimension(30, 60);
                }
            }
        }
        // 未找到对应图片时返回默认尺寸
        return new Dimension(30, 60);
    }

    /**
     * 当fighter1成功攻击fighter2时，返回true，否则返回false
     * @param fighter1 攻击者
     * @param fighter2 被攻击者
     * @return 是否攻击成功
     */
    public static boolean isAttacked(Character fighter1,Character fighter2) {
        // 如果被攻击者处于击倒状态，则无法再次被攻击
        if(fighter2.getDir().FALL) {
            System.out.println("攻击检测: 被攻击者处于FALL状态，无法被攻击");
            return false;
        }
        
        // 如果被攻击者处于防御状态，则不造成伤害
        if(fighter2.getDir().DEFEND) {
            System.out.println("攻击检测: 被攻击者处于DEFEND状态，攻击被防御");
            return false;
        }
        
        // 根据攻击者的方向选择对应的攻击箱进行检测
        AttackBox attackBox = null;
        if (fighter1.getDir().getCurrentDir() == Character.RIGHT) {
            // 使用向左攻击箱
            attackBox = fighter1.getLeftAttackBox();
        } else {
            // 使用向右攻击箱
            attackBox = fighter1.getRightAttackBox();
        }
        
        // 检查攻击箱是否与被攻击者的碰撞箱相交
        boolean isHit = attackBox.intersects(fighter2.getHitbox());
        
        if (isHit) {
            Point p1 = fighter1.getPosition();
            Point p2 = fighter2.getPosition();
            System.out.println("attack! 攻击者位置: (" + p1.x + ", " + p1.y + "), 被攻击者位置: (" + p2.x + ", " + p2.y + ") - 攻击成功！");
            return true;
        } else {
            Point p1 = fighter1.getPosition();
            Point p2 = fighter2.getPosition();
            System.out.println("攻击检测: 攻击者位置: (" + p1.x + ", " + p1.y + "), 被攻击者位置: (" + p2.x + ", " + p2.y + ") - 攻击失败");
        }
        
        return false;
    }
    
    /**
     * 防止两个角色重叠
     * @param character1 角色1
     * @param character2 角色2
     */
    public static void preventOverlap(Character character1, Character character2) {
        
        // 检查两个角色的碰撞箱是否相交
        if (character1.getHitbox().intersects(character2.getHitbox())) {
            // 获取两个角色的位置
            Point pos1 = character1.getPosition();
            Point pos2 = character2.getPosition();
            
            // 计算位置差异
            int dx = pos1.x - pos2.x;
            int dy = pos1.y - pos2.y;
            
            // 确定分离方向（优先水平分离）
            if (Math.abs(dx) > Math.abs(dy)) {
                // 水平分离
                if (dx > 0) {
                    // character1在右侧，将character1向右移动，character2向左移动
                    // 如果角色处于跳跃状态，不改变垂直位置
                    if (!character1.isJumping()) {
                        pos1.x += 5;
                    }
                    if (!character2.isJumping()) {
                        pos2.x -= 5;
                    }
                } else {
                    // character1在左侧，将character1向左移动，character2向右移动
                    if (!character1.isJumping()) {
                        pos1.x -= 5;
                    }
                    if (!character2.isJumping()) {
                        pos2.x += 5;
                    }
                }
            } else {
                // 垂直分离 - 跳跃中的角色不受影响，以保证跳跃动作完整性
                if (dy > 0) {
                    // character1在下方，将character1向下移动，character2向上移动
                    if (!character1.isJumping()) {
                        pos1.y += 5;
                    }
                    if (!character2.isJumping()) {
                        pos2.y -= 5;
                    }
                } else {
                    // character1在上方，将character1向上移动，character2向下移动
                    if (!character1.isJumping()) {
                        pos1.y -= 5;
                    }
                    if (!character2.isJumping()) {
                        pos2.y += 5;
                    }
                }
            }
            
            // 确保角色不会移出边界
            pos1.x = Math.max(0, Math.min(730, pos1.x));
            pos2.x = Math.max(0, Math.min(730, pos2.x));
            pos1.y = Math.max(120, Math.min(260, pos1.y));
            pos2.y = Math.max(120, Math.min(260, pos2.y));
        }
    }

    public Dir getDir() {
        return dir;
    }
    public Point getPosition() {
        return position;
    }
    public Hitbox getHitbox() {
        return hitbox;
    }
    public AttackBox getLeftAttackBox() {
        return leftAttackBox;
    }
    public AttackBox getRightAttackBox() {
        return rightAttackBox;
    }
    public AttackBox getLeftKickBox() {
        return leftKickBox;
    }
    public AttackBox getRightKickBox() {
        return rightKickBox;
    }
    public int getSPEED() {
        return SPEED;
    }
    public int getHP() {
        return HP;
    }
    public void setHP(int HP) {
        this.HP = HP;
    }
    
    /**
     * 设置移动速度
     * @param speed 新的移动速度
     */
    public void setSPEED(int speed) {
        this.SPEED = speed;
    }
    
    /**
     * 检查是否可以攻击（冷却时间是否结束）
     * @return 是否可以攻击
     */
    public boolean canAttack() {
        return System.currentTimeMillis() - lastAttackTime >= ATTACK_COOLDOWN;
    }
    
    /**
     * 设置攻击时间，开始冷却
     */
    public void setAttackTime() {
        lastAttackTime = System.currentTimeMillis();
    }
    
    /**
     * 获取剩余冷却时间
     * @return 剩余冷却时间（毫秒）
     */
    public long getRemainingCooldown() {
        long elapsed = System.currentTimeMillis() - lastAttackTime;
        return Math.max(0, ATTACK_COOLDOWN - elapsed);
    }
    
    /**
     * 检查是否可以踢腿（冷却时间是否结束）
     * @return 是否可以踢腿
     */
    public boolean canKick() {
        return System.currentTimeMillis() - lastKickTime >= KICK_COOLDOWN;
    }
    
    /**
     * 设置踢腿时间，开始冷却
     */
    public void setKickTime() {
        lastKickTime = System.currentTimeMillis();
    }
    
    /**
     * 获取踢腿剩余冷却时间
     * @return 剩余冷却时间（毫秒）
     */
    public long getKickRemainingCooldown() {
        long elapsed = System.currentTimeMillis() - lastKickTime;
        return Math.max(0, KICK_COOLDOWN - elapsed);
    }
    
    /**
     * 检查角色是否正在踢腿
     * @return 是否正在踢腿
     */
    public boolean isKicked() {
        return dir.KICK;
    }
    
    /**
     * 设置踢腿状态
     * @param kicked 是否踢腿
     */
    public void setKicked(boolean kicked) {
        dir.KICK = kicked;
    }
    
    /**
     * 开始踢腿动作
     */
    public void startKick() {
        // 只有不在冷却状态才能踢腿
        if (canKick()) {
            setKicked(true);
            setKickTime();
            // 踢腿动画通常持续较短时间，这里可以添加定时器来重置踢腿状态
            new Thread(() -> {
                try {
                    Thread.sleep(300); // 踢腿动作持续时间
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setKicked(false);
            }).start();
        }
    }
    
    /**
     * 当fighter1成功踢中fighter2时，返回true，否则返回false
     * @param fighter1 踢腿者
     * @param fighter2 被踢者
     * @return 是否踢中成功
     */
    public static boolean isKicked(Character fighter1, Character fighter2) {
        // 如果被踢者处于击倒状态，则无法再次被踢
        if (fighter2.getDir().FALL) {
            System.out.println("踢腿检测: 被踢者处于FALL状态，无法被踢");
            return false;
        }
        
        // 如果被踢者处于防御状态，则不造成伤害
        if (fighter2.getDir().DEFEND) {
            System.out.println("踢腿检测: 被踢者处于DEFEND状态，踢腿被防御");
            return false;
        }
        
        // 根据踢腿者的方向选择对应的踢腿箱进行检测
        AttackBox kickBox = null;
        if (fighter1.getDir().getCurrentDir() == Character.RIGHT) {
            // 使用向左踢腿箱
            kickBox = fighter1.getLeftKickBox();
        } else {
            // 使用向右踢腿箱
            kickBox = fighter1.getRightKickBox();
        }
        
        // 检查踢腿箱是否与被踢者的碰撞箱相交
        boolean isHit = kickBox.intersects(fighter2.getHitbox());
        
        if (isHit) {
            Point p1 = fighter1.getPosition();
            Point p2 = fighter2.getPosition();
            System.out.println("kick! 踢腿者位置: (" + p1.x + ", " + p1.y + "), 被踢者位置: (" + p2.x + ", " + p2.y + ") - 踢腿成功！");
            return true;
        } else {
            Point p1 = fighter1.getPosition();
            Point p2 = fighter2.getPosition();
            System.out.println("踢腿检测: 踢腿者位置: (" + p1.x + ", " + p1.y + "), 被踢者位置: (" + p2.x + ", " + p2.y + ") - 踢腿失败");
        }
        
        return false;
    }
    
    // 跳跃相关方法
    
    /**
     * 检查角色是否正在跳跃
     * @return 是否正在跳跃
     */
    public boolean isJumping() {
        return dir.JUMPING;
    }
    
    /**
     * 设置角色跳跃状态
     * @param jumping 是否跳跃
     */
    public void setJumping(boolean jumping) {
        dir.JUMPING = jumping;
    }
    
    /**
     * 检查角色是否在地面
     * @return 是否在地面
     */
    public boolean isOnGround() {
        return isOnGround;
    }
    
    /**
     * 设置角色地面状态
     * @param onGround 是否在地面
     */
    public void setOnGround(boolean onGround) {
        this.isOnGround = onGround;
    }
    
    /**
     * 获取跳跃速度
     * @return 跳跃速度
     */
    public double getJumpVelocity() {
        return jumpVelocity;
    }
    
    /**
     * 设置跳跃速度
     * @param jumpVelocity 新的跳跃速度
     */
    public void setJumpVelocity(double jumpVelocity) {
        this.jumpVelocity = jumpVelocity;
    }
    
    /**
     * 更新跳跃速度（应用重力）
     */
    public void updateJumpVelocity() {
        // 应用重力增加跳跃速度（正数表示向下）
        jumpVelocity += GRAVITY;
    }
    
    /**
     * 获取地面Y坐标
     * @return 地面Y坐标
     */
    public int getGroundY() {
        // 固定地面Y坐标为260
        return 260;
    }
    
    /**
     * 检查是否可以跳跃（考虑冷却时间）
     * @return 是否可以跳跃
     */
    public boolean canJump() {
        return System.currentTimeMillis() - lastJumpTime >= JUMP_COOLDOWN;
    }
    
    /**
     * 设置跳跃时间（用于冷却计算）
     */
    public void setJumpTime() {
        lastJumpTime = System.currentTimeMillis();
    }
    
    /**
     * 获取跳跃剩余冷却时间
     * @return 剩余冷却时间（毫秒）
     */
    public long getJumpRemainingCooldown() {
        long elapsed = System.currentTimeMillis() - lastJumpTime;
        return Math.max(0, JUMP_COOLDOWN - elapsed);
    }
    
    /**
     * 开始跳跃
     */
    public void startJump() {
        // 只有在地面上且跳跃冷却结束才能开始跳跃
        if (isOnGround() && canJump()) {
            setOnGround(false);
            setJumping(true);
            // 保存起跳位置
            jumpStartPosition.setLocation(position);
            // 设置初始跳跃速度
            setJumpVelocity(JUMP_FORCE);
            // 记录跳跃时间
            setJumpTime();
        }
    }
    
    /**
     * 获取起跳位置
     * @return 起跳位置
     */
    public Point getJumpStartPosition() {
        return jumpStartPosition;
    }
    
    /**
     * 是否在同一位置跳跃
     * @return 是否在同一位置跳跃
     */
    public boolean isJumpingAtSameSpot() {
        return isJumpingAtSameSpot;
    }
    
    /**
     * 设置是否在同一位置跳跃
     * @param jumpingAtSameSpot 是否在同一位置跳跃
     */
    public void setJumpingAtSameSpot(boolean jumpingAtSameSpot) {
        this.isJumpingAtSameSpot = jumpingAtSameSpot;
    }
    
    public boolean isDashing() {
        return isDashing;
    }
    
    public void setDashing(boolean dashing) {
        isDashing = dashing;
    }
    
    public boolean getDashDirection() {
        return dashDirection;
    }
    
    public void setDashDirection(boolean direction) {
        dashDirection = direction;
    }
    
    public int getDASH_SPEED() {
        return DASH_SPEED;
    }
}

