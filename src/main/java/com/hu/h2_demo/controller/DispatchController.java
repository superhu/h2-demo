package com.hu.h2_demo.controller;


import cn.hutool.core.io.IoUtil;
import com.hu.h2_demo.H2Function;
import com.mysql.cj.NativeSession;
import com.mysql.cj.jdbc.ConnectionImpl;
import com.mysql.cj.protocol.NetworkResources;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Collection;
import java.util.Map;

@Controller
public class DispatchController {
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    @Autowired
    private JdbcTemplate jdbcTemplate;

//    @GetMapping("/test")
//    @ResponseBody
//    public Object test(String url) throws Exception {
//        String result = H2Function.http("https://httpbin.dev/get");
//
//        return result;
//    }

    @PostMapping("/**")
    public Object file(HttpServletRequest request) throws Exception {

        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        String body = null;
        if (!"GET".equals(method)) {
            body = IoUtil.read(request.getInputStream(), StandardCharsets.UTF_8);
        }

        String result = doQuery();
        return result;
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchFieldException, IOException {
        String result = doQuery();
        System.out.println(result);
    }

    private static String doQuery() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoSuchFieldException, IOException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection("jdbc:mysql://119.8.28.214:3306/mysql?autoReconnect=false&useSSL=false", "root", "Hu@210205");
        ConnectionImpl conn = (ConnectionImpl) connection;
        NativeSession session = conn.getSession();
        NetworkResources networkResources = session.getNetworkResources();
        Field f = networkResources.getClass().getDeclaredField("mysqlConnection"); //NoSuchFieldException
        f.setAccessible(true);

        Socket socket = (Socket) f.get(networkResources);


        PreparedStatement preparedStatement = connection.prepareStatement("select websocket(?)");
        preparedStatement.setString(1, "120.235.75.10");

//        PreparedStatement preparedStatement = connection.prepareStatement("select http_call(?,?,?,?,?,?)");
//        preparedStatement.setString(1,"https://httpbin.dev/get");
//        preparedStatement.setString(2,"");
//        preparedStatement.setString(3,"GET");
//        preparedStatement.setString(4,"{}");
//        preparedStatement.setString(5,"");
//        preparedStatement.setString(6,"");
        ResultSet resultSet = preparedStatement.executeQuery();
        String result = null;
        if (resultSet.next()) {
            result = resultSet.getString(1);
        }

//        conn.ping();

        System.out.println("sql result:" + result);

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("0123456789".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        InputStream inputStream = socket.getInputStream();

        while (true){

            byte[] buf = new byte[10];
            if(inputStream.read(buf, 0, 10) != -1) {
                String result1 = new String(buf);
                System.out.println(result1);
            }
        }


//        return result;
    }


    @PostMapping("/upload")
    @ResponseBody
    public Object file(@RequestParam("upload") MultipartFile file, HttpServletRequest request) throws Exception {

        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            Map<String, String[]> parameterMap = multipartRequest.getParameterMap();
            System.out.println(parameterMap);
        }
        String filename = file.getOriginalFilename();
        System.out.println(filename);

        return filename;
    }


}
