package com.augmedix.webrtc;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import com.augmedix.utilities.Constants;
import com.augmedix.utilities.JSonParser;

import android.app.Activity;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements
AppRTCClient.IceServersObserver{
	SocketIO socket = null;
	private static final String TAG = "SocketIOSample";
	Button btnCallToBuddy;
	Button btnLogin, btnConnect;
	EditText txtBuddyId, txtLoginId;
	TextView lblStatus;
	private Toast logToast;
	String type, msg, roomkey = null;
	MediaStream lMS=null;
	private MediaConstraints sdpMediaConstraints;
	private PeerConnection pc;
	private final Boolean[] quit = new Boolean[] { false };
	private final PCObserver pcObserver = new PCObserver();
	private final SDPObserver sdpObserver = new SDPObserver();
	//private final GAEChannelClient.MessageHandler gaeHandler = new GAEHandler();
	//private AppRTCClient appRtcClient = new AppRTCClient(this, gaeHandler, this);
	private LinkedList<IceCandidate> queuedRemoteCandidates = new LinkedList<IceCandidate>();
	LinkedList<PeerConnection.IceServer> iceServers=null;
	private VideoStreamsView vsv;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		
		
		linkWithView();
		initializationBtnLogin();
		initializationBtnConnect();
		initializationBtnCallToBuddy();

		try {
			showLog(Constants.SERVER_URL);
			socket = new SocketIO(Constants.SERVER_URL);

		} catch (MalformedURLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		
		// Since the error-handling of this demo consists of throwing
				// RuntimeExceptions and we assume that'll terminate the app, we install
				// this default handler so it's applied to background threads as well.
				Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
					public void uncaughtException(Thread t, Throwable e) {
						e.printStackTrace();
						System.exit(-1);
					}
				});

				Point displaySize = new Point();
				getWindowManager().getDefaultDisplay().getSize(displaySize);
				
				vsv = new VideoStreamsView(this, displaySize);
			// TODO please add this line after call to buddy
			//	setContentView(vsv);

				abortUnless(PeerConnectionFactory.initializeAndroidGlobals(this),
						"Failed to initializeAndroidGlobals");

				((AudioManager) getSystemService(AUDIO_SERVICE))
						.setMode(AudioManager.MODE_IN_COMMUNICATION);

				sdpMediaConstraints = new MediaConstraints();
				sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
						"OfferToReceiveAudio", "true"));
				sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
						"OfferToReceiveVideo", "true"));
		
		socket.connect(new IOCallback() {

			@Override
			public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
				// TODO Auto-generated method stub
				showLog("On Message Method " + arg0);
			}

			@Override
			public void onMessage(String arg0, IOAcknowledge arg1) {
				// TODO Auto-generated method stub
				showLog("On Message Method " + arg0);

			}

			@Override
			public void onError(SocketIOException arg0) {
				// TODO Auto-generated method stub
				showLog("On Error Method " + arg0);
				arg0.printStackTrace();
			}

			@Override
			public void onDisconnect() {
				// TODO Auto-generated method stub
				showLog("On Disconnect Method");
			}

			@Override
			public void onConnect() {
				// TODO Auto-generated method stub
				showLog("On Connect Method in Java");
				socket.emit("webrtc_hi1", "Hello from android");

			}

			@Override
			public void on(String arg0, IOAcknowledge arg1, Object... arg2) {
				// TODO Auto-generated method stub
				/*
				 * if (arg0.equals(Constants.WEBRTC_HI_1)) {
				 * 
				 * try {
				 * 
				 * JSONArray aJsonArray = JSonParser .getJsonParsingData(arg2);
				 * for (int i = 0; i < aJsonArray.length(); i++) { JSONObject
				 * aObject = aJsonArray.getJSONObject(i);
				 * 
				 * Log.e("Type ", aObject.getString(Constants.TYPE));
				 * Log.e("RoomKey ", aObject.getString(Constants.NEW_ROOM_KEY));
				 * 
				 * } } catch (JSONException e) { // TODO Auto-generated catch
				 * block e.printStackTrace(); }
				 * 
				 * } else
				 */
				if (arg0.equals(Constants.ON_UPDATE_ROOM)) {
					Log.e("On Update Room ", "On Update Room");
					try {

						JSONArray aJsonArray = JSonParser
								.getJsonParsingData(arg2);
						for (int i = 0; i < aJsonArray.length(); i++) {
							JSONObject aObject = aJsonArray.getJSONObject(i);

							// Log.e("Type ",
							// aObject.getString(Constants.TYPE));
							Log.e("Msg ", aObject.getString(Constants.MSG));
							lblStatus.setText("Msg "
									+ aObject.getString(Constants.MSG));

						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				else if (arg0.equals(Constants.MESSAGE)) {
					Log.e("Message ", "Broadcast msg "+ Arrays.deepToString(arg2));
					
					try 
					{
						// get the json string
						String serverString = Arrays.deepToString(arg2);
						// now remove the start and end "]"
						String jsonString =  JSonParser.getJsonObjectStrFromServerStr(serverString);
						JSONObject json = new JSONObject(jsonString );
						String type = (String) json.get("type");
						if (type.equals("candidate")) 
						{
							
							String sdpMid = json.getString("sdpMid");
							int sdpMIndex = json.getInt("sdpMLineIndex");
							String candidateStr = json.getString("candidate");
							
							IceCandidate candidate = new IceCandidate(sdpMid, sdpMIndex, candidateStr);
							
							if (queuedRemoteCandidates != null) {
								queuedRemoteCandidates.add(candidate);
							} else {
								pc.addIceCandidate(candidate);
							}
						}
						else if (type.equals("answer") || type.equals("offer")) {
							SessionDescription sdp = new SessionDescription(
									SessionDescription.Type.fromCanonicalForm(type),
									(String) json.get("sdp"));
							pc.setRemoteDescription(sdpObserver, sdp);
							//pc.addStream(lMS, new MediaConstraints());
						}
						
						else if (type.equals("bye")) {
							logAndToast("Remote end hung up; dropping PeerConnection");
							disconnectAndExit();
						} else {
							throw new RuntimeException("Unexpected message: " + Arrays.deepToString(arg2));
						}
					} catch (JSONException e) {
						throw new RuntimeException(e);
					}
					
					
					
					
					/*try {

						JSONArray aJsonArray = JSonParser
								.getJsonParsingData(arg2);
						for (int i = 0; i < aJsonArray.length(); i++) {
							JSONObject aObject = aJsonArray.getJSONObject(i);

							// Log.e("Type ",
							// aObject.getString(Constants.TYPE));
							Log.e("Msg ", aObject.getString(Constants.MSG));
							lblStatus.setText("Msg "
									+ aObject.getString(Constants.MSG));

						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/

				}
				else if (arg0.equals(Constants.ON_CONNECT_USER)) {
					
					showLog("On connect User " + Arrays.deepToString(arg2));
					lblStatus.setText("On connect User "
							+ Arrays.deepToString(arg2));
					try {

						JSONArray aJsonArray = JSonParser
								.getJsonParsingData(arg2);
						for (int i = 0; i < aJsonArray.length(); i++) {
							JSONObject aObject = aJsonArray.getJSONObject(i);

							Log.e("Type ", aObject.getString(Constants.TYPE));
							Log.e("RoomKey ",
									aObject.getString(Constants.NEW_ROOM_KEY));
							roomkey = aObject.getString(Constants.NEW_ROOM_KEY);
							socket.emit(Constants.UPDATE_ROOM, aObject.getString(Constants.NEW_ROOM_KEY));
							lblStatus.setText("Type and Room Key: "
									+ aObject.getString(Constants.TYPE) + " , "
									+ aObject.getString(Constants.NEW_ROOM_KEY));
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				
					

				
/*
				else if (arg0.equals(Constants.ON_CONNECT_USER)) {
					showLog("On connect User " + Arrays.deepToString(arg2));
					lblStatus.setText("On connect User "
							+ Arrays.deepToString(arg2));
					
					try {

						JSONArray aJsonArray = JSonParser
								.getJsonParsingData(arg2);
						for (int i = 0; i < aJsonArray.length(); i++) {
							JSONObject aObject = aJsonArray.getJSONObject(i);

							try {
								type = aObject.getString(Constants.TYPE);
								if (type.equals("false")) {

									msg = aObject.getString(Constants.MSG);
									Log.e("msg ", msg);
									lblStatus.setText("Type: " + type + " Msg"
											+ msg);
								} else {
									roomkey = aObject
											.getString(Constants.NEW_ROOM_KEY);
									Log.e("RoomKey for emit update room", roomkey);
									socket.emit(Constants.UPDATE_ROOM, roomkey);

									lblStatus.setText("Type: " + type
											+ " RoomKey" + roomkey);
								//	  pc.addIceCandidate();
								}

							} catch (Exception e) {
								// TODO: handle exception
								lblStatus.setText("Error");
							}

						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else {
					showLog("Else.......Condition "+ arg0);

				}*/
			}
		});
	}

	private void initializationBtnCallToBuddy() {
		// TODO Auto-generated method stub
btnCallToBuddy.setOnClickListener(new OnClickListener() {
	
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		boolean initiator = true;
		 iceServers = iceServersFromPCConfigJSON(Constants.PC_CONFIG);

		boolean isTurnPresent = true;
		/*for (PeerConnection.IceServer server : iceServers) {
			if (server.uri.startsWith("turn:")) {
				isTurnPresent = true;
				break;
			}
		}
		if (!isTurnPresent) {
			iceServers.add(requestTurnServer(getVarValue(roomHtml,
					"turnUrl", true)));
		}*/

		MediaConstraints pcConstraints = constraintsFromJSON(Constants.PC_CONSTRAINS);
		PeerConnectionFactory factory = new PeerConnectionFactory();
		setContentView(vsv);
		
		
		pc = factory.createPeerConnection(iceServers,
				pcConstraints, pcObserver);
		

		{
			final PeerConnection finalPC = pc;
			final Runnable repeatedStatsLogger = new Runnable() {
				public void run() {
					synchronized (quit[0]) {
						if (quit[0]) {
							return;
						}
						final Runnable runnableThis = this;
						boolean success = finalPC.getStats(new StatsObserver() {
							public void onComplete(StatsReport[] reports) {
								for (StatsReport report : reports) {
									Log.d(TAG, "Stats: " + report.toString());
								}
								vsv.postDelayed(runnableThis, 10000);
							}
						}, null);
						if (!success) {
							throw new RuntimeException(
									"getStats() return false!");
						}
					}
				}
			};
			vsv.postDelayed(repeatedStatsLogger, 10000);
		}

		{
			logAndToast("Creating local video source...");
			VideoCapturer capturer = getVideoCapturer();
			VideoSource videoSource = factory.createVideoSource(capturer,
					new MediaConstraints());
			lMS = factory.createLocalMediaStream("ARDAMS");
			VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0",
					videoSource);
			videoTrack.addRenderer(new VideoRenderer(new VideoCallbacks(vsv,
					VideoStreamsView.Endpoint.LOCAL)));
			lMS.addTrack(videoTrack);
			lMS.addTrack(factory.createAudioTrack("ARDAMSa0"));
			boolean streamAdded = pc.addStream(lMS, new MediaConstraints());
			Log.e(TAG, "Stream added " + streamAdded );
		}
		logAndToast("Waiting for ICE candidates...");
		pc.createOffer(sdpObserver, sdpMediaConstraints);
		socket.emit(Constants.CONNECT_ME, roomkey);
	}
});
	}

	private void initializationBtnConnect() {
		// TODO Auto-generated method stub
		btnConnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				//appRtcClient.connectToRoom("");
				
				String callingBuddyId = txtBuddyId.getText().toString();
				socket.emit(Constants.CONNECT_USER, callingBuddyId);
				lblStatus.setText("Connecting");
			}
		});

	}
	private MediaConstraints constraintsFromJSON(String jsonString) {
		try {
			MediaConstraints constraints = new MediaConstraints();
			JSONObject json = new JSONObject(jsonString);
			if (json.has("mandatory")) {
				JSONObject mandatoryJSON = json.getJSONObject("mandatory");
				JSONArray mandatoryKeys = mandatoryJSON.names();
				for (int i = 0; i < mandatoryKeys.length(); ++i) {
					String key = (String) mandatoryKeys.getString(i);
					String value = mandatoryJSON.getString(key);
					constraints.mandatory
							.add(new MediaConstraints.KeyValuePair(key,
									value));
				}
			}
			if (json.has("optional")) {
				JSONArray optionalJSON = json.getJSONArray("optional");
				for (int i = 0; i < optionalJSON.length(); ++i) {
					JSONObject keyValueDict = optionalJSON.getJSONObject(i);
					String key = keyValueDict.names().getString(0);
					String value = keyValueDict.getString(key);
					constraints.optional
							.add(new MediaConstraints.KeyValuePair(key,
									value));
				}
			}
			return constraints;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	// Return the list of ICE servers described by a WebRTCPeerConnection
		// configuration string.
		private LinkedList<PeerConnection.IceServer> iceServersFromPCConfigJSON(
				String pcConfig)
		{
			LinkedList<PeerConnection.IceServer> iceServerList = new LinkedList<PeerConnection.IceServer>();
			try {
				/*JSONObject json = new JSONObject(pcConfig);
				JSONArray servers = json.getJSONArray("iceServers");
				LinkedList<PeerConnection.IceServer> ret = new LinkedList<PeerConnection.IceServer>();
				for (int i = 0; i < servers.length(); ++i) {
					JSONObject server = servers.getJSONObject(i);
					String url = server.getString("url");
					String credential = server.has("credential") ? server
							.getString("credential") : "";
					ret.add(new PeerConnection.IceServer(url, "", credential));*/
					IceServer server = new PeerConnection.IceServer( Constants.TURN_URL, "", "" );
					iceServerList.add(server);
				}
				
			
			catch ( Exception e) 
			{
				e.printStackTrace();
			}
			return iceServerList;
		}

	private void initializationBtnLogin() {
		// TODO Auto-generated method stub
		btnLogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String loginId = txtLoginId.getText().toString();
				showLog("User Id" + loginId);
				lblStatus.setText("User Id " + loginId);
				socket.emit(Constants.LOGIN_EVENT, loginId);
			}
		});
	}

	private void linkWithView() {
		// TODO Auto-generated method stub
		btnLogin = (Button) findViewById(R.id.button1);
		btnConnect = (Button) findViewById(R.id.button2);
		btnCallToBuddy = (Button) findViewById(R.id.button3);
		txtBuddyId = (EditText) findViewById(R.id.editText2);
		txtLoginId = (EditText) findViewById(R.id.editText1);
		lblStatus = (TextView) findViewById(R.id.textView3);

	}

	private void showLog(String msg) {
		// TODO Auto-generated method stub
		Log.e("MainActivity", msg);
	}
	@Override
	public void onIceServers(List<PeerConnection.IceServer> iceServers) {
			Log.e(TAG,"444 MSG!!!");
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// Implementation detail: observe ICE & stream changes and react
	// accordingly.
	private class PCObserver implements PeerConnection.Observer {
		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			runOnUiThread(new Runnable() {
				public void run() {
					JSONObject json = new JSONObject();
					jsonPut(json, "type", "candidate");
					jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);
					jsonPut(json, "sdpMid", candidate.sdpMid);
					jsonPut(json, "candidate", candidate.sdp);
					Log.e(TAG, "Json in PC Observer "+json+" Called");
					logAndToast("PC Observer Json");
					sendMessage(json);
					 
					//pc.addStream(lMS, new MediaConstraints());
				}
			});
		}

		@Override
		public void onError() {
			runOnUiThread(new Runnable() {
				
				public void run() {
					Log.e(TAG, "On Error run pcobserver");
					throw new RuntimeException("PeerConnection error!");
				}
			});
		}

		@Override
		public void onSignalingChange(PeerConnection.SignalingState newState) {
		}

		@Override
		public void onIceConnectionChange(
				PeerConnection.IceConnectionState newState) {
		}

		@Override
		public void onIceGatheringChange(
				PeerConnection.IceGatheringState newState) {
		}

		@Override
		public void onAddStream(final MediaStream stream) {
			runOnUiThread(new Runnable() {
				public void run() {
					Log.e(TAG, "onAddStream");
					logAndToast("onAddStream");
					abortUnless(stream.audioTracks.size() == 1
							&& stream.videoTracks.size() == 1,
							"Weird-looking stream: " + stream);
					stream.videoTracks.get(0).addRenderer(
							new VideoRenderer(new VideoCallbacks(vsv,
									VideoStreamsView.Endpoint.REMOTE)));
				}
			});
		}

		@Override
		public void onRemoveStream(final MediaStream stream) {
			runOnUiThread(new Runnable() {
				public void run() {
					Log.e(TAG, "on Remove Stream Called");
					stream.videoTracks.get(0).dispose();
				}
			});
		}
	}

	// Put a |key|->|value| mapping in |json|.
	private static void jsonPut(JSONObject json, String key, Object value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	// Poor-man's assert(): die with |msg| unless |condition| is true.
	private static void abortUnless(boolean condition, String msg) {
		if (!condition) {
			throw new RuntimeException(msg);
		}
	}

	// Implementation detail: handle offer creation/signaling and answer
	// setting,
	// as well as adding remote ICE candidates once the answer SDP is set.
	private class SDPObserver implements SdpObserver {
		@Override
		public void onCreateSuccess(final SessionDescription sdp) {
			runOnUiThread(new Runnable() {
				public void run() {
					pc.setLocalDescription(sdpObserver, sdp);
					logAndToast("Sending " + sdp.type);
					JSONObject json = new JSONObject();
					jsonPut(json, "type", sdp.type.canonicalForm());
					jsonPut(json, "sdp", sdp.description);
					// TODO send json metod
					sendMessage(json);
					//pc.addStream(lMS, new MediaConstraints());
				}
			});
		}

		@Override
		public void onSetSuccess() {
			runOnUiThread(new Runnable() {
				public void run()
				{
					Log.e(TAG,"In onsetSuccess of sdp");
					if (pc.getRemoteDescription() != null) {
						// We've set our local offer and received & set the
						// remote
						// answer, so drain candidates.
						Log.e(TAG,"drain remote candidates");
						drainRemoteCandidates();
					}
					//pc.addStream(lMS, new MediaConstraints());
					/*if ( appRtcClient.isInitiator() ) {
						if (pc.getRemoteDescription() != null) {
							// We've set our local offer and received & set the
							// remote
							// answer, so drain candidates.
							drainRemoteCandidates();
						}
					} else {
						if (pc.getLocalDescription() == null) {
							// We just set the remote offer, time to create our
							// answer.
							logAndToast("Creating answer");
							pc.createAnswer(SDPObserver.this,
									sdpMediaConstraints);
						} else {
							// Sent our answer and set it as local description;
							// drain
							// candidates.
							drainRemoteCandidates();
						}
					}*/
				}
			});
		}

		@Override
		public void onCreateFailure(final String error) {
			runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("createSDP error: " + error);
				}
			});
		}

		@Override
		public void onSetFailure(final String error) {
			runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("setSDP error: " + error);
				}
			});
		}

		private void drainRemoteCandidates() {
			for (IceCandidate candidate : queuedRemoteCandidates) {
				pc.addIceCandidate(candidate);
			}
			queuedRemoteCandidates = null;
		}
	}

	// Log |msg| and Toast about it.
	private void logAndToast(String msg) {
		Log.d(TAG, msg);
		if (logToast != null) {
			logToast.cancel();
		}
		logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		logToast.show();
	}
	// Implementation detail: bridge the VideoRenderer.Callbacks interface to
	// the
	// VideoStreamsView implementation.
	private class VideoCallbacks implements VideoRenderer.Callbacks {
		private final VideoStreamsView view;
		private final VideoStreamsView.Endpoint stream;

		public VideoCallbacks(VideoStreamsView view,
				VideoStreamsView.Endpoint stream) {
			this.view = view;
			this.stream = stream;
		}

		@Override
		public void setSize(final int width, final int height) {
			view.queueEvent(new Runnable() {
				public void run() {
					view.setSize(stream, width, height);
				}
			});
		}

		@Override
		public void renderFrame(I420Frame frame) {
			view.queueFrame(stream, frame);
		}
	}
	// Implementation detail: handler for receiving GAE messages and dispatching
		// them appropriately.
