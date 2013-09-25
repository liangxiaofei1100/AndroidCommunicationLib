package com.dreamlink.communication.aidl;

import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.aidl.HostInfo;
import com.dreamlink.communication.aidl.PlatformManagerCallback;
/**@hide*/
interface Communication{
	/**register call back listener with your application id.
	*when you use IPC ,please register call back listener first
	*
	*@param lis {@link OnCommunicationListenerExternal}
	*/
	void registListener(OnCommunicationListenerExternal lis,int appid);
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
	void unRegistListener(int appId);
	User getLocalUser();
	/**
	 * send message to all of users
	 * @param msg
	 *            the data will be send
	 * @param appID
	 *            your application id ,define in manifest meta-data
	 * */
	void sendMessageToAll(in byte[] msg,int appID);
	
	/**platform aidl interface */
	
	void regitserPlatformCallback(PlatformManagerCallback callback,int apId);
	void unregitserPlatformCallback(int apId);
	 void createHost(String appName, String pakcageName, int numberLimit,
			int app_id);


	 void getAllHost(int appID);
 
	 void joinGroup(in HostInfo hostInfo);
 
 	 void exitGroup(in HostInfo hostInfo);
 
 
	 void removeGroupMember(int hostId, int userId);
 
 	
	 void getGroupUser(in HostInfo hostInfo);
  
 
	 void startGroupBusiness(int hostId);
 
 
 	void sendDataAll(in byte[] data,in HostInfo info);
 
 
 	void sendDataSingle(in byte[] data,in HostInfo info, in User targetUser);
 	List<HostInfo> getJoinedHostInfo(int appId);
 /***/
	
}