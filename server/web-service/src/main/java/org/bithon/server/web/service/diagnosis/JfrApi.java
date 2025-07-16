/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.web.service.diagnosis;


import lombok.extern.slf4j.Slf4j;
import one.jfr.JfrReader;
import one.jfr.StackTrace;
import one.jfr.event.Event;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.server.web.service.diagnosis.event.CallStackSample;
import org.bithon.server.web.service.diagnosis.event.IEvent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 21/5/25 6:45 pm
 */
@Slf4j
@CrossOrigin
@RestController
public class JfrApi {

    @GetMapping("/api/diagnosis/profiling")
    public SseEmitter profiling() {
        SseEmitter emitter = new SseEmitter((30 + 10) * 1000L); // 30 seconds timeout

        Profiler.INSTANCE.start(3,
                                30,
                                new StreamResponse<>() {
                                    @Override
                                    public void onNext(IEvent event) {
                                        try {
                                            emitter.send(SseEmitter.event()
                                                                   .name(event.getClass().getSimpleName())
                                                                   .data(event, MediaType.APPLICATION_JSON)
                                                                   .build());
                                        } catch (IOException | IllegalStateException ignored) {
                                        }
                                    }

                                    @Override
                                    public void onException(Throwable throwable) {
                                        emitter.completeWithError(throwable);
                                    }

                                    @Override
                                    public void onComplete() {
                                        emitter.complete();
                                    }
                                });

        return emitter;
    }

    private static void dump(File f) {
        try (JfrReader jfrReader = Profiler.createJfrReader(f.getAbsolutePath())) {
            {
                Event event = jfrReader.readEvent();
                while (event != null) {
                    long time = jfrReader.startNanos + ((event.time - jfrReader.startTicks) / jfrReader.ticksPerSec);
                    System.out.printf("%d, %d, %d %s\n", time, event.samples(), event.value(), event);
                    StackTrace stackTrace = jfrReader.stackTraces.get(event.stackTraceId);
                    if (stackTrace != null) {
                        List<StackFrame> frames = CallStackSample.toStackTrace(jfrReader, stackTrace);
                        System.out.println(frames);
                    }
                    event = jfrReader.readEvent();
                }
            }
        } catch (IOException ignored) {
        }
    }
}