//		private class GAEHandler implements GAEChannelClient.MessageHandler {
//			@JavascriptInterface
//			public void onOpen() {
//				
//				/*if (!appRtcClient.isInitiator()) {
//					return;
//				}*/
//				logAndToast("Creating offer...");
//				pc.createOffer(sdpObserver, sdpMediaConstraints);
//			}
//
//			@JavascriptInterface
//			public void onMessage(String data) {
//				try {
//					JSONObject json = new JSONObject(data);
//					String type = (String) json.get("type");
//					if (type.equals("candidate")) {
//						IceCandidate candidate = new IceCandidate(
//								(String) json.get("id"), json.getInt("label"),
//								(String) json.get("candidate"));
//						if (queuedRemoteCandidates != null) {
//							queuedRemoteCandidates.add(candidate);
//						} else {
//							pc.addIceCandidate(candidate);
//						}
//					} else if (type.equals("answer") || type.equals("offer")) {
//						SessionDescription sdp = new SessionDescription(
//								SessionDescription.Type.fromCanonicalForm(type),
//								(String) json.get("sdp"));
//						pc.setRemoteDescription(sdpObserver, sdp);
//						//pc.addStream(lMS, new MediaConstraints());
//					} else if (type.equals("bye")) {
//						logAndToast("Remote end hung up; dropping PeerConnection");
//						disconnectAndExit();
//					} else {
//						throw new RuntimeException("Unexpected message: " + data);
//					}
//				} catch (JSONException e) {
//					throw new RuntimeException(e);
//				}
//			}
//
//			@JavascriptInterface
//			public void onClose() {
//				disconnectAndExit();
//			}
//
//			@JavascriptInterface
//			public void onError(int code, String description) {
//				disconnectAndExit();
//			}
//		}
		// Disconnect from remote resources, dispose of local resources, and exit.
		private void disconnectAndExit() {
			synchronized (quit[0]) {
				if (quit[0]) {
					return;
				}
				quit[0] = true;
				if (pc != null) {
					pc.dispose();
					pc = null;
				}
				Log.e(TAG,"DAMN!!!");
//				if (appRtcClient != null) {
//					appRtcClient.sendMessage("{\"type\": \"bye\"}");
//					appRtcClient.disconnect();
//					appRtcClient = null;
//				}
				finish();
			}
		}
		// Cycle through likely device names for the camera and return the first
		// capturer that works, or crash if none do.
		private VideoCapturer getVideoCapturer() {
			String[] cameraFacing = { "front", "back" };
			int[] cameraIndex = { 0, 1 };
			int[] cameraOrientation = { 0, 90, 180, 270 };
			for (String facing : cameraFacing) {
				for (int index : cameraIndex) {
					for (int orientation : cameraOrientation) {
						String name = "Camera " + index + ", Facing " + facing
								+ ", Orientation " + orientation;
						VideoCapturer capturer = VideoCapturer.create(name);
						if (capturer != null) {
							logAndToast("Using camera: " + name);
							return capturer;
						}
					}
				}
			}
			throw new RuntimeException("Failed to open capturer");
		}
		@Override
		public void onPause() {
			super.onPause();
			//vsv.onPause();
			// TODO(fischman): IWBN to support pause/resume, but the WebRTC codebase
			// isn't ready for that yet; e.g.
			// https://code.google.com/p/webrtc/issues/detail?id=1407
			// Instead, simply exit instead of pausing (the alternative leads to
			// system-borking with wedged cameras; e.g. b/8224551)
			//disconnectAndExit();
		}

		@Override
		public void onResume() {
			// The onResume() is a lie! See TODO(fischman) in onPause() above.
			super.onResume();
			vsv.onResume();
		}
		@Override
		public void onDestroy() {
			super.onDestroy();
		}
		
		// Send |json| to the underlying AppEngine Channel.
		private void sendMessage(JSONObject json) {
			Log.e(TAG,"769 : sendMessage "+json);
			socket.send(json);
			//socket.emit("message",json.toString());
			
			//appRtcClient.sendMessage(json.toString());
		}
}
