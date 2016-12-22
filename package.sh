#!/bin/sh

######################
# Package Nxt2Wallet #
######################

if [ -z "$1" ] ; then
  echo "You must specify the version to package"
  exit 1
fi

VERSION="$1"

if [ ! -d package ] ; then
  mkdir package
fi

cd package
rm -R *
cp ../ChangeLog.txt ../LICENSE ../README.md ../Nxt2Wallet.conf .
cp ../target/Nxt2Wallet-$VERSION.jar .
cp -R ../target/lib lib
zip -r Nxt2Wallet-$VERSION.zip ChangeLog.txt LICENSE README.md Nxt2Wallet.conf Nxt2Wallet-$VERSION.jar lib
dos2unix ChangeLog.txt LICENSE README.md Nxt2Wallet.conf
tar zcf Nxt2Wallet-$VERSION.tar.gz ChangeLog.txt LICENSE README.md Nxt2Wallet.conf Nxt2Wallet-$VERSION.jar lib
exit 0

