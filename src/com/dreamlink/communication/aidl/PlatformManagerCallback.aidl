package com.dreamlink.communication.aidl;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.aidl.HostInfo;

 interface PlatformManagerCallback {
		 void hostHasCreated(in HostInfo hostInfo);

		 void joinGroupResult(in HostInfo hostInfo, boolean flag);

		 void groupMemberUpdate(int hostId, in byte[] data);//ArrayList<User> userList

		 void hostInfoChange(in byte[] data);//ArrayList<HostInfo> hostList

		 void hasExitGroup(int hostId);

		 void receiverMessage(in byte[] data, in User sendUser,
				boolean allFlag, in HostInfo info);

		 void startGroupBusiness(in HostInfo hostInfo);
	}