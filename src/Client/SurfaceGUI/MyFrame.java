package Client.SurfaceGUI;

import Client.Advance.ImageButton;
import Client.Advance.Music;
import Client.Character.Character;
import Client.Net.NetClient;
import Client.Net.NetThread;
import Client.Protocol.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 游戏主界面，玩家在此界面操控
 */
public class MyFrame extends JFrame {
    private Surface surface;//画板

    private Socket socket;
    PrintStream out;
    BufferedReader in;

    private Character fighter1;//玩家1
    private Character fighter2;//玩家2

    private Character myFighter;//我的角色
    private Character serverFighter;//对方的角色

    private NetThread netThread;//网络接收线程
    private Thread paintThread;//刷新界面线程

    private ImageButton replay;//重玩游戏按钮
    private ImageButton quit;//退出按钮

    private Music mainBgm;//背景音乐

    private boolean isFinish;//游戏是否结束
    
    // 添加是否按下Shift键的标志
    private boolean isShiftPressed = false;

    public MyFrame(int playerNumber,BufferedReader in, PrintStream out,Socket socket) {
        super("GAME"+ playerNumber);
        setSize(840,520);
        isFinish = false;

        //启动bgm
        Music readyBgm = new Music("ready.mp3",Music.ONCE);
        readyBgm.start();
        mainBgm = new Music("gaming.mp3",Music.LOOP);
        mainBgm.start();

        //游戏结束时的ko音效
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    System.out.println("");//为啥去掉就无法进入if语句？

                    if(isFinish) {
                        //结束音效
                        System.out.println("@finished!@");

                        mainBgm.stopMusic();
                        Music koBgm = new Music("ko.mp3",Music.ONCE);
                        koBgm.start();
                        break;
                    }
                }
            }
        }).start();

        this.out = out;
        this.in = in;
        this.socket = socket;

       fighter1 = new Character("fighter1",true,"images/cao",100,150);
        fighter2 = new Character("fighter2", false,"images/Chris",650,150);
        
        // 设置角色间的相互引用，用于防止重叠
        fighter1.getDir().setOtherCharacter(fighter2);
        fighter2.getDir().setOtherCharacter(fighter1);

        //确定玩家的编号，从而分配到相应的角色
       if(playerNumber == 1) {
           myFighter = fighter1;
           serverFighter = fighter2;

       } else if(playerNumber == 2) {
           myFighter = fighter2;
           serverFighter = fighter1;
       } else {
           System.out.println("ERROR");
           System.exit(1);
       }


        launchFrame();


        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);


    }


    public void launchFrame() {

        //添加画板
        surface = new Surface();
        this.addKeyListener(new KeyListener());
        this.add(surface);

        surface.setDoubleBuffered(true);//开启双缓冲

        //添加重玩按钮
        replay = new ImageButton("/images/component/replayBt.png", 2.0/5,2.0/5);
        replay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    out.println("@EXIT@");
                    socket.close();
                    NetClient nc = new NetClient(null);
                    dispose();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    System.out.println("Connection failed.");
                    //e1.printStackTrace();
                    InstructionGUI ins = new InstructionGUI("images/component/fail.png");
                }
            }
        });

        //添加返回菜单按钮
        quit = new ImageButton("/images/component/quitBt.png", 2.0/5,2.0/5);
        quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    out.println("@EXIT@");
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                BeginGUI beginGUI = new BeginGUI();
                dispose();
            }
        });

        surface.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.add(replay);
        panel.add(quit);

        //一开始不可见
        replay.setVisible(false);
        quit.setVisible(false);

        surface.add(panel,BorderLayout.SOUTH);



        netThread = new NetThread(this,serverFighter,in);
        paintThread = new Thread(new PaintThread());
        paintThread.start();//开启刷新线程
        new Thread(netThread).start();//开启网络线程



        }//启动
    class PaintThread implements Runnable {
        public void run() {
            while(true) {
                surface.repaint();
                try {
                    Thread.sleep(20);//设别帧数：15 -> 60FPS
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }//刷新帧数



    //画板
    class Surface extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {//重写JLabel画组件
            super.paintComponent(g);

            //加载背景
            Toolkit tk =Toolkit.getDefaultToolkit();
            Image backGround = tk.getImage(Character.class.getClassLoader().getResource("images/background2.png"));//加载背景
            g.drawImage(backGround,0,0,backGround.getWidth(null),backGround.getHeight(null),null);

            //加载状态栏
            Image status = tk.getImage(Character.class.getClassLoader().getResource("images/component/blood/status.png"));
            g.drawImage(status,40,0,status.getWidth(null) / 5 * 2,
                    status.getHeight(null) / 5 * 2,null);




            if(myFighter.getHP() > 0 && serverFighter.getHP() > 0) {
                //加载血条
                Image p1HP = tk.getImage(Character.class.getClassLoader().getResource("images/component/blood/P1/" + fighter1.getHP() + ".png"));
                Image p2HP = tk.getImage(Character.class.getClassLoader().getResource("images/component/blood/P2/"+ fighter2.getHP() +".png"));
                g.drawImage(p1HP,40,0,status.getWidth(null) / 5 * 2,
                        status.getHeight(null) / 5 * 2,null);
                g.drawImage(p2HP,40,0,status.getWidth(null) / 5 * 2,
                        status.getHeight(null) / 5 * 2,null);

                //调整视角
                if(serverFighter.getPosition().y < myFighter.getPosition().y) {

                    serverFighter.getDir().move(g,serverFighter);

                    myFighter.getDir().move(g,myFighter);

                } else {

                    myFighter.getDir().move(g,myFighter);

                    serverFighter.getDir().move(g,serverFighter);

                }
                
                // 绘制碰撞箱和攻击箱边框（调试用）
                /*myFighter.getHitbox().drawBounds(g);
                myFighter.getLeftAttackBox().drawBounds(g);
                myFighter.getRightAttackBox().drawBounds(g);
                
                serverFighter.getHitbox().drawBounds(g);
                serverFighter.getLeftAttackBox().drawBounds(g);
                serverFighter.getRightAttackBox().drawBounds(g);*/
            }

            //判断输赢
            else if(serverFighter.getHP() <= 0 && myFighter.getHP() > 0) {

                //结束
                isFinish = true;

                //加载背景
                Image victory = tk.getImage(Character.class.getClassLoader().getResource("images/victory.png"));
                g.drawImage(victory,135,50,victory.getWidth(null)/ 5 * 4,
                        victory.getHeight(null) / 5 * 4,null);

                replay.setVisible(true);
                quit.setVisible(true);
                 //paintThread.stop();
            } else if(myFighter.getHP() <= 0 && serverFighter.getHP() > 0) {

                //结束
                isFinish = true;

                //加载背景
                Image defeat = tk.getImage(Character.class.getClassLoader().getResource("images/defeat.png"));
                g.drawImage(defeat,160,30, defeat.getWidth(null) / 3 * 2,
                        defeat.getHeight(null) / 3 * 2,null);

                replay.setVisible(true);
                quit.setVisible(true);
                 //paintThread.stop();
            }

        }
    }



//监控本客户端控制的角色：键盘控制
    class KeyListener extends KeyAdapter {

    @Override
    public void keyPressed(KeyEvent e) {
        Message m = new Message(Message.PRESS,e.getKeyCode(),
                myFighter.getPosition().x,myFighter.getPosition().y);

        try{
            out.println(m.toString());//发送指令
        } catch (NullPointerException e1) {
            System.out.println("网络无连接");
        }

        getKeyPressed(myFighter,e.getKeyCode());

    }

    public void keyReleased(KeyEvent e) {
        Message m = new Message(Message.RELEASE,e.getKeyCode(),
                myFighter.getPosition().x,myFighter.getPosition().y);

        try{
            out.println(m.toString());//发送指令
        } catch (NullPointerException e1) {
            System.out.println("网络无连接");
        }

        getKeyReleased(myFighter,e.getKeyCode());

    }


}

    public void getKeyPressed(Character fighter,int keyCode){
        Character otherFighter = getOtherFighter(fighter);//获取另外的角色

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
                
                //延迟300ms，给予足够反应时间
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

                //如果是客户端触发的攻击
                if(Character.isAttacked(fighter,otherFighter) && fighter.equals(myFighter) ) {
                        try{
                            out.println("@ATTACK@");//发送给客户端2攻击成功
                            otherFighter.setHP(otherFighter.getHP() - 1);
                            otherFighter.getDir().fighterFall();
                        } catch (NullPointerException e) {
                            System.out.println("a");
                        }
                }
                break;
            case KeyEvent.VK_K:
                // 检查踢腿冷却时间
                if (!fighter.canKick()) {
                    System.out.println("踢腿冷却中，剩余时间：" + fighter.getKickRemainingCooldown() + "ms");
                    break;
                }

                fighter.setKickTime();

                // 执行踢腿动作
                Runnable kickTask = new Runnable() {
                    @Override
                    public void run() {
                        fighter.getDir().KICK = true;
                        try {
                            Thread.sleep(300); // 踢腿动作持续时间
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        fighter.getDir().KICK = false;
                    }
                };
                Thread kickThread = new Thread(kickTask);
                kickThread.start();

                if(Character.isKicked(fighter,otherFighter) && fighter.equals(myFighter) ) {
                    try{
                        out.println("@KICK@");//发送给客户端2攻击成功
                        otherFighter.setHP(otherFighter.getHP() - 1);
                        otherFighter.getDir().fighterFall();
                    } catch (NullPointerException e) {
                        System.out.println("kick");
                    }
                }
                break;
            case KeyEvent.VK_SPACE: // 绑定空格键为跳跃键
                if (fighter.isOnGround()) {
                    fighter.startJump();
                }
                break;
            case KeyEvent.VK_L:
                // 防御动作
                fighter.getDir().DEFEND = true;
                break;
        }
        
        if(keyCode == KeyEvent.VK_W ||
                keyCode == KeyEvent.VK_S ||keyCode == KeyEvent.VK_A ||keyCode == KeyEvent.VK_D ||keyCode == KeyEvent.VK_J || keyCode == KeyEvent.VK_K || keyCode == KeyEvent.VK_L || keyCode == KeyEvent.VK_K || keyCode == KeyEvent.VK_SPACE) {
            fighter.getDir().LS = false;
            fighter.getDir().RS = false;
        }
    }//检测按键
    public void getKeyReleased(Character fighter, int keyCode) {

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



        //locateDirection();
    }//检测按键松开


    //获得另一个玩家的对象
    public Character getOtherFighter(Character fighter) {
        Character hisFighter = null;
        if(fighter.equals(myFighter)) {
            System.out.println("My fighter");
            hisFighter = serverFighter;
        }
        else if(fighter.equals(serverFighter)) {
            hisFighter = myFighter;
            System.out.println("his fighter");
        }
        return hisFighter;
    }

    //获得server控制对象
    public Character getServerFighter() {
        return serverFighter;
    }

    //获得本客户端控制对象
    public Character getMyFighter() {
        return myFighter;
    }

    public static void main(String[] args) {
        MyFrame f = new MyFrame(1,null,null,null);
    }
}
