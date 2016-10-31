# Command: nativeCommEditHW
##### ID: de.mlessmann.commands.edithw

## Request
```  
{  
	"command": "edithw",  
	"date": [yyyy<int>, MM<int>, dd<int>],  
	"id": id<String>,  
	"homework": hwObj<JSONObject>  
}  
```  
  * ```date``` - Date of the old HomeWork
  * ```id``` - ID of the old HomeWOrk
  * ```hwObj``` - The new HomeWork object, see doc for details  
  
  
## Response
If the homework was edited successfully the response will be Status-201  
  
  
## Possible errors raised:  
* _InsufficientPermissionError_ - User is not allowed to edit a homework  
* _EditHWError_ - HomeWork/Date/ID was invalid or an internal error occurred  
	- Status:500 Internal Error
	- Status:400 HomeWork invalid
* _ProtocolError_ - The request was invalid  
* _LoginRequiredError_ - User needs to login first  
  
  
## References:  
* Code implementation: [nativeCommEditHW.java](https://github.com/MarkL4YG/Homework_Server/blob/bleeding/src/main/java/de/mlessmann/network/commands/nativeCommEditHW.java)  
  
