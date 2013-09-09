package com.dreamlink.communication.aidl;

import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.User;
/**@hide*/
interface Communication{
	/**register call back listener with your application id.
	*when you use IPC ,please register call back listener first
	*
	*@param lis {@link OnCommunicationListenerExternal}
	*/
	void registListenr(OnCommunicationListenerExternal lis,int appid);
	/**
	 * send message to user
	 * @param msg
	 *            the data will be send
	 * @param appID
	 *            your application id ,define in manifest meta-data
	 * @param user
	 *            which user will be send to 
	 * */
	void sendMessage(in byte[] msg,int appID,in User user);
	/**get all of users in connect*/
	List<User> getAllUser();
	/**unregister call back listener.
	* when you don't use the IPC ,please unregister call back
	*
	*@param lis {@link OnCommunicationListenerExternal}
	*/
	void unRegistListenr(OnCommunicationListenerExternal lis);
	User getLocalUser();
	/**
	 * send message to all of users
	 * @param msg
	 *            the data will be send
	 * @param appID
	 *            your application id ,define in manifest meta-data
	 * */
	void sendMessageToAll(in byte[] msg,int appID);
}