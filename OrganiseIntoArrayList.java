package editor;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.ArrayList;

public class OrganiseIntoArrayList {


    public static ArrayList<Nodee> OrganiseIntoArrayList(TextLinkedList<Text> raw_text, double ww, String fName, double fSize) {
        ArrayList<Nodee> renderedTextNodes = new ArrayList<>();
        Double xPosNext = 5.0;
        Double yPosNext = 0.0;
        TextLinkedList<Text>.Node rt = raw_text.sentinelA.next; //create another node
        int currentIndex = 0;
        if (raw_text.size() == 0) {

        }
        else {
            Text y = new Text("y");
            y.setFont(Font.font(fName, fSize));
            double charHeight = y.getLayoutBounds().getHeight();
            Nodee firstInLine = new Nodee(null, 0); //creates a new blank node
            renderedTextNodes.add(firstInLine); //add the blank node to the rendered arraylist
            while (currentIndex < raw_text.size()) {
                //while a whole word can be accommodated in a line
                if (getWordWidth(rt).size() > 0 && xPosNext + getWordWidth(rt).get(1) <= ww - 5) {
                    int charCount = getWordWidth(rt).get(2).intValue();
                    //enter keys
                    if (getWordWidth(rt).get(0) == 1.0) {
                        for (int i = 0; i < charCount; i ++) {
                            Nodee currNode = renderedTextNodes.get(renderedTextNodes.size() - 1);
                            if (currNode.next == null) {
                                currNode.next = rt;
                            }
                            firstInLine.count += 1;
                            xPosNext = 5.0;
                            yPosNext += charHeight; //increase yPos
                            rt.item.setFont(Font.font(fName, fSize));
                            rt.item.setX(xPosNext);
                            rt.item.setY(yPosNext);
                            firstInLine = new Nodee(null, 0);
                            renderedTextNodes.add(firstInLine);
                            currentIndex += 1;
                            rt = rt.next; //move to next character in linkedlist
                        }
                    }
                    else {
                        for (int x = 0; x < charCount; x++) {
                            Nodee currNode = renderedTextNodes.get(renderedTextNodes.size() - 1);
                            if (currNode.next == null) {
                                currNode.next = rt;
                            }
                            firstInLine.count += 1;
                            rt.item.setFont(Font.font(fName, fSize));
                            rt.item.setX(xPosNext);
                            rt.item.setY(yPosNext);
                            xPosNext += rt.item.getLayoutBounds().getWidth();
                            currentIndex += 1;
                            rt = rt.next;
                        }
                    }
                }
                //if a word is longer than the width
                else if (getWordWidth(rt).size() > 0 && getWordWidth(rt).get(0) == 2.0 && getWordWidth(rt).get(1) > ww - 10) {
                    int charCount = getWordWidth(rt).get(2).intValue();
                    for (int i = 0; i < charCount; i++) {
                        if (xPosNext + rt.item.getLayoutBounds().getWidth() <= ww - 5) {
                            Nodee currNode = renderedTextNodes.get(renderedTextNodes.size() - 1);
                            if (currNode.next == null) {
                                currNode.next = rt;
                            }
                            firstInLine.count += 1;
                            rt.item.setFont(Font.font(fName, fSize));
                            rt.item.setX(xPosNext);
                            rt.item.setY(yPosNext);
                            xPosNext += rt.item.getLayoutBounds().getWidth();
                            currentIndex += 1;
                            rt = rt.next;
                        }
                        else {
                            firstInLine = new Nodee(rt, 1);
                            renderedTextNodes.add(firstInLine);
                            xPosNext = 5.0;
                            yPosNext += charHeight;
                            rt.item.setFont(Font.font(fName, fSize));
                            rt.item.setX(xPosNext);
                            rt.item.setY(yPosNext);
                            xPosNext += rt.item.getLayoutBounds().getWidth();
                            currentIndex += 1;
                            rt = rt.next;
                        }
                    }
                }
                //if a word cannot fit into a line
                else if (getWordWidth(rt).size() > 0 && xPosNext + getWordWidth(rt).get(1) > ww - 5) {
                    //check if it is spaces
                    if (getWordWidth(rt).get(0) == 0.0) {
                        int charCount = getWordWidth(rt).get(2).intValue();
                        for (int i = 0; i < charCount; i++) {
                            //while each space still fits into the line
                            if (xPosNext + rt.item.getLayoutBounds().getWidth() <= ww - 5) {
                                rt.item.setFont(Font.font(fName, fSize));
                                rt.item.setX(xPosNext);
                                rt.item.setY(yPosNext);
                                xPosNext += rt.item.getLayoutBounds().getWidth();
                                currentIndex += 1;
                                rt = rt.next;
                            }
                            else {
                                rt.item.setFont(Font.font(fName, fSize));
                                rt.item.setX(xPosNext);
                                rt.item.setY(yPosNext);
                                currentIndex += 1;
                                rt = rt.next;
                            }
                        }
                        firstInLine.count += 1;
                    }
                    yPosNext += charHeight;
                    xPosNext = 5.0;
                }
            }
        }
        return renderedTextNodes;
    }

    public static ArrayList<Double> getWordWidth(TextLinkedList<Text>.Node original) {
        ArrayList<Double> results = new ArrayList<>();
        TextLinkedList<Text>.Node copy = original; //create a copy of the node
        Double word_width = 0.0; //keeps track of the width of the word
        Double char_count = 0.0; //keeps track of number of characters in a word
        //if item is a space
        if (copy.item != null && copy.item.getText().equals(" ")) {
            //while item is a space, lump the spaces into a word
            while (copy.item != null && copy.item.getText().equals(" ")) {
                word_width += copy.item.getLayoutBounds().getWidth(); //add width of the space to the word width
                char_count += 1.0;
                copy = copy.next;

            }
            results.add(0.0); //spaces are represented by 0
            results.add(word_width);
            results.add(char_count);
        }
        //if item is an enter key
        else if (copy.item != null && copy.item.getText().equals("\r")) {
            while (copy.item != null && copy.item.getText().equals("\r")) {
                char_count += 1;
                copy = copy.next;
            }
            results.add(1.0); //enter keys are represented by 1
            results.add(word_width);
            results.add(char_count);
        }
        //if item is a character
        else if (copy.item != null) {
            //while item is a character, group it together to form a word
            while (copy.item != null && !copy.item.getText().equals(" ") && !copy.item.getText().equals("\r")) {
                word_width += copy.item.getLayoutBounds().getWidth(); //add width of the space to the word width
                char_count += 1;
                copy = copy.next;
            }
            results.add(2.0); //words are represented by 2
            results.add(word_width);
            results.add(char_count);
        }
        return results; //an arraylist of space/word, width, character count
    }
}