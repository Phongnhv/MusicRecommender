package com.example.mrs;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {
    public static Connection connectDB()
    {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            return DriverManager.getConnection("jdbc:mysql://localhost/mr_sys","root","");
        }catch (Exception e) {e.printStackTrace(System.out);}
        return null;
    }
}
