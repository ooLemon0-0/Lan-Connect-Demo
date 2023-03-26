import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.StringJoiner;

import javax.swing.plaf.synth.SynthStyle;

// public class MaintainArray extends ArrayList{
public class MaintainArray {
    //房间之间自动维护、同步的string 数组
    ArrayList<String> arrayList;//继承的arraylist，因为不会做泛型，所以采用这种方式


    static MaintainThread maintainthread=new MaintainThread() ;//自动维护线程（原理静态线程接收同步信息更新对应的数组）
    static MaintainArray[] maintainArrays=new MaintainArray[10];//最多十个维护数组
    static int num_array=0;//独一无二的数组编号

    // static boolean is_run=false;//用于判断维护接收的线程是否被启动

    public static MaintainArray ip_list=new MaintainArray();//房间内所有ip的列表
    // ArrayList<String> local_ip_list;//本地IP列表

    
    int my_array_num;//自己的类编号

    String Msg_Maintain;//同步信息string，这里就声明，空间换时间

    //构造方法
    // public MaintainArray(ArrayList<String> ip_list){
    public MaintainArray(){
        arrayList=new ArrayList<String>();
        
        // local_ip_list=ip_list;//给本地iplist赋值

        //给自己赋编号值
        my_array_num=MaintainArray.num_array;
        MaintainArray.num_array++;

        //将数组添加进数组表里
        MaintainArray.maintainArrays[my_array_num]=this;
        

        //看自己是否是第一个数组，是第一个则启动自维护线程，监听
        if(my_array_num==0){//在第一个自动维护数组创建时
            MaintainArray.maintainthread.start();//若未启动接收，则启动接收
        }

        Msg_Maintain="Triple/MaintainArray/"+my_array_num;//初始化发送信息

        // maintainthread=new MaintainThread();//初始化维护线程
        // maintainthread.start();

    }

    // @Override
    public int size(){
        return arrayList.size();
    }

    public int get_num(){//获取自己的数组编号
        return my_array_num;
    }

    public void clear(){
        arrayList.clear();
        Msg_Maintain="Triple/MaintainArray/"+my_array_num;
    }
    //返回string
    public String get(int index){
        return arrayList.get(index);
    }
    //set
    public void set(int index,String s){
        arrayList.set(index, s);
        String spl[]=Msg_Maintain.split("/");
        spl[index+3]=s;

        
        StringJoiner sj = new StringJoiner("/");
        for (String str : spl) {
            sj.add(str);
        }
        Msg_Maintain=sj.toString();
        System.out.println("缓存string内容："+Msg_Maintain);
        Send_Maintain();//发送缓存数组出去
    }

    //add
    public void add(String s){
        arrayList.add(s);
        Msg_Maintain=Msg_Maintain+"/"+s;
        Send_Maintain();//发送缓存数组
        System.out.println("发送MSG："+Msg_Maintain);
    }

    //remove
    public void remove(int index){
        arrayList.remove(index);
        String spl[]=Msg_Maintain.split("/");
        String new_spl[]=new String[spl.length-1];

        // spl[index+3]="";//这里可能会出问题
        for (int i = 0; i < spl.length - 1; i++) {
            if(i<index+3)
                new_spl[i] = spl[i];
            else
                new_spl[i]=spl[i+1];
        }


        StringJoiner sj = new StringJoiner("/");
        for (String str : new_spl) {
            sj.add(str);
        }
        Msg_Maintain=sj.toString();
        System.out.println("缓存string内容："+Msg_Maintain);
        Send_Maintain();//发送缓存数组出去
    }
    

    //维护线程
    static class MaintainThread extends Thread{
        DatagramSocket soc;
        MaintainThread(){

        }
        @Override
        public void run(){
            try {
                soc = new DatagramSocket(1236);
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
                    Check_Maintain(rs, ip);

                }
            }
            catch(Exception e){

                    }
        }
    
        void Check_Maintain(String msg, String ip){//判断收到的同步信息
            String spl[] = msg.split("/");
                if (spl[0].equals("Triple")) {// 如果是游戏信息
                    if (spl[1].equals("MaintainArray")) {// 如果是数组同步消息
                        for(int k=0;k<MaintainArray.num_array;k++){
                            if(spl[2].equals(Integer.toString(MaintainArray.maintainArrays[k].my_array_num))){//遍历寻找同步信息
                                MaintainArray.maintainArrays[k].arrayList.clear();//清空当前数组
                                for (int i = 3; i < spl.length; i++) {
                                    // if(arrayList.size()>i-3)//如果在数组范围内，就设置其
                                    //     arrayList.set(i-3, spl[i]);
                                    // else
                                    MaintainArray.maintainArrays[k].arrayList.add(spl[i]);//收到的信息，添加进目标数组
                                    MaintainArray.maintainArrays[k].Msg_Maintain=msg;//发送的赋值
                                }
                                System.out.println("收到数组"+msg+"，并且处理刷新了");
                                break;
                            //基本思想为：在所有自维护数组的数组里遍历找到要操作的数组，然后清空他，把所有收到的信息添加进这个数组
                            }

                        }
                        // if(spl[2].equals(Integer.toString(my_array_num))){//如果是自己的数组同步消息
                        //     arrayList.clear();//清空当前数组
                        //     for (int i = 3; i < spl.length; i++) {
                        //         // if(arrayList.size()>i-3)//如果在数组范围内，就设置其
                        //         //     arrayList.set(i-3, spl[i]);
                        //         // else
                        //             arrayList.add(spl[i]);
                        //     }

                        // }
                        //打印
                        
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


    //将目标array复制过来
    void Set_Array(ArrayList <String>e){
        arrayList=e;//粗暴浅copy
    }

    //发送缓存数组出去
    void Send_Maintain(){
        for(int i=0;i<MaintainArray.ip_list.arrayList.size();i++){
            Send_Msg(ip_list.get(i), Msg_Maintain, 1236);//向所有房间内ip发送
            System.out.println("向"+ip_list.get(i)+"发送");
        }
    }
}
