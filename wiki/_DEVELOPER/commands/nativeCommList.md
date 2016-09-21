# Command: nativeCommList
##### ID: de.mlessmann.commands.list

## Request
```  
{  
	"command": "list",  
	(, "group": groupName<String>)
}  
```  
  * ```command``` - will only search for handlers implementing that command  
  
  
## Response
Status-200  
 Payload: Object of Arrays\<String\> containing the groups users.  
  
## Possible errors raised:  
* _NotFoundError_ - Group does not exist  
(keep in mind that this section _only_ tells you what errors can be raised by THIS handler)  
  
  
## References:  
* Code implementation: [nativeCommList.java](https://github.com/MarkL4YG/Homework_Server/blob/bleeding/src/main/java/de/mlessmann/network/commands/nativeCommList.java)  
