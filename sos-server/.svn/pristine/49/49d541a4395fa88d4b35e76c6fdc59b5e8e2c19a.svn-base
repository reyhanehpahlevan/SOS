MACHINES="ali@ali-vaio.local"

if [ ! -z $1 ]; then
	MACHINES=$*
fi


for MACHINE in $MACHINES; do
	echo "copying to $MACHINE"
	rsync -vazq --delete --exclude "soslogs" --exclude "test-results" --exclude "logs*.tgz" --exclude "*~" --rsh=ssh ~/Desktop/sos-binary-code/ ${MACHINE}:~/Desktop/sos-binary-code/ &
done
wait
echo "copying finished!"

