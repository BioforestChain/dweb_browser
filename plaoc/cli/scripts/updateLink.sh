#!/bin/bash

sudo npm unlink @bfex/bundle
sudo rm -rf /usr/local/bin/bfex && rm -rf /usr/local/lib/node_modules/@bfex/bundle
sudo npm link