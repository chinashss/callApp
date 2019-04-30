package com.hv.calllib.peermgr;


/**
 * Created by liuhongyu on 2016/12/12.
 */

import com.hv.calllib.live.VideoCallLive;
import com.hv.calllib.peermgr.group.VideoCallGroup;

/**
 * 注意peerfactory管理所有peer类的创建销毁和分配
 * 如果将来创建会议或者群组模式，p2p跟他们也许是互斥的，只能同时创建一种模式的peer
 */
public class PeerFactory {
    private final boolean canCreatePeer = true;

    private static PeerFactory ourInstance = new PeerFactory();
    private VideoCallGroup groupVideoCallClient = null;
    private VideoCallLive liveVideoCallClient  = null;

    public static PeerFactory getInstance() {

        return ourInstance;
    }

    private PeerFactory() {
    }

    public boolean CanCreatePeer() {
        return canCreatePeer;
    }

    public VideoCallGroup CreateGroupCallPeer(  ) {
        if( groupVideoCallClient == null ) {
            groupVideoCallClient = new VideoCallGroup(  );
        }

        return groupVideoCallClient;
    }

    public VideoCallLive CreateLivevideoPeer(  ) {
        if( liveVideoCallClient == null ) {
            liveVideoCallClient = new VideoCallLive(  );
        }

        return liveVideoCallClient;
    }

}
