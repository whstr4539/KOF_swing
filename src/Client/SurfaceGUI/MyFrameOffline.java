package Client.SurfaceGUI;

import Client.Advance.ImageButton;
import Client.Advance.Music;
import Client.Character.Character;
import Client.SurfaceGUI.BeginGUI;
import Client.SurfaceGUI.CharacterSelectionGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 单机模式游戏主界面
 * 继承JFrame，实现人机对战
 */
public class MyFrameOffline extends JFrame {
    private Character playerFighter;//玩家控制的角色
    private Character aiFighter;//AI控制的角色
    private AIController aiController;//AI控制器
    private ImageButton replay;//重玩按钮
    private ImageButton quit;//退出按钮
    
    private Surface surface;//画板
    private Thread paintThread;//刷新界面线程
    
    private boolean isFinish;//游戏是否结束
    // 胜利图像标签
    private JLabel victoryLabel;
    // 失败图像标签
    private JLabel defeatLabel;
    
    // 音乐
    private Music readyBgm; // 游戏准备音效
    private Music mainBgm; // 游戏进行中的背景音乐

    public MyFrameOffline(int playerNumber) {
        this(playerNumber, "images/cao"); // 默认使用cao角色
    }
    
    public MyFrameOffline(int playerNumber, String playerCharacterPath) {
        super("单机模式");
        
        // 创建两个战斗机对象
        // 根据玩家选择的角色路径创建玩家角色
        Character playerFighterObj = new Character("player", true, playerCharacterPath, 50, 180);
        // AI角色使用另一个角色
        String aiCharacterPath = playerCharacterPath.equals("images/cao") ? "images/Chris" : "images/cao";
        Character aiFighterObj = new Character("ai", true, aiCharacterPath, 650, 180);
        
        if (playerNumber == 1) {
            playerFighter = playerFighterObj;
            aiFighter = aiFighterObj;
        } else {
            playerFighter = aiFighterObj;
            aiFighter = playerFighterObj;
        }
        
        // 设置角色间的相互引用，用于防止重叠
        playerFighter.getDir().setOtherCharacter(aiFighter);
        aiFighter.getDir().setOtherCharacter(playerFighter);
        
        // 设置AI移动速度为较慢的值（玩家速度的一半）
        aiFighter.setSPEED(5);
        
        // 初始化AI控制器
        aiController = new AIController(aiFighter, playerFighter);
        
        // 启动游戏音乐
        readyBgm = new Music("ready.mp3", Music.ONCE);
        readyBgm.start();
        mainBgm = new Music("gaming.mp3", Music.LOOP);
        mainBgm.start();
        
        // 游戏结束时的ko音效监听线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(isFinish) {
                        //结束音效
                        System.out.println("@finished!@");

                        mainBgm.stopMusic();
                        Music koBgm = new Music("ko.mp3", Music.ONCE);
                        koBgm.start();
                        break;
                    }
                    // 添加适当延迟，减少CPU占用
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        
        launchFrame();
        
        // 设置窗口属性
        setSize(840, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
        
        // 启动AI控制线程
        new Thread(aiController).start();
    }
    
