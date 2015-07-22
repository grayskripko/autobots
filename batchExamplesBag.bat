exit
pause 

if exist c:\temp\regex.chg del c:\temp\regex.chg

for /f "tokens=* delims=" %%f in (c:\temp\regex.txt) do (
  set strLine=%%f
  set subLine=!strLine:~0,4!
  if /i .!subLine! EQU .GOTO call :getLoc
  echo !strLine! >> c:\temp\regex.chg
)
goto :eof

:getLoc
  for /l %%i in (0, 1, 67) do (
    call set strChunk=%%strLine:~%%i,2%%
    if .!strChunk! EQU .PT call set strLine=%%strLine:~0,%%i%% & goto :eof

  )
  goto :eof
  
my part starts  
  @echo off
setlocal disableDelayedExpansion

:Variables
set InputFile=line.txt
set OutputFile=line2.txt
set "_strFind=seconds"
set "_strInsert=fourth"

:Replace
>"%OutputFile%" (
  for /f "usebackq delims=" %%A in ("%InputFile%") do (
    if "%%A" equ "%_strFind%" (echo "d") else (echo %%A)
  )
)