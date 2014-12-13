unit mainthread;

{$mode objfpc}{$H+}

interface

uses
    Classes, SysUtils,
    //
    CustApp;

type

  { THWServer }

  THWServer = class(TCustomApplication)
  //Homework-Server, Main Thread
  protected
    Procedure DoRun; override;
    Procedure DoLog(EventType: TEventType; const Msg: String); override;
  public
    //Public Procedures
    procedure GotException(Sender:TObject; E:Exception);
  private
    //Private Procedures
    function pGetLogStart : String;
  public
    //Public Vars

    //Properties
    property LogStart : String read pGetLogStart;
  private
    //Private Vars
  end;

implementation

{ THWServer }

Procedure THWServer.DoRun;
begin

end;

Procedure THWServer.DoLog(EventType: TEventType; const Msg: String);
var
  LS : String;
begin
case EventType of

etCustom:LS:='[CUSTOM]';
etInfo:LS:='[INFO]';
etDebug:LS:='[DEBUG]';
etError:LS:='[ERROR/Fatal]';
etWarning:LS:='[WARNING]';

end;

WriteLn(LS+LogStart+'|'+MSG);

end;

procedure THWServer.GotException(Sender: TObject; E: Exception);
begin
    DoLog(etError,'=======================================');
    DoLog(etError,'Exception caught!');
    DoLog(etError,'ExName: '+E.ClassName);
    DoLog(etError,'ExMSG: '+E.Message);
    DoLog(etDebug,'ExClassParent: '+E.ClassParent.ClassName);
    DoLog(etError,'=======================================');

    Sleep(50000);
    { TODO 70 -oL4YG -cDebug : Remove debug sleep }
end;

function THWServer.pGetLogStart: String;
begin
    Result:='['+DateToStr(now)+'|'+TimeToStr(now)+']';
end;

end.

