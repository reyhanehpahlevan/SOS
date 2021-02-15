FOLDER="S.O.S"
if [ ! -z "$1" ]; then
	FOLDER="$1"
fi
	
src=~/Desktop/sos-svn/$FOLDER
dst=~/Desktop/sos-binary-code

echo "copying from '$src' to built folder($dst)"
pushd $src
ant clean
ant build
popd
rm -r -f $dst
mkdir -p $dst/lib/
cp -r $src/lib/* $dst/lib/
cp -r $src/bin/* $dst
cp -r $src/xml/ $dst
cp -r $src/*.gif $dst
cp -r $src/*.png $dst
cp -r $src/*.config $dst
cp -r $src/log4j.properties $dst
cp agent-start.sh $dst/start.sh
echo "copying finished to $dst"
