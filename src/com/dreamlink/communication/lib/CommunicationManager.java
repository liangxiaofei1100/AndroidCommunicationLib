package com.dreamlink.communication.lib;

import java.util.List;

import com.dreamlink.communication.aidl.Communication;
import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.User;

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
	/** Intent to start communication service */
	private final String ACTION_COMMUNICATION_SERVICE = "com.dreamlink.communication.ComService";
	private Context mContext;
	private OnConnectionChangeListener mOnConnectionChangeListener;
	private OnCommunicationListener mOnCommunicationListener;
	private int mAppID = -1;
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

	/**
	 * Service connection with Communication service.
	 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (mOnConnectionChangeListener != null) {
				mOnConnectionChangeListener.onCommunicationDisconnected();
			}
			if (mOnCommunicationListener != null && mAppID != -1) {

			}

			mCommunication = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mCommunication = Communication.Stub.asInterface(service);
			if (mOnConnectionChangeListener != null) {
				mOnConnectionChangeListener.onCommunicationConnected();
			}

			if (mOnCommunicationListener != null && mAppID != -1) {
				try {
					mCommunication.registListener(mStub, mAppID);
				} catch (RemoteException e) {
					Log.e(TAG, "onServiceConnected() registListener error " + e);
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
	 */
	public boolean connectCommunicatonService(
			OnConnectionChangeListener connectionChangeListener,
			OnCommunicationListener communicationListener, int appID) {
		Log.d(TAG, "connectCommunicatonService appID　= " + appID);
		mOnConnectionChangeListener = connectionChangeListener;
		mOnCommunicationListener = communicationListener;
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
			mCommunication.unRegistListener(mStub);
		} catch (RemoteException e) {
			e.printStackTrace();
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
	 *            the mesaage will send to
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
		}
		return true;
	}
}
