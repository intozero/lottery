package com.vipin.lottery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnectionTry {

    public static void main(String[] args) {

        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con= DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/lottery","root","Admin@123");
            Statement stmt=con.createStatement();
            ResultSet rs=stmt.executeQuery("select * from LotteryData");
            while(rs.next())
                System.out.println(rs.getDate(1)+"  "+rs.getInt(2)+"  "+rs.getInt(3));
            con.close();
        }catch(Exception e){ System.out.println(e);}
    }
}
