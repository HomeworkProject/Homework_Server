# Command: nativeCommGetHW
##### ID: de.mlessmann.commands.gethw

## Request
```  
{  
	"command": "gethw",  
	"date": [yyyy<int>, MM<int>, dd<int>]  
	(, "cap": "short"/"long")  
	(, "subjects": [<String(s)>])  
}  
```  
OR  
```  
{
	"command": "gethw",  
	"dateFrom": [yyyy<int>, MM<int>, dd<int>],  
	"dateTo": [yyy<int>, MM<int>, dd<int>],  
	(, "cap": "short"/"long")  
	(, "subjects": [<String(s)>])  
}
//Note that the timespan is internally limited to a maximum of 64 days per request  
```  
  * ```cap``` - Send only the short/long info  
  * ```subjects``` - WhiteList of subjects to include (only)  
  
  
## Response
Includes an Array of HWObjects.  
(Note: If a cap was provided, the objects will also be valid HWObjects but only contain ```id```, ```date```,```subject``` and ```long/short``` as keys)  
  
  
  
## Possible errors raised:  
* _LoginRequiredError_ - User needs to login first  
* _ProtocolError_ - The request was invalid  
* _DateTimeError_ - Supplied date was invalid (Meaning that it couldn't be converted to a DateTime object)  
  
  
## References:  
* Code implementation: [nativeCommGetHW.java](https://github.com/MarkL4YG/Homework_Server/blob/Latest/src/main/java/de/mlessmann/network/commands/nativeCommGetHW.java)  
