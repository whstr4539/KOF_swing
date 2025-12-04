package Client.SurfaceGUI;

import Client.Advance.ImageButton;
import Client.Advance.Music;
import Client.Character.Character;
import Client.Net.NetClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * 界面启动类，运行开始界面
 */
public class BeginGUI extends JFrame {
    private Music bgm;

    public BeginGUI() {
        super("The King of Fighters");
        setSize(1232 / 3 * 2,750 / 3 * 2);

        bgm = new Music("bgm.mp3",Music.LOOP);

        launchFrame();


        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    class Poster extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Toolkit tk =Toolkit.getDefaultToolkit();
            Image poster = tk.getImage(Character.class.getClassLoader().getResource("images/poster.jpg"));//加载背景
            g.drawImage(poster,0,0,poster.getWidth(null) / 3 * 2,
                    poster.getHeight(null) / 3 * 2,null);
            repaint();
        }
    }

    public void launchFrame() {

        bgm.start();

        Poster poster = new Poster();
        this.add(poster);

        poster.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setOpaque(false);

        //单机模式按钮
        ImageButton offlineButton = new ImageButton("/images/component/begin.png",2.0 / 3, 2.0 / 3);
        offlineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 停止开始界面音乐
                if (bgm != null) {
                    bgm.stopMusic();
                }
                // 启动单机模式 - 打开角色选择界面
                dispose();
                // 创建角色选择界面
                CharacterSelectionGUI selectionGUI = new CharacterSelectionGUI(1, true);
                System.out.println("单机模式角色选择界面启动");
            }
        });

        //联网模式按钮
        ImageButton onlineButton = new ImageButton("/images/component/begin.png",2.0 / 3, 2.0 / 3);
        onlineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 停止开始界面音乐
                if (bgm != null) {
                    bgm.stopMusic();
                }
                
                try{
                    NetClient nc = new NetClient(BeginGUI.this);
                    dispose();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    System.out.println("Connection failed.");
                    //e1.printStackTrace();
                    InstructionGUI ins = new InstructionGUI("images/instruction/fail.png");
                }

            }
        });

        //帮助按钮
        ImageButton helpButton = new ImageButton("/images/component/help.png", 2.0 / 5,2.0 / 5);
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InstructionGUI ins = new InstructionGUI("images/instruction/instruction.png");
            }
        });

        //退出按钮
        ImageButton quitButton = new ImageButton("/images/component/quit.png",3.0 / 7,3.0 / 7);
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        //音乐按钮
        ImageButton musicButton = new ImageButton("/images/component/musicOn.png",3.0 / 7,3.0 / 7);
        musicButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(bgm.isAlive()) {
                    musicButton.setMyIcon("/images/component/musicOff.png",3.0 / 7,3.0 / 7);
                    bgm.stopMusic(); // 使用stopMusic代替stop
                } else {
                    musicButton.setMyIcon("/images/component/musicOn.png",3.0 / 7,3.0 / 7);
                    bgm = new Music("bgm.mp3",Music.LOOP);
                    bgm.start();
                }
            }
        });



        JLabel author = new JLabel("");
        author.setForeground(Color.white);
        author.setFont(new Font("Dialog", 1, 1));



        panel.add(author);
        
        //添加模式选择标签
        JLabel offlineLabel = new JLabel("单机模式");
        offlineLabel.setForeground(Color.white);
        offlineLabel.setFont(new Font("Dialog", 1, 15));
        
        JLabel onlineLabel = new JLabel("联网模式");
        onlineLabel.setForeground(Color.white);
        onlineLabel.setFont(new Font("Dialog", 1, 15));
        
        panel.add(offlineLabel);
        panel.add(offlineButton);
        panel.add(onlineLabel);
        panel.add(onlineButton);
        panel.add(helpButton);
        panel.add(musicButton);
        panel.add(quitButton);
        poster.add(panel,BorderLayout.SOUTH);

    }

    public Music getBgm() {
        return bgm;
    }
//        public static void main(String[] args) {
//        BeginGUI beginGUI = new BeginGUI();
//    }
}
