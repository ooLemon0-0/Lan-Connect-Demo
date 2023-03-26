import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.xml.sax.SAXException;

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

public class Room extends JFrame {
    int tag = 0;// 0房主，1用户
    // 房间界面
    JScrollPane scrollPane;
    JPanel panel;
    JButton button_start, button_exit, button_ready, button_send,button_test;
    boolean in_use;// 当前界面是否在被启用
    Room room;
    RoomSearch roomsearch;

    String host_name;// 房主名字
    String my_name;// 玩家名字
    String my_IP;// 玩家名字

    // ArrayList<String> player_name_information;
    MaintainArray player_name_information;
    // ArrayList<String> player_ip_information;
    

    int num_id;//房间内ID

    DatagramSocket soc;// 套接字接收端口

    public JTable jt;

    // Synchronization synchronization;

    JTextField chat_Send;// 发送框
    JTextArea chat_Area;// 显示区

    MaintainArray test_array;//测试maintainarray，在init函数第一行实例化，存储0~10
    //测试，按下游戏开始按钮，就会给ip列表赋值

    public Room(RoomSearch roomsearch) {

        // UI布局
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {

                // soc.close();//关闭套接字端口
            }
        });
        this.roomsearch = roomsearch;// 连接大厅和房间两个窗口

        setTitle("新房间");
        setBounds(400, 200, 400, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        init();
        WaitingThread waitingthread = new WaitingThread();
        waitingthread.start();
        ban_use(false);// 房间初始化为禁用状态

        // // 启用同步机制
        // synchronization = new Synchronization();
        // synchronization.start();                                 //暂时禁用，目前有两种机制，一种是不断发的机制，另外一种是设定的自同步数组机制

    }

    void init() {
        test_array=new MaintainArray();
        test_array.add("0");
        test_array.add("0");

        // 房主姓名初始化
        host_name = roomsearch.player_name.getText();
        my_name = host_name;
        num_id=0;//初始化自己的ID为1
        // 初始化房间信息
        // player_name_information = new ArrayList<String>();
        player_name_information=new MaintainArray();
        player_name_information.add(host_name);
        // player_ip_information = new ArrayList<String>();
       
        try {
            String so[] = InetAddress.getLocalHost().toString().split("/");
            my_IP = so[1];// myip赋值
            // player_ip_information.add(so[1]);
            MaintainArray.ip_list.add(so[1]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Test_Data_InArray();// 测试数据

        // 布局初始化
        scrollPane = new JScrollPane();
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        panel = new JPanel();
        getContentPane().add(panel, BorderLayout.NORTH);

        // 盒子布局
        Box chat_box, chat_box_all;
        chat_box_all = Box.createVerticalBox();
        chat_box = Box.createHorizontalBox();

        // 聊天文本

        // 发言框
        chat_Send = new JTextField();// 编辑框
        chat_Send.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e){
                if(e.getKeyCode()==KeyEvent.VK_ENTER){
                    speak();
                }
            }
        }
        );



            // 键盘监听器，如果按回车，就等于发送
         
    

        // 显示框
        chat_Area = new JTextArea("等待发言", 8, 30); // 显示框
        chat_Area.setEditable(false);// 设置为不可编辑
        chat_Area.setLineWrap(true);// 激活换行功能
        chat_Area.setWrapStyleWord(true); // 激活断行不断字功能


        // 表格信息
        jt = init_table();
        scrollPane.setViewportView(jt);
        // 开始按钮
        button_start = new JButton("开始游戏");
        button_start.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                //测试
                // MaintainArray.ip_list.Set_Array(player_ip_information);//测试ip赋值
                // System.out.println("ip赋值");
            }
        });
        panel.add(button_start);


        // 测试按钮
        button_test = new JButton("测试");
        button_test.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                //测试test_array
                test_array.add("0");
                System.out.println("测试数组加0");
                for(int i=0;i<test_array.size();i++ ){
                    System.out.print(test_array.get(i)+",");
                }
                System.out.println("");
                
            }
        });
        panel.add(button_test);

        // 准备按钮
        button_ready = new JButton("准备");
        button_ready.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // MaintainArray.ip_list.Set_Array(player_ip_information);//测试ip赋值
                System.out.println("ip赋值");

            }});
        panel.add(button_ready);
        button_ready.setVisible(false);

        // 退出按钮
        button_exit = new JButton("返回大厅");
        button_exit.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if(tag==0){
                    //如果是房主退出房间，则所有人退出大厅
                    Broad_Room("Triple/RoomDismiss");
                }
                
                ban_use(false);// 禁用房间
                roomsearch.ban_use(true);// 启用大厅
                if(tag!=0)//如果不是房主
                exit_room(true);// 退出房间，清空自己的数组。并通知房间里其他人，自己已退出房间

            }

        });
        panel.add(button_exit);
        // 发言按钮
        button_send = new JButton("发送");
        button_send.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                speak();// 发言

            }

        });


        // 发言区布局

        // getContentPane().add(chat,BorderLayout.SOUTH);
        // getContentPane().add(button_send,BorderLayout.SOUTH);
        chat_box.add(chat_Send);
        chat_box.add(button_send);
        getContentPane().add(chat_box_all, BorderLayout.SOUTH);
        // chat_box_all.add(chat_Area);
        // panel与jtextfield相结合
        JPanel panelOutput;
        panelOutput = new JPanel();
        panelOutput.add(new JScrollPane(chat_Area));
        chat_box_all.add(panelOutput);
        chat_box_all.add(chat_box);

    }

    public void ban_use(boolean x) {// 将自己界面设置为x状态，且x显示
        in_use = x;
        setVisible(x);

    }

    // 等待线程，UDP端口请求
    class WaitingThread extends Thread {
        // 等待线程，用于接收呼叫
        public WaitingThread() {

        }

        public void run() {
            try {
                soc = new DatagramSocket(1234);
                byte[] buffer = new byte[1024 * 64];
                DatagramPacket pac = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    soc.receive(pac);// 在接受到数据包之前会一直阻塞，就是等待，程序不往下走
                    String ip = pac.getSocketAddress().toString();// 获取对方的ip和端口
                    ip = ip.substring(1);// 去除分号
                    String ips[] = ip.split(":");
                    ip = ips[0];// 去除端口
                    // int port = pac.getPort();//如果有上面那句，这句就不需要
                    String rs = new String(buffer, 0, pac.getLength());
                    Check_Search(rs, ip);// 查看是否为房间查询信息
                    Check_Join(rs, ip);// 查看是否是加入信息
                    Check_IP(rs, ip);// 借助别人刷新自己IP
                    Check_Chat(rs, ip);
                    Check_Exit(rs,ip);
                    Check_Dismiss(rs,ip);
                    Check_ID(rs,ip);
                    // // String split[] = rs.split(" ");
                    // System.out.println(split[0]);
                    System.out.println("房间received: " + rs);
                    System.out.println("It's came from ip: " + ip);
                    // System.out.println("The port is: "+port);
                    // soc.close();//此处实验，关闭资源
                    // }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void Check_Search(String msg, String ip) {// 判断是否收到房间搜索请求，并实时应答
            if (in_use) {// 如果房间启用
                String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("SearchRoom")) {// 如果是房间搜索请求
                        Send_Msg(ip, "Triple/RoomInformation/" + host_name + "/" + player_name_information.size(),
                                1235);// 向1235端口发送房间信息
                        System.out.println("向" + ip + "发送房间信息");
                    }
                }
            }

        }

        void Check_Join(String msg, String ip) {// 判断是否收到房间加入请求，并实时应答
            if (in_use) {// 如果房间启用
                String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("JoinRoom")) {// 如果是房间加入请求
                        if (spl[2].equals(MaintainArray.ip_list.get(0))) {// 如果申请加入的IP等于自己的IP
                            MaintainArray.ip_list.set(0, spl[2]);// ！！！！要求发出加入请求时，将房间ip发回来

                            Send_Msg(ip, "Triple/ConfirmJoin/" + host_name, 1235);// 向1235端口发送确认收到加入房间的信息
                            // 目标大厅在收到comfirm过后，不再接收其他房间的confirm

                            // 将加入的玩家信息添加到玩家列表中
                            
                            MaintainArray.ip_list.add(ip);
                            player_name_information.add(spl[3]);
                            // // 向加入的玩家发房间玩家明细
                            // String RoomNameDetail = "";
                            // for (int i = 0; i < player_name_information.size(); i++) {
                            //     RoomNameDetail = RoomNameDetail + player_name_information.get(i) + "/";
                            // }
                            // Send_Msg(ip, "Triple/RoomNameDetail/" + RoomNameDetail, 1235);// 向目标发房间名字明细
                            // System.out.println("向" + ip + "发送房间name信息:" + RoomNameDetail);

                            // // 向加入的玩家发房间ip明细
                            // String RoomIPDetail = "";
                            // for (int i = 0; i < MaintainArray.ip_list.size(); i++) {
                            //     RoomIPDetail = RoomIPDetail + MaintainArray.ip_list.get(i) + "/";
                            // }
                            // Send_Msg(ip, "Triple/RoomIPDetail/" + RoomIPDetail, 1235);// 向目标发IP名字明细
                            // System.out.println("向" + ip + "发送房间ip信息" + RoomIPDetail);
                            // 目标大厅在收到ip明细后，立即禁用大厅，启用房间

                            // 更新房间的显示
                            DefaultTableModel model = (DefaultTableModel) jt.getModel();
                            Vector<String> hang = new Vector<String>();
                            hang.add(spl[3]);
                            hang.add(ip);
                            model.addRow(hang);
                            fresh_table();

                            // 房间人数加1

                            //通知所有玩家自己的序号是多少
                            num_id_assign();
                        }

                    }
                }
            }

        }

        void Check_IP(String msg, String ip) {
            if (in_use) {// 如果房间启用
                String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("IPConfirm")) {// 如果是房间加入请求
                        MaintainArray.ip_list.set(0, spl[2]);// ！！！！要求发出加入请求时，将房间ip发回来
                        my_IP = spl[2];
                        fresh_table();// 刷新一下表格
                    }
                }
            }
        }
    
        void Check_Chat(String msg,String ip){//判断是否是发言信息
            if (in_use) {// 如果房间启用
                String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("Speak")) {// 如果是发言信息
                        // if(!spl[2].equals(my_name)){//如果不是自己发的言
                        // }
                        chat_Area.append("\n" + spl[2] + " : " + spl[3]); // 将收到的信息发送到文本区去
                    }
                }
            }
        }
        
        void Check_Dismiss(String msg,String ip){
            if (in_use) {// 如果房间启用
                String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("RoomDismiss")) {// 如果是房间解散
                        ban_use(false);// 禁用房间
                        roomsearch.ban_use(true);// 启用大厅
                        exit_room(false);// 退出房间，清空自己的数组。并通知房间里其他人，自己已退出房间
                        
                    }
                }
            }
        }
        
        void Check_Exit(String msg,String ip){//判断是否是退出房间信息
            if (in_use) {// 如果房间启用
                String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("ExitRoom")) {// 如果是房间解散
                        int Exiter=Integer.valueOf(spl[2]) ;//退出的人的ID
                            System.out.println(player_name_information.get(Exiter)+"离开了房间");
                            MaintainArray.ip_list.remove(Exiter);
                            player_name_information.remove(Exiter);
                            fresh_table();
                                
                                
                            
                        
                    }
                }
            }
        }
    
        void Check_ID(String msg,String ip){//是否是通知改ID的消息
            
                String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("NumID")) {// 如果是房间ID信息
                        num_id=Integer.valueOf(spl[2]);//给自己的ID赋值
                    }
                }
        }
    }

    // UDP发送接口
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

    JTable init_table() {
        JTable table;
        Vector<Vector<String>> rowData;// 储存data的二维数组
        Vector<String> columnNames;// 列名
        columnNames = new Vector<String>();
        columnNames.add("玩家名");
        columnNames.add("IP");
        rowData = new Vector<Vector<String>>();// rowData行数据，可以存放多行
        for (int i = 0; i < player_name_information.size(); i++) {
            Vector<String> hang = new Vector<String>();
            hang.add(player_name_information.get(i));
            hang.add(MaintainArray.ip_list.get(i));// ip
            rowData.add(hang);
        }
        DefaultTableModel model = new DefaultTableModel(rowData, columnNames) {
            // 重写，整个表格不能编辑
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model) {
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                // Alternate row color

                // if (!isRowSelected(row))
                // c.setBackground(row % 2 == 0 ? getBackground() : Color.LIGHT_GRAY);
                // 如果是房主，这一页为黄色
                // c.setBackground(player_name_information.get(row).equals(host_name)?
                // Color.yellow:Color.white);
                // 如果是自己，这一页为橙色
                c.setBackground(player_name_information.get(row).equals(my_name) ? Color.orange : Color.white);

                return c;
            }
        };
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    /* // 房间信息同步机制
    // class Synchronization extends Thread {
    //     String name_info = "";
    //     String ip_info = "";

    //     public Synchronization() {

    //     }

    //     // 向房间内的玩家隔一段时间发送一次房间信息，接收到后更新为自己的信息
    //     public void run() {
    //         while (true) {
    //             // 向加入的玩家发房间玩家明细
    //             if (player_name_information.size() > 1 && in_use) {
    //                 for (int i = 0; i < player_name_information.size(); i++) {
    //                     name_info = name_info + player_name_information.get(i) + "/";
    //                 }
    //                 for (int i = 0; i < player_ip_information.size(); i++) {
    //                     ip_info = ip_info + player_ip_information.get(i) + "/";
    //                 }
    //                 for (int i = 0; i < player_ip_information.size(); i++) {
    //                     Send_Msg(player_ip_information.get(i), "Triple/RoomNameSynchronization/" + name_info, 1234);// 向目标发房间名字明细
    //                     System.out.println("向" + player_ip_information.get(i) + "发送房间name信息:" + name_info);
    //                     Send_Msg(player_ip_information.get(i), "Triple/RoomIPSynchronization/" + ip_info, 1235);// 向目标发IP名字明细
    //                     System.out.println("向" + player_ip_information.get(i) + "发送房间ip信息" + ip_info);
    //                 }
    //             }

    //             // 向加入的玩家发房间ip明细
    //             try {
    //                 Thread.sleep(5000);
    //             } // 每5000ms发一次
    //             catch (Exception e) {
    //             }
    //             ;

    //             name_info = "";
    //             ip_info = "";
    //         }
    //     }
    // }*/

    // 初始化作客人时房间的参数
    public void init_guest() {
        button_start.setVisible(false);// 开始按钮变得不可见
        button_ready.setVisible(true);
    }

    // 测试数据
    void Test_Data_InArray() {
        // player_name_information.add("Test1");
        // player_name_information.add("Test2");
        // player_ip_information.add("192.168.65.4");
        // player_ip_information.add("192.168.65.5");
    }

    // 向全房间通告
    void Broad_Room(String msg) {// 向全房间通告；
        for (int i = 0; i < MaintainArray.ip_list.size(); i++) {
            Send_Msg(MaintainArray.ip_list.get(i), msg, 1234);
        }

    }

    // 刷新表格
    public void fresh_table() {
        // 将表格所有的信息更新为现在数组里的信息
        DefaultTableModel model = (DefaultTableModel) jt.getModel();
        for (int i = 0; i < MaintainArray.ip_list.size(); i++) {
            if (i < model.getRowCount()) {
                model.setValueAt(player_name_information.get(i), i, 0);// 将表格名字改为自己的名字
                model.setValueAt(MaintainArray.ip_list.get(i), i, 1);// 将表格名字改为自己的名字
            } else {
                Vector<String> hang = new Vector<String>();
                hang.add(player_name_information.get(i));
                hang.add(MaintainArray.ip_list.get(i));
                model.addRow(hang);
            }

        }
        for(int i=MaintainArray.ip_list.size();i<model.getRowCount();i++){
            model.removeRow(i);
        }
    }

    // 退出
    public void exit_room(boolean broad) {
        // 退出房间
        // 还原房间列表
        if(broad)
            Broad_Room("Triple/ExitRoom/" + num_id);

        // 当手动退出时，清空自己的数组
        MaintainArray.ip_list.clear();
        player_name_information.clear();
        // player_ip_information = new ArrayList<String>();
        // player_name_information = new ArrayList<String>();

        MaintainArray.ip_list.add(my_IP);
        player_name_information.add(host_name);

        clear_chat();//清空发言
        fresh_table();
        num_id=0;
    }

    void clear_chat(){//清空发言
        chat_Area.selectAll();
        chat_Area.replaceSelection("等待发言");
    }

    // 发言
    void speak() {
        // chat_Area.append("\n" + my_name + " : " + chat_Send.getText()); // 将要发送的信息发送到文本区去
        Broad_Room("Triple/Speak/"+my_name+"/"+chat_Send.getText());//广播发出自己要说的话
        chat_Send.setText("");// 发言后，清空发言区
        
    }

    //通知房间内序号
    void num_id_assign(){
        for(int i=1;i<MaintainArray.ip_list.size();i++){
            Send_Msg(MaintainArray.ip_list.get(i),"Triple/NumID/"+i, 1234);
        }
    }
}
