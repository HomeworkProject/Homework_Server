# Type: ```error```  
## Typical construct of response  
```  
{  
	("status": SomeErrorCode<int>,)  
	"payload_type": "error",  
	"payload": ErrorObj<JSONObject>  
}  
```  

## Minimal error object:  
```  
{  
	"error": ErrorName<String>,  
	"error_message": Message<String>,  
	(, "friendly_message": ReadAbleMessage<String>  
	(, "perm": PermFailInfo<String>  
}  
### Fields:  
  
- ```error``` - Name of the error, take this out of the command documentation  
- ```error_message``` - Contains a description of the error [For the client dev]  
	(May also be the result of Exception.toString())  
- ```friendly_message``` - If present, contains a message that could be shown to the user  
- ```perm``` - If present, descripes why an InsufficientPermError has been raised (details below)  
  
  
### ```perm``` field:  
Value  
```has:<permissionID>```  
(See the IDs in the corresponding [class](https://github.com/MarkL4YG/Homework_Server/blob/Latest/src/main/java/de/mlessmann/perms/Permission.java))
Means that the user has not enough permission of ID \<permissionID\>  
(PermValues are numerical, thus sometimes "user has the permission" is not enough)  
  
```cangive:<permissionID>```  
(Not yet effective, will be used when editing users is implemented)  
Means that the user has not enough 'grant power' to give someone ore something that permission  
OR isn't allowed to set a permission value as high as requested.  
