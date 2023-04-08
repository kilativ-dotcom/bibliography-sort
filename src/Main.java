import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static String files = "files/";
    public static final String bibliographyTex = "bibliography.tex";
    public static final String bibliographyBib = "biblio.bib";

    public static final String bibliographyTexOut = "bibliography.out.tex";

    public static final String header = "header";
    public static final String shorthand = "shorthand";
    public static final String shorthandValue = "shorthandValue";
//  (?si)\\scnciteheader\{(?<header>[\w\-]+?)\Q}\E.*?\n(?=\s*\n)
    public static final String texEntryRegex = "(?si)\\\\scnciteheader\\{(?<" + header + ">[\\w\\-]+?)\\Q}\\E.*?\\n(?=\\s*\\n)";
//  (?si)@.*?\{(?<header>.+?),.*?(?<shorthand>shorthand\s*?=\s*?\{(?<shorthandValue>.*?))?\n(?=\s*@|$)
    public static final String bibEntryRegex   = "(?si)@.*?\\{(?<" + header + ">.+?),.*?(?<" + shorthand + ">shorthand\\s*?=\\s*?\\{(?<" + shorthandValue + ">.*?))?\\n(?=\\s*@|$)";
    public static final String shorthandRegex = "(?si)shorthand\\s*=\\s*\\{(?<" + shorthandValue + ">.*?)}";


    public static final String scnTemplate =
            "scnciteheader{%1$s}\n" +
            "\\scnfullcite{%1$s}\n" +
            "\\scnciteannotation{%1$s}\n" +
            "\\begin{scnreltolist}{библиографическая ссылка}\n" +
            "\t\\scnitem{\\ref{} \\nameref{}}\n" +
            "\\end{scnreltolist}\n";

    public static void main(String[] args) {
        boolean force = args.length != (args = Arrays.stream(args).filter(s -> !"-f".equalsIgnoreCase(s)).toArray(String[]::new)).length;
        if (args.length > 0) {
            files = args[0];

            try {
                System.out.println("Errors in " + bibliographyTex);
                Map<String, String> headerToScn = getHeaderToScn();
                System.out.println("amount of records in " + bibliographyTex + " is " + headerToScn.size());
                System.out.println("\n\n\nErrors in " + bibliographyBib);
                Map<String, String> headerToShorthand = getHeaderToShorthand();
                System.out.println("amount of records in " + bibliographyBib + " is " + headerToShorthand.size());
                System.out.println("\n\n\nMapping errors");
                Iterator<String> iterator = headerToScn.keySet().iterator();
                while (iterator.hasNext()) {
                    String headerContent = iterator.next();
                    if (!headerToShorthand.containsKey(headerContent)) {
                        System.out.println("tex has " + headerContent + " but bib does not have shorthand");
                        iterator.remove();
                    }
                }
                System.out.println("\n--------------------------------------\n");
                for (String headerContent : headerToShorthand.keySet()) {
                    if (!headerToScn.containsKey(headerContent)) {
//                        System.out.println("cannot find scnheader for " + headerContent);
                        if (force) {
                            headerToScn.put(headerContent, String.format(scnTemplate, headerContent));
                        }
                    }
                }
                System.out.println("\n\n\n");
                List<String> headers = new ArrayList<>(headerToScn.keySet());
//                System.out.println("before sort ");
//                headers.forEach(s -> System.out.print(headerToShorthand.get(s) + ", "));
//                System.out.println();
                headers.sort((h1, h2) -> Main.compareCyrillicFirst(headerToShorthand.get(h1), headerToShorthand.get(h2)));
//                System.out.println("after sort  ");
//                headers.forEach(s -> System.out.print(headerToShorthand.get(s) + ", "));
//                System.out.println();
                StringBuilder entiretyOfAFile = new StringBuilder();
                for (String header : headers) {
                    entiretyOfAFile.append(headerToScn.get(header)).append("\n");
                }

                Path outputFile = Path.of(files, bibliographyTexOut);
                Files.write(outputFile, entiretyOfAFile.toString().getBytes());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("pass directory with files '" + bibliographyTex + "' and '" + bibliographyBib + "' as first argument");
        }
    }

    private static Map<String, String> getHeaderToShorthand() throws IOException {
        Pattern bibEntryPattern = Pattern.compile(bibEntryRegex);
        StringBuilder entiretyOfAFile = new StringBuilder(Files.readString(Path.of(files,  bibliographyBib)));
        Matcher bibMatcher = bibEntryPattern.matcher(entiretyOfAFile);
        Map<String, String> bibEntries = new HashMap<>();
        while(bibMatcher.find()) {
            String headerContent = bibMatcher.group(header);
            if (headerContent.contains("\n")) {
                System.out.println(headerContent + " does not have ',' after header");
                headerContent = headerContent.substring(0, headerContent.indexOf("\n"));
            }
            String shorthandContent = bibMatcher.group(shorthand);
            if (shorthandContent == null || shorthandContent.isEmpty()) {
                System.out.println(headerContent + " does not have shorthand");
                continue;
            }
            if (bibEntries.containsKey(headerContent)) {
                System.out.println(headerContent + " is duplicated in bib");
                continue;
            }
            Matcher shorthandValueMatcher = Pattern.compile(shorthandRegex).matcher(shorthandContent);
            if (!shorthandValueMatcher.find()) {
                System.out.println(headerContent + " probably has shorthand without {}");
                continue;
            }
            shorthandContent = shorthandValueMatcher.group(shorthandValue);
            if (shorthandContent == null || shorthandContent.isEmpty()) {
                System.out.println(headerContent + " has empty shorthand");
                continue;
            }
            bibEntries.put(headerContent, shorthandContent);
        }
        return bibEntries;
    }

    private static Map<String, String> getHeaderToScn() throws IOException {
        Pattern texEntryPattern = Pattern.compile(texEntryRegex);
        StringBuilder entiretyOfAFile = new StringBuilder(Files.readString(Path.of(files,  bibliographyTex)));
        Matcher texMatcher = texEntryPattern.matcher(entiretyOfAFile);
        Map<String, String> texEntries = new HashMap<>();
        while (texMatcher.find()) {
            String headerContent = texMatcher.group(header);
            if (!texEntries.containsKey(headerContent)) {
                texEntries.put(headerContent, texMatcher.group());
            } else {
                System.out.println("duplicate key in tex " + headerContent);
            }
        }
        return texEntries;
    }

    private static boolean isCyrillic(String word) {
        byte wordByte = word.getBytes()[0];
        return !(('a' <= wordByte && wordByte <= 'z') || ('A' <= wordByte && wordByte <= 'Z') || ('0' <= wordByte && wordByte <= '9'));
    }

    private static int compareCyrillicFirst(String first, String second) {
        boolean isCyrillic1 = isCyrillic(first);
        boolean isCyrillic2 = isCyrillic(second);
        if (isCyrillic1 && !isCyrillic2) {
            return -1;
        } else if (!isCyrillic1 && isCyrillic2) {
            return 1;
        } else {
            return first.compareToIgnoreCase(second);
        }
    }
}