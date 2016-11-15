# Simple Java Chatserver
### UE Verteilte Systeme WS 2016/17 184.167

aaaa

## Todo

- ~~Exit shell Client~~
- ~~Exit shell Server~~
- ~~UDP really no fixed port?~~ (Page 6 says no)
- ~~Close connection on !logout~~ -> close user shell ??
- ~~msg lookup~~ ~> msg works now complete
- msg synchronity
  bar: `!msg foo <message>` ~> NOT FOUND
  foo: `!register <IP:PORT>` ~> OK
  bar: `!msg foo <message>` ~> NOT FOUND => EHH. 2/2 replizierbar
- `!register <IP:PORT>` ~> NOT LOGGED IN
  further typing (shell) is impossible (broken?)
- ~~lastMsg~~