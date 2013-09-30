package com.dreamlink.communication.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.dreamlink.communication.aidl.Communication;
import com.dreamlink.communication.aidl.HostInfo;
import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.PlatformManagerCallback;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.lib.util.ArrayUtil;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class is used for bind communication service and communicate with it by
 * android service.
 * 
 */
public class CommunicationManager {
	private static final String TAG = "CommunicationManager";
	private Communication mCommunication;
	private PlatformCallback mPlatformCallback;
	/** Intent to start communication service */
	private final String ACTION_COMMUNICATION_SERVICE = "com.dreamlink.communication.ComService";
	private Context mContext;
	private OnConnectionChangeListener mOnConnectionChangeListener;
	private OnCommunicationListener mOnCommunicationListener;
	private int mAppID = -1;
	private boolean platformRegisted = false;

	private OnCommunicationListenerExternal.Stub mStub = new OnCommunicationListenerExternal.Stub() {
		@Override
		public void onUserDisconnected(User user) throws RemoteException {
			Log.d(TAG, "onUserDisconnected " + user);
			if (mOnCommunicationListener != null) {
				mOnCommunicationListener.onUserDisconnected(user);
			}
		}

		@Override
		public void onUserConnected(User user) throws RemoteException {
			Log.d(TAG, "onUserConnected " + user);
			if (mOnCommunicationListener != null) {
				mOnCommunicationListener.onUserConnected(user);
			}
		}

		@Override
		public void onReceiveMessage(byte[] msg, User sendUser)
				throws RemoteException {
			Log.d(TAG, "onReceiveMessage sendUser = " + sendUser);
			if (mOnCommunicationListener != null) {
				mOnCommunicationListener.onReceiveMessage(msg, sendUser);
			}
		}
	};

	private PlatformManagerCallback.Stub platStub = new PlatformManagerCallback.Stub() {

		@Override
		public void startGroupBusiness(HostInfo hostInfo)
				throws RemoteException {
			if (mPlatformCallback != null)
				mPlatformCallback.startGroupBusiness(hostInfo);
		}

		@Override
		public void receiverMessage(byte[] data, User sendUser,
				boolean allFlag, HostInfo info) throws RemoteException {
			if (mPlatformCallback != null)
				mPlatformCallback
						.receiverMessage(data, sendUser, allFlag, info);
		}

		@Override
		public void joinGroupResult(HostInfo hostInfo, boolean flag)
				throws RemoteException {
			if (mPlatformCallback != null)
				mPlatformCallback.joinGroupResult(hostInfo, flag);
		}

		@Override
		public void hostInfoChange(byte[] data) throws RemoteException {
			@SuppressWarnings("unchecked")
			ConcurrentHashMap<Integer, HostInfo> hostList = (ConcurrentHashMap<Integer, HostInfo>) ArrayUtil
					.byteArrayToObject(data);
			List<HostInfo> tem = new ArrayList<HostInfo>();
			for (java.util.Map.Entry<Integer, HostInfo> entry : hostList
					.entrySet()) {
				HostInfo hostInfo = entry.getValue();
				if (hostInfo.app_id == mAppID) {
					tem.add(hostInfo);
				}
			}
			if (hostList != null && mPlatformCallback != null)
				mPlatformCallback.hostInfoChange(tem);
		}

		@Override
		public void hostHasCreated(HostInfo hostInfo) throws RemoteException {
			if (mPlatformCallback != null)
				mPlatformCallback.hostHasCreated(hostInfo);
		}

		@Override
		public void hasExitGroup(int hostId) throws RemoteException {
			if (mPlatformCallback != null)
				mPlatformCallback.hasExitGroup(hostId);
		}

		@Override
		public void groupMemberUpdate(int hostId, byte[] data)
				throws RemoteException {
			@SuppressWarnings("unchecked")
			ArrayList<User> userIdList = (ArrayList<User>) ArrayUtil
					.byteArrayToObject(data);
			if (userIdList != null && mPlatformCallback != null)
				mPlatformCallback.groupMemberUpdate(hostId, userIdList);
		}

	};

	/** the call back interface ,must be implement */
	public interface OnCommunicationListener {
		/**
		 * Received a message from user.</br>
		 * 
		 * Be careful, this method is not run in UI thread. If do UI operation,
		 * we can use {@link android.os.Handler} to do UI operation.</br>
		 * 
		 * @param msg
		 *            the message.
		 * @param sendUser
		 *            the message from.
		 */
		void onReceiveMessage(byte[] msg, User sendUser);

		/**
		 * There is new user connected.
		 * 
		 * @param user
		 *            the connected user
		 */
		void onUserConnected(User user);

