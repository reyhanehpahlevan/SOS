#!/bin/bash

. $(dirname $0)/constant.sh
scp killprocess.sh $CLIENT1:~/
scp killprocess.sh $CLIENT2:~/
scp killprocess.sh $CLIENT3:~/

./commandToClients.sh "~/./killprocess.sh"
