compile?
javac -cp lib/guava-18.0.jar -sourcepath src -d bin src/*.java

read file source?
java -Djava.util.logging.config.file=logging.properties -cp bin/:lib/guava-18.0.jar TfidfCalculator r8-test-stemmed.txt r8-test-stemmed.txt.csv [-n]

generate file from weka mutual information result?
java -Djava.util.logging.config.file=logging.properties -cp bin/:lib/guava-18.0.jar MutualInfoResultExtractor r8-train-mi-result.txt r8-train-stemmed.txt.csv r8-train-stemmed-mi.csv [-n]

run PSO?
java -Djava.util.logging.config.file=logging.properties -cp bin/:lib/guava-18.0.jar BPSOSearch 20 16 r8-test-stemmed.txt.csv[_normalized] >output_1.txt 2>error_1.txt

prepare the file for weka model building based on PSO result?
java -Djava.util.logging.config.file=logging.properties -cp bin/:lib/guava-18.0.jar FeatureSelectedDocGenerator r8-train-stemmed-mi.csv r8-train-stemmed-knn.csv gbest-position.txt