		/**
		 * There is a user disconnected.
		 * 
		 * @param user
		 *            the disconnected user
		 */
		void onUserDisconnected(User user);
	}

	public interface PlatformCallback {

		void hostHasCreated(HostInfo hostInfo);

		void joinGroupResult(HostInfo hostInfo, boolean flag);

		void groupMemberUpdate(int hostId, ArrayList<User> userList);

		void hostInfoChange(List<HostInfo> hostList);

		void hasExitGroup(int hostId);

		void receiverMessage(byte[] data, User sendUser, boolean allFlag,
				HostInfo info);

		void startGroupBusiness(HostInfo hostInfo);
	}

	/**
	 * notify the connect can use,and it disconnect.</br> you must implements
	 * this interface ,then you will know when you can use the instance of
	 * {@link CommunicationManager},and when you should not
	 * */
	public interface OnConnectionChangeListener {

		/**
		 * notify disconnect,please don't use the instance of
		 * {@link CommunicationManager} anymore
		 */
		public void onCommunicationDisconnected();

		/**
		 * notify connect can use
		 * */
		public void onCommunicationConnected();
	}

	/** register callback method,please invoke this method only */
	public void registerPlatformCallback(PlatformCallback platformCallback) {
		this.mPlatformCallback = platformCallback;
	}
	/** register callback method,please invoke this method only */
	public void registerOnCommunicationListener(
			OnCommunicationListener onCommunicationListener) {
		this.mOnCommunicationListener = onCommunicationListener;
	}

