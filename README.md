# DLPJavaProtocols
Protocols for DLP Java simulator

Simulator can be found here https://www.macs.hw.ac.uk/~pjbk/dlpjava/

Protocols for simulator are in the repo

## How to run?
### With Mac

1. Open the bin folder with terminal
2. use "bash dlpjava.command" to get information about the .command file
use as: bash dlpjava.command [-b] <protocol> [parameter]

-b : Batch mode (Optional)
protocol : protocol file to be used (must be compiled as .class file) eg. Stopwait
parameter : Parameter file to be used (optional)

eg. "bash dlpjava.command stopwait"
this will open up the simulator with stopwait protocol loaded

### With Windows

1. Open bin folder with command prompt
2. Use "java -classpath .;dlpjava.jar datalink.Dlpsim [-b] <protocol> [Parameter file]
