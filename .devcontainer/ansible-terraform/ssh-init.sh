#!/bin/sh

set -e

find ~/.ssh/*.pub -type f -exec chmod 644 {} +
find ~/.ssh/*.pem -type f -exec chmod 600 {} +
chmod 600 ~/.ssh/github

eval $(ssh-agent -s)

ssh-add ~/.ssh/github
