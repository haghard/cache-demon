cache-demon 
===================

### How to build ###

`sbt stage`

### How to run ###

`./target/universal/stage/bin/cache-demon`

## How to query ###

### With Httpie

`http GET localhost:9000/cache/{id}`

`http GET localhost:9000/index/{id}`


One thing to notice being that the index starts with 0. If the cache size is 999, the index can be queried in 0-998