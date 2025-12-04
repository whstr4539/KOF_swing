package Client.Net;

import Client.SurfaceGUI.BeginGUI;
import Client.SurfaceGUI.MyFrame;
import Client.SurfaceGUI.SuccessGUI;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 客户端网络层
 */
public class NetClient {
    private BeginGUI beginGUI;
    Socket socket;
    PrintStream out;
    BufferedReader in;

    public NetClient(BeginGUI b) throws IOException{
            beginGUI = b;
        // socket
            System.out.println("Client: Waiting for connection...");
            socket = new Socket("127.0.0.1", 9999);
            out = new PrintStream(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Client: Connected.");

            try {
                if (b.getBgm() != null) {
                    b.getBgm().stopMusic();
                }
            } catch (NullPointerException e) {
                System.out.println("没有bgm");
            }

            SuccessGUI successGUI = new SuccessGUI(this, in ,out);

    }

    public void launchGUI(JFrame frame,BufferedReader in, PrintStream out) throws NullPointerException {
        String fromUser = null;
        int playerNumber = 0;
        try {
            fromUser = in.readLine();//获得玩家的编号


            if(fromUser.equals("PLAYER_1")) {
                playerNumber = 1;
            } else if(fromUser.equals("PLAYER_2")) {
                playerNumber = 2;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("等待中");

        try {
            if(in.readLine().equals("ACTION")) {
                System.out.println("玩家" + playerNumber + "进入游戏");

                frame.dispose();

                MyFrame myFrame = new MyFrame(playerNumber,in,out,socket);//创建主界面


            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }


    }

    public Socket getSocket() {
        return socket;
    }
}
