rm -rf ./archives
rm -rf ./xcframeworks
./archive.sh
./create-xc.sh

# TODO gradle sync
rm -rf ../../../../../build