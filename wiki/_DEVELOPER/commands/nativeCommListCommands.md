# Command: nativeCommListCommands
##### ID: de.mlessmann.commands.listCommands

## Request
```  
{  
	"command": "listcommandhandler",  
	(, "search": command<String>)
}  
```  
  * ```command``` - will only search for handlers implementing that command  
  
  
## Response
Status-200  
Including an array of Handler references  
  
  
## Possible errors raised:  
* None 
(keep in mind that this section _only_ tells you what errors can be raised by THIS handler)  
  
  
## References:  
* Code implementation: [nativeCommListCommands.java](https://github.com/MarkL4YG/Homework_Server/blob/bleeding/src/main/java/de/mlessmann/network/commands/nativeCommListCommands.java)  
