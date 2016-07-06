# Command: nativeCommDelHW
##### ID: de.mlessmann.commands.delHW

## Request
```  
{  
	"command": "addhw",  
	"date": [yyyy<int>, MM<int>, dd<int>]  
	"id": idOfHW<String>  
}  
```  
  * ```id``` - ID can be aquired from HomeWork object  
  
  
## Response
If the homework was deleted successfully the response will be Status-200  
  
  
## Possible errors raised:  
* _InsufficientPermissionError_ - User is not allowed to delete a homework  
* _DelHWError_ - An internal error occurred  
* _ProtocolError_ - The request was invalid  
* _LoginRequiredError_ - User needs to login first  
* _DateTimeError_ - Supplied date was invalid (Meaning that it couldn't be converted to a DateTime object)  
  
  
## References:  
* Code implementation: [nativeCommDelHW.java](https://github.com/MarkL4YG/Homework_Server/blob/Latest/src/main/java/de/mlessmann/network/commands/nativeCommDelHW.java)  
