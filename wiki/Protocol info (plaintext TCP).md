# Syntax-Help for Syntax version 1.1.0.X
##### Last updated: 2016-07-06T21:06:00+0200

This is the Syntax and command information for unencrypted TCP connections to the server  
- Default tcp-plaintext-port is 11900  
- Messages end with ```CRLF``` or ```LF```
- Communication is in valid JSON [see JSON.org](http://json.org)  
- Command is a sample of what the client should send  
- Response is the response of the server if successful  
  
  
## Status:
The server provides status information in every response  
As expected it will be embedded into JSON with two fields:  
- "status"\<int\> from [Status.java](https://github.com/MarkL4YG/Homework_Server/blob/Latest/src/main/java/de/mlessmann/network/Status.java)  

## Responses:  
Any response of the server will follow this pattern:  
```
{  
	"status"<int>: <status>,  
	"payload_type"<String>: <Type of payload>,  
	"payload"<see above>: <payload>	 
	(, "array_type"<String>: <type of array content>)
}  
```
  
Valid Payload (and Array) types are:  
* JSONObject  
* JSONArray (will include "array_type" field)
* HWObject  
* Int (JSON - integer)  
* Str (JSON - String)  
* null (no payload)  
* float (JSON - float)  
* Message (JSONObject - more info below)

### Type: JSONObject
  
see [JSON.org](http://json.org)  
  
### Type: JSONArray  
  
see [JSON.org](http://json.org)  
  
If the type is an array, the response will include the key "array_type"\<String\> which indicates the type nested in the array  
  
### Type: HWObject  
  
see [HWObject type definition](https://github.com/MarkL4YG/Homework_Server/blob/Latest/wiki/types/HWObject.md)  
  
### Type: Message  
  
___currently unimplemented___  
  
  
## *** IMPORTANT: ALWAYS CHECK THE SERVERs SYNTAX VERSION! ***  

###### . 

## GetInfo
### Protocoll information
#### Command
(this command is guarranteed to be accepted ALWAYS by ANY Server version)
```
{  
	"command": "getInfo",  
	"cap": "proto"  
}  
```
 OR
```
 "protoInfo"
```
(THIS is the ONLY case in which non-json is accepted!)
#### Response:
Response will ALWAYS contain AT LEAST this:
```
{
	"protoVersion": "MAJOR.MINOR.RELEASE.BUILD"
}
```
##### Details on if something does not match:  
- MAJOR: NONE or at least one critical of the commands is compatible
- MINOR: Some or major part of commands are INCOMPATIBLE  
- RELEASE: Not all commands () are supported, but those who are should work (DOES NOT MEAN IT'S COMPATIBLE!)  
- BUILD: Some aditional commands or parameters may be available  

