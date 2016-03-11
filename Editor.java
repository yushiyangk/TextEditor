package editor;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class Editor extends Application {
    private static double WINDOW_WIDTH = 500;
    private static double WINDOW_HEIGHT = 500;
    private static double MARGIN = 5;
    private static double sbWidth;
    protected static Group root; //main root
    private final Rectangle cursor; //cursor object
    private int fontSize = 12;
    private String fontName = "Verdana";
    private static double xPos; //cursor x position
    private static double yPos; //cursor y position
    private static TextLinkedList<Text> wordlist = new TextLinkedList<Text>(); //raw text linkedlist
    private static ArrayList<Nodee> renderedtext = new ArrayList<>(); //arraylist of rendered text
    private static String filename;
    private static Stack<ArrayList<Object>> undoStack = new Stack<>();
    private static Stack<ArrayList<Object>> redoStack = new Stack<>();

    public Editor() {
        cursor = new Rectangle(1, 12); //creates new cursor object of 1 px, (font height)px height
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected usage: Editor <source filename>");
            System.exit(1);
        }
        launch(args);
    }

    protected class MouseClickEventHandler implements EventHandler<MouseEvent> {
        /** A Text object that will be used to print the current mouse position. */
        Text positionText;

        MouseClickEventHandler(Group root) {
            positionText = new Text("");
            positionText.setTextOrigin(VPos.BOTTOM);
            root.getChildren().add(positionText);
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            double mousePressedX = mouseEvent.getX();
            double mousePressedY = mouseEvent.getY();

            Text y = new Text("y");
            y.setFont(Font.font(fontName, fontSize));
            double charHeight = y.getLayoutBounds().getHeight();
            Integer index = (int) ((mousePressedY / charHeight) - ((mousePressedY / charHeight) % 1));
            if (index >= renderedtext.size()) {
                index = renderedtext.size() - 1;
            }
            if (index < 0) {
                index = 0;
            }
            if (mousePressedX > WINDOW_WIDTH - MARGIN) {
                TextLinkedList<Text>.Node currCharNode = renderedtext.get(index).next;
                for (int i = 0; i < renderedtext.get(index).count; i++) {
                    currCharNode = currCharNode.next;
                }
                wordlist.cursor.previous = currCharNode;
                wordlist.cursor.next = currCharNode.next;
                UpdateCursor(currCharNode.item.getX() + currCharNode.item.getLayoutBounds().getWidth(),
                        currCharNode.item.getY());
            }
            else if (mousePressedX < 5) {
                TextLinkedList<Text>.Node currCharNode = renderedtext.get(index).next;
                wordlist.cursor.next = currCharNode;
                wordlist.cursor.previous = currCharNode.previous;
                UpdateCursor(5.0, currCharNode.item.getY());
            }
            else {
                TextLinkedList<Text>.Node nextCharNode = renderedtext.get(index).next;
                double currCharXPos = 0.0;
                double nextCharXPos = nextCharNode.item.getX();
                //find the character with x position just before the cursor's x position
                while (nextCharNode.item != null && nextCharXPos < mousePressedX && nextCharXPos >= currCharXPos
                        && nextCharNode.next != null && nextCharNode.next != wordlist.sentinelB) {
                    //updating prev and curr Char stuff;
                    currCharXPos = nextCharXPos; //update curr char xPos
                    nextCharNode = nextCharNode.next; //update node to the next
                    nextCharXPos = nextCharNode.item.getX(); //update next char xPos
                }
                TextLinkedList<Text>.Node currCharNode = nextCharNode.previous;
                double diffCurr = Math.abs(mousePressedX - currCharXPos);
                double diffNext = Math.abs(mousePressedX - nextCharXPos);
                if (Math.min(diffCurr, diffNext) == diffCurr) {
                    wordlist.cursor.previous = currCharNode.previous;
                    wordlist.cursor.next = currCharNode;
                    UpdateCursor(currCharNode.item.getX(), currCharNode.item.getY());
                } else {
                    wordlist.cursor.previous = currCharNode;
                    wordlist.cursor.next = nextCharNode;
                    UpdateCursor(nextCharNode.item.getX(), nextCharNode.item.getY());
                }
                positionText.setText("(" + mousePressedX + ", " + mousePressedY + ")");
                positionText.setX(mousePressedX);
                positionText.setY(mousePressedY);
            }
        }
    }

    protected class KeyEventHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(KeyEvent keyEvent) {
            //key_typed event, not shortcut, character length > 0, not a backspace
            if (keyEvent.getEventType() == KeyEvent.KEY_TYPED && !keyEvent.isShortcutDown() &&
                    keyEvent.getCharacter().length() > 0 && keyEvent.getCharacter().charAt(0) != 8) {
                Text charTyped = new Text(keyEvent.getCharacter()); //keeps track of character that is typed
                if (wordlist.cursor.next == wordlist.sentinelB) {
                    wordlist.addLast(charTyped);
                }
                else if (wordlist.cursor.previous == wordlist.sentinelA) {
                    wordlist.addFirst(charTyped);
                }
                else {
                    TextLinkedList<Text>.Node oldLeft = wordlist.cursor.previous;
                    TextLinkedList<Text>.Node oldRight = wordlist.cursor.next;
                    TextLinkedList<Text>.Node newLeft = wordlist.new Node(oldLeft, charTyped, oldRight);
                    oldLeft.next = newLeft;
                    oldRight.previous = newLeft;
                    wordlist.cursor.previous = newLeft;
                    wordlist.size += 1;
                }
                ArrayList<Object> charInfo = new ArrayList<Object>();
                charInfo.add("-"); //signifies addition
                charInfo.add(wordlist.cursor.previous); //add the node that was just added
                undoStack.push(charInfo); //push this info to the undo stack
                root.getChildren().add(charTyped); //add text object to the root
                charTyped.setTextOrigin(VPos.TOP); //normalises character's position
                renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                        WINDOW_WIDTH - sbWidth, fontName, fontSize);
                //re-renders the text & updates cursor position
                double x;
                double y;
                x = wordlist.cursor.previous.item.getLayoutBounds().getWidth() + wordlist.cursor.previous.item.getX();
                y = wordlist.cursor.previous.item.getY();
                UpdateCursor(x, y);
                keyEvent.consume();
            }
            //key_pressed event
            else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                KeyCode code = keyEvent.getCode(); //associated keycode
                //short cut
                if (keyEvent.isShortcutDown()) {
                    //increase font size
                    if (code == KeyCode.PLUS || code == KeyCode.EQUALS) {
                        fontSize += 4; //increase font size by 4
                        renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                                WINDOW_WIDTH - sbWidth, fontName, fontSize);
                        //re-renders the text & updates cursor position
                        double x;
                        double y;
                        double h;
                        Text txt = new Text("t");
                        txt.setFont(Font.font(fontName, fontSize));
                        h = txt.getLayoutBounds().getHeight();
                        if (wordlist.cursor.previous == wordlist.sentinelA) {
                            x = 5.0;
                            y = 0.0;
                        }
                        else {
                            x = wordlist.cursor.previous.item.getLayoutBounds().getWidth() + wordlist.cursor.previous.item.getX();
                            y = wordlist.cursor.previous.item.getY();
                        }
                        UpdateCursor(h, x, y);
                        keyEvent.consume();
                    }
                    //decrease font size
                    else if (code == KeyCode.MINUS && fontSize > 4) {
                        fontSize = Math.max(0, fontSize - 4); //decrease font size by 4 unless it results to 0
                        renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                                WINDOW_WIDTH - sbWidth, fontName, fontSize);
                        //re-renders the text & updates cursor position
                        double x;
                        double y;
                        double h;
                        Text txt = new Text("t");
                        txt.setFont(Font.font(fontName, fontSize));
                        h = txt.getLayoutBounds().getHeight();
                        if (wordlist.cursor.previous == wordlist.sentinelA) {
                            x = 5.0;
                            y = 0.0;
                        }
                        else {
                            x = wordlist.cursor.previous.item.getLayoutBounds().getWidth() + wordlist.cursor.previous.item.getX();
                            y = wordlist.cursor.previous.item.getY();
                        }
                        UpdateCursor(h, x, y);
                        keyEvent.consume();
                    }
                    //save file
                    else if (code == KeyCode.S) {
                        try {
                            FileWriter writer = new FileWriter(filename);
                            TextLinkedList<Text>.Node current = wordlist.sentinelA.next;
                            //node points to first element in wordlist
                            for (int i = 0; i < wordlist.size(); i++) {
                                String charRead = current.item.getText();
                                writer.write(charRead);
                                current = current.next;
                            }
                            writer.close();
                        } catch (IOException ioException) {
                            System.out.println(ioException);
                        }
                    }
                    //print coordinates of cursor
                    else if (code == KeyCode.P) {
                        Double xd = xPos;
                        Double yd = yPos;
                        Integer xi = xd.intValue(); //convert double to integer
                        Integer yi = yd.intValue(); //convert double to integer
                        System.out.println(xi + ", " + yi);
                    }
                    //undo
                    else if (code == KeyCode.Z && undoStack.size() > 0) {
                        double x;
                        double y;
                        redoStack.push(undoStack.pop());
                        Object action = redoStack.peek().get(0);
                        TextLinkedList<Text>.Node noode = (TextLinkedList<Text>.Node) redoStack.peek().get(1);
                        if (action.equals("+")) {
                            noode.previous.next = noode;
                            noode.next.previous = noode;
                            redoStack.peek().remove(0);
                            redoStack.peek().add(0, "-");
                            wordlist.size += 1;
                            Text charAdded = noode.item;
                            root.getChildren().add(charAdded);
                            renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                                    WINDOW_WIDTH - sbWidth, fontName, fontSize);
                            x = charAdded.getX() + charAdded.getLayoutBounds().getWidth();
                            y = charAdded.getY();
                            UpdateCursor(x, y);
                        }
                        else {
                            //remove it from the list
                            noode.previous.next = noode.next;
                            noode.next.previous = noode.previous;
                            redoStack.peek().remove(0);
                            redoStack.peek().add(0, "+");
                            wordlist.size -= 1;
                            Text charDeleted = noode.item;
                            root.getChildren().remove(charDeleted);
                            renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                                    WINDOW_WIDTH - sbWidth, fontName, fontSize);
                            if (noode.previous == wordlist.sentinelA) {
                                x = 5.0;
                                y = 0.0;
                            }
                            else {
                                Text curr = noode.previous.item;
                                x = curr.getX() + curr.getLayoutBounds().getWidth();
                                y = curr.getY();
                            }
                            UpdateCursor(x, y);
                        }
                        if (redoStack.size() > 100) {
                            redoStack.remove(100);
                        }

                    }
                    //redo
                    else if (code == KeyCode.Y && redoStack.size() > 0) {
                        double x;
                        double y;
                        undoStack.push(redoStack.pop());
                        Object action = undoStack.peek().get(0);
                        TextLinkedList<Text>.Node noode = (TextLinkedList<Text>.Node) undoStack.peek().get(1);
                        if (action.equals("+")) {
                            noode.previous.next = noode;
                            noode.next.previous = noode;
                            undoStack.peek().remove(0);
                            undoStack.peek().add(0, "-");
                            wordlist.size += 1;
                            Text charAdded = noode.item;
                            root.getChildren().add(charAdded);
                            renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                                    WINDOW_WIDTH - sbWidth, fontName, fontSize);
                            x = charAdded.getX() + charAdded.getLayoutBounds().getWidth();
                            y = charAdded.getY();
                            UpdateCursor(x, y);
                        }
                        else {
                            //remove it from the list
                            noode.previous.next = noode.next;
                            noode.next.previous = noode.previous;
                            undoStack.peek().remove(0);
                            undoStack.peek().add(0, "+");
                            wordlist.size -= 1;
                            Text charDeleted = noode.item;
                            root.getChildren().remove(charDeleted);
                            renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                                    WINDOW_WIDTH - sbWidth, fontName, fontSize);
                            if (noode.previous == wordlist.sentinelA) {
                                x = 5.0;
                                y = 0.0;
                            }
                            else {
                                Text curr = noode.previous.item;
                                x = curr.getX() + curr.getLayoutBounds().getWidth();
                                y = curr.getY();
                            }
                            UpdateCursor(x, y);
                        }
                        if (undoStack.size() > 100) {
                            undoStack.remove(100);
                        }
                        renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                                WINDOW_WIDTH - sbWidth, fontName, fontSize);
                        UpdateCursor(x, y);
                    }
                }
                //back space
                else if (wordlist.size() > 0 && code == KeyCode.BACK_SPACE &&
                        wordlist.cursor.previous != wordlist.sentinelA) {
                    Text charDeleted;
                    if (wordlist.cursor.next == wordlist.sentinelB) {
                        charDeleted = wordlist.removeLast();
                        ArrayList<Object> charInfo = new ArrayList<Object>();
                        charInfo.add("+"); //signifies deletion
                        charInfo.add(charDeleted); //add the node that was just removed
                        redoStack.push(charInfo); //add info to the redo stack
                    }
                    else {
                        charDeleted = wordlist.cursor.previous.item;
                        TextLinkedList<Text>.Node nodeToDelete = wordlist.cursor.previous;
                        nodeToDelete.previous.next = wordlist.cursor.next;
                        wordlist.cursor.next.previous = nodeToDelete.previous;
                        wordlist.cursor.previous = nodeToDelete.previous;
                        wordlist.size -= 1;
                        ArrayList<Object> charInfo = new ArrayList<Object>();
                        charInfo.add("+"); //signifies deletion
                        charInfo.add(nodeToDelete); //add the node that was just removed
                        redoStack.push(charInfo); //add info to the redo stack
                    }

                    root.getChildren().remove(charDeleted); //remove last character from raw text
                    renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                            WINDOW_WIDTH - sbWidth, fontName, fontSize);
                    //re-renders the text & updates cursor position
                    double x;
                    double y;
                    if (wordlist.cursor.previous == wordlist.sentinelA) {
                        x = 5.0;
                        y = 0.0;
                    }
                    else {
                        x = wordlist.cursor.previous.item.getLayoutBounds().getWidth() + wordlist.cursor.previous.item.getX();
                        y = wordlist.cursor.previous.item.getY();
                    }
                    UpdateCursor(x, y);
                    keyEvent.consume();
                }
                //left arrow key
                else if (code == KeyCode.LEFT && wordlist.cursor.previous != wordlist.sentinelA) {
                    //if we are not at the end of the raw text, move cursor one character left
                    //we know that cursor.previous is not null
                    wordlist.cursor.next = wordlist.cursor.previous;
                    if (wordlist.cursor.previous.previous != wordlist.sentinelA) {
                        wordlist.cursor.previous = wordlist.cursor.previous.previous;
                    }
                    else {
                        wordlist.cursor.previous = wordlist.sentinelA;
                    }
                    double x;
                    double y;
                    if (wordlist.cursor.previous == wordlist.sentinelA){
                        x = 5.0;
                        y = 0.0;
                    }
                    else {
                        x = wordlist.cursor.previous.item.getLayoutBounds().getWidth() + wordlist.cursor.previous.item.getX();
                        y = wordlist.cursor.previous.item.getY();
                    }
                    //cursor should always be displayed at the back of current cursor.previous
                    UpdateCursor(x, y); //update cursor
                }
                //right arrow key
                else if (code == KeyCode.RIGHT && wordlist.cursor.next != wordlist.sentinelB) {
                    //if we are not at the beginning of the raw text, move cursor one character right
                    wordlist.cursor.previous = wordlist.cursor.next;
                    if (wordlist.cursor.next.next != wordlist.sentinelB) {
                        wordlist.cursor.next = wordlist.cursor.next.next;
                    }
                    else {
                        wordlist.cursor.next = wordlist.sentinelB;
                    }
                    double x;
                    double y;
                    x = wordlist.cursor.previous.item.getLayoutBounds().getWidth() + wordlist.cursor.previous.item.getX();
                    y = wordlist.cursor.previous.item.getY();
                    //you always want the cursor to be displayed at the end of the character traversed
                    UpdateCursor(x, y); //update cursor
                }
                //down arrow key
                else if (code == KeyCode.DOWN && renderedtext.size() > 0) {
                    Text y = new Text("y");
                    y.setFont(Font.font(fontName, fontSize));
                    double charHeight = y.getLayoutBounds().getHeight();
                    Double ind = cursor.getY() / charHeight;
                    Integer index = ind.intValue();
                    //ensure that we are not at the last line yet
                    if (index + 1 < renderedtext.size()) {
                        TextLinkedList<Text>.Node nextCharNode = renderedtext.get(index + 1).next;
                        double currCharXPos = 0.0;
                        double nextCharXPos = nextCharNode.item.getX();
                        //find the character with x position just before the cursor's x position
                        while (nextCharXPos < cursor.getX() && nextCharXPos >= currCharXPos
                                && nextCharNode.next != null && nextCharNode.next != wordlist.sentinelB) {
                            //updating prev and curr Char stuff;
                            currCharXPos = nextCharXPos; //update curr char xPos
                            nextCharNode = nextCharNode.next; //update node to the next
                            nextCharXPos = nextCharNode.item.getX(); //update next char xPos
                        }
                        TextLinkedList<Text>.Node currCharNode = nextCharNode.previous;
                        double diffCurr = Math.abs(cursor.getX() - currCharXPos);
                        double diffNext = Math.abs(cursor.getX() - nextCharXPos);
                        if (Math.min(diffCurr, diffNext) == diffCurr) {
                            wordlist.cursor.previous = currCharNode.previous;
                            wordlist.cursor.next = currCharNode;
                            UpdateCursor(currCharNode.item.getX(), currCharNode.item.getY());
                        }
                        else {
                            wordlist.cursor.previous = currCharNode;
                            wordlist.cursor.next = nextCharNode;
                            UpdateCursor(nextCharNode.item.getX(), nextCharNode.item.getY());
                        }
                        if (wordlist.cursor.next.next.item.getText().equals("\r")) {
                            wordlist.cursor.previous = wordlist.cursor.next;
                            wordlist.cursor.next = wordlist.cursor.next.next;
                            UpdateCursor(wordlist.cursor.previous.item.getX() +
                                    wordlist.cursor.previous.item.getLayoutBounds().getWidth(), wordlist.cursor.previous.item.getY());
                        }
                    }
                }
                //up arrow key
                else if (code == KeyCode.UP && renderedtext.size() > 0) {
                    Text y = new Text("y");
                    y.setFont(Font.font(fontName, fontSize));
                    double charHeight = y.getLayoutBounds().getHeight();
                    Double ind = cursor.getY() / charHeight;
                    Integer index = ind.intValue();
                    //ensure that we are not at the top line yet
                    if (index > 0) {
                        TextLinkedList<Text>.Node nextCharNode = renderedtext.get(index - 1).next;
                        double currCharXPos = 0.0;
                        double nextCharXPos = nextCharNode.item.getX();
                        //find the character with x position just before the cursor's x position
                        while (nextCharXPos < cursor.getX() && nextCharXPos >= currCharXPos && nextCharNode.next != null) {
                            //updating prev and curr Char stuff;
                            currCharXPos = nextCharXPos; //update curr char xPos
                            nextCharNode = nextCharNode.next; //update node to the next
                            nextCharXPos = nextCharNode.item.getX(); //update next char xPos
                        }
                        TextLinkedList<Text>.Node currCharNode = nextCharNode.previous;
                        double diffCurr = Math.abs(cursor.getX() - currCharXPos);
                        double diffNext = Math.abs(cursor.getX() - nextCharXPos);
                        if (Math.min(diffCurr, diffNext) == diffCurr) {
                            wordlist.cursor.previous = currCharNode.previous;
                            wordlist.cursor.next = currCharNode;
                            UpdateCursor(currCharNode.item.getX(), currCharNode.item.getY());
                        }
                        else {
                            wordlist.cursor.previous = currCharNode;
                            wordlist.cursor.next = nextCharNode;
                            UpdateCursor(nextCharNode.item.getX(), nextCharNode.item.getY());
                        }
                        if (wordlist.cursor.next.next.item.getText().equals("\r")) {
                            wordlist.cursor.previous = wordlist.cursor.next;
                            wordlist.cursor.next = wordlist.cursor.next.next;
                            UpdateCursor(wordlist.cursor.previous.item.getX() +
                                    wordlist.cursor.previous.item.getLayoutBounds().getWidth(), wordlist.cursor.previous.item.getY());
                        }
                    }
                }
            }
        }
    }

    //updates x y coordinates of cursor
    protected void UpdateCursor(double x, double y) {
        xPos = x;
        yPos = y;
        cursor.setX(x); //sets x position
        cursor.setY(y); //sets y position
        cursor.toFront(); //Display text behind cursor.
    }
    //updates height and x y coordinates of cursor (for changing font size)
    protected void UpdateCursor(double textHeight, double x, double y) {
        xPos = x;
        yPos = y;
        cursor.setHeight(textHeight); //sets height of cursor
        cursor.setX(x); //sets x position
        cursor.setY(y); //sets y position
        cursor.toFront(); //Display text behind cursor.
    }

    protected class CursorBlinkEventHandler implements EventHandler<ActionEvent> {
        private int currentColorIndex = 0;
        private Color[] boxColors = {Color.BLACK, Color.WHITE};

        CursorBlinkEventHandler() {
            changeColor(); //Set the color to be black.
        }
        private void changeColor() {
            cursor.setFill(boxColors[currentColorIndex]);
            currentColorIndex = (currentColorIndex + 1) % boxColors.length;
        }
        @Override
        public void handle(ActionEvent event) {
            changeColor();
        }
    }

    public void makeCursorColorChange() {
        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        CursorBlinkEventHandler cursorChange = new CursorBlinkEventHandler();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(1), cursorChange);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        root = new Group();
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT, Color.WHITE);

        List<String> parameters = getParameters().getUnnamed();
        filename = parameters.get(0);
        File inputFile = new File(filename);
        OpenFile(inputFile);
        EventHandler<KeyEvent> keyEventHandler = new KeyEventHandler();
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);

        //scroll bar
        ScrollBar scrollBar = new ScrollBar();
        sbWidth = scrollBar.getWidth();
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setPrefHeight(WINDOW_HEIGHT);
        scrollBar.setVisibleAmount(WINDOW_HEIGHT);
        scrollBar.setLayoutX(WINDOW_WIDTH-scrollBar.getWidth() + 3.0);
        scrollBar.setVisibleAmount(1.0);
        scrollBar.setMin(0);
        scrollBar.setMax(0);
        root.getChildren().add(scrollBar);

        // All new Nodes need to be added to the root in order to be displayed.
        root.getChildren().add(cursor);
        cursor.setX(5.0);
        cursor.setY(0.0);
        makeCursorColorChange();
        scene.setOnMouseClicked(new MouseClickEventHandler(root));
        primaryStage.setTitle(filename);

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                // Re-compute Allen's width.
                scrollBar.setLayoutX((double) newScreenWidth - scrollBar.getWidth());
                WINDOW_WIDTH = (double) newScreenWidth;
                renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                        WINDOW_WIDTH - sbWidth, fontName, fontSize);
                double x;
                double y;
                if (wordlist.cursor.previous == wordlist.sentinelA) {
                    x = 5.0;
                    y = 0.0;
                }
                else {
                    x = wordlist.cursor.previous.item.getLayoutBounds().getWidth() + wordlist.cursor.previous.item.getX();
                    y = wordlist.cursor.previous.item.getY();
                }
                UpdateCursor(x, y);
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                double newScrollHeight = (double) newScreenHeight;
                scrollBar.setPrefHeight(newScrollHeight);
                WINDOW_HEIGHT = newScrollHeight;
            }
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void OpenFile(File lol) {

        try {
            if (!lol.exists()) {
                return;
            }
            FileReader reader = new FileReader(lol);
            BufferedReader bufferedReader = new BufferedReader(reader);
            int intRead = -1;
            while ((intRead = bufferedReader.read()) != -1) {
                // The integer read can be cast to a char, because we're assuming ASCII.
                char charRead = (char) intRead;
                String toRead = Character.toString(charRead);
                Text justRead = new Text(toRead);
                wordlist.addLast(justRead); //add texts to the wordlist
                root.getChildren().add(justRead); //add text object to the root
                justRead.setTextOrigin(VPos.TOP); //normalises character's position
                renderedtext = OrganiseIntoArrayList.OrganiseIntoArrayList(wordlist,
                        WINDOW_WIDTH - sbWidth, fontName, fontSize);
            }
            wordlist.cursor.previous = wordlist.sentinelA;
            wordlist.cursor.next = wordlist.sentinelA.next;
            bufferedReader.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
    }
}