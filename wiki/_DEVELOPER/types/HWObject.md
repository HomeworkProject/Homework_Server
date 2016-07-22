# Type: ```HWObject```  
## Typical construct in response (payload)  
```
{  
	"type": "homework",  
	"subject": NameOfSubject<String>,  
	"long": LongInfo<JSONObject>,  
	"short": ShortInfo<JSONObject>,  
	"date": [yyyy<int>, MM<int>, dd<int>],  
	"id": ID<String>  
}  
```
### Fields:  
  
```long``` - Content not specified: Consider looking at Sebastians Client repo for closer information  
```short``` - Same as 'long'  
```id``` - __Filled by server__ only needed when editing an existing HomeWork  
