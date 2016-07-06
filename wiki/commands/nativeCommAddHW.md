# Command: nativeCommAddHW
##### ID: de.mlessmann.commands.addHW

## Request
```  
{  
	"command": "addhw",  
	"homework": hwObj<JSONObject>  
}  
```  
  * ```hwObj``` - HomeWork object see doc for details  
  
  
## Response
If the homework was added successfully the response will be Status-201  
  
  
## Possible errors raised:  
* _InsufficientPermissionError_ - User is not allowed to add a homework  
* _AddHWError_ - HomeWork was invalid or an internal error occurred  
	- Status:500 Internal Error
	- Status:400 HomeWork invalid
* _ProtocolError_ - The request was invalid  
* _LoginRequiredError_ - User needs to login first  
  
  
## References:  
* Code implementation: [nativeCommAddHW.java](https://github.com/MarkL4YG/Homework_Server/blob/Latest/src/main/java/de/mlessmann/network/commands/nativeCommAddHW.java)  
