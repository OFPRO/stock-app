; StockPro Windows Installer - Inno Setup Script
; Build with: iscc installer.iss

#define MyAppName "StockPro"
#define MyAppVersion "1.8.0"
#define MyAppPublisher "Bibliotheque Badr"
#define MyAppURL "http://<IP_DU_PC>:5001"
#define MyAppExeName "stock-app.exe"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\StockPro
DefaultGroupName=StockPro
DisableProgramGroupPage=yes
OutputDir=..\dist
OutputBaseFilename=StockPro-Setup
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
UninstallDisplayIcon={app}\stock-app.exe
AllowNoIcons=yes

; Allow user to choose data directory
#define DataDir "{code:GetDataDir}"

[Languages]
Name: "french"; MessagesFile: "compiler:Languages\French.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Types]
Name: "typical"; Description: "Installation standard"
Name: "custom"; Description: "Installation personnalisée"; Flags: iscustom

[Components]
Name: "app"; Description: "Application StockPro"; Types: typical custom; Flags: fixed
Name: "desktopicon"; Description: "Icône sur le bureau"; Types: custom

[Files]
Source: "..\dist\stock-app.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "postinstall.cmd"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\StockPro"; Filename: "{app}\{#MyAppExeName}"; Parameters: "--data-dir ""{code:GetDataDir}"" --host 0.0.0.0 --open-browser"; WorkingDir: "{app}"
Name: "{group}\Désinstaller StockPro"; Filename: "{uninstallexe}"
Name: "{commondesktop}\StockPro"; Filename: "{app}\{#MyAppExeName}"; Parameters: "--data-dir ""{code:GetDataDir}"" --host 0.0.0.0 --open-browser"; WorkingDir: "{app}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Créer une icône sur le bureau"; GroupDescription: "Icônes supplémentaires:"

[Run]
Filename: "{app}\{#MyAppExeName}"; Parameters: "--data-dir ""{code:GetDataDir}"" --host 0.0.0.0 --open-browser"; Description: "Lancer StockPro"; Flags: postinstall nowait skipifsilent shellexec

[Code]
var
  DataDirPage: TInputDirWizardPage;

function GetDataDir(Param: string): string;
begin
  Result := DataDirPage.Values[0];
end;

procedure InitializeWizard;
begin
  DataDirPage := CreateInputDirPage(
    wpSelectDir,
    'Dossier de données',
    'Où voulez-vous stocker les données de StockPro ?',
    'Les données (base de données, logs, etc.) seront stockées dans ce dossier.'#13#13
    'Ce dossier ne sera PAS supprimé lors de la désinstallation.',
    False,
    ''
  );
  DataDirPage.Add('Choisissez le dossier de données :');
  DataDirPage.Values[0] := ExpandConstant('{localappdata}') + '\StockPro';
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = DataDirPage.ID then
  begin
    if not DirExists(DataDirPage.Values[0]) then
    begin
      if not CreateDir(DataDirPage.Values[0]) then
      begin
        MsgBox('Impossible de créer le dossier : ' + DataDirPage.Values[0], mbError, MB_OK);
        Result := False;
      end;
    end;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    if not DirExists(DataDirPage.Values[0]) then
      CreateDir(DataDirPage.Values[0]);
  end;
end;