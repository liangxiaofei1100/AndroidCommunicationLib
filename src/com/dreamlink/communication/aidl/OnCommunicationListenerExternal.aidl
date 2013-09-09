package com.dreamlink.communication.aidl;
import com.dreamlink.communication.aidl.User;
/**@hide*/
interface OnCommunicationListenerExternal{
 	/**
	 * Received a message from user.</br>
	 *
	 * Be careful, this method is not run in UI thread. If do UI operation,
	 * we can use {@link android.os.Handler} to do UI operation.</br>
	 *
	 * @param msg
	 * the message.
	 * @param sendUser
	 * the message from.
	 */
		void onReceiveMessage(in byte[] msg,in User sendUser);
	/**
	 * There is new user connected.
	 *
	 * @param user the connected user
	 */
		void onUserConnected(in User user);
	/**
	 * There is a user disconnected.
	 *
	 * @param user the disconnected user
	 */
		void onUserDisconnected(in User user);
}