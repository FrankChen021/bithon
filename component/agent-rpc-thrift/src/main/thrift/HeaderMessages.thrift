namespace java com.sbss.bithon.agent.rpc.thrift.service

enum ApplicationType {
    JAVA
}

/***************************************************************************/
/*************************** MessageHeader       ***************************/
/***************************************************************************/
struct MessageHeader {
    1:string appName;
    2:string env;
    3:string instanceName;
    4:string hostIp;
    5:i32 port;
    6:ApplicationType appType;
}

