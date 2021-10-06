#!/bin/bash

echo "Seeding database from existing dump..."
cd /docker-entrypoint-initdb.d
tar xvjpf dump.tar.bz2
mongorestore
