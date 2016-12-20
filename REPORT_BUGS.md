1. Enable debug log

```
// Before creating a new ChatClient with ChatClient.create() add this line:
ChatClient.setLogLevel(android.util.Log.DEBUG);
...

ChatClient.create(....
```

2. Run your application and reproduce the problem

3. Capture entire device log using

```
adb logcat -d > my_error_log
```

4. Send my_error_log together with the problem description to Twilio

5. YES, we need the entire log to reconstruct order of events on the client side.

6. NO, only sending the lines with
```
Native thread exiting without having called DetachCurrentThread (maybe it's going to use a pthread_key_create destructor?): Thread[28,tid=22833,Native,Thread*=0xeec28600,peer=0x132c4a40,"EventThread - 2 - 22833"]
```
will not help - those are harmless and accounted for.
