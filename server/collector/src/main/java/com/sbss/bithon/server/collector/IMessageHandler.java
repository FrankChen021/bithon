package com.sbss.bithon.server.collector;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:40 下午
 */
public interface IMessageHandler<MSG_HEADER, MSG_BODY> {

    void submit(MSG_HEADER header, MSG_BODY body);

    void submit(MSG_HEADER header, List<MSG_BODY> body);
}
