# Type: ```UserObject```  
## Typical construct in response and command (payload)  
```
{  
	("type": "user",)  
	"name": NameOfUser<String>,  
	"authMethod": AuthMethodID<String>,  
	("password": AuthData<String>,)  
	"permissions": [<HWPermission>]  
}  
```
### Fields:  
  
```password``` - Content not specified: Depends on [IAuthMethod.masqueradePass](https://github.com/MarkL4YG/Homework_Server/blob/Latest/src/main/java/de/mlessmann/authentication/IAuthMethod.java)  
```permissions``` - Array of HWPermission objects determining the permissions the user has  
