package com.hu.ws;

import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

import static com.hu.ws.WebsocketService.TUPLE_MAP;

@ServerEndpoint(value = "/ws/mag",subprotocols = "xmpp")
@Component
public class WsMagServer {

    @Value("${saas.ws_mag_path:/ws/}")
    private String wsPath = "/ws/";
    private static final Logger logger = LoggerFactory.getLogger(WsMagServer.class);
    // 连接建立成功调用的方法
    @OnOpen
    public void onOpen(Session session, EndpointConfig conf) {
        ReflectUtil.setFieldValue(session,"id", UUID.fastUUID().toString(true));
        Map<String, Object> userProperties = conf.getUserProperties();

        String queryString = session.getQueryString();
//        logger.info("Connected to client, session ID: " + session.getId());
        new Thread(() -> {
            try {
                WebsocketService.setupWs(session,userProperties,wsPath, queryString);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).start();
    }

    @OnClose
    public void onClose() {
        logger.info("Connection closed");
    }

    // 收到手机端消息后，发送给db
    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("Message from app: " + message);
        Tuple tuple = TUPLE_MAP.get(session.getId());
        if (tuple != null) {
            Socket socket = tuple.get(0);
            try {
                if (socket != null && !socket.isClosed()) {
                    String sessionId = session.getId();
                    // 发送给db的消息加上当前sessionId的前缀
                    String newMsg = StrUtil.format("{}##{},,", sessionId, message);
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(newMsg.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    logger.info("write msg to  remote db: " + newMsg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 发生错误时调用
    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("Error occurred");
        error.printStackTrace();
    }
}
