cache-demon 
===================

### How to build ###

`sbt stage`

### How to run ###

`./target/universal/stage/bin/cache-demon`

## How to query ###

One thing to notice being that the index starts with 0. If the cache size is 999, the index can be queried inside 0-998

### With Httpie

`http GET localhost:9000/cache/{id}`

`http GET localhost:9000/index/{id}`

### With curl 

`curl  http://localhost:9000/cache/{id}`

`curl  http://localhost:9000/index/{id}`