    public void launchFrame() {
        // 添加画板
        surface = new Surface();
        this.addKeyListener(new KeyListener());
        this.add(surface);
        
        surface.setDoubleBuffered(true);//开启双缓冲
        
        // 添加重玩按钮
        replay = new ImageButton("/images/component/replayBt.png", 2.0/5,2.0/5);
        replay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 回到角色选择界面
                dispose();
                CharacterSelectionGUI selectionGUI = new CharacterSelectionGUI(1, true);
            }
        });
        
        // 添加返回菜单按钮
        quit = new ImageButton("/images/component/quitBt.png", 2.0/5,2.0/5);
        quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 返回主菜单
                dispose();
                BeginGUI beginGUI = new BeginGUI();
            }
        });
        
        // 创建一个面板来容纳胜利/失败标签
        JPanel resultPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 设置面板背景透明
            }
        };
        resultPanel.setOpaque(false);
        resultPanel.setLayout(new BorderLayout()); // 使用BorderLayout更容易控制居中
        
        // 添加胜利图像标签
        victoryLabel = new JLabel();
        ImageIcon victoryIcon = new ImageIcon(Character.class.getClassLoader().getResource("images/victory.png"));
        // 缩放图像到合适大小
        Image victoryImg = victoryIcon.getImage();
        Image scaledVictoryImg = victoryImg.getScaledInstance(400, 200, Image.SCALE_SMOOTH);
        victoryLabel.setIcon(new ImageIcon(scaledVictoryImg));
        victoryLabel.setHorizontalAlignment(JLabel.CENTER);
        victoryLabel.setVerticalAlignment(JLabel.CENTER);
        victoryLabel.setVisible(false); // 一开始不可见
        
        // 添加失败图像标签
        defeatLabel = new JLabel();
        ImageIcon defeatIcon = new ImageIcon(Character.class.getClassLoader().getResource("images/defeat.png"));
        // 缩放图像到合适大小
        Image defeatImg = defeatIcon.getImage();
        Image scaledDefeatImg = defeatImg.getScaledInstance(400, 200, Image.SCALE_SMOOTH);
        defeatLabel.setIcon(new ImageIcon(scaledDefeatImg));
        defeatLabel.setHorizontalAlignment(JLabel.CENTER);
        defeatLabel.setVerticalAlignment(JLabel.CENTER);
        defeatLabel.setVisible(false); // 一开始不可见
        
        // 创建一个容器面板用于居中显示标签，并添加垂直边距
        JPanel labelContainer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        labelContainer.setOpaque(false);
        labelContainer.setBorder(BorderFactory.createEmptyBorder(100, 0, 0, 0)); // 上边距100像素
        labelContainer.add(victoryLabel);
        labelContainer.add(defeatLabel);
        
        resultPanel.add(labelContainer, BorderLayout.CENTER);
        
        surface.setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(replay);
        buttonPanel.add(quit);
        
        // 一开始不可见
        replay.setVisible(false);
        quit.setVisible(false);
        
        surface.add(resultPanel, BorderLayout.NORTH);
        surface.add(buttonPanel, BorderLayout.SOUTH);
        
        // 启动刷新线程
        paintThread = new Thread(new PaintThread());
        paintThread.start();
    }
    
    // 单机模式的PaintThread
     class PaintThread implements Runnable {
         public void run() {
             while(!isFinish) {
                 surface.repaint();
                 try {
                     Thread.sleep(20);//设备帧数：15 -> 60FPS
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                     break;
                 }
             }
         }
     }
     
     // 单机模式的键盘监听器
     class KeyListener extends KeyAdapter {
         @Override
         public void keyPressed(KeyEvent e) {
             getKeyPressed(playerFighter, e.getKeyCode());
         }
         
         @Override
         public void keyReleased(KeyEvent e) {
             getKeyReleased(playerFighter, e.getKeyCode());
         }
     }
    
    // 单机模式的Surface类
    class Surface extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            //加载背景 - 适应窗口大小
            Toolkit tk =Toolkit.getDefaultToolkit();
            Image backGround = tk.getImage(Character.class.getClassLoader().getResource("images/background2.png"));//加载背景
            g.drawImage(backGround,0,0,getWidth(),getHeight(),null);

            //加载状态栏 - 左右两侧各一个
            Image status = tk.getImage(Character.class.getClassLoader().getResource("images/component/blood/status.png"));
            int statusWidth = status.getWidth(null) / 5 * 2;
            int statusHeight = status.getHeight(null) / 5 * 2;
            
            // 左侧状态栏（玩家）
            g.drawImage(status,40,0,statusWidth,statusHeight,null);
            

            if(playerFighter.getHP() > 0 && aiFighter.getHP() > 0) {
                //加载血条 - 分别绘制在左右两侧状态栏上方
                Image p1HP = tk.getImage(Character.class.getClassLoader().getResource("images/component/blood/P1/" + playerFighter.getHP() + ".png"));
                Image p2HP = tk.getImage(Character.class.getClassLoader().getResource("images/component/blood/P2/"+ aiFighter.getHP() +".png"));
                
                // 玩家血条绘制在左侧状态栏上方
                g.drawImage(p1HP,40,0,statusWidth,statusHeight,null);
                
                // AI血条绘制在右侧状态栏上方
                g.drawImage(p2HP,40,0,statusWidth,statusHeight,null);

                //调整视角，处理单机模式下的角色移动
                if(aiFighter.getPosition().y < playerFighter.getPosition().y) {
                    // AI在上方，先绘制AI
                    aiFighter.getDir().move(g, aiFighter);
                    playerFighter.getDir().move(g, playerFighter);
                } else {
                    // 玩家在上方，先绘制玩家
                    playerFighter.getDir().move(g, playerFighter);
                    aiFighter.getDir().move(g, aiFighter);
                }
                
                // 绘制碰撞箱和攻击箱边框（调试用）
                /*playerFighter.getHitbox().drawBounds(g);
                playerFighter.getLeftAttackBox().drawBounds(g);
                playerFighter.getRightAttackBox().drawBounds(g);
                playerFighter.getLeftKickBox().drawBounds(g);
                playerFighter.getRightKickBox().drawBounds(g);
                
                aiFighter.getHitbox().drawBounds(g);
                aiFighter.getLeftAttackBox().drawBounds(g);
                aiFighter.getRightAttackBox().drawBounds(g);
                aiFighter.getLeftKickBox().drawBounds(g);
                aiFighter.getRightKickBox().drawBounds(g);*/
            }
            
            //判断输赢
            else if(aiFighter.getHP() <= 0 && playerFighter.getHP() > 0) {
                //玩家胜利
                isFinish = true; // 设置游戏结束标志
                
                if (aiController != null) {
                    aiController.stop();
                }
                
                // 显示胜利图像
                victoryLabel.setVisible(true);
                
                // 显示重玩和退出按钮
                replay.setVisible(true);
                quit.setVisible(true);
            } else if(playerFighter.getHP() <= 0 && aiFighter.getHP() > 0) {
                //AI胜利
                isFinish = true; // 设置游戏结束标志
                
                if (aiController != null) {
                    aiController.stop();
                }
                
                // 显示失败图像
                defeatLabel.setVisible(true);
                
                // 显示重玩和退出按钮
                replay.setVisible(true);
                quit.setVisible(true);
            }
        }
    }
    
    // 添加是否按下Shift键的标志
    private boolean isShiftPressed = false;
    
    public void getKeyPressed(Character fighter, int keyCode) {
        // 只处理玩家控制的角色
        if (fighter.equals(playerFighter)) {
            // 处理移动按键
            boolean currentDir = fighter.getDir().getCurrentDir();
            switch (keyCode) {
                case KeyEvent.VK_W:
                    if(currentDir == Character.LEFT) fighter.getDir().LU = true;
                    else fighter.getDir().RU = true;
                    break;
                case KeyEvent.VK_S:
                    if(currentDir == Character.LEFT) fighter.getDir().LD = true;
                    else fighter.getDir().RD = true;
                    break;
                case KeyEvent.VK_SHIFT:
                    isShiftPressed = true;
                    break;
                case KeyEvent.VK_A:
                    fighter.getDir().setCurrentDir(Character.RIGHT);//保存当前方向
                    if (isShiftPressed) {
                        // Shift+A：快速向左移动
                        fighter.getDir().DASH_LEFT = true;
                        fighter.getDir().LS = false;
                        fighter.getDir().RS = false;
                    } else {
                        fighter.getDir().RF = true;
                    }
                    break;
                case KeyEvent.VK_D:
                    fighter.getDir().setCurrentDir(Character.LEFT);//保存当前方向
                    if (isShiftPressed) {
                        // Shift+D：快速向右移动
                        fighter.getDir().DASH_RIGHT = true;
                        fighter.getDir().LS = false;
                        fighter.getDir().RS = false;
                    } else {
                        fighter.getDir().LF = true;
                    }
                    break;
                case KeyEvent.VK_J:
                    // 检查攻击冷却时间
                    if (!fighter.canAttack()) {
                        System.out.println("攻击冷却中，剩余时间：" + fighter.getRemainingCooldown() + "ms");
                        break;
                    }
                    
                    // 设置攻击时间，开始冷却
                    fighter.setAttackTime();
                    
                    // 延迟300ms，给予足够反应时间
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            fighter.getDir().A = true;
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            fighter.getDir().A = false;
                        }
                    };
                    Thread thread = new Thread(task);
                    thread.start();

                    // 检查攻击是否命中AI
                    if (Character.isAttacked(fighter, aiFighter)) {
                        aiFighter.setHP(aiFighter.getHP() - 1);
                        aiFighter.getDir().fighterFall();
                        System.out.println("玩家攻击AI成功！AI剩余血量：" + aiFighter.getHP());
                    }
                    break;
                case KeyEvent.VK_K:
                    // 检查踢腿冷却时间
                    if (!fighter.canKick()) {
                        System.out.println("踢腿冷却中，剩余时间：" + fighter.getKickRemainingCooldown() + "ms");
                        break;
                    }
                    
                    // 设置踢腿时间，开始冷却
                    fighter.setKickTime();
                    
                    // 延迟300ms，给予足够反应时间
                    Runnable kickTask = new Runnable() {
                        @Override
                        public void run() {
                            fighter.getDir().KICK = true;
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            fighter.getDir().KICK = false;
                        }
                    };
                    Thread kickThread = new Thread(kickTask);
                    kickThread.start();

                    // 检查踢腿是否命中AI
                    if (Character.isKicked(fighter, aiFighter)) {
                        aiFighter.setHP(aiFighter.getHP() - 1);
                        aiFighter.getDir().fighterFall();
                        System.out.println("玩家踢腿AI成功！AI剩余血量：" + aiFighter.getHP());
                    }
                    break;
                case KeyEvent.VK_SPACE:
                    if (fighter.isOnGround()) {
                        fighter.startJump();
                    }
                    break;
                case KeyEvent.VK_L:
                    fighter.getDir().DEFEND = true;
                    break;
            }
            
            if(keyCode == KeyEvent.VK_W ||
                    keyCode == KeyEvent.VK_S ||keyCode == KeyEvent.VK_A ||keyCode == KeyEvent.VK_D ||keyCode == KeyEvent.VK_J) {
                fighter.getDir().LS = false;
                fighter.getDir().RS = false;
            }
        }
    }
    
    public void getKeyReleased(Character fighter, int keyCode) {
        // 只处理玩家控制的角色
        if (fighter.equals(playerFighter)) {
            switch (keyCode) {
                case KeyEvent.VK_W:
                    fighter.getDir().LU = false;
                    fighter.getDir().RU = false;
                    break;
                case KeyEvent.VK_S:
                    fighter.getDir().LD = false;
                    fighter.getDir().RD = false;
                    break;
                case KeyEvent.VK_A:
                    fighter.getDir().RF = false;
                    fighter.getDir().DASH_LEFT = false;
                    break;
                case KeyEvent.VK_D:
                    fighter.getDir().LF = false;
                    fighter.getDir().DASH_RIGHT = false;
                    break;
                case KeyEvent.VK_L:
                    fighter.getDir().DEFEND = false;
                    break;
                case KeyEvent.VK_SHIFT:
                    isShiftPressed = false;
                    // 松开Shift键时，确保快速移动状态被清除
                    fighter.getDir().DASH_LEFT = false;
                    fighter.getDir().DASH_RIGHT = false;
                    break;
            }
            if(keyCode == KeyEvent.VK_W ||
                    keyCode == KeyEvent.VK_S ||keyCode == KeyEvent.VK_A ||keyCode == KeyEvent.VK_D ||keyCode == KeyEvent.VK_J || keyCode == KeyEvent.VK_K || keyCode == KeyEvent.VK_L || keyCode == KeyEvent.VK_SPACE) {
                fighter.getDir().LS = false;
                fighter.getDir().RS = false;
                //gif方向
                //locateDirection();
            }
        }
    }
    
    // AI控制器类
    class AIController implements Runnable {
        private Character aiFighter;
        private Character playerFighter;
        private boolean running = true;
        
        public AIController(Character aiFighter, Character playerFighter) {
            this.aiFighter = aiFighter;
            this.playerFighter = playerFighter;
        }
        
        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(400); // AI决策间隔增加到400ms，进一步降低频率
                    makeAIDecision();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void makeAIDecision() {
            // 获取双方位置
            Point aiPos = aiFighter.getPosition();
            Point playerPos = playerFighter.getPosition();
            
            // 计算距离
            int distance = Math.abs(aiPos.x - playerPos.x);
            
            // AI决策逻辑
            if (distance > 200) {
                // 距离较远，向玩家移动
                moveTowardsPlayer();
            } else if (distance > 80) {
                // 中等距离，较低概率攻击
                if (Math.random() < 0.15) {
                    // 15%概率攻击
                    performAttack();
                } else if(Math.random() < 0.1 && aiFighter.isOnGround()){
                    aiFighter.startJump();
                }else {
                    moveRandomly();
                }
            } else {
                // 近距离，中等概率攻击
                if (Math.random() < 0.4) {
                    performAttack();
                } else if(Math.random() < 0.1 && aiFighter.isOnGround()){
                    aiFighter.startJump();
                }else {
                    moveRandomly();
                }
            }
        }
        
        private void moveTowardsPlayer() {
            Point aiPos = aiFighter.getPosition();
            Point playerPos = playerFighter.getPosition();
            
            // 先重置所有移动标志
            aiFighter.getDir().LF = false;
            aiFighter.getDir().RF = false;
            aiFighter.getDir().LU = false;
            aiFighter.getDir().LD = false;
            aiFighter.getDir().RU = false;
            aiFighter.getDir().RD = false;
            aiFighter.getDir().LS = false;
            aiFighter.getDir().RS = false;
            
            // 根据方向正确设置移动标志
            if (aiPos.x < playerPos.x) {
                // 玩家在右边，AI向右移动
                aiFighter.getDir().setCurrentDir(Character.LEFT);
                aiFighter.getDir().LF = true;  // 向右移动
            } else {
                // 玩家在左边，AI向左移动
                aiFighter.getDir().setCurrentDir(Character.RIGHT);
                aiFighter.getDir().RF = true;  // 向左移动
            }
            
            // 随机垂直移动
            if (Math.random() < 0.3) {
                if (Math.random() < 0.5) {
                    // 向上移动
                    if (aiFighter.getDir().getCurrentDir() == Character.LEFT) {
                        aiFighter.getDir().LU = true;
                    } else {
                        aiFighter.getDir().RU = true;
                    }
                } else {
                    // 向下移动
                    if (aiFighter.getDir().getCurrentDir() == Character.LEFT) {
                        aiFighter.getDir().LD = true;
                    } else {
                        aiFighter.getDir().RD = true;
                    }
                }
            }
        }
        
        private void moveRandomly() {
            // 先重置所有移动标志
            aiFighter.getDir().LF = false;
            aiFighter.getDir().RF = false;
            aiFighter.getDir().LU = false;
            aiFighter.getDir().LD = false;
            aiFighter.getDir().RU = false;
            aiFighter.getDir().RD = false;
            aiFighter.getDir().LS = false;
            aiFighter.getDir().RS = false;
            
            // 随机选择移动方向
            double rand = Math.random();
            if (rand < 0.25) {
                // 向右移动
                aiFighter.getDir().setCurrentDir(Character.LEFT);
                aiFighter.getDir().LF = true;
            } else if (rand < 0.5) {
                // 向左移动
                aiFighter.getDir().setCurrentDir(Character.RIGHT);
                aiFighter.getDir().RF = true;
            } else if (rand < 0.75) {
                // 向上移动
                if (aiFighter.getDir().getCurrentDir() == Character.LEFT) {
                    aiFighter.getDir().LU = true;
                } else {
                    aiFighter.getDir().RU = true;
                }
            } else {
                // 向下移动
                if (aiFighter.getDir().getCurrentDir() == Character.LEFT) {
                    aiFighter.getDir().LD = true;
                } else {
                    aiFighter.getDir().RD = true;
                }
            }
        }
        
        private void performAttack() {
            // 检查攻击冷却时间
            if (!aiFighter.canAttack()) {
                //System.out.println("AI攻击冷却中");
                return;
            }
            
            // 设置攻击时间，开始冷却
            aiFighter.setAttackTime();
            
            // 执行攻击
            aiFighter.getDir().A = true;
            //System.out.println("AI开始攻击");
            
            // 检查攻击是否命中
            if (Character.isAttacked(aiFighter, playerFighter)) {
                playerFighter.setHP(playerFighter.getHP() - 1);
                playerFighter.getDir().fighterFall();
                System.out.println("AI攻击玩家成功！玩家剩余血量：" + playerFighter.getHP());
            } else {
                System.out.println("AI攻击未命中");
            }
            
            // 攻击后重置状态
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            aiFighter.getDir().A = false;
        }
        
        public void stop() {
            running = false;
        }
    }
    
    @Override
    public void dispose() {
        // 停止AI控制器线程
        if (aiController != null) {
            aiController.stop();
        }
        
        // 停止音乐
        if (readyBgm != null) {
            readyBgm.stopMusic();
        }
        if (mainBgm != null) {
            mainBgm.stopMusic();
        }
        
        // 设置游戏结束标志，使paintThread退出循环
        isFinish = true;
        
        // 停止刷新线程
        if (paintThread != null && paintThread.isAlive()) {
            paintThread.interrupt();
        }
        
        super.dispose();
    }
}
