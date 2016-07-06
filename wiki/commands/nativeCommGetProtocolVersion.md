# Command: nativeCommGetProtocolVersion
##### ID: de.mlessmann.commands.getProtocolVersion  

## Request
```  
{  
	"command": "getinfo"  
}  
```  
  
## Response
```
{
	"protoVersion": "X.X.X.X(.X.X)"
}
```  
```MAJOR.MINOR.RELEASE.BUILD```  
##### Details on if something does not match:  
- MAJOR: NONE of the commands is compatible or at least one critical is incompatible (e.g. login)  
- MINOR: Some or major part of commands are INCOMPATIBLE  
- RELEASE: Not all commands () are supported, but those who are should work (DOES NOT MEAN IT'S REALLY COMPATIBLE!)  
- BUILD: Some aditional commands, features or parameters may be available or some bugs might have been fixed  

  
  
## Possible errors raised:  
* None 
(keep in mind that this section _only_ tells you what errors can be raised by THIS handler)  
  
  
## References:  
* Code implementation: [nativeCommGetProtocolVersion.java](https://github.com/MarkL4YG/Homework_Server/blob/Latest/src/main/java/de/mlessmann/network/commands/nativeCommGetProtocolVersion.java)  
