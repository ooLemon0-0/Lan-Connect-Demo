import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class RoomSearch extends JFrame{
    //大厅界面
    JScrollPane scrollPane;
    JPanel panel;
    JButton button_rename, button_search,button_create,button_join;
    JLabel player_name;

    Room room;
    RoomSearch roomsearch;

    DatagramSocket soc;//套接字接收端口

    boolean in_use;//当前界面是否在启用状态

    ArrayList<String> room_name_information;
    ArrayList<String> room_ip_information;

    JTable jt;//表格

    public RoomSearch() {
        //初始化信息列表
        room_name_information=new ArrayList<String>();
        room_ip_information=new ArrayList<String>();

        room_name_information.add("Empty_Game");
        room_ip_information.add("255.255.255.255");
        // UI布局
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                
                // soc.close();//关闭套接字端口
            }
        });
        
        setTitle("新房间");
        setBounds(400, 200, 500, 375);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        init();
        //初始化房间

            

        room=new Room(this);//在大厅启动的时候，就把房间初始化好，创建房间时只是启用其端口和功能
        roomsearch=this;

        ban_use(true);//大厅启用

        //启动接收信息端口
        WaitingThread waitingthread = new WaitingThread();
        waitingthread.start();

    }
    void init(){
        //布局初始化
        scrollPane = new JScrollPane();
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        panel = new JPanel();
        getContentPane().add(panel, BorderLayout.NORTH);

        //玩家名字
        player_name=new JLabel("Player001");
        panel.add(player_name);

        //创建按钮
        button_create = new JButton("创建游戏");
        button_create.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                //设置大厅不可见，房间可见
                room.ban_use(true);//启用房间
                roomsearch.ban_use(false);//禁用大厅
                room.host_name=player_name.getText();//设置房间主人的名字
                room.tag=0;//tag设为房主
            }

        });
        panel.add(button_create);

        //搜索按钮
        button_search = new JButton("搜索房间");
        button_search.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                
                SearchRoom();//搜索房间

            }

        });
        panel.add(button_search);

        //改名按钮
        button_rename = new JButton("修改名字");
        button_rename.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                
                String s=JOptionPane.showInputDialog("请输入:");
                player_name.setText(s);
                room.host_name=s;
                room.my_name=s;
                room.player_name_information.set(0, s);
                room.fresh_table();//刷新表格
                
            }

        });
        panel.add(button_rename);

        //加入按钮
        button_join = new JButton("加入房间");
        button_join.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                //发出加入房间的请求
                
                // Send_Msg(room_ip_information.get(0),"Triple/JoinRoom/"+room_ip_information.get(0), 1234);//向目标房间发出加入请求
                Send_Msg(room_ip_information.get(jt.getSelectedRow()),"Triple/JoinRoom/"+room_ip_information.get(jt.getSelectedRow())+"/"+player_name.getText().toString(), 1234);//向目标房间发出加入请求
            }

        });
        panel.add(button_join);

        //表格
        jt=init_table();
        scrollPane.setViewportView(jt);
        // panel.add(jt);
    }

    void SearchRoom(){
        //清除所有表项
        DefaultTableModel model = (DefaultTableModel) jt.getModel();
        model.setRowCount(1);
        //删除表
        for(int i=1;i<room_ip_information.size();i++){
            room_ip_information.remove(i);
        }
        for(int i=1;i<room_name_information.size();i++){
            room_name_information.remove(i);
        }
        
        Send_Msg("255.255.255.255", "Triple/SearchRoom", 1234);//向1234端口广播搜索房间信息
    }
    
    //等待线程，UDP端口请求
    class WaitingThread extends Thread {
        // 等待线程，用于接收呼叫
        public WaitingThread() {}
        public void run() {
            try {
                soc = new DatagramSocket(1235);//大厅接收房间信息的端口
                byte[] buffer = new byte[1024 * 64];
                DatagramPacket pac = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    soc.receive(pac);// 在接受到数据包之前会一直阻塞，就是等待，程序不往下走
                    String ip = pac.getSocketAddress().toString();// 获取对方的ip和端口
                    ip=ip.substring(1);//去除分号
                    String ips[]=ip.split(":");
                    ip=ips[0];//去除端口
                    String rs = new String(buffer, 0, pac.getLength());
                    System.out.println("大厅received: " + rs);
                    System.out.println("It's came from ip: " + ip); 
                    Check_RoomInformation(rs, ip);    //检查房间信息，储存进列表     
                    Check_RoomJoinName(rs,ip);      //检查房间加入的名字
                    Check_RoomJoinIP(rs, ip);       //检查房间加入的IP
                }           
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //检查房间信息
        void Check_RoomInformation(String rs,String ip){
                if(in_use){//如果大厅启用
                    String spl[]=rs.split("/");
                    if(spl[0].equals("Triple")){//如果是游戏信息
                        if(spl[1].equals("RoomInformation")){//如果是房间信息
                            //储存进房间信息列表里面
                            room_name_information.add(rs);
                            room_ip_information.add(ip);
                            DefaultTableModel model = (DefaultTableModel) jt.getModel();
                            Vector <String>hang=new Vector<String>();
                            hang.add(spl[2]);
                            hang.add(ip);
                            hang.add(spl[3]);
                            model.addRow(hang);

                            System.out.println("储存房间信息");

                            Send_Msg(ip, "Triple/IPConfirm/"+ip, 1234);//告知目标房间，你的IP是xxx
                        }
                    }
                }
               
            
        }
        void Check_RoomJoinName(String rs,String ip){
            if(in_use){//如果大厅启用
                String spl[]=rs.split("/");
                if(spl[0].equals("Triple")){//如果是游戏信息
                    if(spl[1].equals("RoomNameDetail")){//如果是房间详细信息
                        //把所有房间详细信息插入
                        room.player_name_information.clear();//清空已有的房间姓名表
                        for(int i=2;i<spl.length;i++){
                            if(i>=room.player_name_information.size()){
                                room.player_name_information.add(spl[i]);
                            }
                            else
                                room.player_name_information.set(i-2, spl[i]);
                        }
                    }
                }
            
            }
        }
        void Check_RoomJoinIP(String rs,String ip){
            if(in_use){//如果大厅启用
                String spl[]=rs.split("/");
                if(spl[0].equals("Triple")){//如果是游戏信息
                    if(spl[1].equals("RoomIPDetail")){//如果是房间详细信息
                        //把所有房间详细信息插入
                        room.player_ip_information.clear();//清空已有的房间IP表
                        for(int i=2;i<spl.length;i++){
                            if(i>=room.player_ip_information.size()){
                                room.player_ip_information.add(spl[i]);
                            }
                            else
                            room.player_ip_information.set(i-2, spl[i]);
                        }

                        //接收房间IP详细信息后，启用房间
                        room.tag=1;//tag设置为客人
                        ban_use(false);//禁用大厅
                        room.ban_use(true);//启用房间
                        room.init_guest();//初始化客房的参数
                        room.my_name=player_name.getText();
                        room.host_name=room.player_name_information.get(0);//设置房间主人
                        room.fresh_table();
                    }
                    
                }
                
            }
        }
    }

    //UDP发送接口
    public void Send_Msg(String ip, String msg, int port) {
        // 对某IP发送
        // String msg="Triple";
        DatagramSocket soc;
        try {
            soc = new DatagramSocket();
            byte[] buffer = msg.getBytes();
            DatagramPacket pac = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), port);
            soc.send(pac);
        } catch (Exception e) {
            e.printStackTrace();
        } // 发送端可以配端口，但多实例运行会java.net.BindException,还是让系统指定

    }

    //初始化表格
    JTable init_table(){
        JTable table;
        Vector<Vector<String>> rowData;//储存data的二维数组
        Vector <String>columnNames;//列名
        columnNames = new Vector<String>();
        columnNames.add("房间号");
        columnNames.add("IP");
        columnNames.add("人数");
        rowData = new Vector<Vector<String>>();// rowData行数据，可以存放多行
        for(int i=0;i<room_name_information.size();i++){
            Vector <String>hang=new Vector<String>();
            hang.add(room_name_information.get(i));//玩家名称
            hang.add(room_ip_information.get(i));//ip
            hang.add("0");//人数
            rowData.add(hang);
        }
        DefaultTableModel model = new DefaultTableModel(rowData,columnNames){
            //重写，整个表格不能编辑
            @Override
            public boolean isCellEditable(int row,int column){
                return false;
            }
        };
        table=new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    public void ban_use(boolean x){//将自己界面设置为x状态，且x显示
        in_use=x;
        setVisible(x);
    }
}
