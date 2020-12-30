namespace java com.sbss.bithon.agent.rpc.thrift.service.setting

struct FetchResponse {
 // 响应状态码
 1:i32 statusCode = 200;
 // 响应结果
 2:string message = "SUCCESS";

 3:map<string, string> settings;
}

struct FetchRequest {
    1:string appName;
    2:string envName;
    3:i64 since;
}

service SettingService {

    FetchResponse fetch(1:required FetchRequest request);

}