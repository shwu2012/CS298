compile?
javac -cp lib/guava-18.0.jar -sourcepath src -d bin src/*.java

run?
java -Djava.util.logging.config.file=logging.properties -cp bin/:lib/guava-18.0.jar BPSOSearch r8-test-stemmed.txt.csv

