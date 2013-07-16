package com.dreamlink.communication.lib;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.dreamlink.aidl.Communication;
import com.dreamlink.aidl.OnCommunicationListenerExternal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ManagerConnect {

	private Communication mCommunication;
	private static ManagerConnect p;
	private final String INTENT_STRING = "com.dreamlink.communication.ComService";
	private HashMap<NotifyInterface, OnCommunicationListenerExternal.Stub> hashMap;
	private Context mContext;
	private Vector<Connected> vector;

	/** the call back interface ,must be implement */
	public interface NotifyInterface {
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
	 * {@link ManagerConnect},and when you should not
	 * */
	public interface Connected {

		/**
		 * notify disconnect,please don't use the instance of
		 * {@link ManagerConnect} anymore
		 */
		public void onDisconnected();

		/**
		 * notify connect can use
		 * 
		 * @param managerConnect
		 *            {@link ManagerConnect} instance,you can use
		 * */
		public void onConnected(ManagerConnect managerConnect);
	}

	private ManagerConnect() {
		vector = new Vector<ManagerConnect.Connected>();
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (vector != null && vector.size() != 0) {
				for (Connected connected : vector) {
					connected.onDisconnected();
				}
			}
			mCommunication = null;
			vector.clear();
			vector = null;
			p = null;

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mCommunication = Communication.Stub.asInterface(service);
			if (vector != null && vector.size() != 0) {
				for (Connected connected : vector) {
					connected.onConnected(p);
				}
			}
		}
	};

	/**
	 * get the instance of the ManagerConnect
	 * 
	 * @param context
	 *            like {@link getApplicationContext()}
	 * @return maybe null ,please check it
	 */
	public static ManagerConnect getInstance(final Context context,
			Connected connected) {
		if (p == null) {
			p = new ManagerConnect();
			p.mContext = context;
			if (!context.getApplicationContext().bindService(
					new Intent(p.INTENT_STRING), p.mServiceConnection,
					Context.BIND_AUTO_CREATE)) {
				p = null;
				return null;
			}
		}
		p.rigisterConnected(connected);
		return null;
	}

	/**
	 * register call back listener with your application id. when you use IPC
	 * ,please register call back listener first
	 * 
	 * @param i
	 *            {@link NotifyInterface},must implements
	 * @param appid
	 *            your application id
	 */
	public void register(final NotifyInterface i, final int appid) {
		if (hashMap == null) {
			hashMap = new HashMap<ManagerConnect.NotifyInterface, OnCommunicationListenerExternal.Stub>();
		}
		final OnCommunicationListenerExternal.Stub s = new OnCommunicationListenerExternal.Stub() {

			@Override
			public void onUserDisconnected(User user) throws RemoteException {
				i.onUserDisconnected(user);
			}

			@Override
			public void onUserConnected(User user) throws RemoteException {
				i.onUserConnected(user);
			}

			@Override
			public void onReceiveMessage(byte[] msg, User sendUser)
					throws RemoteException {
				i.onReceiveMessage(msg, sendUser);
			}
		};
		hashMap.put(i, s);
		try {
			mCommunication.registListenr(s, appid);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	/**
	 * unregister call back listener. when you don't use the IPC ,please
	 * unregister call back
	 * 
	 * @param i
	 *            {@link NotifyInterface}
	 */
	public void unregister(NotifyInterface i) {
		if (!hashMap.containsKey(i)) {
			Log.e("ArbiterLiu", " This ITest interface is not  registered ");
			return;
		}
		try {
			mCommunication.unRegistListenr(hashMap.get(i));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		hashMap.remove(i);
		if (hashMap.size() == 0) {
			mContext.unbindService(mServiceConnection);
		}
	}

	/**
	 * send message to the user
	 * 
	 * @param msg
	 *            the data will be send
	 * @param appID
	 *            your application id ,define in manifest meta-data
	 * @param user
	 *            the mesaage will send to
	 * */
	public void sendMessage(byte[] msg, int appID, User user) {
		try {
			mCommunication.sendMessage(msg, appID, user);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};

	/** get all of users in connect */
	public List<User> getAllUser() {
		try {
			return mCommunication.getAllUser();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	};

	/**
	 * get local user info
	 * 
	 * @return {@link User} the local user info, maybe null
	 * */
	public User getLocalUser() {
		try {
			return mCommunication.getLocalUser();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	};

	/**
	 * send message to all of users
	 * 
	 * @param msg
	 *            the data will be send
	 * @param appID
	 *            your application id ,define in manifest meta-data
	 * */
	public void sendMessageToAll(byte[] msg, int appID) {
		try {
			mCommunication.sendMessageToAll(msg, appID);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};

	private void rigisterConnected(Connected connected) {
		vector.add(connected);
		if (mCommunication != null) {
			connected.onConnected(p);
		}
	}
}
