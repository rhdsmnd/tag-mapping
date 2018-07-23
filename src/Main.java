import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        int numTagged = 0;
        int lineNum  = 1;

        CmdParameters initData = CmdParameters.readCommand(args);

        try {
            String dir = initData.getDir();
            
            File inpFile = new File(dir + initData.getInputFile());
            File outFile = new File(dir + initData.getOutputFile());

            FileWriter outFileWriter = new FileWriter(outFile);

            Scanner s = new Scanner(inpFile);

            boolean hasTagColumn = false;
            int headerColumns = 0;

            while (s.hasNextLine()) {
                String line = s.nextLine();
                String[] values = line.split(",");
                String outLine = "";
                for (int i = 0; i < values.length; i += 1) {
                    outLine += values[i] + ",";
                }


                if (lineNum == 1) {
                    headerColumns = values.length;
                    if (values[values.length - 1].equalsIgnoreCase("tag")) {
                        outLine = outLine.substring(0, outLine.length() - 1);
                        hasTagColumn = true;
                    } else {
                        outLine += "tag";
                    }
                } else {
                    String desc = values[2].toUpperCase();
                    HashMap<String, String> tagMapping = initData.getTagMapping();
                    for (String txPrefix : initData.getTagMapping().keySet()) {
                        if (desc.toUpperCase().startsWith(txPrefix.toUpperCase())) {
                            numTagged += 1;
                            outLine = insertTag (headerColumns, values.length, hasTagColumn, outLine,tagMapping.get(txPrefix));
                            break;
                        }
                    }
                }
                outFileWriter.write(outLine + "\n");
                lineNum += 1;
            }
            outFileWriter.close();
            s.close();
        } catch(Exception e) {
            System.err.println("Exception processing transactions file: " + e);
        }

        System.out.println(numTagged + " / " + (lineNum - 1) + " tagged.");
    }

    public static String insertTag(int headerColumns, int colsInLine, boolean hasTagColumn, String inp, String tag) {
        if (hasTagColumn && headerColumns == colsInLine) {
            return inp.substring(0, inp.length() - 1);
        } else {
            return inp + tag;
        }
    }

    public static class CmdParameters {

        public static final String TAG_MAP_OPTION = "-TAGMAPPING";
        public static final String DESCR_COL_INDEX = "-DESCRCOL";

        public static final String CMD_SYNTAX = "Command syntax: [-tagMapping mappingFile] [-descrCol columnNumber] rootDirectory  inputFile  outputFile";

        private CmdParameters(int descriptionCol, HashMap<String, String> tagMapping,
                              String directory, String inpFile, String outFile) {
            this.descrCol = descriptionCol;
            this.tagMapping = tagMapping;
            this.dir = directory;
            this.inputFile = inpFile;
            this.outputFile = outFile;
        }

        public static CmdParameters readCommand(String[] args) {
            if (args.length < 3) {
                System.err.println("There must be at least three arguments to the program.");
                handleCommandParseError();
            }

            int i = 0;

            HashMap<String, Integer> options = new HashMap<String, Integer>();
            options.put(TAG_MAP_OPTION, 0);
            options.put(DESCR_COL_INDEX, 0);

            int descriptionCol = -1;
            HashMap<String, String> tagMapping = null;
            while (options.containsKey(args[i].toUpperCase())) {
                if (args[i].equalsIgnoreCase(TAG_MAP_OPTION)) {

                    if (options.get(TAG_MAP_OPTION) == 1) {
                        System.err.println("There can only be one -tagMapping option");
                        handleCommandParseError();
                    }

                    tagMapping = initTagMapping(args, i + 1);
                    options.put(TAG_MAP_OPTION, 1);
                } else if (args[i].equalsIgnoreCase(DESCR_COL_INDEX)) {

                    if (options.get(DESCR_COL_INDEX) == 1) {
                        System.err.println("There can only be one -descrCol option");
                        handleCommandParseError();
                    }

                    descriptionCol = initDescriptionCol(args, i + 1);
                    options.put(DESCR_COL_INDEX, 1);
                }
                i += 2;
            }

            if (args.length < i + 3 ) {
                System.out.println(i + " " + args.length);
                System.err.println("There must be an input directory, input file, and output file.");
                handleCommandParseError();
            }

            descriptionCol = getDefaultIfUnset(descriptionCol);

            return new CmdParameters(descriptionCol, tagMapping, args[i], args[i + 1], args[i + 2]);
        }

        private static int getDefaultIfUnset(int descriptionCol) {
            return (descriptionCol == -1 ? 2 : descriptionCol);
        }

        private static void handleCommandParseError() {
            System.err.println(CMD_SYNTAX);
            System.exit(1);
        }

        private static HashMap<String, String> initTagMapping(String[] args, int index) {
            if (outOfBounds(args, index)) {
                return null;
            }

            File tagMappingFile = null;
            HashMap<String, String> descriptionTagMapping = new HashMap<String, String>();
            try {
                tagMappingFile = new File(args[index]);
                Scanner fileScanner = new Scanner(tagMappingFile);
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine();
                    String[] tagMapping = line.split("\\|\\|\\|\\|\\|");
                    if (tagMapping.length != 2) {
                        throw new IllegalArgumentException("Tag mapping file not formatted properly: lines should follow the pattern 'description-prefix|||||tag-name'");
                    }
                    descriptionTagMapping.put(tagMapping[0], tagMapping[1]);
                }
            } catch (FileNotFoundException | IllegalArgumentException e) {
                System.err.println("Error reading tag mapping file.  " + e);
                return null;
            }
            return descriptionTagMapping;
        }

        private static int initDescriptionCol(String[] args, int index) {
            if (outOfBounds(args, index)) {
                return -1;
            }


            try {
                int colIndex = Integer.parseInt(args[index]);
                if (colIndex >= 0) {
                    return colIndex;
                } else {
                    return -1;
                }
            } catch(IllegalArgumentException e) {
                return -1;
            }
        }

        private static boolean outOfBounds(Object[] args, int index) {
            return index >= args.length;
        }

        public HashMap<String, String> getTagMapping() {
            return tagMapping;
        }

        public int getDescrCol() {
            return descrCol;
        }

        public String getDir() {
            return dir;
        }

        public String getInputFile() {
            return inputFile;
        }

        public String getOutputFile() {
            return outputFile;
        }

        private HashMap<String, String> tagMapping;
        private int descrCol;
        private String dir;
        private String inputFile;
        private String outputFile;
    }
    /** Add in to optimize if necessary: combine with a new HashMap<String, List<String>> where list contains various categories with same 3-letter prefix
    public static class StringNewHashPrefix {

        private String value;

        public StringNewHashPrefix(String value) {
            this.setValue(value);
        }

        @Override
        public int hashCode() {
            if (value == null) {
                return 0;
            }

            int hash = 0;
            for (int i = 0; i < 3 && i < value.length(); i += 1) {
                hash += value.charAt(i) * Math.pow(26, i);
            }

            return hash;
        }


        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
     */
}
