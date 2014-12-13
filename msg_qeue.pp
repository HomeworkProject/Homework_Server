unit msg_qeue;

{$mode objfpc}{$H+}

interface

uses
    Classes, SysUtils,
    //
    contnrs, fpjson
    ;

Type

  { TQeue }

  TQeue = class(TObject)
  public
    CONSTRUCTOR Create(Owner:TObject; Ident:String); override;
  private
    pQeueIdentifier : String;
  public
    Property Ident : String read pQeueIdentifier;

  end;

  { TQeueList }

  TQeueList = class(TObjectList)
  private
    function GetObject(I: Integer): TQeue;
    procedure SetObject(I: Integer; Obj: TQeue);
  public
    property qeue[I: Integer]: TQeue read GetObject write SetObject;
  end;

  { TMSGQeue }

  TMSGQeue = class(TObject)
  public
    CONSTRUCTOR Create (Owner:TObject); override;
    Procedure AddQeue(child : TQeue);
    Procedure DeleteQeue(index:Integer);
  private
    Function GetQeue(index:Integer):TQeue;
    Function GetQeueByIdent(ident: String): TQeue;
  private
    pQeues : TQeueList;
  public
    Property Qeue[index:Integer] : TQeue read GetQeue;
    Property QeueBI[Ident:String] : TQeue read GetQeueByIdent;
  end;

implementation

{ TMSGQeue }

CONSTRUCTOR TMSGQeue.Create(Owner: TObject);
begin
    inherited Create(Owner);
    pQeues:=TQeueList.create(self);
    pQeues.OwnsObjects := True;
end;

Procedure TMSGQeue.AddQeue(child: TQeue);
begin
     pQeues.Add(child);
end;

Procedure TMSGQeue.DeleteQeue(index: Integer);
begin
     pQeues.Delete(index);
end;

Function TMSGQeue.GetQeue(index: Integer): TQeue;
begin
    Result:=pQeues.qeue[index];
end;

Function TMSGQeue.GetQeueByIdent(ident: String): TQeue;
var
  I:Integer;
begin
    for I:=0 to pQeues.Count-1 do begin
      if pQeues.qeue[I].Ident = ident then begin
        Result := I;
        Exit;
      end;
    end;
end;

{ TQeue }

CONSTRUCTOR TQeue.Create(Owner: TObject; Ident: String);
begin
    inherited Create(Owner);
    pQeueIdentifier := Ident;
end;

{ TQeueList }

function TQeueList.GetObject(I: Integer): TQeue;
begin
     Result:=TQeue(Items[I]);
end;

procedure TQeueList.SetObject(I: Integer; Obj: TQeue);
begin
     Items[I]:=Obj;
end;

end.

