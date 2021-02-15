. constant.sh

ssh $CLIENT1 "killall xterm" &
ssh $CLIENT2 "killall xterm" &
ssh $CLIENT3 "killall xterm" &


ssh $CLIENT1 "killall java" &
ssh $CLIENT2 "killall java" &
ssh $CLIENT3 "killall java" &


wait
