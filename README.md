cache-demon 
===================

### How to build ###

`sbt stage`

### How to run ###

`./target/universal/stage/bin/cache-demon`

### Using ParallelGC and GC logging ### 

`./target/universal/stage/bin/cache-demon -J-Xmx1500m -J-XX:MaxMetaspaceSize=1024m -J-XX:+UseParallelGC -J-Xloggc:gc.txt -J-XX:+PrintGCDetails -J-XX:+PrintGCDateStamps -J-XX:+PrintGCTimeStamps`

### Using ConcMarkSweep and GC logging ###
 
`./target/universal/stage/bin/cache-demon -J-Xmx1500m -J-XX:MaxMetaspaceSize=1024m -J-XX:+UseConcMarkSweepGC -J-Xloggc:gc.txt -J-XX:+PrintGCDetails -J-XX:+PrintGCDateStamps -J-XX:+PrintGCTimeStamps`

### Using G1 and GC logging ###

`./target/universal/stage/bin/cache-demon -J-Xmx1500m -J-XX:MaxMetaspaceSize=1024m -J-XX:+UseG1GC -XX:MaxGCPauseMillis=80 -J-Xloggc:gc.txt -J-XX:+PrintGCDetails -J-XX:+PrintGCDateStamps -J-XX:+PrintGCTimeStamps`

  -XX:MaxGCPauseMillis=200 - Sets a target for the maximum GC pause time. This is a soft goal, and the JVM will make its best effort to achieve it. Therefore, the pause time goal will sometimes not be met. The default value is 200 milliseconds.

  -XX:InitiatingHeapOccupancyPercent=45 - Percentage of the (entire) heap occupancy to start a concurrent GC cycle. It is used by G1 to trigger a concurrent GC cycle based on the occupancy of the entire heap, not just one of the generations. A value of 0 denotes 'do constant GC cycles'. The default value is 45 (i.e., 45% full or occupied).


## How to query ###

### With Httpie

`http GET localhost:9000/cache/{id}`

`http GET localhost:9000/index/{id}`

`http GET localhost:9000/metrics/jvm`

`http GET localhost:9000/words/deliver`


One thing to notice being that the index starts with 0. If the cache size is 999, the index can be queried in 0-998
