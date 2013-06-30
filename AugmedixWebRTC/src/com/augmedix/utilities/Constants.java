package com.augmedix.utilities;

public class Constants {

	public final static String TURN_CREDENTIALS="augmedix2013";
	//public final static String TURN_URL="turn:augmedix@203.76.151.87";
	public final static String TURN_URL="stun:stun.l.google.com:19302";
	
	public final static String PC_CONFIG= "{"
		  +" \"iceServers\" : [ {"
		  +" \"url\" : \"turn:augmedix@203.76.151.87\", "
		  +" \"credential\" : \"augmedix2013\" "
		  +"	  }, { "
		  +" \"url\" : \"stun:stun.l.google.com:19302\" "
		  +"	  } ] }";
	public final static String SERVER_URL = "http://203.76.151.87:8090/";
	//public final static String SERVER_URL = "http://192.168.0.136:8090/";
	public final static String LOGIN_EVENT = "log_in";
	public final static String ON_LOGIN_USER = "on_log_in";
	public final static String CONNECT_USER = "connect_user";
	public final static String ON_CONNECT_USER = "on_connect_user";
	public final static String UPDATE_ROOM = "update_room";
	public final static String ON_UPDATE_ROOM = "on_update_room";
	public final static String MESSAGE = "message";
	public final static String ON_MESSAGE = "on_message";
	public final static String RESET_STATUS = "reset_status";
	public final static String CONNECT_ME = "connect_me";
	public final static String ON_CONNECT_ME = "on_connect_me";
	public final static String HANG_UP = "hang_up";
	public final static String DISCONNECT = "disconnect";
	public final static String CONNECTION_TIMEOUT = "";

	public final static String ARRAY_NAME = "data";
	public final static String WEBRTC_HI_1 = "on_webrtc_hi1";
	public final static String MSG = "msg";
	public final static String TYPE = "type";

	public final static String NEW_ROOM_KEY = "new_roomkey";
	
	public final static String PC_CONSTRAINS= " { \"optional\" : [ {   \"DtlsSrtpKeyAgreement\" : true } ]  }";


}
