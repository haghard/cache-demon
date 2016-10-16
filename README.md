cache-demon 
===================

### How to build ###

`agent/assembly`
`demon/assembly`

### Run with an agent ###

`java -Xmx1500m -XX:MaxMetaspaceSize=1024m -XX:+UseParallelGC -javaagent:agent/target/scala-2.11/agent.jar -jar demon/target/scala-2.11/demon.jar`


### Using ParallelGC and GC logging ### 

`java -Xmx1500m -XX:MaxMetaspaceSize=1024m -XX:+UseParallelGC -Xloggc:gc.txt -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -javaagent:agent/target/scala-2.11/agent.jar -jar demon/target/scala-2.11/demon.jar`

### Using ConcMarkSweep and GC logging ###
 
`java -Xmx1500m -XX:MaxMetaspaceSize=1024m -XX:+UseConcMarkSweepGC -Xloggc:gc.txt -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -javaagent:agent/target/scala-2.11/agent.jar -jar demon/target/scala-2.11/demon.jar`

### Using G1 and GC logging ###

`java -Xmx1500m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=80 -Xloggc:gc.txt -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -javaagent:agent/target/scala-2.11/agent.jar -jar demon/target/scala-2.11/demon.jar`

  -XX:MaxGCPauseMillis=200 - Sets a target for the maximum GC pause time. This is a soft goal, and the JVM will make its best effort to achieve it. Therefore, the pause time goal will sometimes not be met. The default value is 200 milliseconds.

  -XX:InitiatingHeapOccupancyPercent=45 - Percentage of the (entire) heap occupancy to start a concurrent GC cycle. It is used by G1 to trigger a concurrent GC cycle based on the occupancy of the entire heap, not just one of the generations. A value of 0 denotes 'do constant GC cycles'. The default value is 45 (i.e., 45% full or occupied).


## How to query ###

### With Httpie

`http GET localhost:9000/cache/{id}`

`http GET localhost:9000/index/{id}`

`curl --no-buffer localhost:9000/metrics/jvm`


One thing to notice being that the index starts with 0. If the cache size is 999, the index can be queried in 0-998


