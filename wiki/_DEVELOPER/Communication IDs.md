# Communication Identifiers

- Are tags added to the upper-most JSONObject of the client request.  
- They will be reused by the server for EACH answer it sends corresponding to the associated request.  

## On single requests

- If the client does not supply such an ID, the server will just generate one.  
- The server indicates that no additional answer will follow by negating the numerical ID (e.g. 422 would change to -422)
  
## On request chains
  
- In request chains, every request can contain it's own ID, so the client can determine to which request the answers belong  
- Negated IDs will indicate the same as above for each request  
- When the chain has been finished, the status "OK" with ID ```0``` will be send as the last message.
- In fact: Not providing IDs in chains could end very painful for client developers, but the server won't care and just generate one for each element that's missing one.

## Example
##### Client be like  
```   
{  
  "command": <blablabla>,  
  "commID": 3390  
}  
```
##### Server be like
```   
{  
  <response1>,  
  "commID": 3390  
}  
```
```   
{  
  <response2>,  
  "commID": 3390  
}  
```
```   
{  
  <response3>,  
  "commID": -3390  
}  
```  
  
## Reserved IDs:  
```1```: Server: Greeting, sent before anything has been received  
```0```: Server: Chain processed, ready to accept requests again  
