//package com.hu.h2_demo.config;//import org.h2.tools.Server;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import javax.annotation.PostConstruct;
//import javax.sql.DataSource;
//
//@Configuration
//public class H2Config {
//
//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
////    @Bean
////    Server h2Server() throws SQLException {
////        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
////    }
//
//    @PostConstruct
//    public void registerCustomFunctions() {
//
//        // 创建别名以注册自定义函数
//        jdbcTemplate.execute("CREATE ALIAS IF NOT EXISTS http FOR \"com.hu.h2_demo.H2Function.http\"");
//    }
//}