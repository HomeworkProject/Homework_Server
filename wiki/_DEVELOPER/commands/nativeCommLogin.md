# Command: nativeCommLogin
##### ID: de.mlessmann.commands.login

## Request
```  
{  
	"command": "login",  
	"parameters": [group<String>(, user<String>), authData<String>]  
}  
```  
  * ```authData``` - Data provided to the authMethod (e.g. password for plaintext)  
  
  
## Response
If the credentials are correct the response will be Status-200  
  
  
## Possible errors raised:  
* _InvalidCredentialsError_ - Username or password invalid  
* _NotFoundError_ - Group does not exist  
* _ProtocolError_ - The request was invalid  
  
## References:  
* Code implementation: [nativeCommLogin.java](https://github.com/MarkL4YG/Homework_Server/blob/bleeding/src/main/java/de/mlessmann/network/commands/nativeCommLogin.java)  
