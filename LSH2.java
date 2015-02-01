import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Scanner;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.lang.StringBuilder;

/*
** LSH Assignment Part 2, due 1/21
** Written by Brynna Mering and Risako Owan
** Message: Tried to make faster but failed. Let us know if you have any suggestions!
*/


public class LSH2 {
    //Given by user
    private static int numOfHashFunc;
    private static int numOfDocs;
    private static int k;
    private static int r;

    private static ArrayList<HashSet<String>> setArrayList;
    private static int gotRandom = 0;

    /*
    ** Takes the file as input and creates an array of HashSets out of it for global var setArrayList.
    ** Called once in main function.
    */
    private static void copyFileToSets(File data) {
        
        setArrayList = new ArrayList<HashSet<String>>();

        try {

            Scanner scanner = new Scanner(data);

            scanner.nextLine(); //number of total docs
            scanner.nextLine(); //num of words
            scanner.nextLine(); //number of non-distinct words

            String[] fileLine = scanner.nextLine().split(" ");

            //Creates an array keeping track of the words in each document
            for (int i = 0; i < numOfDocs; i++) {
                
                HashSet<String> hashSet = new HashSet<String>();
                
                while (fileLine[0].equals(Integer.toString(i + 1))) {
                    hashSet.add(fileLine[1]);
                    fileLine = scanner.nextLine().split(" ");
                }
                
                setArrayList.add(hashSet);

            }//end of for-loop

            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    ** Takes two document IDs and creates an intersection set.
    */
    private static HashSet<String> getIntersection(int docID1, int docID2) {
        
        HashSet<String> intersection = new HashSet<String>();
        HashSet<String> set1 = setArrayList.get(docID1);
        HashSet<String> set2 = setArrayList.get(docID2);
        intersection.addAll(set1);
        intersection.retainAll(set2);
        return intersection;
    }

    /*
    ** Takes two document IDs and creates an union set.
    */
    private static HashSet<String> getUnion(int docID1, int docID2) {
        HashSet<String> union = new HashSet<String>();
        HashSet<String> set1 = setArrayList.get(docID1);
        HashSet<String> set2 = setArrayList.get(docID2);
        union.addAll(set1);
        union.addAll(set2);
        return union;
    }

    /*
    ** Takes two document IDs, calculates, and returns the exact Jaccard similarity.
    ** Called in findKNN, 
    */
    private static double calculateJaccardSim(int docID1, int docID2) {
        
        HashSet<String> union = getUnion(docID1, docID2);
        HashSet<String> intersection = getIntersection(docID1, docID2);
        
        return (double)intersection.size()/(double)union.size();
    }

    /*
    ** Creates the union of entire document set and uses it to create signature matrix.
    */
    private static Integer[][] produceSignatureMatrix() {
        
        //Creates matrix filled with infinities
        Integer[][] signatureMatrix = new Integer[numOfHashFunc][numOfDocs];
        for (int i = 0; i < numOfHashFunc; i++) {
            for (int j = 0; j < numOfDocs; j++) { 
                signatureMatrix[i][j] = Integer.MAX_VALUE;
            }
        }

        //Creates positive variable lists for hash function
        
        Random generator = new Random(3);
        Integer[] bList = new Integer[numOfHashFunc];
        Integer[] aList = new Integer[numOfHashFunc];

        //Maximum is set for random generator because extremely large ints turn negative.
        for (int i = 0; i < numOfHashFunc; i++) {
            Integer a = -1;
            while (a < 0) {
                a = generator.nextInt(10000);
            }
            aList[i] = a;
        }
        
        Random bGenerator = new Random(6);
        for (int i = 0; i < numOfHashFunc; i++) {
            Integer b = -1;
            while (b < 0) {
                b = bGenerator.nextInt(10000);
            }
            bList[i] = b;
        }


        //Creates union of all the words in the doccument
        HashSet<String> union = new HashSet<String>();
        for(int i = 0; i < numOfDocs; i++){
            union.addAll(setArrayList.get(i));
        }

        //Adds word numbers to the sets' respective lists to keep track of where the ones are.
        ArrayList<ArrayList<Integer>> listOfColumnInfo = new ArrayList<ArrayList<Integer>>();
        for(int i = 0; i < numOfDocs; i++){
            int row = 0;
            ArrayList<Integer> wordIndices = new ArrayList<Integer>();
            for (String word : union) {
                if(setArrayList.get(i).contains(word)){
                    wordIndices.add(row);
                }
                row++;
            }
            listOfColumnInfo.add(wordIndices);
        }

        //Using row numbers, creates hash function values
        //Fills in signature matrix in the same loop.
        for (int i = 0; i < numOfHashFunc; i++) {
            int column = 0;
            for(List<Integer> docInfo : listOfColumnInfo){
                for(Integer rowNum : docInfo){
                    int hash = ((numOfHashFunc * aList[i] + 1) * rowNum + bList[i])%numOfHashFunc;
                    //Puts into matrix if smaller
                    //Index 0 for set1
                    if (hash < signatureMatrix[i][column]) {
                        signatureMatrix[i][column] = hash;
                    }
                }
                column++;
            }
        }

        return signatureMatrix;
    }

//Part 2:
//Brute Force Approach-----------------------------------------------------------------------------------

    /*
    ** Takes a docID and finds the average Jaccard similarity of its k-nearest neighbors.
    ** Used in knnForAll; not called in main function.
    */
    private static double findKNN(int docID) {
        PriorityQueue<Double> neighborsQueue = new PriorityQueue<Double>();

        //Computes exact J similarity between given document ID doc and the rest.
        //Adds similarities to the priority queue

        for (int i = 0; i < numOfDocs; i++) {
            //Makes sure it does't calculate jSim with itself.
            if (docID != i) {
                double jSim = calculateJaccardSim(docID, i);
                neighborsQueue.add(1-jSim);
            }

        }
        if (docID%200 == 0) {
            System.out.println("Currently on doc ID: " + docID);
        }
        //neighborsQueue has the k largest Jaccard similarities now.
        //Calculates and returns the average Jaccard similarity of knn.

        double jSimAverage = 0.0;
        for (int i = 0; i < k; i++) {
            jSimAverage += (1-neighborsQueue.poll());

        }
        jSimAverage = jSimAverage/k;

        return jSimAverage;
    }

    /*
    ** Finds the average Jaccard similarity for the k-nearest neighbors of each document.
    ** Calculates the average of them all.
    */
    private static double knnForAll() {
        double[] averagesArray = new double[numOfDocs];

        for (int i = 0; i < numOfDocs; i++) {
            double jSim = findKNN(i);
            averagesArray[i] = jSim;
        }

        //Gets average of averages
        double avgOfAvg = 0.0;
        for (int i = 0; i < numOfDocs; i++) {
            avgOfAvg += averagesArray[i];
        }
        avgOfAvg = avgOfAvg/numOfDocs;

        return avgOfAvg;
    }

//LSH Approach-----------------------------------------------------------------------------------

    /*
    ** Takes a candidate group set and returns a list of all possible pairings in that set.
    ** Gets called in generateCandidatePairSet
    */
    private static HashSet<Integer[]> generatePairsFromSet(HashSet<Integer> candidateSet) {

        HashSet<Integer[]> generatedPairs = new HashSet<Integer[]>();

        //Turns given set into array
        Object[] tempArray = candidateSet.toArray();
        Integer[] candidateArray = Arrays.copyOf(tempArray, tempArray.length, Integer[].class);

        //Generates all possible pairings and puts them in generatePairs set.
        for (int i = 0; i < candidateArray.length; i++) {
            for (int j = i+1; j < candidateArray.length; j++) {
                Integer[] pair1 = new Integer[2];
                pair1[0] = candidateArray[i];
                pair1[1] = candidateArray[j];
                generatedPairs.add(pair1);
            }
        }
        
        return generatedPairs;
    }

    /*
    ** Iterates through the given banded signature matrix (each band has r rows),
    ** creates a dictionary of candidate groupings, and returns a set of all candidate pairs.
    ** Calls generatePairsFromSet
    */
    private static HashSet<Integer[]> generateCandidatePairSet(Integer[][] sigMatrix) {
        int numOfBands = numOfHashFunc/r;
        HashSet<Integer[]> setOfCandidatePairs = new HashSet<Integer[]>();
        StringBuilder dictKey = new StringBuilder();
        String dictKeyString = "";

        for (int b = 0; b < numOfHashFunc; b = b + r) {
            if ((b/r)%200 == 0) {
                System.out.println("Currently working on band #" + (b/r));
            }
            //Creates a dictionary for each band
            Map<String, HashSet<Integer>> bandDict = new HashMap<String, HashSet<Integer>>(); 

            for (int col = 0; col < numOfDocs; col++) {

                //Creates dictionary key for each column/document
                dictKey.delete(0, dictKey.length());
                HashSet<Integer> candidateSet = new HashSet<Integer>();

                for (int row = 0; row < r; row++) {    
                    if (b + row < numOfHashFunc) {
                        dictKey.append(sigMatrix[b+row][col]+",");
                    }
                }

                dictKeyString = dictKey.toString();

                //Create dictionary value out of the column number
                candidateSet.add(col); //col = (docID - 1)

                //Adds key-value to dictionary
                if (!bandDict.containsKey(dictKeyString)) {
                    bandDict.put(dictKeyString, candidateSet);
                } else {
                    HashSet<Integer> modifiedVal = bandDict.get(dictKeyString); //Gets candidates
                    modifiedVal.addAll(candidateSet);
                    bandDict.put(dictKeyString, modifiedVal);
                }
                //Creates candidate pairs for each band and adds them to set
                setOfCandidatePairs.addAll(generatePairsFromSet(bandDict.get(dictKeyString)));


            }
        }
        return setOfCandidatePairs;
    }

    /*
    ** Takes a set of candidate pairs and puts that information into a matrix for easier access.
    ** Gets called in implementLSH.
    */
    private static Integer[][] generateCandidatePairMatrix(HashSet<Integer[]> setOfCandidatePairs) {
        //Iterates through setOfCandidatePairs, and creates matrix to keep track of candidate pairs
        System.out.println("Working on generating the candidate pair matrix. Sometimes runs slowly.");
        Integer[][] candidatePairMatrix = new Integer[numOfDocs][numOfDocs];
        for (int i = 0; i < numOfDocs; i++) {
            for (int j = 0; j < numOfDocs; j++) {
                candidatePairMatrix[i][j] = 0;
            }
        }

        for (Integer[] pair : setOfCandidatePairs) {
            
            Integer doc1 = pair[0];
            Integer doc2 = pair[1];
            candidatePairMatrix[doc1][doc2] = candidatePairMatrix[doc1][doc2] + 1;
            candidatePairMatrix[doc2][doc1] = candidatePairMatrix[doc2][doc1] + 1;
            
        }


        return candidatePairMatrix;    
    }


    /*
    ** Takes a candidate pair matrix, calculates the Jaccard similarity for all pairings,
    ** finds average of the similarities of the k-nearest neighbors for each document.
    ** Returns the average of the averages.
    */
    private static double calculateJSimFromMatrix(Integer[][] candidatePairMatrix) {
        double averageOfAverage = 0;

        //Calculate Jaccard similarity for each pairing
        //Goes through column in the matrix
        for (int i = 0; i < numOfDocs; i++) {
            PriorityQueue<Double> jSimNeighborsQueue = new PriorityQueue<Double>();

            //Goes through row in the matrix
            for (int j = 0; j < numOfDocs; j++) {

                //Makes sure it does't calculate jSim with itself.
                if (candidatePairMatrix[i][j] != 0) {
                    double jSim = calculateJaccardSim(i, j);
                    jSimNeighborsQueue.add(1-jSim);
                }
            }

            //Fills up queue to length k is necessary
            if (jSimNeighborsQueue.size() < k) {
                gotRandom++;
                Random generator = new Random(3);
                while (jSimNeighborsQueue.size() < k) {
                    //Pick random document and push in Jsim
                    int random = generator.nextInt(numOfDocs-1);
                    if (candidatePairMatrix[i][random] == 0 && random != i) {
                        double jSim = calculateJaccardSim(i, random);
                        jSimNeighborsQueue.add(1-jSim);
                    } 
                }
            }
            //At this point, jSimNeighborsQueue contains all jSims for every candidate pair of doc i
            //Now calculate average
            double tempAverage = 0.0;

            for (int h = 0; h < k; h++) {
                tempAverage = tempAverage + (1-jSimNeighborsQueue.poll());
                //System.out.println(tempAverage);
            }

            tempAverage = (tempAverage/k);
            averageOfAverage = averageOfAverage + tempAverage;

        }//finished with all documents

        averageOfAverage = averageOfAverage/numOfDocs;
        return averageOfAverage;
    }

    /*
    ** Implements the LSH approach to compare with brute force
    */
    private static double implementLSH() {

        Integer[][] sigMatrix = produceSignatureMatrix();

        Integer[][] cPMatrix = generateCandidatePairMatrix(generateCandidatePairSet(sigMatrix));

        //Calculates Jaccard similarity for all candidate pairs
        double avgJSimForAll = calculateJSimFromMatrix(cPMatrix);

        return avgJSimForAll;
    }

    /*
    ** MAIN FUNCTION!
    */
    public static void main(String[] args) {
        
        //LOCATION OF FILE HERE
        File file = new File("docword.enron.txt");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));


        try {

            //Prompts user for program details.
            System.out.println("Enter number of documents for the program.");
            numOfDocs = Integer.parseInt(reader.readLine()) - 1;

            System.out.println("Enter number of rows to use for the signature matrix (hash functions).");
            numOfHashFunc = Integer.parseInt(reader.readLine());

            System.out.println("Enter number of nearest neighbors (k).");
            k = Integer.parseInt(reader.readLine());

            System.out.println("Enter number of rows in each band.");
            r = Integer.parseInt(reader.readLine());

            copyFileToSets(file);

            System.out.println(); //Remind users the values they put in?

            //Brute force approach:
            long bStart = System.currentTimeMillis();
            System.out.println("Calculating using the brute force approach...");
            double bruteForceKnn = knnForAll();
            System.out.println("The average using the brute force approach is: ");
            System.out.println(bruteForceKnn);
            long bEnd = System.currentTimeMillis();
            long bDelta = bEnd - bStart;
            System.out.println("Brute Force took: "+bDelta/1000+" seconds");

            System.out.println();

            //LSH approach:
            
            System.out.println("Calculating using the LSH approach...");
            long lshStart = System.currentTimeMillis();
            double lshEstimate = implementLSH();
            long lshEnd = System.currentTimeMillis();
            System.out.println("The average of averages using the LSH force approach is: ");
            System.out.println(lshEstimate);
            System.out.println("The number of times we had to choose a random neighbor is: ");
            System.out.println(gotRandom);
            long lshDelta = lshEnd - lshStart;
            System.out.println("Took: "+lshDelta/1000+" seconds");

        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }//end of main
}//end of class