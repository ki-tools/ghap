@echo off
set reportsDir=junit-report
set reportFile=report.html
del %reportsDir%\%reportFile% > nul 2>&1
node node_modules\junit-viewer\bin\junit-viewer --results=%reportsDir% --save=%reportsDir%\%reportFile% > nul
start %reportsDir%\%reportFile%
