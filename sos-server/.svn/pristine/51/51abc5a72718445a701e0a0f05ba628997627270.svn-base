src=~/Desktop/sos-svn/S.O.S
dst=~/Desktop/sos-binary-code

echo "copying from '$src' to built folder($dst)"
pushd $src
ant clean
ant build
popd

mkdir -p $dst/lib
cp -r $src/lib/* $dst/lib
cp -r $src/bin/* $dst
cp -r $src/xml/ $dst
cp -r $src/*.gif $dst
cp -r $src/*.config $dst
cp -r $src/log4j.properties $dst
cp agent-start.sh $dst/start.sh
cp agent-kill.sh $dst/kill.sh
echo "copying finished to $dst"
