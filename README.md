# sbin

A minimal scala pastebin backed by redis

## install

with sbt

    > sbt
    update

## usage

### redis

Start your redis server

    > path/to/redis-version/redis-server

### sbin

Run with defaults

    > sbt
    run
  
Will start server at http://localhost:8080 with default login/password "admin"/"admin"

Run with provided username and password

    > sbt
    run jim:jam
    
Will start server at http://localhost:8080 with login/password "jim"/"jam"

