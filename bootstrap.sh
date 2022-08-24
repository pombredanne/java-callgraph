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
git clone https://github.com/bitslab/java-callgraph
cd java-callgraph
mvn install