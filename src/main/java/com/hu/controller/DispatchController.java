package com.hu.controller;


import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hu.util.ConnectionUtil;
import com.mysql.cj.NativeSession;
import com.mysql.cj.jdbc.ConnectionImpl;
import com.mysql.cj.protocol.NetworkResources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DispatchController {

    @RequestMapping("/**")
    public ResponseEntity dispatch(HttpServletRequest request) throws Exception {
        String baseUrl = "https://isdpcloud.huawei.com";
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        String url = baseUrl + requestURI;
        String queryString = request.getQueryString();
        String body = null;
        if (!"GET".equals(method)) {
            body = IoUtil.read(request.getInputStream(), StandardCharsets.UTF_8);
        }
        Enumeration<String> headerNames = request.getHeaderNames();

        Map<String, String> headersMap = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersMap.put(headerName, headerValue);
        }

        Connection jdbcConnection = ConnectionUtil.getJdbcConnection();
        PreparedStatement preparedStatement = jdbcConnection.prepareStatement("select http_call(?,?,?,?,?)");
        preparedStatement.setString(1, url);
        preparedStatement.setString(2, queryString);
        preparedStatement.setString(3, method);
        preparedStatement.setString(4, JSONUtil.toJsonStr(headersMap));
        preparedStatement.setString(5, body);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        String result = resultSet.getString("result");
        JSONObject jsonObject = JSONUtil.parseObj(result);
        String result1 = jsonObject.getStr("result");
        String headers1 = jsonObject.getStr("headers");
        HttpHeaders headers = new HttpHeaders();

        JSONObject headers2 = JSONUtil.parseObj(headers1);
        for (Map.Entry<String, Object> entry : headers2.entrySet()) {
            String key = entry.getKey();
            List<String> value = (List<String>) entry.getValue();
            headers.add(key, value.get(0));
        }
        String status = jsonObject.getStr("status");

        return new ResponseEntity<>(
                result1,
                HttpStatus.resolve(Integer.parseInt(status)));
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


        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("0123456789".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        InputStream inputStream = socket.getInputStream();

        while (true) {

            byte[] buf = new byte[10];
            if (inputStream.read(buf, 0, 10) != -1) {
                String result1 = new String(buf);
                System.out.println(result1);
            }
        }
    }

}
