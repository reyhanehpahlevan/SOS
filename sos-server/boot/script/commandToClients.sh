#!/bin/bash
. $(dirname $0)/constant.sh

ssh -X $CLIENT1 $1 &
ssh -X $CLIENT2 $1 &
ssh -X $CLIENT3 $1 &
wait

