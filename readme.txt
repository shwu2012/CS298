compile?
javac -cp lib/guava-18.0.jar -sourcepath src -d bin src/*.java

read file source?
java -Djava.util.logging.config.file=logging.properties -cp bin/:lib/guava-18.0.jar TfidfCalculator r8-test-stemmed.txt r8-test-stemmed.txt.csv [-n]

run?
java -Djava.util.logging.config.file=logging.properties -cp bin/:lib/guava-18.0.jar BPSOSearch 20 16 r8-test-stemmed.txt.csv[_normalized] >output_1.txt 2>error_1.txt
