# Makerun for dlpsim

.SUFFIXES: .java.class
CLASSPATH= .:../dlpjava.jar
all: $(DLP).class
	@ java -classpath $(CLASSPATH) datalink.Dlpsim $(BATCH) $(DLP) $(ARG)

%.class : %.java
	javac -classpath $(CLASSPATH)  $<
