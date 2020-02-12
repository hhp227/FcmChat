package com.hhp227.fcmchat.app;

public interface EndPoints {
    String BASE_URL = "http://hhp227.dothome.co.kr/gcm_chat/v1";
    String LOGIN = BASE_URL + "/user/login";
    String USER = BASE_URL + "/user/_ID_";
    String CHAT_ROOMS = BASE_URL + "/chat_rooms";
    String CHAT_THREAD = BASE_URL + "/chat_rooms/_ID_";
    String CHAT_ROOM_MESSAGE = BASE_URL + "/chat_rooms/_ID_/message";
}
