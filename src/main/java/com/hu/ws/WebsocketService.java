package com.hu.ws;

import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hu.util.ConnectionUtil;
import com.hu.util.SpringContextUtil;
import com.mysql.cj.NativeSession;
import com.mysql.cj.jdbc.ConnectionImpl;
import com.mysql.cj.protocol.NetworkResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebsocketService {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketService.class);
    private static Tuple FIXED_TWO_SOCKET_TUPLE = null;
    public static ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<String, Session>();
    public static ConcurrentHashMap<String, Tuple> TUPLE_MAP = new ConcurrentHashMap<>();

    public static Tuple setUpSocket() {
        try {
            String randomId = UUID.fastUUID().toString(true);
            Socket java2dbSocket = connectDbSocket(randomId);
            String fd = getFd(randomId, java2dbSocket);
            String randomId2 = UUID.fastUUID().toString(true);
            Socket db2javaSocket = connectDbSocket(randomId2);
            String fd2 = getFd(randomId2, db2javaSocket);
            Tuple tuple = new Tuple(java2dbSocket, db2javaSocket, fd, fd2);
//            FIXED_TWO_SOCKET_TUPLE = new Tuple(db2javaSocket, db2javaSocket, fd2, fd2);
            // 监听数据库发回的消息,并发送给手机端
            new Thread(() -> {
                while (!db2javaSocket.isClosed()) {
                    try {
                        InputStream inputStream1 = db2javaSocket.getInputStream();
                        int len = inputStream1.available();
                        if (len == 0) {
                            len = 1;
                        }
                        byte[] buf0 = new byte[len];
                        int a = inputStream1.read(buf0, 0, len);
                        String db2javaMsg = new String(buf0);
                        if (len == 1) {
                            int len2 = inputStream1.available();
                            byte[] buf1 = new byte[len2];
                            inputStream1.read(buf1, 0, len2);
                            db2javaMsg += new String(buf1);
                        }

                        logger.info(db2javaMsg);
                        if (db2javaMsg.contains(",,")) {
                            String format = StrUtil.format("receive msg:{} from database java2dbSocket", db2javaMsg);
                            logger.info(format);
                            String[] msgArray = db2javaMsg.split(",");
                            for (String oneMsg : msgArray) {
                                if (oneMsg.contains("##")) {
                                    String[] split = oneMsg.split("##");
                                    String sessionId = split[0];
                                    String msg = split[1];
                                    Session session = SESSION_MAP.get(sessionId);
                                    if (session != null) {
                                        if (session.isOpen()) {
                                            session.getBasicRemote().sendText(msg);
                                            logger.info("send above msg to app success");
                                        } else {
                                            logger.error("webSocketSession has closed, " + sessionId);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                }
            }).start();
            return tuple;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Tuple getTuple() {
        return FIXED_TWO_SOCKET_TUPLE;
    }

    public static void setupWs(Session websocketSession, Map<String, Object> userProperties, String wsPath, String queryString) throws SQLException, IOException {
        Map<String, String> headersMap = new HashMap<>();
        if (userProperties != null && !userProperties.isEmpty()) {
            Map<String, List<String>> headers = (Map<String, List<String>>) userProperties.get("headers");
            for (Map.Entry<String, List<String>> en : headers.entrySet()) {
                String key = en.getKey();
                List<String> value = en.getValue();
                logger.info("{}:{}", key, value.get(0));
                headersMap.put(key, value.get(0));
            }
        }
        headersMap.remove("baseinfo");
        headersMap.remove("host");
        String sessionId = websocketSession.getId();
        Tuple tuple = setUpSocket();
        String fd = tuple.get(2);
        String fd2 = tuple.get(3);

        String ws_url = SpringContextUtil.getSpringProperties("saas.isdp_ws_url") + wsPath;
        if (!StrUtil.isEmpty(queryString)) {
            ws_url += "?" + queryString;
        }
        logger.info("ws_url:{}", ws_url);
        String domain_url = SpringContextUtil.getSpringProperties("saas.isdp_domain_url");

        Connection connection = ConnectionUtil.getJdbcConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("select setup_ws(?,?,?,?,?,?,?)");

        preparedStatement.setLong(1, Long.parseLong(fd));
        preparedStatement.setLong(2, Long.parseLong(fd2));
        preparedStatement.setString(3, sessionId);
        preparedStatement.setString(4, ws_url);
        preparedStatement.setString(5, domain_url);
        preparedStatement.setString(6, "xmpp");
        preparedStatement.setString(7, JSONUtil.toJsonStr(headersMap));

        ResultSet resultSet = preparedStatement.executeQuery();
        String result = null;
        while (resultSet.next()) {
            result = resultSet.getString(1);
        }
        logger.info(result);
        resultSet.close();
        connection.close();

        SESSION_MAP.put(websocketSession.getId(), websocketSession);
        TUPLE_MAP.put(websocketSession.getId(), tuple);

    }

    public static String getFd(String randomId, Socket socket) throws IOException, InterruptedException {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("ok".getBytes(StandardCharsets.UTF_8));
        int bufLen = inputStream.available();
        if (bufLen == 0) {
            Thread.sleep(2000L);
            bufLen = 1;
        }
        byte[] buf = new byte[bufLen];
        int n = inputStream.read(buf, 0, bufLen);
        String info = new String(buf);
        if (bufLen == 1) {
            int bufLen2 = inputStream.available();
            byte[] buf2 = new byte[bufLen2];
            int n2 = inputStream.read(buf2, 0, bufLen2);
            info += new String(buf2);
        }
        logger.info(info);
        String fd = null;
        if (info.startsWith(randomId + ",")) {
            String[] split = info.split(",");
            fd = split[1];
        }


        return fd;
    }

    public static Socket connectDbSocket(String id) throws SQLException, SocketException {

        Connection connection = ConnectionUtil.getJdbcConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("select setup_socket(?)");
        preparedStatement.setString(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();
        String result = null;
        resultSet.next();
        result = resultSet.getString(1);

        logger.info("ufd setup socket success id:" + result);
        Socket socket;
       if (connection instanceof ConnectionImpl) {
            ConnectionImpl conn = (ConnectionImpl) connection;
            NativeSession nativeSession = conn.getSession();
            NetworkResources networkResources = nativeSession.getNetworkResources();
            socket = (Socket) ReflectUtil.getFieldValue(networkResources, "mysqlConnection");
        } else {
            throw new RuntimeException("other connection");
        }
        socket.setKeepAlive(true);
        return socket;
    }

}
