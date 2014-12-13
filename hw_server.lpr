program hw_server;

{$mode objfpc}{$H+}

uses
    {$IFDEF UNIX}{$IFDEF UseCThreads}
    cthreads,
    {$ENDIF}{$ENDIF}
    Classes
    { you can add units after this },
    mainthread, msg_qeue
    ;

var
  Application : THWServer;

begin

  Application:=THWServer.Create(Nil);
  Application.OnException := @Application.GotException;
  Application.Title := 'HW_Server';
  Application.Run;
  Application.Free;

end.