	/**
	 * Service connection with Communication service.
	 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (mOnConnectionChangeListener != null) {
				mOnConnectionChangeListener.onCommunicationDisconnected();
			}
			if (mAppID != -1) {
				try {
					mCommunication.unRegistListener(mAppID);
				} catch (RemoteException e) {
					Log.e(TAG,
							"onServiceDisconnected() unRegistListener error "
									+ e);
				} catch (NullPointerException e) {
				}
			}

			mCommunication = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mCommunication = Communication.Stub.asInterface(service);
			if (mOnConnectionChangeListener != null) {
				mOnConnectionChangeListener.onCommunicationConnected();
			}
			if (mAppID != -1) {
				try {
					mCommunication.registListener(mStub, mAppID);
					mCommunication.regitserPlatformCallback(platStub, mAppID);
				} catch (RemoteException e) {
					Log.e(TAG, "onServiceConnected() registListener error " + e);
				} catch (NullPointerException e) {
				}
			}
		}
	};

	public CommunicationManager(Context context) {
		mContext = context;
	}

	/**
	 * Connect to communication service.register call back listener with your
	 * application id.
	 * 
	 * @param listener
	 * @return success ? true : false.
	 * @deprecated
	 */
	public boolean connectCommunicatonService(
			OnConnectionChangeListener connectionChangeListener,
			OnCommunicationListener communicationListener, int appID) {
		Log.d(TAG, "connectCommunicatonService appID銆� " + appID);
		mOnConnectionChangeListener = connectionChangeListener;
		mOnCommunicationListener = communicationListener;
		mAppID = appID;

		Intent intent = new Intent();
		intent.setAction(ACTION_COMMUNICATION_SERVICE);
		return mContext.bindService(intent, mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	public boolean connectCommunicatonService(
			OnConnectionChangeListener connectionChangeListener, int appID) {
		Log.d(TAG, "connectCommunicatonService appID銆� " + appID);
		mOnConnectionChangeListener = connectionChangeListener;
		mAppID = appID;
		Intent intent = new Intent();
		intent.setAction(ACTION_COMMUNICATION_SERVICE);
		return mContext.bindService(intent, mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	/**
	 * unregister call back listener. when you don't use the IPC ,please
	 * unregister call back
	 */
	public void disconnectCommunicationService() {
		Log.d(TAG, "disconnectCommunicationService");
		try {
			mCommunication.unRegistListener(mAppID);
			mCommunication.unregitserPlatformCallback(mAppID);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
		mContext.unbindService(mServiceConnection);
	}

	/**
	 * Send message to the user
	 * 
	 * @param msg
	 * @param user
	 * @return
	 */
	public boolean sendMessage(byte[] msg, User user) {
		return sendMessage(msg, mAppID, user);
	}

	/**
	 * Send message to the user
	 * 
	 * @param msg
	 *            the data will be send
	 * @param appID
	 *            your application id ,define in manifest meta-data
	 * @param user
	 *            the message will send to
	 * */
	public boolean sendMessage(byte[] msg, int appID, User user) {
		Log.d(TAG, "sendMessage: appid = " + appID + ", user = " + user);
		if (mCommunication == null) {
			Log.e(TAG, "Service is not connected");
			return false;
		}
		try {
			mCommunication.sendMessage(msg, appID, user);
		} catch (RemoteException e) {
			Log.e(TAG, "sendMessage error " + e);
			return false;
		} catch (NullPointerException e) {
		}
		return true;
	}

	/** get all of users in connect */
	public List<User> getAllUser() {
		if (mCommunication == null) {
			Log.e(TAG, "Service is not connected");
			return null;
		}
		try {
			return mCommunication.getAllUser();
		} catch (RemoteException e) {
			Log.e(TAG, "getAllUser error " + e);
		} catch (NullPointerException e) {
		}
		return null;
	};

	/**
	 * get local user info
	 * 
	 * @return {@link User} the local user info, maybe null
	 * */
	public User getLocalUser() {
		if (mCommunication == null) {
			Log.e(TAG, "Service is not connected");
			return null;
		}
		try {
			return mCommunication.getLocalUser();
		} catch (RemoteException e) {
			Log.e(TAG, "getLocalUser error " + e);
		} catch (NullPointerException e) {
		}
		return null;
	};

	/**
	 * Send message to all of users
	 * 
	 * @param msg
	 * @return
	 */
	public boolean sendMessageToAll(byte[] msg) {
		return sendMessageToAll(msg, mAppID);
	}

	/**
	 * Send message to all of users
	 * 
	 * @param msg
	 *            the data will be send
	 * @param appID
	 *            your application id ,define in manifest meta-data
	 * */
	public boolean sendMessageToAll(byte[] msg, int appID) {
		Log.d(TAG, "sendMessageToAll: appid = " + appID);
		if (mCommunication == null) {
			Log.e(TAG, "Service is not connected");
			return false;
		}
		try {
			mCommunication.sendMessageToAll(msg, appID);
		} catch (RemoteException e) {
			Log.e(TAG, "sendMessageToAll error " + e);
			return false;
		} catch (NullPointerException e) {
			Log.e(TAG, "sendMessageToAll error " + e);
			return false;
		}
		return true;
	}

	/* for platform manager- start */
	/**
	 * if want use platform ,please invoke this method first
	 * 
	 * @deprecated
	 * */
	public void registerPlatformCallback(PlatformCallback platformCallback,
			int appId) {
		if (mCommunication != null && platformCallback != null) {
			this.mPlatformCallback = platformCallback;
			platformRegisted = true;
			try {
				mCommunication.regitserPlatformCallback(platStub, appId);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
			}
		}
	}

	/** @deprecated */
	public void unregitserPlatformCallback(int appId) {
		Log.e("ArbiterLiu", "unregitserPlatformCallback      " + appId);
		if (platformRegisted && mCommunication != null) {
			try {
				mCommunication.unregitserPlatformCallback(appId);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
			}
		}
	}

	/**
	 * @param pakcageName
	 *            your package name
	 * @param appName
	 *            if your application just create one kind of host,please keep
	 *            this value only,else use this to differentiate host
	 * @param numberLimit
	 *            your host allow how many person to join in.if 0 mean no
	 *            limited;1 mean not allow person join;else allow numberLimit-1
	 *            join
	 * @param app_id
	 *            your app_id
	 * */
	public void createHost(String appName, String pakcageName, int numberLimit,
			int app_id) {
		try {
			mCommunication
					.createHost(appName, pakcageName, numberLimit, app_id);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void getAllHost(int appID) {
		try {
			mCommunication.getAllHost(appID);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void joinGroup(HostInfo hostInfo) {
		try {
			mCommunication.joinGroup(hostInfo);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void exitGroup(HostInfo hostInfo) {
		try {
			mCommunication.exitGroup(hostInfo);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void removeGroupMember(int hostId, int userId) {
		try {
			mCommunication.removeGroupMember(hostId, userId);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void getGroupUser(HostInfo hostInfo) {
		try {
			mCommunication.getGroupUser(hostInfo);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void startGroupBusiness(int hostId) {
		try {
			mCommunication.startGroupBusiness(hostId);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void sendDataGroupAll(byte[] data, HostInfo info) {
		try {
			mCommunication.sendDataAll(data, info);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public void sendDataGroupSingle(byte[] data, HostInfo info, User targetUser) {
		try {
			mCommunication.sendDataSingle(data, info, targetUser);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
		}
	};

	public List<HostInfo> getJoinedHostInfo() {
		try {
			return mCommunication.getJoinedHostInfo(mAppID);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		} catch (NullPointerException e) {
			return null;
		}
	}

}
