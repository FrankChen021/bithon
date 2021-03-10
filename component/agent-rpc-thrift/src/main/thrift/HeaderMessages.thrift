namespace java com.sbss.bithon.agent.rpc.thrift.service

/***************************************************************************/
/*************************** MessageHeader       ***************************/
/***************************************************************************/
struct MessageHeader {
    1:string appName;
    2:string env;
    3:string hostName;
    4:i32 port;
}