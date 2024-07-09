package com.hu.util;

import java.sql.*;

public class ConnectionUtil {

    static final String DB_URL = "jdbc:mysql://119.8.28.214:3306/front";
    static final String USER = "root";
    static final String PASS = "Hu@210205";
    public static Connection getJdbcConnection(){

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try(Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)){
           return conn;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static void main(String[] args) {
        Connection jdbcConnection = getJdbcConnection();
    }
}
