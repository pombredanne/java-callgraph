#!/usr/bin/env bash
echo 'root access'
apt-get update
echo 'update'
apt-get install git -y
echo 'git install'
apt-get install openjdk-18-jre-headless -y
echo 'java install'
apt-get install maven -y
echo 'maven install'
apt-get install graphviz -y
echo 'graphviz install'
cd ~/Desktop
# git clone https://github.com/bitslab/java-callgraph
# /vagrant not mounting on my system by default currently. It is classified as an abnormal behavior and should work in general scenarios.
# added logic for package building after copying from vagrant file directory.
cp /vagrant ~/Desktop/java-callgraph
cd java-callgraph
mvn install