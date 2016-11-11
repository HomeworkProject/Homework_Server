# Type: ```HWObject```  
## Typical construct in response (payload)  
```
{  
	"type": "homework",  
	"subject": NameOfSubject<String>,  
    "desc": Description<String>,
	"date": [yyyy<int>, MM<int>, dd<int>],  
	"id": ID<String>  (,
	"attachments": [
	    <HWAttachment>
    ])
}  
```
### Fields:  
  
```subject``` - what subject does this homework belong to  
```date``` - on what date is it scheduled  
```id``` - __Filled by server(On #AddHW)__ only needed when editing an existing HomeWork  
```attachments``` - Attachment references, see the HWAttachment type definition