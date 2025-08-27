This document describe the continuous profiling feature.

# Illustration
The below picture illustrates the use of continuous profiling.
![preview](./preview.png)

# Steps to use
1. Select a application to profile
2. Click the 'Start' button to start the profiling
   > By default, the profiling duration is 180 seconds, and the interval to collect profiling data is 3 seconds.
   > 
   > You can change these settings by clicking the button next to 'Start' button.
3. Once the profiling is started, the Web UI will continuously receive data from target application and shows CPU/Memory usage as well as stack traces in flamegraph. 

# Section description

Once the profiling is started, the Web UI will show the following sections:
- System Properties
- CPU Load
- Heap Memory(if any)
- Flamegraph

The description of each section is as follows:

| No. | Section           | Description                                                                                                                                                                                                                                                                                                                              |
|-----|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | System Properties | Shows the Java properties of the target application instance.                                                                                                                                                                                                                                                                            |
| 2   | CPU Load          | Shows the CPU usage of the target application instance, including 3 different levels CPU load:</br> User - The CPU load your application consumes.<br/> System - The CPU load spent in the kernel space(e.g., I/O, syscalls, context switches).<br/> Machine - Overall CPU usage across the entire system (all processes + kernel).<br/> |
| 3   | Heap Memory       | Show the Java Heap memory during the profiling. This section will only be shown if there's GC(whether it's young GC or full GC happens).                                                                                                                                                                                                 |
| 4   | Flamegraph        | Show the stack traces of the target application instance in flame graph.                                                                                                                                                                                                                                                                                  |

