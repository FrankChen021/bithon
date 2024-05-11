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

package org.bithon.agent.observability.jvm;

import java.io.InputStream;

import com.sun.tools.attach.AttachOperationFailedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachNotSupportedException;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 * To make it work, tools.jar with different releases of jdk needs to be shipped
 *
 * @author frank.chen021@outlook.com
 * @date 11/5/24 9:33 pm
 */
public class NativeMemoryTrackingService {

    public void dump() {
        VirtualMachine vm = VirtualMachine.attach(pid);

        // Cast to HotSpotVirtualMachine as this is an
        // implementation specific method.
        HotSpotVirtualMachine hvm = (HotSpotVirtualMachine) vm;
        String lines[] = command.split("\\n");
        for (String line : lines) {
            if (line.trim().equals("stop")) {
                break;
            }

            InputStream is = hvm.executeJCmd(line);

            if (PrintStreamPrinter.drainUTF8(is, System.out) == 0) {
                System.out.println("Command executed successfully");
            }
        }
        vm.detach();
    }
}
