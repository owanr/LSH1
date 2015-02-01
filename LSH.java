// import java.io.File;
// import java.io.FileNotFoundException;
// import java.io.IOException;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/*
** LSH Assignment Part 1, due 1/17
** Written by Brynna Mering and Risako Owan
**
*/


public class LSH {
    //Given by user
    private static int numOfHashFunc;
    private static int numOfDocs;

    //Our program is currently built for only two documents (Part 1)
    private static int compareTwo = 2;
    private static int numOfWords;
    private static ArrayList<HashSet<String>> setArrayList;
    private static HashSet<String> set1;
    private static HashSet<String> set2;



    /*
    ** Takes the file as input and creates an array of HashSets out of it.
    */
    private static void copyFileToSets(File data) {
        
        setArrayList = new ArrayList<HashSet<String>>();

        try {

            Scanner scanner = new Scanner(data);

            scanner.nextLine(); //number of total docs
            numOfWords = Integer.parseInt(scanner.nextLine());
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
    ** Takes the union of two document sets as input and creates signature matrix
    ** to be used for estimation.
    */
    private static int[][] produceSignatureMatrix(HashSet<String> union) {
        
        //Creates matrix filled with infinities
        int[][] signatureMatrix = new int[numOfHashFunc][compareTwo];
        for (int i = 0; i < numOfHashFunc; i++) {
            for (int j = 0; j < compareTwo; j++) { //compareTwo = two
                signatureMatrix[i][j] = Integer.MAX_VALUE;
            }
        }

        //Creates hash function variables
        Random generator = new Random(3);
        int[] bList = new int[numOfHashFunc];
        int[] aList = new int[numOfHashFunc];

        for (int i = 0; i < numOfHashFunc; i++) {
            int a = -1;
            while (a < 0) {
                a = generator.nextInt();
            }
            aList[i] = a;
        }
        
        Random bGenerator = new Random(6);
        for (int i = 0; i < numOfHashFunc; i++) {
            int b = -1;
            while (b < 0) {
                b = bGenerator.nextInt();
            }
            bList[i] = b;
        }

        //Adds word numbers to the sets' respective lists to keep track of where the ones are.
        int row = 0;
        List<Integer> set1RowNumList = new ArrayList<Integer>();
        List<Integer> set2RowNumList = new ArrayList<Integer>();
        for (String word : union) {
            if (set1.contains(word)) {
                set1RowNumList.add(row);
            }
            if (set2.contains(word)) {
                set2RowNumList.add(row);
            }
            row++;
        }

        //Using row numbers, creates hash function values
        //Fills in signature matrix in the same loop.
        for (int i = 0; i < numOfHashFunc; i++) {
            
            //Work on set1 first
            for (Integer rowNum : set1RowNumList) {
                int hash1 = ((numOfHashFunc * aList[i] + 1) * rowNum + bList[i])%numOfHashFunc;
                
                //Puts into matrix if smaller
                //Index 0 for set1
                if (hash1 < signatureMatrix[i][0]) {
                    signatureMatrix[i][0] = hash1;
                }
            }

            //Works on set2
            for (Integer rowNum2 : set2RowNumList) {
                int hash2 = ((numOfHashFunc * aList[i] + 1) * rowNum2 + bList[i])%numOfHashFunc;
                
                //Puts into matrix if smaller
                //Index 1 for set2
                if (hash2 < signatureMatrix[i][1]) {
                    signatureMatrix[i][1] = hash2;
                }
            }
        }
        
        return signatureMatrix;
    }

    /*
    ** Takes two document IDs and creates an intersection set.
    */
    private static HashSet<String> getIntersection(int docID1, int docID2) {
        
        HashSet<String> intersection = new HashSet<String>();
        set1 = setArrayList.get(docID1);
        set2 = setArrayList.get(docID2);
        intersection.addAll(set1);
        intersection.retainAll(set2);
        return intersection;
    }

    /*
    ** Takes two document IDs and creates an union set.
    */
    private static HashSet<String> getUnion(int docID1, int docID2) {
        HashSet<String> union = new HashSet<String>();
        set1 = setArrayList.get(docID1);
        set2 = setArrayList.get(docID2);
        union.addAll(set1);
        union.addAll(set2);
        return union;
    }

    /*
    ** Takes two document IDs, calculates, and returns the exact Jaccard similarity.
    */
    private static double calculateJaccardSim(int docID1, int docID2) {
        
        HashSet<String> union = getUnion(docID1, docID2);
        HashSet<String> intersection = getIntersection(docID1, docID2);
        
        return (double)intersection.size()/(double)union.size();
    }     

    /*
    ** Takes the signature matrix and returns the estimated Jaccard similarity.
    */
    private static double estimateJaccardSim(int[][] signatureMatrix) {
        double estimate = 0.0;

        //Estimates from matrix int[numDocs][numOfHashFunc]
        for (int i = 0; i < numOfHashFunc; i++) {
            if (signatureMatrix[i][0] == signatureMatrix[i][1]) {
                estimate = estimate + 1.0;
            }
        }

        return estimate/numOfHashFunc;
    } 

    public static void main(String[] args) {
        
        //LOCATION OF FILE HERE
        File file = new File("docword.enron.txt");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {

            //Prompts user for program details.
            System.out.println("Enter number of documents for the program.");
            numOfDocs = Integer.parseInt(reader.readLine()) - 1;

            copyFileToSets(file);

            System.out.println("Enter ID for document #1.");
            int givenDocID1 = Integer.parseInt(reader.readLine());

            System.out.println("Enter ID for document #2");
            int givenDocID2 = Integer.parseInt(reader.readLine());

            int docID1 = givenDocID1 - 1;
            int docID2 = givenDocID2 - 1;

            double cJ = calculateJaccardSim(docID1, docID2);

            //Prints out exact Jaccard similarity
            System.out.println("Exact Jaccard similarity is: " + cJ);

            System.out.println("Enter number of rows to use for the signature matrix (hash functions).");
            numOfHashFunc = Integer.parseInt(reader.readLine());

            HashSet<String> union = getUnion(docID1, docID2);
            int[][] sigMatrix = produceSignatureMatrix(union);

            double eJ = estimateJaccardSim(sigMatrix);

            //Prints out estimate Jaccard similarity
            System.out.println("Estimate Jaccard similarity is: " + eJ);

        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }//end of main
}//end of class