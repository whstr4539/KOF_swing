package Client.SurfaceGUI;

import Client.Advance.ImageButton;
import Client.Character.Character;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.awt.Toolkit;

/**
 * 角色选择界面
 */
public class CharacterSelectionGUI extends JFrame {
    private List<CharacterInfo> characters; // 角色信息列表
    private String selectedCharacterPath; // 选中的角色路径
    private int playerNumber; // 玩家编号
    private boolean isOfflineMode; // 是否为单机模式
    
    // 按钮
    private ImageButton confirmButton;
    private ImageButton backButton;
    
    public CharacterSelectionGUI(int playerNumber, boolean isOfflineMode) {
        this.playerNumber = playerNumber;
        this.isOfflineMode = isOfflineMode;
        this.characters = new ArrayList<>();
        
        // 初始化角色信息
        loadCharacters();
        
        setTitle("选择角色");
        setSize(840, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initUI();
        setVisible(true);
    }
    
    /**
     * 加载角色信息
     */
    private void loadCharacters() {
        // 添加角色信息
        characters.add(new CharacterInfo("cao", "images/cao","cao：擅长操控火焰技能，攻击范围广、爆发力强，是正面输出的好手"));
        characters.add(new CharacterInfo("Chris", "images/Chris","Chris：掌握魔法能量，技能附带控制效果，生存能力出色，适合持续作战"));
        // 可以根据需要添加更多角色
    }
    
    /**
     * 初始化界面
     */
    private void initUI() {
        // 设置背景
        setBackgroundImage();
        
        // 获取背景面板（在setBackgroundImage中设置的content pane）
        JPanel backgroundPanel = (JPanel) getContentPane();
        
        // 创建标题
        JLabel titleLabel = new JLabel("请选择角色", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        backgroundPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 创建角色选择面板
        JPanel characterPanel = new JPanel(new GridLayout(1, characters.size(), 20, 20));
        characterPanel.setOpaque(false);
        characterPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        
        // 为每个角色创建选择按钮
        for (CharacterInfo character : characters) {
            JPanel charPanel = createCharacterPanel(character);
            characterPanel.add(charPanel);
        }
        
        backgroundPanel.add(characterPanel, BorderLayout.CENTER);
        
        // 创建底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // 确认按钮
        confirmButton = new ImageButton("/images/queren.png", 2.0/5, 2.0/5);
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedCharacterPath != null) {
                    startGame();
                } else {
                    JOptionPane.showMessageDialog(CharacterSelectionGUI.this, 
                        "请先选择一个角色！", "提示", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        
        // 返回按钮
        backButton = new ImageButton("/images/qvxiao.png", 2.0/5, 2.0/5);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                backToMainMenu();
            }
        });
        
        buttonPanel.add(confirmButton);
        buttonPanel.add(backButton);
        backgroundPanel.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 创建角色面板
     */
    private JPanel createCharacterPanel(CharacterInfo character) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        // 移除默认边框
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 角色头像
        JLabel avatarLabel = new JLabel();
        avatarLabel.setHorizontalAlignment(JLabel.CENTER);
        avatarLabel.setVerticalAlignment(JLabel.CENTER);
        avatarLabel.setPreferredSize(new Dimension(150, 150));
        
        // 尝试加载角色头像
        try {
            String imagePath = character.getPath() + "/role.png";
            URL imageUrl = Character.class.getClassLoader().getResource(imagePath);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(imageUrl);
                // 缩放图像到统一尺寸
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                avatarLabel.setIcon(new ImageIcon(scaledImg));
            } else {
                avatarLabel.setText("无图像");
                avatarLabel.setForeground(Color.WHITE);
            }
        } catch (Exception e) {
            avatarLabel.setText("加载失败");
            avatarLabel.setForeground(Color.WHITE);
        }
        
        // 角色名称
        panel.add(avatarLabel, BorderLayout.CENTER);

        JPanel nameBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        nameBtnPanel.setOpaque(false); // 透明背景，适配黑色界面

        JLabel nameLabel = new JLabel(character.getName());
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        nameBtnPanel.add(nameLabel);

        JButton introBtn = new JButton("角色介绍");
        introBtn.setForeground(Color.WHITE);
        introBtn.setBackground(new Color(60, 60, 60)); // 深色背景适配界面
        introBtn.setFocusPainted(false); // 去掉焦点边框

        introBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                        CharacterSelectionGUI.this,
                        character.getDescription(), // 显示角色介绍文本
                        character.getName() + " 角色详情", // 弹窗标题
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
        nameBtnPanel.add(introBtn);

        panel.add(nameBtnPanel, BorderLayout.SOUTH);
        
        // 添加点击事件
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectCharacter(character);
                // 更新所有面板的边框样式以显示选中状态
                updateSelectionVisuals(panel);
            }
        });
        
        return panel;
    }
    
    /**
     * 选择角色
     */
    private void selectCharacter(CharacterInfo character) {
        this.selectedCharacterPath = character.getPath();
        System.out.println("选择了角色: " + character.getName() + " (" + character.getPath() + ")");
    }
    
    /**
     * 更新选中视觉效果
     */
    private void updateSelectionVisuals(JPanel selectedPanel) {
        // 获取角色面板容器
        JPanel characterPanel = (JPanel) ((JPanel) getContentPane()).getComponent(1);
        Component[] components = characterPanel.getComponents();
        
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel == selectedPanel) {
                    // 选中的面板添加金色发光边框效果
                    panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.YELLOW, 2),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                    ));
                } else {
                    // 其他面板使用透明边框
                    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                }
            }
        }
    }
    
    /**
     * 开始游戏
     */
    private void startGame() {
        dispose(); // 关闭当前窗口
        
        if (isOfflineMode) {
            // 创建单机模式游戏界面，传递选中的角色路径
            MyFrameOffline offlineFrame = new MyFrameOffline(playerNumber, selectedCharacterPath);
        } else {
            // 联机模式暂时保持原有逻辑
            // 这里可以根据需要扩展联机模式的角色选择
        }
    }
    
    /**
     * 返回主菜单
     */
    private void backToMainMenu() {
        dispose(); // 关闭当前窗口
        BeginGUI beginGUI = new BeginGUI(); // 重新打开主菜单
    }
    
    /**
     * 设置背景图像
     */
    private void setBackgroundImage() {
        // 创建一个自定义面板作为主面板，用于绘制背景
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 加载背景图像
                try {
                    URL imageUrl = null;//Character.class.getClassLoader().getResource("images/background3.png");
                    if (imageUrl != null) {
                        Image backgroundImage = Toolkit.getDefaultToolkit().getImage(imageUrl);
                        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                    } else {
                        // 如果没有背景图像，则绘制黑色背景
                        g.setColor(Color.BLACK);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                } catch (Exception e) {
                    // 出错时绘制黑色背景
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);
    }
    
    /**
     * 角色信息内部类
     */
    private static class CharacterInfo {
        private String name;
        private String path;
        private String description;
        
        public CharacterInfo(String name, String path,String description) {
            this.name = name;
            this.path = path;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getPath() {
            return path;
        }

        public String getDescription() {
            return description;
        }
    }
}